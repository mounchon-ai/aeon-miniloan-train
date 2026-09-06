package com.miniloan.service;

import com.miniloan.domain.EarlySettlementCalculator.Payoff;
import com.miniloan.domain.IdempotencyKey;
import com.miniloan.domain.Installment;
import com.miniloan.domain.LoanAccount;
import com.miniloan.domain.Money;
import com.miniloan.domain.Payment;
import com.miniloan.domain.RepaymentSchedule;
import com.miniloan.repository.IdempotencyKeyRepository;
import com.miniloan.repository.InstallmentRepository;
import com.miniloan.repository.LoanAccountRepository;
import com.miniloan.repository.PaymentRepository;
import com.miniloan.repository.RepaymentScheduleRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * บันทึกการชำระยอดปิดบัญชีก่อนกำหนด (UC-miniloan-016 · API-026 · ACL-014 · AC-miniloan-005).
 *
 * <p><b>This is BR-miniloan-021@v1's second door.</b> The first — every instalment settled — is
 * {@code PaymentService}'s and closes with {@link LoanAccount.CloseReason#FullyPaid}. This one closes
 * with {@link LoanAccount.CloseReason#EarlySettlement}, retires the instalments still Due exactly as
 * BR-miniloan-023@v1 says to, and is the reason those two enum values were written apart.
 *
 * <p><b>The amount is checked against the figure this system computed, never against one the caller
 * supplied.</b> API-026 exists because API-014 could not serve this: API-014 charges 1% of the
 * EXCESS over an instalment (BR-miniloan-046@v2) while a payoff charges 1% of the REMAINING PRINCIPAL
 * (BR-miniloan-022@v1) — different bases, so one route cannot honour both without breaking its own
 * contract. The payoff is recomputed here through {@link EarlyClosureQuoteService#payoffOf}, the same
 * calculator the quote route uses, so the number the borrower was shown and the number Operations may
 * record are the same number by construction rather than by agreement.
 *
 * <p><b>Short and over are refused identically, and the whole record is refused</b> — nothing is
 * written, no instalment moves, the account stays Active. A payoff that accepted "close enough" would
 * leave a debt of a few satang behind a Closed account, and one that accepted more would take money
 * for nothing. This is the shape BR-miniloan-019@v1 already chose for instalments, applied to the one
 * amount that has to match exactly.
 *
 * <p><b>The closing date is the caller's, and it is what the interest was computed over</b>
 * (CALC-miniloan-004@v1). A payoff quoted for the 25th and recorded on the 26th is a different figure,
 * and it is refused as a mismatch rather than silently re-quoted — the alternative is a system that
 * charges an amount nobody was shown.
 *
 * <p><b>Scope is per account, not per role</b> (BR-miniloan-054@v1 · ACL-014's {@code scope: own}).
 * The Operations person the account was assigned to at disbursement, and nobody else holding the same
 * role.
 */
@Service
public class EarlyClosureSettlementService {

    /** FE-miniloan-002 mock scheme: one identity per role, so the Operations role is the person. */
    private static final String OPERATIONS = "ROLE-004";

    private final LoanAccountRepository accounts;
    private final RepaymentScheduleRepository schedules;
    private final InstallmentRepository installments;
    private final PaymentRepository payments;
    private final IdempotencyKeyRepository idempotencyKeys;
    private final EarlyClosureQuoteService quotes;
    private final Clock clock;

    public EarlyClosureSettlementService(
            LoanAccountRepository accounts,
            RepaymentScheduleRepository schedules,
            InstallmentRepository installments,
            PaymentRepository payments,
            IdempotencyKeyRepository idempotencyKeys,
            EarlyClosureQuoteService quotes,
            Clock clock) {
        this.accounts = accounts;
        this.schedules = schedules;
        this.installments = installments;
        this.payments = payments;
        this.idempotencyKeys = idempotencyKeys;
        this.quotes = quotes;
        this.clock = clock;
    }

    /** ACL-014 names ROLE-004 and rbac.json is default-deny — owning the account does not help. */
    public static class OperationsOnlyException extends RuntimeException {
        public OperationsOnlyException() {
            super("ไม่มีสิทธิ์บันทึกการชำระยอดปิดบัญชีก่อนกำหนด — ทำได้เฉพาะเจ้าหน้าที่ Operations");
        }
    }

    /** AC-miniloan-136's sentence — BR-miniloan-054@v1, among Operations holding the same role. */
    public static class NotAssignedOperationsException extends RuntimeException {
        public NotAssignedOperationsException() {
            super("ไม่มีสิทธิ์ดำเนินการกับบัญชีสินเชื่อนี้ — ไม่ได้ถูก assign ดูแลบัญชีนี้");
        }
    }

    /** ACL-014's condition: {@code STM-miniloan-002} state Active. */
    public static class AccountClosedException extends RuntimeException {
        public AccountClosedException() {
            super("บัญชีนี้ปิดแล้ว — บันทึกการชำระเพิ่มไม่ได้");
        }
    }

    public static class LoanAccountNotFoundException extends RuntimeException {
        public LoanAccountNotFoundException(UUID id) {
            super("ไม่พบบัญชีสินเชื่อ " + id);
        }
    }

    /**
     * The one amount that has to match exactly. The sentence carries both figures because a caller
     * who paid the wrong amount needs to see the right one, and the date it was computed for.
     */
    public static class PayoffAmountMismatchException extends RuntimeException {
        public PayoffAmountMismatchException(BigDecimal paid, Payoff payoff, LocalDate closingDate) {
            super(
                    "ยอดที่บันทึก "
                            + Money.exact(paid)
                            + " บาท ไม่ตรงกับยอดปิดบัญชีก่อนกำหนด ณ วันที่ "
                            + closingDate
                            + " ซึ่งเท่ากับ "
                            + Money.exact(payoff.earlySettlementAmount())
                            + " บาท — ระบบไม่รับชำระบางส่วนและไม่รับเกิน");
        }
    }

    /** BR-miniloan-043@v1 — the database refuses the repeat, and the caller re-issues it by hand. */
    public static class DuplicateCommandException extends RuntimeException {
        public DuplicateCommandException(String requestId) {
            super("คำสั่งปิดบัญชีก่อนกำหนด " + requestId + " ถูกประมวลผลไปแล้ว — คำสั่งซ้ำถูกปฏิเสธ");
        }
    }

    /** What one accepted payoff did — the figure it was measured against, and what it retired. */
    public record SettlementResult(
            Payment payment, LoanAccount account, Payoff payoff, List<Installment> cancelled) {}

    /**
     * @param requestId BR-miniloan-043@v1's dedup key. As with API-014, the interface declares no
     *     header and ENT-012 answers DQ-miniloan-009 with the SHAPE of the key only, so this unit
     *     decides it and says so: a client may send its own through {@code Idempotency-Key}, and the
     *     fallback is {@code <accountId>:payoff}, which needs no contract nobody wrote because an
     *     account can be settled early exactly once.
     */
    @Transactional
    public SettlementResult settle(
            UUID loanAccountId,
            String callerRole,
            LocalDate closingDate,
            BigDecimal amount,
            String requestId) {

        // ACL-014 is about WHO, and it does not depend on the account existing.
        if (!OPERATIONS.equals(callerRole)) {
            throw new OperationsOnlyException();
        }

        LoanAccount account =
                accounts
                        .findById(loanAccountId)
                        .orElseThrow(() -> new LoanAccountNotFoundException(loanAccountId));
        // Assignment before state: an unassigned account must not change in any way, and telling a
        // stranger whether it is open or closed is already telling them something.
        if (!account.getAssignedOperationsId().equals(callerRole)) {
            throw new NotAssignedOperationsException();
        }
        if (account.getStatus() != LoanAccount.Status.Active) {
            throw new AccountClosedException();
        }

        // The payoff is recomputed, and the caller's number is measured against it. A mismatch is
        // refused before the key is claimed, so a corrected amount can be sent straight afterwards
        // rather than coming back as a duplicate command.
        Payoff payoff = quotes.payoffOf(account, closingDate);
        BigDecimal paid = Money.round(amount);
        if (paid.compareTo(payoff.earlySettlementAmount()) != 0) {
            throw new PayoffAmountMismatchException(paid, payoff, closingDate);
        }

        String key =
                requestId == null || requestId.isBlank() ? loanAccountId + ":payoff" : requestId;
        claim(key, loanAccountId);

        Instant now = Instant.now(clock);
        RepaymentSchedule current =
                schedules
                        .findByLoanAccountIdAndCurrentIsTrue(loanAccountId)
                        .orElseThrow(
                                () -> new EarlyClosureQuoteService.NoCurrentScheduleException(loanAccountId));

        // BR-miniloan-023@v1 — งวดที่เหลือถูกยกเลิก, and AC-miniloan-005 measures that they read
        // "ยกเลิก (ปิดบัญชีก่อนกำหนด)" afterwards rather than disappearing from the table.
        List<Installment> remaining =
                installments.findByRepaymentScheduleIdOrderByInstallmentNumberAsc(current.getId()).stream()
                        .filter(row -> row.getStatus() == Installment.Status.Due)
                        .toList();
        remaining.forEach(Installment::cancel);
        installments.saveAll(remaining);

        account.reducePrincipal(account.getOutstandingPrincipal());
        account.close(LoanAccount.CloseReason.EarlySettlement, now);
        accounts.save(account);

        Payment payment =
                payments.save(
                        new Payment(
                                loanAccountId,
                                null,
                                paid,
                                Payment.PaymentType.EarlySettlement,
                                null,
                                payoff.earlySettlementFee(),
                                callerRole,
                                now));

        return new SettlementResult(payment, account, payoff, remaining);
    }

    private void claim(String requestId, UUID loanAccountId) {
        try {
            idempotencyKeys.saveAndFlush(
                    new IdempotencyKey(
                            IdempotencyKey.CommandType.SettleEarly, requestId, loanAccountId.toString()));
        } catch (DataIntegrityViolationException duplicate) {
            throw new DuplicateCommandException(requestId);
        }
    }
}
