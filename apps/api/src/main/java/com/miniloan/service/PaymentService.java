package com.miniloan.service;

import com.miniloan.domain.IdempotencyKey;
import com.miniloan.domain.Installment;
import com.miniloan.domain.InterestRateVersion;
import com.miniloan.domain.LoanAccount;
import com.miniloan.domain.Money;
import com.miniloan.domain.Payment;
import com.miniloan.domain.RepaymentSchedule;
import com.miniloan.repository.IdempotencyKeyRepository;
import com.miniloan.repository.InstallmentRepository;
import com.miniloan.repository.InterestRateVersionRepository;
import com.miniloan.repository.LoanAccountRepository;
import com.miniloan.repository.PaymentRepository;
import com.miniloan.repository.RepaymentScheduleRepository;
import com.miniloan.service.AmortizationScheduleService.Schedule;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * บันทึกการชำระ · ปิดบัญชีอัตโนมัติ · โปะเงินต้น (UC-miniloan-012 · UC-miniloan-013 ·
 * UC-miniloan-014 · UC-miniloan-024 · API-014).
 *
 * <p><b>Three amounts, three answers, and the line is exact</b> (BR-miniloan-019@v1 ·
 * BR-miniloan-046@v2). Short of the instalment by a single satang is refused and changes nothing;
 * exactly the instalment settles it; over it is accepted and the excess pays down principal. There
 * is no tolerance band anywhere, and there is no partially-paid state to fall into — AC-miniloan-014
 * pays half twice and both halves are refused separately, which is only true because a refusal
 * writes no {@link Payment} at all.
 *
 * <p><b>The prepayment fee is subtracted, and that is a decision.</b> BR-miniloan-046@v2 states the
 * mechanism twice: "หักออกจากส่วนเกินก่อนนำไปตัดเงินต้น" and "เงินต้นลดลง = ส่วนเกิน × 99%". The two
 * agree at AC-miniloan-086's 20,000.00 (fee 200.00, cut 19,800.00) and at AC-miniloan-087's 0.01
 * (fee 0.00, cut 0.01), so neither criterion separates them — but they diverge elsewhere, and
 * BR-miniloan-050@v1, which BR-miniloan-046@v2 names as the rule that pins the exact mechanism,
 * <b>is not in req's contract at all</b> (nothing under {@code requirements[].rules[]} carries that
 * id, though AC-miniloan-086 cites it too). The subtraction reading is implemented because ENT-009
 * STORES {@code prepaymentFee}: BR-miniloan-035@v1 forbids keeping an unrounded copy, so the fee
 * exists as a rounded figure before any principal is cut, and the cut is what remains of the overage
 * after it — not a second, independent rounding of the same overage. Raised for design rather than
 * settled here; see the build report. AC-miniloan-087 also defers the number of decimal places to
 * DQ-miniloan-001, so the fee is rounded at 2 through {@link Money#round} like every other figure in
 * this codebase, and that answer moves when DQ-miniloan-001 does.
 *
 * <p><b>An overpayment reissues the table, always</b> (BR-miniloan-046@v2 → BR-miniloan-044@v1). The
 * new revision holds the instalment the borrower is already paying and ends sooner — see
 * {@link AmortizationScheduleService#buildAtFixedInstalment}. The revision bookkeeping is
 * {@link RepaymentScheduleReissueService#issueRevision}'s, so the race fence on
 * {@code uk_repayment_schedule_revision} stays in one place.
 *
 * <p><b>Closing is a consequence, never a command</b> (BR-miniloan-021@v1). This service closes an
 * account when the last Due instalment is settled, and that is the only door it owns.
 *
 * <p><b>An overpayment that clears the principal exactly closes the account too, and that is a
 * decision.</b> No acceptance criterion covers it: BR-miniloan-021@v1's first door is "ชำระครบทุก
 * งวด", and here the principal reaches zero with instalments still listed as Due. It cannot be left
 * alone — the alternative is reissuing a table over a principal of nothing, which
 * {@link AmortizationScheduleService#buildAtFixedInstalment} refuses outright — and the only
 * coherent answer is that a debt of 0.00 is a debt that has been paid: the remaining rows are
 * retired exactly as BR-miniloan-023@v1 retires them, and the reason recorded is
 * {@link LoanAccount.CloseReason#FullyPaid}, because the borrower did pay everything they owed and
 * the OTHER reason belongs to the BR-miniloan-022@v1 payoff path, which computes a fee and accrued
 * interest that nothing charged here. Raised for design in the build report rather than settled as
 * an obvious consequence.
 *
 * <p><b>The other door is not built here.</b> The early-settlement payment of UC-miniloan-016 — its guard has to
 * compare against the payoff figure CALC-miniloan-004@v1 defines, and that calculation, its golden
 * dataset GD-miniloan-005 and the service that owns them belong to FE-miniloan-014. Accepting a
 * caller-supplied "this is the payoff" would make the guard prove nothing, and computing it here
 * would put a signed calculation in a unit that traces none. AC-miniloan-005 is therefore left to
 * the follow-up; {@link LoanAccount.CloseReason#EarlySettlement} and {@link Installment#cancel} are
 * in place for it.
 *
 * <p><b>Scope is per account, not per role</b> (BR-miniloan-054@v1 · ACL-020). Every command here
 * checks the Operations person the account was assigned to at disbursement, so another Operations
 * holding the same role is refused — AC-miniloan-136 measures exactly that, through the API, with no
 * screen involved.
 */
@Service
public class PaymentService {

    /** FE-miniloan-002 mock scheme: one identity per role, so the Operations role is the person. */
    private static final String OPERATIONS = "ROLE-004";

    /** BR-miniloan-046@v2 · BR-miniloan-022@v1 share the base: 1% of the excess. */
    private static final BigDecimal PREPAYMENT_FEE_RATE = new BigDecimal("0.01");

    private final LoanAccountRepository accounts;
    private final RepaymentScheduleRepository schedules;
    private final InstallmentRepository installments;
    private final PaymentRepository payments;
    private final IdempotencyKeyRepository idempotencyKeys;
    private final InterestRateVersionRepository rateVersions;
    private final AmortizationScheduleService amortization;
    private final RepaymentScheduleReissueService reissueService;
    private final Clock clock;

    public PaymentService(
            LoanAccountRepository accounts,
            RepaymentScheduleRepository schedules,
            InstallmentRepository installments,
            PaymentRepository payments,
            IdempotencyKeyRepository idempotencyKeys,
            InterestRateVersionRepository rateVersions,
            AmortizationScheduleService amortization,
            RepaymentScheduleReissueService reissueService,
            Clock clock) {
        this.accounts = accounts;
        this.schedules = schedules;
        this.installments = installments;
        this.payments = payments;
        this.idempotencyKeys = idempotencyKeys;
        this.rateVersions = rateVersions;
        this.amortization = amortization;
        this.reissueService = reissueService;
        this.clock = clock;
    }

    public static class LoanAccountNotFoundException extends RuntimeException {
        public LoanAccountNotFoundException(UUID id) {
            super("ไม่พบบัญชีสินเชื่อ " + id);
        }
    }

    /** AC-miniloan-088, word for word — BR-miniloan-045@v1 keeps a closed account's table locked. */
    public static class AccountClosedException extends RuntimeException {
        public AccountClosedException() {
            super("บัญชีนี้ปิดแล้ว — บันทึกการชำระเพิ่มไม่ได้");
        }
    }

    /** AC-miniloan-136, word for word — BR-miniloan-054@v1 at the API, not on the screen. */
    public static class NotAssignedOperationsException extends RuntimeException {
        public NotAssignedOperationsException() {
            super("ไม่มีสิทธิ์ดำเนินการกับบัญชีสินเชื่อนี้ — ไม่ได้ถูก assign ดูแลบัญชีนี้");
        }
    }

    /** AC-miniloan-070, word for word — BR-miniloan-034@v1, and owning the account does not help. */
    public static class OperationsOnlyException extends RuntimeException {
        public OperationsOnlyException() {
            super("ไม่มีสิทธิ์บันทึกการชำระ — การบันทึกการชำระทำได้เฉพาะเจ้าหน้าที่ Operations");
        }
    }

    /**
     * BR-miniloan-034@v1 names TWO actions — "บันทึก Payment และปิดบัญชีสินเชื่อทำได้เฉพาะ
     * Operations" — and no acceptance criterion quotes a sentence for the closing half. Reusing
     * {@link OperationsOnlyException} would answer a close attempt with a sentence about recording
     * payments, which is a different thing being refused; the wording here is this unit's, built on
     * AC-miniloan-070's shape so the two read as one rule.
     */
    public static class CloseOperationsOnlyException extends RuntimeException {
        public CloseOperationsOnlyException() {
            super("ไม่มีสิทธิ์ปิดบัญชีสินเชื่อ — การปิดบัญชีทำได้เฉพาะเจ้าหน้าที่ Operations");
        }
    }

    public static class NoCurrentScheduleException extends RuntimeException {
        public NoCurrentScheduleException(UUID loanAccountId) {
            super("บัญชี " + loanAccountId + " ไม่มีตารางผ่อนฉบับที่ใช้อยู่");
        }
    }

    public static class InstallmentNotFoundException extends RuntimeException {
        public InstallmentNotFoundException(int installmentNumber) {
            super("ไม่พบงวดที่ " + installmentNumber + " ในตารางผ่อนฉบับที่ใช้อยู่");
        }
    }

    /** ACL-011 · ACL-012 both condition on STM-miniloan-003 {@code Due}. */
    public static class InstallmentNotDueException extends RuntimeException {
        public InstallmentNotDueException(Installment installment) {
            super(
                    "งวดที่ "
                            + installment.getInstallmentNumber()
                            + " ไม่ได้อยู่ในสถานะค้างชำระ — สถานะปัจจุบันคือ "
                            + installment.getStatus().name());
        }
    }

    /**
     * AC-miniloan-012 · AC-miniloan-013 · AC-miniloan-014 quote this sentence, and all three measure
     * that nothing moved: the instalment is still Due and the balance is unchanged. Short by 0.01 is
     * as refused as short by half.
     */
    public static class PartialPaymentException extends RuntimeException {
        public PartialPaymentException(int installmentNumber, BigDecimal emiAmount) {
            super(
                    "ยอดชำระไม่ครบงวด — งวดที่ "
                            + installmentNumber
                            + " ต้องชำระเต็มจำนวน "
                            + Money.exact(emiAmount)
                            + " บาท ระบบไม่รับชำระบางส่วน");
        }
    }

    /** BR-miniloan-043@v1 — the database refuses the repeat, and the caller re-issues by hand. */
    public static class DuplicateCommandException extends RuntimeException {
        public DuplicateCommandException(String requestId) {
            super("คำสั่งบันทึกการชำระ " + requestId + " ถูกประมวลผลไปแล้ว — คำสั่งซ้ำถูกปฏิเสธ");
        }
    }

    /** BR-miniloan-036@v1: the version the account is bound to must still be readable. */
    public static class BoundRateVersionMissingException extends RuntimeException {
        public BoundRateVersionMissingException(UUID versionId) {
            super("ออกตารางผ่อนฉบับใหม่ไม่ได้ — ไม่พบเวอร์ชันอัตราดอกเบี้ย " + versionId + " ที่บัญชีนี้ผูกไว้");
        }
    }

    // ── ปิดบัญชีตรง ๆ · AC-miniloan-006 · AC-miniloan-007 ──────────────────────

    /** AC-miniloan-006, word for word, with the count of instalments still owing. */
    public static class DirectCloseRefusedException extends RuntimeException {
        public DirectCloseRefusedException(int dueCount) {
            super(
                    "ปิดบัญชีไม่ได้ — บัญชีนี้ยังมีงวดค้าง "
                            + dueCount
                            + " งวด · ปิดบัญชีได้เมื่อชำระครบทุกงวด หรือชำระยอดปิดบัญชีก่อนกำหนดครบเท่านั้น");
        }
    }

    /** AC-miniloan-007, word for word. */
    public static class AlreadyClosedException extends RuntimeException {
        public AlreadyClosedException() {
            super("บัญชีนี้ปิดแล้ว — ปิดซ้ำไม่ได้");
        }
    }

    /**
     * What one accepted payment did.
     *
     * @param reissued the revision BR-miniloan-046@v2 forces after an overpayment, absent otherwise
     * @param closed whether this payment was the one that shut the account (BR-miniloan-021@v1)
     */
    public record PaymentResult(
            Payment payment,
            LoanAccount account,
            Installment installment,
            BigDecimal principalReduction,
            Optional<RepaymentSchedule> reissued,
            boolean closed) {}

    /**
     * @param requestId BR-miniloan-043@v1's dedup key. API-014 declares neither a body field nor a
     *     header for it, and ENT-012 answers DQ-miniloan-009 with the SHAPE of the key only — so, as
     *     with the mock-token scheme, this unit decides it and says so. A client may send its own
     *     through {@code Idempotency-Key}; when it does not, the key is
     *     {@code <accountId>:<installmentNumber>}, which needs no contract nobody has written because
     *     one instalment can be settled exactly once. A payment that legitimately repeats on the same
     *     account is a payment of a DIFFERENT instalment, and it carries a different key by
     *     construction.
     */
    @Transactional
    public PaymentResult record(
            UUID loanAccountId,
            String callerRole,
            int installmentNumber,
            BigDecimal amount,
            String requestId) {

        // BR-miniloan-034@v1 first: it is about WHO, and it does not depend on the account existing.
        if (!OPERATIONS.equals(callerRole)) {
            throw new OperationsOnlyException();
        }

        LoanAccount account = requireAssignedActiveAccount(loanAccountId, callerRole);
        RepaymentSchedule current =
                schedules
                        .findByLoanAccountIdAndCurrentIsTrue(loanAccountId)
                        .orElseThrow(() -> new NoCurrentScheduleException(loanAccountId));

        List<Installment> rows =
                installments.findByRepaymentScheduleIdOrderByInstallmentNumberAsc(current.getId());
        Installment installment =
                rows.stream()
                        .filter(row -> row.getInstallmentNumber() == installmentNumber)
                        .findFirst()
                        .orElseThrow(() -> new InstallmentNotFoundException(installmentNumber));
        if (installment.getStatus() != Installment.Status.Due) {
            throw new InstallmentNotDueException(installment);
        }

        BigDecimal paid = Money.round(amount);
        BigDecimal owed = installment.getEmiAmount();
        if (paid.compareTo(owed) < 0) {
            // BR-miniloan-019@v1 — before the key is claimed, so AC-miniloan-014's second half is
            // refused with this sentence rather than as a duplicate command.
            throw new PartialPaymentException(installmentNumber, owed);
        }

        String key = requestId == null || requestId.isBlank()
                ? loanAccountId + ":" + installmentNumber
                : requestId;
        claim(key, loanAccountId);

        Instant now = Instant.now(clock);
        installment.markPaid(now);
        installments.save(installment);
        account.reducePrincipal(installment.getPrincipalPortion());

        BigDecimal overage = paid.subtract(owed);
        BigDecimal fee = Money.round(overage.multiply(PREPAYMENT_FEE_RATE));
        BigDecimal principalReduction = installment.getPrincipalPortion();
        Payment.PaymentType type = Payment.PaymentType.InstallmentExact;
        Optional<RepaymentSchedule> reissued = Optional.empty();

        if (overage.signum() > 0) {
            type = Payment.PaymentType.InstallmentOverpayment;
            BigDecimal cut = overage.subtract(fee);
            account.reducePrincipal(cut);
            principalReduction = principalReduction.add(cut);
        }

        boolean closed = account.getOutstandingPrincipal().signum() == 0 || noDueLeft(current.getId());
        if (closed) {
            // BR-miniloan-021@v1 door 1. Whatever is still Due in the table is retired for the same
            // reason BR-miniloan-023@v1 retires it on early settlement: nothing is owed any more.
            cancelRemaining(current.getId());
            account.close(LoanAccount.CloseReason.FullyPaid, now);
        } else if (type == Payment.PaymentType.InstallmentOverpayment) {
            // BR-miniloan-046@v2 → BR-miniloan-044@v1: the old table no longer describes the debt.
            reissued = Optional.of(reissueAtSameInstalment(account, installment.getEmiAmount()).schedule());
        }
        accounts.save(account);

        Payment payment =
                payments.save(
                        new Payment(
                                loanAccountId,
                                installment.getId(),
                                paid,
                                type,
                                type == Payment.PaymentType.InstallmentOverpayment ? overage : null,
                                type == Payment.PaymentType.InstallmentOverpayment ? fee : null,
                                callerRole,
                                now));

        return new PaymentResult(payment, account, installment, principalReduction, reissued, closed);
    }

    /**
     * UC-miniloan-014's exception flow · AC-miniloan-006 · AC-miniloan-007 · AC-miniloan-136. There
     * is no "close the account" command in this system and this method never closes one — it exists
     * so that asking for it is REFUSED rather than answered by a 404, which is what an undeclared
     * route would give and what BR-miniloan-025@v1 rules out. See {@code PaymentController}.
     */
    @Transactional(readOnly = true)
    public void refuseDirectClose(UUID loanAccountId, String callerRole) {
        if (!OPERATIONS.equals(callerRole)) {
            throw new CloseOperationsOnlyException();
        }
        LoanAccount account =
                accounts
                        .findById(loanAccountId)
                        .orElseThrow(() -> new LoanAccountNotFoundException(loanAccountId));
        if (!account.getAssignedOperationsId().equals(callerRole)) {
            throw new NotAssignedOperationsException();
        }
        if (account.getStatus() == LoanAccount.Status.Closed) {
            throw new AlreadyClosedException();
        }
        throw new DirectCloseRefusedException(dueCountOf(loanAccountId));
    }

    private LoanAccount requireAssignedActiveAccount(UUID loanAccountId, String operationsId) {
        LoanAccount account =
                accounts
                        .findById(loanAccountId)
                        .orElseThrow(() -> new LoanAccountNotFoundException(loanAccountId));
        // Assignment before state: AC-miniloan-136 says an unassigned account must not change in any
        // way, and telling a stranger whether it is open or closed is already telling them something.
        if (!account.getAssignedOperationsId().equals(operationsId)) {
            throw new NotAssignedOperationsException();
        }
        if (account.getStatus() != LoanAccount.Status.Active) {
            throw new AccountClosedException();
        }
        return account;
    }

    private void claim(String requestId, UUID loanAccountId) {
        try {
            idempotencyKeys.saveAndFlush(
                    new IdempotencyKey(
                            IdempotencyKey.CommandType.RecordPayment, requestId, loanAccountId.toString()));
        } catch (DataIntegrityViolationException duplicate) {
            throw new DuplicateCommandException(requestId);
        }
    }

    private RepaymentScheduleReissueService.Reissue reissueAtSameInstalment(
            LoanAccount account, BigDecimal instalment) {
        InterestRateVersion rateVersion =
                rateVersions
                        .findById(account.getInterestRateVersionId())
                        .orElseThrow(
                                () -> new BoundRateVersionMissingException(account.getInterestRateVersionId()));
        Schedule computed =
                amortization.buildAtFixedInstalment(
                        account.getOutstandingPrincipal(), rateVersion.annualRateFraction(), instalment);
        return reissueService.issueRevision(account, computed);
    }

    private boolean noDueLeft(UUID scheduleId) {
        return installments.findByRepaymentScheduleIdOrderByInstallmentNumberAsc(scheduleId).stream()
                .noneMatch(row -> row.getStatus() == Installment.Status.Due);
    }

    private void cancelRemaining(UUID scheduleId) {
        List<Installment> remaining =
                installments.findByRepaymentScheduleIdOrderByInstallmentNumberAsc(scheduleId).stream()
                        .filter(row -> row.getStatus() == Installment.Status.Due)
                        .toList();
        remaining.forEach(Installment::cancel);
        installments.saveAll(remaining);
    }

    private int dueCountOf(UUID loanAccountId) {
        return schedules
                .findByLoanAccountIdAndCurrentIsTrue(loanAccountId)
                .map(
                        current ->
                                (int)
                                        installments
                                                .findByRepaymentScheduleIdOrderByInstallmentNumberAsc(current.getId())
                                                .stream()
                                                .filter(row -> row.getStatus() == Installment.Status.Due)
                                                .count())
                .orElse(0);
    }
}
