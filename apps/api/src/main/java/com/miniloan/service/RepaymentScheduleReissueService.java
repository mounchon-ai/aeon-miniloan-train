package com.miniloan.service;

import com.miniloan.domain.Installment;
import com.miniloan.domain.InterestRateVersion;
import com.miniloan.domain.LoanAccount;
import com.miniloan.domain.RepaymentSchedule;
import com.miniloan.repository.InstallmentRepository;
import com.miniloan.repository.InterestRateVersionRepository;
import com.miniloan.repository.LoanAccountRepository;
import com.miniloan.repository.RepaymentScheduleRepository;
import com.miniloan.service.AmortizationScheduleService.Schedule;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ออกตารางผ่อนฉบับใหม่ทับ (UC-miniloan-010 · API-011 · BR-miniloan-044@v1 · BR-miniloan-045@v1).
 *
 * <p><b>A schedule is replaced whole, or not at all.</b> BR-miniloan-044@v1 forbids editing a row of
 * the revision the borrower already holds; the only way to change anything is to issue the next
 * revision over the top of it, and the one it replaced stays readable for ever. That is why this is
 * an insert plus one flag flip, and why nothing here deletes an {@link Installment} or a
 * {@link RepaymentSchedule}.
 *
 * <p><b>Closed is permanent</b> (BR-miniloan-045@v1). AC-miniloan-107 refuses the reissue on a
 * Closed account, AC-miniloan-108 refuses it again when an adjustment has been approved, and
 * AC-miniloan-109 fixes the boundary at the instant the account closes, with no grace period. All
 * three are the SAME guard, and it is unconditional on purpose: this method takes no approval, no
 * override and no reason, so there is no argument a caller could pass that would get past it.
 * AC-miniloan-108's own wording ("ถึงมีผู้อนุมัติก็ออกฉบับใหม่ทับไม่ได้") belongs to the unit that
 * builds the adjustment path (FE-miniloan-015/016) — the refusal it asserts is here; the sentence
 * that names an approver cannot be, because nothing in this codebase can yet produce one.
 *
 * <p><b>What the new revision is computed from, and why nothing is invented.</b> API-011 declares no
 * request body and no screen carries this action, so the inputs are the account's own facts:
 *
 * <ul>
 *   <li>principal = {@link LoanAccount#getOutstandingPrincipal()} — what is actually still owed, so
 *       BR-miniloan-017@v1's "sum of principal = the principal" stays true of the new table;
 *   <li>term = the instalments still {@code Due} in the current revision, NOT the account's original
 *       {@code termMonths} — reissuing must not silently lend back the months already paid off;
 *   <li>rate = the version the account is BOUND to (BR-miniloan-037@v1), never the one in force
 *       today — an account keeps its own rate after two more versions are published.
 * </ul>
 *
 * <p>Today every instalment is {@code Due} and outstanding always equals the principal, because
 * FE-miniloan-013 is what records a payment. Reading both anyway is what makes this correct on the
 * day that unit lands, rather than on the day somebody notices.
 *
 * <p><b>Due dates</b> follow the rule disbursement used: instalment t falls due t months after the
 * revision was issued (BR-miniloan-004@v1 counts the term in months, and no rule says more).
 */
@Service
public class RepaymentScheduleReissueService {

    private final LoanAccountRepository accounts;
    private final RepaymentScheduleRepository schedules;
    private final InstallmentRepository installments;
    private final InterestRateVersionRepository rateVersions;
    private final AmortizationScheduleService amortization;
    private final Clock clock;

    public RepaymentScheduleReissueService(
            LoanAccountRepository accounts,
            RepaymentScheduleRepository schedules,
            InstallmentRepository installments,
            InterestRateVersionRepository rateVersions,
            AmortizationScheduleService amortization,
            Clock clock) {
        this.accounts = accounts;
        this.schedules = schedules;
        this.installments = installments;
        this.rateVersions = rateVersions;
        this.amortization = amortization;
        this.clock = clock;
    }

    public static class LoanAccountNotFoundException extends RuntimeException {
        public LoanAccountNotFoundException(UUID id) {
            super("ไม่พบบัญชีสินเชื่อ " + id);
        }
    }

    /** AC-miniloan-107 · AC-miniloan-108 · AC-miniloan-109 — BR-miniloan-045@v1, with no exception. */
    public static class AccountClosedException extends RuntimeException {
        public AccountClosedException() {
            super("บัญชีนี้ปิดแล้ว — ออกตารางผ่อนฉบับใหม่ทับไม่ได้");
        }
    }

    /** ACL-009 is scope {@code own}: the Operations person this account was assigned to. */
    public static class NotAssignedOperationsException extends RuntimeException {
        public NotAssignedOperationsException() {
            super("ออกตารางผ่อนฉบับใหม่ไม่ได้ — บัญชีนี้อยู่ในความดูแลของเจ้าหน้าที่คนอื่น");
        }
    }

    /**
     * An Active account always has a current revision, because disbursement issues revision 1 in the
     * same transaction that opens it (BR-miniloan-015@v1). Refusing beats reissuing over nothing.
     */
    public static class NoCurrentScheduleException extends RuntimeException {
        public NoCurrentScheduleException(UUID loanAccountId) {
            super("บัญชี " + loanAccountId + " ไม่มีตารางผ่อนฉบับที่ใช้อยู่");
        }
    }

    /**
     * Every instalment already paid while the account is still Active — not reachable until
     * FE-miniloan-013 both records payments and closes the account on the last one, and refused
     * rather than handed to the calculator as a zero-instalment table.
     */
    public static class NothingLeftToRescheduleException extends RuntimeException {
        public NothingLeftToRescheduleException() {
            super("ออกตารางผ่อนฉบับใหม่ไม่ได้ — ไม่มีงวดที่ยังไม่ชำระเหลืออยู่");
        }
    }

    /**
     * Two reissues racing on the same account both read revision n and both compute n + 1;
     * {@code uk_repayment_schedule_revision} lets exactly one of them commit. CLAUDE.md puts that
     * fence on the database rather than on client-side dedup, and BR-miniloan-042@v1 has the loser
     * re-issue by hand — so it is turned down with a name, the way
     * {@code DisbursementService.DuplicateCommandException} is, and never as a 500.
     */
    public static class ConcurrentReissueException extends RuntimeException {
        public ConcurrentReissueException(UUID loanAccountId) {
            super("บัญชี " + loanAccountId + " กำลังถูกออกตารางผ่อนฉบับใหม่อยู่ — กรุณาสั่งใหม่อีกครั้ง");
        }
    }

    /** BR-miniloan-036@v1: the version the account is bound to must still be readable. */
    public static class BoundRateVersionMissingException extends RuntimeException {
        public BoundRateVersionMissingException(UUID versionId) {
            super("ออกตารางผ่อนฉบับใหม่ไม่ได้ — ไม่พบเวอร์ชันอัตราดอกเบี้ย " + versionId + " ที่บัญชีนี้ผูกไว้");
        }
    }

    /**
     * One revision as a reader sees it. {@code supersededAt} is derived, never stored — see
     * {@link RepaymentSchedule}'s note — and is null for the revision in use.
     */
    public record Revision(RepaymentSchedule schedule, Instant supersededAt) {}

    /**
     * @param revisions every revision this account has ever held, oldest first, so
     *     AC-miniloan-008's "ฉบับที่ 1 — ถูกแทนที่เมื่อ …" and AC-miniloan-011's "ไม่มีฉบับใดหายไป"
     *     are answerable from this one response. The ROWS of a superseded revision are
     *     FE-miniloan-012's to serve (API-012); what is proven here is that they were kept, not how
     *     they are browsed.
     */
    public record Reissue(
            LoanAccount account,
            RepaymentSchedule schedule,
            List<Installment> installments,
            List<Revision> revisions) {}

    @Transactional
    public Reissue reissue(UUID loanAccountId, String operationsId) {
        LoanAccount account =
                accounts
                        .findById(loanAccountId)
                        .orElseThrow(() -> new LoanAccountNotFoundException(loanAccountId));

        // BR-miniloan-045@v1 before ACL-009's scope, the order disbursement already uses: the caller
        // is told which of the two things is wrong, and a locked account is locked for everyone.
        if (account.getStatus() != LoanAccount.Status.Active) {
            throw new AccountClosedException();
        }
        if (!account.getAssignedOperationsId().equals(operationsId)) {
            throw new NotAssignedOperationsException();
        }

        RepaymentSchedule previous =
                schedules
                        .findByLoanAccountIdAndCurrentIsTrue(loanAccountId)
                        .orElseThrow(() -> new NoCurrentScheduleException(loanAccountId));

        int remainingTerm =
                (int)
                        installments
                                .findByRepaymentScheduleIdOrderByInstallmentNumberAsc(previous.getId())
                                .stream()
                                .filter(row -> row.getStatus() == Installment.Status.Due)
                                .count();
        if (remainingTerm == 0) {
            throw new NothingLeftToRescheduleException();
        }

        InterestRateVersion rateVersion =
                rateVersions
                        .findById(account.getInterestRateVersionId())
                        .orElseThrow(
                                () ->
                                        new BoundRateVersionMissingException(
                                                account.getInterestRateVersionId()));

        BigDecimal principal = account.getOutstandingPrincipal();
        Schedule computed = amortization.build(principal, rateVersion.annualRateFraction(), remainingTerm);

        // Flushed before the insert: both rows are RepaymentSchedule, and Hibernate orders its
        // statements by entity type rather than by call order. Without this the INSERT can reach the
        // database while the old row still reads current = true.
        previous.supersede();
        schedules.saveAndFlush(previous);

        RepaymentSchedule next;
        try {
            next =
                    schedules.saveAndFlush(
                            new RepaymentSchedule(
                                    loanAccountId, previous.getRevisionNumber() + 1, computed.totalPrincipal()));
        } catch (DataIntegrityViolationException racing) {
            throw new ConcurrentReissueException(loanAccountId);
        }

        LocalDate issuedOn = LocalDate.now(clock);
        List<Installment> rows = new ArrayList<>(computed.rows().size());
        for (AmortizationScheduleService.Row row : computed.rows()) {
            rows.add(
                    new Installment(
                            next.getId(),
                            row.number(),
                            issuedOn.plusMonths(row.number()),
                            row.instalment(),
                            row.interest(),
                            row.principal(),
                            row.remainingBalance()));
        }
        List<Installment> saved = installments.saveAll(rows);

        return new Reissue(account, next, List.copyOf(saved), revisionsOf(loanAccountId));
    }

    /** AC-miniloan-011: every revision, oldest first, each dated by the revision that replaced it. */
    @Transactional(readOnly = true)
    public List<Revision> revisionsOf(UUID loanAccountId) {
        List<RepaymentSchedule> ordered = schedules.findByLoanAccountIdOrderByRevisionNumberAsc(loanAccountId);
        List<Revision> revisions = new ArrayList<>(ordered.size());
        for (int i = 0; i < ordered.size(); i++) {
            Instant supersededAt = i + 1 < ordered.size() ? ordered.get(i + 1).getIssuedAt() : null;
            revisions.add(new Revision(ordered.get(i), supersededAt));
        }
        return List.copyOf(revisions);
    }
}
