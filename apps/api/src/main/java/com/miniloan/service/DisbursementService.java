package com.miniloan.service;

import com.miniloan.domain.IdempotencyKey;
import com.miniloan.domain.Installment;
import com.miniloan.domain.InterestRateVersion;
import com.miniloan.domain.LoanAccount;
import com.miniloan.domain.LoanApplication;
import com.miniloan.domain.RepaymentSchedule;
import com.miniloan.repository.IdempotencyKeyRepository;
import com.miniloan.repository.InstallmentRepository;
import com.miniloan.repository.InterestRateVersionRepository;
import com.miniloan.repository.LoanAccountRepository;
import com.miniloan.repository.LoanApplicationRepository;
import com.miniloan.repository.RepaymentScheduleRepository;
import com.miniloan.service.AmortizationScheduleService.Schedule;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * เบิกจ่ายเงินกู้ (UC-miniloan-009 · API-010 · BR-miniloan-014@v1 · BR-miniloan-015@v1).
 *
 * <p><b>One transaction, or none of it.</b> AC-miniloan-093 is explicit that the schedule is built
 * in the same beat as the account — "ไม่ใช่งานที่รอทำทีหลัง" — and AC-miniloan-094 measures that as
 * the very next request seeing every row, with no "building your schedule" state to wait through.
 * {@code @Transactional} over the whole method is what makes both true: an account without its
 * table never becomes visible, because it never commits.
 *
 * <p>The order of the refusals:
 *
 * <ol>
 *   <li>already disbursed — AC-miniloan-060, quoting the account time, before the state guard so the
 *       officer is told which of the two things happened;
 *   <li>state — Approved and nothing else (AC-miniloan-059);
 *   <li>assignment — ACL-008 is scope {@code own}, the same guard approve and reject call;
 *   <li>the rate version in force on today (BR-miniloan-036@v1), refused if none is;
 *   <li>BR-miniloan-043@v1's key, claimed last, as every other command claims it.
 * </ol>
 *
 * <p><b>AC-miniloan-132 has two fences, not one.</b> The unique constraint on
 * {@code LoanAccount.applicationId} and the idempotency key both refuse a second disbursement, and
 * the state guard refuses a third time over. That is BR-miniloan-042@v1's point: there is no retry
 * queue anywhere, the caller re-issues by hand, and the second call has to be turned down by the
 * database rather than by nobody having pressed the button twice.
 *
 * <p><b>Due dates.</b> No rule states how they are generated; BR-miniloan-004@v1 says the term is
 * counted in months, so instalment t falls due t months after disbursement.
 * {@link LocalDate#plusMonths} clamps to the last valid day, so disbursing on the 31st gives the
 * 28th/30th where that month is shorter — recorded by a test rather than left incidental.
 */
@Service
public class DisbursementService {

    /** FE-miniloan-002 mock scheme: one identity per role, so the Operations role is the person. */
    private static final String OPERATIONS = "ROLE-004";

    private final LoanApplicationRepository applications;
    private final LoanAccountRepository accounts;
    private final InterestRateVersionRepository rateVersions;
    private final RepaymentScheduleRepository schedules;
    private final InstallmentRepository installments;
    private final IdempotencyKeyRepository idempotencyKeys;
    private final AmortizationScheduleService amortization;
    private final Clock clock;

    public DisbursementService(
            LoanApplicationRepository applications,
            LoanAccountRepository accounts,
            InterestRateVersionRepository rateVersions,
            RepaymentScheduleRepository schedules,
            InstallmentRepository installments,
            IdempotencyKeyRepository idempotencyKeys,
            AmortizationScheduleService amortization,
            Clock clock) {
        this.applications = applications;
        this.accounts = accounts;
        this.rateVersions = rateVersions;
        this.schedules = schedules;
        this.installments = installments;
        this.idempotencyKeys = idempotencyKeys;
        this.amortization = amortization;
        this.clock = clock;
    }

    public static class ApplicationNotFoundException extends RuntimeException {
        public ApplicationNotFoundException(UUID id) {
            super("ไม่พบใบสมัคร " + id);
        }
    }

    /** AC-miniloan-060 — and the count of accounts for this application stays at one. */
    public static class AlreadyDisbursedException extends RuntimeException {
        public AlreadyDisbursedException(LoanAccount account) {
            super("เบิกจ่ายไม่ได้ — ใบสมัครนี้เบิกจ่ายไปแล้วเมื่อ " + account.getDisbursedAt());
        }
    }

    /**
     * BR-miniloan-036@v1 makes the rate versioned master data. Nothing in this build plan publishes
     * a version (no API, no screen, no unit owns ENT-005 administration), so the one this project
     * runs on is bootstrapped — and if even that is missing, refusing beats inventing a rate.
     */
    public static class NoEffectiveRateException extends RuntimeException {
        public NoEffectiveRateException(LocalDate on) {
            super("เบิกจ่ายไม่ได้ — ไม่มีเวอร์ชันอัตราดอกเบี้ยที่มีผล ณ วันที่ " + on);
        }
    }

    public static class DuplicateCommandException extends RuntimeException {
        public DuplicateCommandException(UUID id) {
            super("ใบสมัคร " + id + " ถูกเบิกจ่ายไปแล้ว — คำสั่งเบิกจ่ายซ้ำถูกปฏิเสธ");
        }
    }

    /** What the caller gets back: the account that was opened and the table that came with it. */
    public record Disbursement(
            LoanAccount account,
            InterestRateVersion rateVersion,
            RepaymentSchedule schedule,
            List<Installment> installments) {}

    @Transactional
    public Disbursement disburse(UUID applicationId, String loanOfficerId) {
        LoanApplication application =
                applications
                        .findById(applicationId)
                        .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        accounts
                .findByApplicationId(applicationId)
                .ifPresent(
                        existing -> {
                            throw new AlreadyDisbursedException(existing);
                        });

        if (application.getStatus() != LoanApplication.Status.Approved) {
            throw new LoanApplication.NotDisbursableException(application);
        }
        application.requireAssignedTo(loanOfficerId);

        LocalDate today = LocalDate.now(clock);
        InterestRateVersion rateVersion =
                rateVersions
                        .findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(today)
                        .orElseThrow(() -> new NoEffectiveRateException(today));

        application.disburse();

        LoanAccount account =
                accounts.saveAndFlush(
                        new LoanAccount(
                                applicationId,
                                rateVersion.getId(),
                                application.getApprovedAmount(),
                                application.getRequestedTermMonths(),
                                OPERATIONS));

        Schedule computed =
                amortization.build(
                        account.getPrincipalAmount(),
                        rateVersion.annualRateFraction(),
                        account.getTermMonths());

        RepaymentSchedule schedule =
                schedules.save(new RepaymentSchedule(account.getId(), 1, computed.totalPrincipal()));

        List<Installment> rows = new ArrayList<>(computed.rows().size());
        for (AmortizationScheduleService.Row row : computed.rows()) {
            rows.add(
                    new Installment(
                            schedule.getId(),
                            row.number(),
                            today.plusMonths(row.number()),
                            row.instalment(),
                            row.interest(),
                            row.principal(),
                            row.remainingBalance()));
        }
        List<Installment> saved = installments.saveAll(rows);

        claimDisburseCommand(applicationId);
        applications.save(application);

        return new Disbursement(account, rateVersion, schedule, List.copyOf(saved));
    }

    /** AC-miniloan-095: before disbursement there is no account and therefore no schedule at all. */
    @Transactional(readOnly = true)
    public boolean hasSchedule(UUID applicationId) {
        return accounts
                .findByApplicationId(applicationId)
                .flatMap(account -> schedules.findByLoanAccountIdAndCurrentIsTrue(account.getId()))
                .isPresent();
    }

    private void claimDisburseCommand(UUID applicationId) {
        try {
            idempotencyKeys.saveAndFlush(
                    new IdempotencyKey(
                            IdempotencyKey.CommandType.DisburseLoan,
                            applicationId.toString(),
                            applicationId.toString()));
        } catch (DataIntegrityViolationException duplicate) {
            throw new DuplicateCommandException(applicationId);
        }
    }

}
