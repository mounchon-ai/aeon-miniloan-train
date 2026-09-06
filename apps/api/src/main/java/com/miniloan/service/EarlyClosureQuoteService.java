package com.miniloan.service;

import com.miniloan.domain.EarlySettlementCalculator;
import com.miniloan.domain.EarlySettlementCalculator.Payoff;
import com.miniloan.domain.Installment;
import com.miniloan.domain.InterestRateVersion;
import com.miniloan.domain.LoanAccount;
import com.miniloan.domain.LoanApplication;
import com.miniloan.domain.RepaymentSchedule;
import com.miniloan.repository.InstallmentRepository;
import com.miniloan.repository.InterestRateVersionRepository;
import com.miniloan.repository.LoanAccountRepository;
import com.miniloan.repository.LoanApplicationRepository;
import com.miniloan.repository.RepaymentScheduleRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ขอยอดปิดบัญชีก่อนกำหนด (UC-miniloan-015 · API-015 · ACL-013 · BR-miniloan-022@v1).
 *
 * <p><b>Showing a number changes nothing</b> (AC-miniloan-071): this service writes no row, moves no
 * status and creates no payment. The account is still Active when the caller closes the page, and
 * the figure is recomputed the next time somebody asks — a quote nobody stored cannot go stale.
 *
 * <p><b>ROLE-001, and only ROLE-001.</b> ACL-013 is the single permission entry for
 * UC-miniloan-015 — {@code role: ROLE-001 · scope: own · state: [Active] · enforceAt: [api, domain]}
 * — and {@code rbac.json} is default-deny, so no other role reaches this. AC-miniloan-073/074/075
 * each write "Operations เปิดหน้า …" in their {@code when}, which disagrees with the matrix; those
 * three criteria are about the NUMBERS (AC-miniloan-073 says so itself), the matrix is about WHO, and
 * a second role typed into this route because a sentence implied it would be dev deciding a
 * permission. Recorded in the build report; Operations does not need this route to settle an account,
 * because {@code EarlyClosureSettlementService} computes the same figure through the same calculator.
 *
 * <p><b>Where {@code remainingPrincipal} comes from — the schedule, not the account.</b>
 * CALC-miniloan-004@v1's input list pins it: "อ่านจากคอลัมน์ยอดคงเหลือของตารางผ่อนฉบับล่าสุดตาม
 * BR-miniloan-053@v1 เท่านั้น ไม่คำนวณขึ้นใหม่จากสูตรอื่น". So it is the {@code remainingBalance} of
 * the highest-numbered SETTLED instalment of the revision in force, and when nothing has been settled
 * on that revision it is the principal the revision was issued over
 * ({@link RepaymentSchedule#getTotalPrincipal()}) — which is the same column read one row earlier, not
 * a different formula. {@link LoanAccount#getOutstandingPrincipal()} is deliberately NOT used: it is a
 * running total {@code PaymentService} maintains, and the contract asked for the table.
 *
 * <p><b>{@code lastPaidDueDate} falls back to the disbursement date</b> when no instalment has been
 * settled yet — CALC-miniloan-004@v1's fourth boundary, "ไม่ใช่ค่าว่างหรือ error". The account's
 * {@code disbursedAt} is an {@code Instant} and the contract wants a date, so the conversion goes
 * through the injected {@link Clock}'s zone rather than a hard-coded one; the clock is a bean in this
 * project for exactly this class of decision (see {@code ClockConfig}).
 *
 * <p><b>The rate is the account's, not today's</b> (BR-miniloan-036@v1 · BR-miniloan-037@v1). The
 * version bound at disbursement is read by id, and a missing one is refused rather than silently
 * replaced with the current master rate.
 */
@Service
public class EarlyClosureQuoteService {

    /** FE-miniloan-002 mock scheme: one identity per role, so the Applicant role is the person. */
    private static final String APPLICANT = "ROLE-001";

    private final LoanAccountRepository accounts;
    private final LoanApplicationRepository applications;
    private final RepaymentScheduleRepository schedules;
    private final InstallmentRepository installments;
    private final InterestRateVersionRepository rateVersions;
    private final Clock clock;

    public EarlyClosureQuoteService(
            LoanAccountRepository accounts,
            LoanApplicationRepository applications,
            RepaymentScheduleRepository schedules,
            InstallmentRepository installments,
            InterestRateVersionRepository rateVersions,
            Clock clock) {
        this.accounts = accounts;
        this.applications = applications;
        this.schedules = schedules;
        this.installments = installments;
        this.rateVersions = rateVersions;
        this.clock = clock;
    }

    /** ACL-013 is the only entry for UC-miniloan-015, and rbac.json is default-deny. */
    public static class ApplicantOnlyException extends RuntimeException {
        public ApplicantOnlyException() {
            super("ไม่มีสิทธิ์ขอยอดปิดบัญชีก่อนกำหนด — ทำได้เฉพาะเจ้าของบัญชี");
        }
    }

    /**
     * AC-miniloan-072's second half, word for word — BR-miniloan-025@v1. The Applicant may ask for
     * the figure and may not close the account, and the two sit next to each other on one screen.
     */
    public static class CloseNotPermittedException extends RuntimeException {
        public CloseNotPermittedException() {
            super("ไม่มีสิทธิ์ปิดบัญชีสินเชื่อ");
        }
    }

    /** BR-miniloan-033@v1 · ACL-013's {@code scope: own}, refused at the API and not on the screen. */
    public static class NotAccountOwnerException extends RuntimeException {
        public NotAccountOwnerException() {
            super("ไม่มีสิทธิ์เข้าถึงบัญชีสินเชื่อนี้");
        }
    }

    /** ACL-013's condition: {@code STM-miniloan-002} state Active. */
    public static class AccountNotActiveException extends RuntimeException {
        public AccountNotActiveException() {
            super("ขอยอดปิดบัญชีก่อนกำหนดไม่ได้ — บัญชีนี้ปิดแล้ว");
        }
    }

    public static class LoanAccountNotFoundException extends RuntimeException {
        public LoanAccountNotFoundException(UUID id) {
            super("ไม่พบบัญชีสินเชื่อ " + id);
        }
    }

    public static class NoCurrentScheduleException extends RuntimeException {
        public NoCurrentScheduleException(UUID loanAccountId) {
            super("บัญชี " + loanAccountId + " ไม่มีตารางผ่อนฉบับที่ใช้อยู่");
        }
    }

    /** BR-miniloan-036@v1: the version the account is bound to must still be readable. */
    public static class BoundRateVersionMissingException extends RuntimeException {
        public BoundRateVersionMissingException(UUID versionId) {
            super("คำนวณยอดปิดบัญชีก่อนกำหนดไม่ได้ — ไม่พบเวอร์ชันอัตราดอกเบี้ย " + versionId + " ที่บัญชีนี้ผูกไว้");
        }
    }

    /** ACL-013 for the Applicant's own account; the figure and nothing else. */
    @Transactional(readOnly = true)
    public Payoff quoteFor(UUID loanAccountId, String callerRole, LocalDate closingDate) {
        if (!APPLICANT.equals(callerRole)) {
            throw new ApplicantOnlyException();
        }
        LoanAccount account = requireOwnActiveAccount(loanAccountId, callerRole);
        return payoffOf(account, closingDate);
    }

    /**
     * The same figure for a caller this service has already authorised elsewhere — the settlement
     * route's guards are ACL-014's, not ACL-013's, and putting the arithmetic in one place is what
     * keeps the quote the borrower saw and the amount Operations records the same number.
     */
    @Transactional(readOnly = true)
    public Payoff payoffOf(LoanAccount account, LocalDate closingDate) {
        RepaymentSchedule current =
                schedules
                        .findByLoanAccountIdAndCurrentIsTrue(account.getId())
                        .orElseThrow(() -> new NoCurrentScheduleException(account.getId()));
        List<Installment> rows =
                installments.findByRepaymentScheduleIdOrderByInstallmentNumberAsc(current.getId());

        Installment lastSettled =
                rows.stream()
                        .filter(row -> row.getStatus() == Installment.Status.Paid)
                        .max(Comparator.comparingInt(Installment::getInstallmentNumber))
                        .orElse(null);

        BigDecimal remainingPrincipal =
                lastSettled == null ? current.getTotalPrincipal() : lastSettled.getRemainingBalance();
        LocalDate lastPaidDueDate =
                lastSettled == null
                        ? LocalDate.ofInstant(account.getDisbursedAt(), clock.getZone())
                        : lastSettled.getDueDate();

        InterestRateVersion rateVersion =
                rateVersions
                        .findById(account.getInterestRateVersionId())
                        .orElseThrow(
                                () -> new BoundRateVersionMissingException(account.getInterestRateVersionId()));

        return EarlySettlementCalculator.quote(
                remainingPrincipal, rateVersion.annualRateFraction(), lastPaidDueDate, closingDate);
    }

    /**
     * Not found, then owner, then Active — the order the rest of this codebase reads its account
     * guards in, and the owner check comes first on purpose so a stranger cannot tell an Active
     * account from a Closed one by the refusal they get back.
     */
    private LoanAccount requireOwnActiveAccount(UUID loanAccountId, String applicantId) {
        LoanAccount account =
                accounts
                        .findById(loanAccountId)
                        .orElseThrow(() -> new LoanAccountNotFoundException(loanAccountId));
        LoanApplication application =
                applications
                        .findById(account.getApplicationId())
                        .orElseThrow(NotAccountOwnerException::new);
        if (!application.getApplicantId().equals(applicantId)) {
            throw new NotAccountOwnerException();
        }
        if (account.getStatus() != LoanAccount.Status.Active) {
            throw new AccountNotActiveException();
        }
        return account;
    }
}
