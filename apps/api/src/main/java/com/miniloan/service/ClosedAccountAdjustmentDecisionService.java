package com.miniloan.service;

import com.miniloan.domain.ApproverRoleSetting.ApproverRole;
import com.miniloan.domain.ClosedAccountAdjustment;
import com.miniloan.domain.ClosedAccountAdjustment.AdjustableField;
import com.miniloan.domain.ClosedAccountAdjustment.Status;
import com.miniloan.domain.LoanAccount;
import com.miniloan.domain.LoanAccount.CloseReason;
import com.miniloan.domain.Payment;
import com.miniloan.repository.ClosedAccountAdjustmentRepository;
import com.miniloan.repository.LoanAccountRepository;
import com.miniloan.repository.PaymentRepository;
import com.miniloan.service.ClosedAccountAdjustmentService.AccountNotClosedException;
import com.miniloan.service.ClosedAccountAdjustmentService.ApproverRoleNotSetException;
import com.miniloan.service.ClosedAccountAdjustmentService.LoanAccountNotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * อนุมัติหรือปฏิเสธคำขอปรับปรุงบัญชีที่ปิดแล้ว (UC-miniloan-018 · API-017 · API-018 · API-024 ·
 * API-025 · ACL-016 · STM-miniloan-004).
 *
 * <p><b>Who the approver is, is a configured fact rather than a fixed role.</b> ACL-016 names
 * ROLE-005 — "ผู้อนุมัติการปรับปรุงบัญชีที่ปิดแล้ว" — and UC-miniloan-018's actor spells out what
 * that means: "ผู้ถือ role ผู้อนุมัติตาม BR-miniloan-039@v1". So ROLE-005 is a CAPABILITY, and the
 * holder of it is whoever holds the role ENT-011 currently stores. This service therefore maps the
 * stored {@link ApproverRole} onto the caller's role id and compares; ROLE-005 is not accepted as a
 * caller in its own right, because a caller who could approve regardless of the setting would make
 * BR-miniloan-039@v1 decorative and would also put the approver permanently outside the set of
 * people who can file a request — which is the very collision AC-miniloan-079 is written to measure.
 *
 * <p><b>The setting is read on every call, never cached</b> — see
 * {@link ApproverRoleSettingService#currentApproverRole()}, which is ungated for exactly this
 * caller: a rule reading its own configuration is not a user asking to see it.
 *
 * <p><b>Four-eyes is checked here AND in the aggregate</b> (BR-miniloan-049@v1 · BR-miniloan-025@v1
 * · ACL-016's {@code enforceAt: [api, domain]}). The sentence AC-miniloan-079 quotes belongs to this
 * layer because the criterion measures what the API says; {@link ClosedAccountAdjustment#approve}
 * keeps its own guard so that no future caller reaching the aggregate directly can slip past it.
 * The same is true of "พิจารณาซ้ำไม่ได้": ACL-016's condition is STM-miniloan-004 state Pending, and
 * both layers hold it.
 *
 * <p><b>The approved value is applied, and nothing downstream is recomputed.</b> Approving
 * {@code Payment.amount} writes the corrected amount onto the payment and stops there — the
 * account's {@code outstandingPrincipal} and {@code closeReason} are left as they were. That is not
 * an oversight: BR-miniloan-021@v1 owns both values, no rule in req's contract says a corrected
 * payment recomputes them, and STM-miniloan-002 has no state for an account that has become
 * outstanding again. GAP-miniloan-009 asks req for that decision. The repayment schedule is out of
 * every case already (BR-miniloan-045@v1 · AC-miniloan-108) and nothing here touches it.
 *
 * <p><b>What the text of {@code newValue} may look like is undeclared</b>, so each field states the
 * one form it accepts and refuses everything else with a message naming it, rather than silently
 * coercing. Money is read in the {@code #,##0.00} form {@link com.miniloan.domain.Money#exact}
 * renders, so a value copied off a screen round-trips; timestamps are ISO-8601 instants. Raised with
 * GAP-miniloan-010, which is about the same silence on the reading side.
 */
@Service
public class ClosedAccountAdjustmentDecisionService {

    private final ClosedAccountAdjustmentRepository adjustments;
    private final LoanAccountRepository accounts;
    private final PaymentRepository payments;
    private final ApproverRoleSettingService approvers;
    private final Clock clock;

    public ClosedAccountAdjustmentDecisionService(
            ClosedAccountAdjustmentRepository adjustments,
            LoanAccountRepository accounts,
            PaymentRepository payments,
            ApproverRoleSettingService approvers,
            Clock clock) {
        this.adjustments = adjustments;
        this.accounts = accounts;
        this.payments = payments;
        this.approvers = approvers;
        this.clock = clock;
    }

    // ── refusals ────────────────────────────────────────────────────────────

    public static class AdjustmentNotFoundException extends RuntimeException {
        public AdjustmentNotFoundException(UUID id) {
            super("ไม่พบคำขอปรับปรุงบัญชี " + id);
        }
    }

    /** ACL-016 · UC-miniloan-018's actor, resolved through BR-miniloan-039@v1's setting. */
    public static class NotTheApproverException extends RuntimeException {
        public NotTheApproverException(ApproverRole approverRole) {
            super(
                    "ไม่มีสิทธิ์พิจารณาคำขอปรับปรุงบัญชี — ทำได้เฉพาะผู้ถือ role ผู้อนุมัติที่ตั้งไว้ ("
                            + approverRole
                            + ")");
        }
    }

    /** AC-miniloan-079's sentence, word for word (BR-miniloan-049@v1 · BR-miniloan-025@v1). */
    public static class SelfApprovalRefusedException extends RuntimeException {
        public SelfApprovalRefusedException() {
            super("อนุมัติคำขอของตัวเองไม่ได้ — ผู้อนุมัติต้องเป็นคนละคนกับผู้ขอแก้");
        }
    }

    /** ACL-016's condition — STM-miniloan-004 leaves Pending once, and Pending is the only door. */
    public static class AdjustmentAlreadyDecidedException extends RuntimeException {
        public AdjustmentAlreadyDecidedException() {
            super("คำขอปรับปรุงนี้ถูกพิจารณาไปแล้ว — พิจารณาซ้ำไม่ได้");
        }
    }

    /** The stored {@code newValue} does not read as the field it is for — see the class comment. */
    public static class AdjustedValueUnreadableException extends RuntimeException {
        public AdjustedValueUnreadableException(AdjustableField field, String expected) {
            super(
                    "ค่าใหม่ของ "
                            + field.declaredName()
                            + " อ่านไม่ได้ — ต้องอยู่ในรูป "
                            + expected);
        }
    }

    public static class PaymentNotFoundException extends RuntimeException {
        public PaymentNotFoundException(String id) {
            super("ไม่พบรายการชำระ " + id);
        }
    }

    // ── results ─────────────────────────────────────────────────────────────

    /**
     * What one decision did. AC-miniloan-076 renders the approval time in its sentence, so the
     * message is built from the value that was actually written to the row, not from a second read
     * of the clock.
     */
    public record DecisionResult(ClosedAccountAdjustment adjustment) {

        /** AC-miniloan-076 for an approval; UC-miniloan-018's alternate flow for a rejection. */
        public String message() {
            return adjustment.getStatus() == Status.Approved
                    ? "อนุมัติคำขอปรับปรุงแล้ว — การแก้ไขมีผลเมื่อ " + adjustment.getApprovedAt()
                    : "ปฏิเสธคำขอปรับปรุงแล้ว — ค่าเดิมของบัญชียังคงอยู่";
        }
    }

    // ── the two decisions ───────────────────────────────────────────────────

    /**
     * API-017 · AC-miniloan-076's second step. The value reaches the account only here, which is
     * BR-miniloan-038@v1's "ค่าบนบัญชีเปลี่ยนเป็นค่าใหม่ก็ต่อเมื่อผ่านขั้นที่สองแล้วเท่านั้น" said as
     * code: {@link ClosedAccountAdjustmentService} has no path to these setters at all.
     */
    @Transactional
    public DecisionResult approve(UUID adjustmentId, String callerRole) {
        ClosedAccountAdjustment adjustment = decidable(adjustmentId, callerRole);

        // Applied BEFORE the row is marked Approved so that an unreadable value refuses the whole
        // decision rather than leaving a request marked approved with nothing changed by it.
        apply(adjustment);

        adjustment.approve(callerRole, Instant.now(clock));
        return new DecisionResult(adjustments.save(adjustment));
    }

    /** API-018 · UC-miniloan-018's alternate flow — the account is not read and not touched. */
    @Transactional
    public DecisionResult reject(UUID adjustmentId, String callerRole) {
        ClosedAccountAdjustment adjustment = decidable(adjustmentId, callerRole);
        adjustment.reject(callerRole, Instant.now(clock));
        return new DecisionResult(adjustments.save(adjustment));
    }

    // ── the two reads the approver's screens are fed from ───────────────────

    /**
     * API-024 · UI-miniloan-013 — the queue, oldest first, for the person who may act on it.
     *
     * <p><b>Pending and nothing else</b>, because ACL-016's condition is STM-miniloan-004 state
     * Pending and design declares exactly one query: {@code GET /adjustments?status=Pending}. A
     * listing of decided requests would be a system-wide history under a permission row scoped to
     * the queue, and where the history of an account's adjustments belongs is the question
     * GAP-miniloan-010 puts to design — so it is not answered here by widening a parameter.
     */
    @Transactional(readOnly = true)
    public List<ClosedAccountAdjustment> listPending(String callerRole) {
        requireApprover(callerRole);
        return adjustments.findByStatusOrderByRequestedAtAsc(Status.Pending);
    }

    /**
     * API-025 · UI-miniloan-014 — one request, to compare the old value against the new.
     *
     * <p>This one is deliberately NOT narrowed to Pending, and the exception is worth seeing: it is
     * the route UI-miniloan-014 opens a request on, and after a decision it is the only place
     * {@code approvedBy} and {@code approvedAt} can be read at all. Refusing it once the request has
     * been decided would mean the approver cannot see what they just did. That AC-miniloan-084 has
     * no declared home is GAP-miniloan-010's question; leaving this readable does not answer it.
     */
    @Transactional(readOnly = true)
    public ClosedAccountAdjustment view(String callerRole, UUID adjustmentId) {
        requireApprover(callerRole);
        return adjustments
                .findById(adjustmentId)
                .orElseThrow(() -> new AdjustmentNotFoundException(adjustmentId));
    }

    // ── gates ───────────────────────────────────────────────────────────────

    /**
     * The gate order: the request has to exist, the caller has to hold the configured approver role,
     * it has to still be waiting (ACL-016's condition), and only then does four-eyes decide.
     * Permission comes before state so that somebody with no business here learns nothing about the
     * request's status. AC-miniloan-079 depends on the LAST step being last — its person passes the
     * role check and is still refused, which is what makes "ถือ role ผู้อนุมัติยังไม่พอ
     * ต้องเป็นคนละคนด้วย" a thing a test can see.
     */
    private ClosedAccountAdjustment decidable(UUID adjustmentId, String callerRole) {
        ClosedAccountAdjustment adjustment =
                adjustments
                        .findById(adjustmentId)
                        .orElseThrow(() -> new AdjustmentNotFoundException(adjustmentId));

        requireApprover(callerRole);
        if (adjustment.getStatus() != Status.Pending) {
            throw new AdjustmentAlreadyDecidedException();
        }
        if (adjustment.getRequestedBy().equals(callerRole)) {
            throw new SelfApprovalRefusedException();
        }
        return adjustment;
    }

    /** BR-miniloan-039@v1's setting IS the permission — read fresh, mapped, compared. */
    private void requireApprover(String callerRole) {
        ApproverRole approverRole =
                approvers.currentApproverRole().orElseThrow(ApproverRoleNotSetException::new);
        if (!roleIdOf(approverRole).equals(callerRole)) {
            throw new NotTheApproverException(approverRole);
        }
    }

    /**
     * ENT-011's three values against rbac.json's role ids — เจ้าหน้าที่สินเชื่อ, หัวหน้าเจ้าหน้าที่
     * สินเชื่อ and ฝ่ายปฏิบัติการ. ROLE-001 (ผู้สมัคร) and ROLE-005 are absent because ENT-011 cannot
     * hold them: the enum has three values and this is the whole of it.
     *
     * <p>The comparison is exact, so a caller id like {@code ROLE-004-another-person} — the shape
     * FE-miniloan-015's fixtures use to exercise ACL-015's {@code scope: own} among people holding
     * one role — would not match. {@link com.miniloan.service.MockTokenService} issues five ids and
     * none of them takes that shape, so this is unreachable today; it becomes a real question the
     * day identity stops being the mock stand-in CLAUDE.md declares for this round.
     */
    private static String roleIdOf(ApproverRole approverRole) {
        return switch (approverRole) {
            case LoanOfficer -> "ROLE-002";
            case Supervisor -> "ROLE-003";
            case Operations -> "ROLE-004";
        };
    }

    // ── applying the approved value ─────────────────────────────────────────

    /**
     * ENT-010's five fields, each landing on the row {@code targetRecordId} names. The account is
     * loaded in every case — including the two {@code Payment.*} fields — because the Closed state
     * is ACL-015's and ACL-016's shared precondition and a payment carries no status to ask.
     */
    private void apply(ClosedAccountAdjustment adjustment) {
        LoanAccount account =
                accounts
                        .findById(adjustment.getLoanAccountId())
                        .orElseThrow(() -> new LoanAccountNotFoundException(adjustment.getLoanAccountId()));
        if (account.getStatus() != LoanAccount.Status.Closed) {
            throw new AccountNotClosedException();
        }

        AdjustableField field = adjustment.getFieldName();
        String newValue = adjustment.getNewValue();

        if (field.targetEntity() == ClosedAccountAdjustment.TargetEntity.LoanAccount) {
            switch (field) {
                case LOAN_ACCOUNT_CLOSED_AT -> account.applyApprovedClosedAt(instant(field, newValue));
                case LOAN_ACCOUNT_CLOSE_REASON -> account.applyApprovedCloseReason(closeReason(newValue));
                case LOAN_ACCOUNT_ASSIGNED_OPERATIONS_ID ->
                        account.applyApprovedAssignedOperationsId(newValue);
                default -> throw new IllegalStateException("ฟิลด์ของบัญชีที่ไม่รู้จัก: " + field);
            }
            accounts.save(account);
            return;
        }

        Payment payment = targetPayment(adjustment);
        switch (field) {
            case PAYMENT_AMOUNT -> payment.applyApprovedAmount(money(field, newValue));
            case PAYMENT_RECORDED_AT -> payment.applyApprovedRecordedAt(instant(field, newValue));
            default -> throw new IllegalStateException("ฟิลด์ของรายการชำระที่ไม่รู้จัก: " + field);
        }
        payments.save(payment);
    }

    private Payment targetPayment(ClosedAccountAdjustment adjustment) {
        String targetRecordId = adjustment.getTargetRecordId();
        UUID paymentId;
        try {
            paymentId = UUID.fromString(targetRecordId);
        } catch (IllegalArgumentException ex) {
            throw new PaymentNotFoundException(targetRecordId);
        }
        return payments
                .findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(targetRecordId));
    }

    /** ISO-8601, which is the form {@code Instant} itself prints — so a value round-trips. */
    private static Instant instant(AdjustableField field, String value) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException | NullPointerException ex) {
            throw new AdjustedValueUnreadableException(field, "ISO-8601 เช่น 2026-09-06T10:15:30Z");
        }
    }

    /** ENT-006's two doors — a third reason has no constant to become. */
    private static CloseReason closeReason(String value) {
        try {
            return CloseReason.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new AdjustedValueUnreadableException(
                    AdjustableField.LOAN_ACCOUNT_CLOSE_REASON, "FullyPaid หรือ EarlySettlement");
        }
    }

    /**
     * The {@code #,##0.00} form {@link com.miniloan.domain.Money#exact} renders, separators and all,
     * so a figure copied off a screen is accepted as it was shown. Rounding stays where it belongs —
     * {@link Payment#applyApprovedAmount} does it (BR-miniloan-035@v1), not this parser.
     */
    private static BigDecimal money(AdjustableField field, String value) {
        try {
            return new BigDecimal(value.replace(",", "").trim());
        } catch (NumberFormatException | NullPointerException ex) {
            throw new AdjustedValueUnreadableException(field, "จำนวนเงิน เช่น 8,050.00");
        }
    }
}
