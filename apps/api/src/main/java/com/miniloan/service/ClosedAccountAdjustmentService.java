package com.miniloan.service;

import com.miniloan.domain.ApproverRoleSetting.ApproverRole;
import com.miniloan.domain.ClosedAccountAdjustment;
import com.miniloan.domain.ClosedAccountAdjustment.AdjustableField;
import com.miniloan.domain.ClosedAccountAdjustment.TargetEntity;
import com.miniloan.domain.LoanAccount;
import com.miniloan.domain.Payment;
import com.miniloan.repository.ClosedAccountAdjustmentRepository;
import com.miniloan.repository.LoanAccountRepository;
import com.miniloan.repository.PaymentRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ยื่นคำขอปรับปรุงบัญชีที่ปิดแล้ว (UC-miniloan-017 · API-016 · ACL-015 · BR-miniloan-038@v1).
 *
 * <p><b>Filing a request changes nothing about the account</b> (BR-miniloan-041@v1 ·
 * AC-miniloan-076). One row is written, {@link LoanAccount} is not read for anything but its gate
 * conditions, and the new value only reaches the account when FE-miniloan-016 approves it. That is
 * the whole of BR-miniloan-038@v1's "แก้ทับทันทีไม่ได้", expressed as a service that has no path to
 * the account's setters at all.
 *
 * <p><b>BR-miniloan-040@v1 is a gate on the feature, not on the account.</b> Its wording is "ปฏิเสธ
 * คำขอปรับปรุงบัญชีที่ปิดแล้วทุกกรณี", and UC-miniloan-017's exception flow says the same, so it is
 * asked before the account is even looked up: with no approver named, there is nothing to tell a
 * caller about any particular account, and no request may exist waiting for somebody to be found
 * later. AC-miniloan-081 is that sentence said out loud — the system has no "แก้แล้วรออนุมัติ
 * ย้อนหลัง" state to walk into.
 *
 * <p><b>Two routes here answer to no API- id, and that is deliberate</b> — the same decision
 * {@code RepaymentScheduleController} recorded for BR-miniloan-044@v1. AC-miniloan-077 and
 * AC-miniloan-081 measure a caller editing a closed account "ด้วยการเรียก API แก้ไขบัญชีโดยตรง", and
 * AC-miniloan-078 measures one deleting it. {@code interfaces.json} declares no endpoint for either,
 * which would leave all three answered by a 404 — the ABSENCE of a route rather than the ENFORCEMENT
 * of a rule, and indistinguishable from a typo in the path. BR-miniloan-025@v1 asks for the rule at
 * the API, so both refusals are declared and both carry the criterion's own sentence.
 *
 * <p><b>What may be adjusted is design's closed list, not this unit's</b> (ADR-006, answering
 * GAP-miniloan-006). {@link AdjustableField} is ENT-010's five values verbatim, and each one carries
 * the entity it names, so {@code targetRecordId} is checked against the field rather than against a
 * second flag repeating it. A LoanAccount.* field may only name this account; a Payment.* field may
 * only name a payment OF this account — the scope stays per account exactly as ACL-015 is.
 *
 * <p><b>One field per request.</b> ENT-010 holds one {@code fieldName} with its own old and new
 * value, and design mints no group id anywhere — so a call that took several fields would write
 * several independent rows and make "one คำขอ" unrecoverable from the data, which AC-miniloan-085
 * counts. UC-miniloan-017's alternate flow ("ยื่นคำขอปรับปรุงมากกว่าหนึ่งฟิลด์ในคำขอเดียวกัน") has
 * no acceptance criterion pinning it; it is raised in the build report rather than guessed at here.
 */
@Service
public class ClosedAccountAdjustmentService {

    /** ACL-015 names ROLE-004 — and the mock scheme makes the Operations role the person. */
    private static final String OPERATIONS = "ROLE-004";

    private final ClosedAccountAdjustmentRepository adjustments;
    private final LoanAccountRepository accounts;
    private final PaymentRepository payments;
    private final ApproverRoleSettingService approvers;
    private final Clock clock;

    public ClosedAccountAdjustmentService(
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

    /** ACL-015 names ROLE-004 alone for UC-miniloan-017, and rbac.json is default-deny. */
    public static class OperationsOnlyException extends RuntimeException {
        public OperationsOnlyException() {
            super("ไม่มีสิทธิ์ยื่นคำขอปรับปรุงบัญชีที่ปิดแล้ว — ทำได้เฉพาะเจ้าหน้าที่ Operations");
        }
    }

    /** AC-miniloan-080's sentence, word for word — and nothing is written when it is thrown. */
    public static class ApproverRoleNotSetException extends RuntimeException {
        public ApproverRoleNotSetException() {
            super(
                    "ยังไม่ได้ตั้ง role ผู้อนุมัติ — ใช้ฟีเจอร์แก้ข้อมูลบัญชีที่ปิดแล้วไม่ได้ · ให้ Loan Officer ตั้งค่า role ผู้อนุมัติก่อน");
        }
    }

    public static class LoanAccountNotFoundException extends RuntimeException {
        public LoanAccountNotFoundException(UUID id) {
            super("ไม่พบบัญชีสินเชื่อ " + id);
        }
    }

    /** ACL-015's {@code scope: own} · BR-miniloan-054@v1, among Operations holding the same role. */
    public static class NotAssignedOperationsException extends RuntimeException {
        public NotAssignedOperationsException() {
            super("ไม่มีสิทธิ์ดำเนินการกับบัญชีสินเชื่อนี้ — ไม่ได้ถูก assign ดูแลบัญชีนี้");
        }
    }

    /** ACL-015's condition: {@code STM-miniloan-002} state Closed — this feature is for closed accounts. */
    public static class AccountNotClosedException extends RuntimeException {
        public AccountNotClosedException() {
            super("บัญชีนี้ยังไม่ปิด — คำขอปรับปรุงใช้กับบัญชีที่ปิดแล้วเท่านั้น");
        }
    }

    /** ENT-010 declares all four capture attributes required, and a blank is not a value. */
    public static class AdjustmentFieldsRequiredException extends RuntimeException {
        public AdjustmentFieldsRequiredException() {
            super("ต้องระบุรายการที่ขอแก้ ชื่อฟิลด์ ค่าเดิม และค่าใหม่ให้ครบ");
        }
    }

    /**
     * ENT-010's validation on {@code targetRecordId} (ADR-006), enforced rather than described: a
     * LoanAccount.* field names this account and nothing else, and a Payment.* field names a payment
     * OF this account. Pointing at another account's payment would be ACL-015's {@code scope: own}
     * walked around through a field nobody checked.
     */
    public static class TargetRecordMismatchException extends RuntimeException {
        public TargetRecordMismatchException(String message) {
            super(message);
        }
    }

    /**
     * AC-miniloan-077 and AC-miniloan-081 — the same refusal wearing two sentences, chosen by whether
     * an approver has been named. With one named, the caller is told where the change has to go; with
     * none, they are told the door is shut for every account.
     */
    public static class DirectAccountEditRefusedException extends RuntimeException {
        public DirectAccountEditRefusedException(boolean approverRoleSet) {
            super(
                    approverRoleSet
                            ? "แก้ข้อมูลบัญชีที่ปิดแล้วโดยตรงไม่ได้ — ต้องยื่นคำขอปรับปรุงและรออนุมัติก่อน"
                            : "ยังไม่ได้ตั้ง role ผู้อนุมัติ — การแก้ไขมีผลก่อนได้รับอนุมัติไม่ได้ทุกกรณี");
        }
    }

    /** AC-miniloan-078's sentence, word for word. */
    public static class AccountDeletionRefusedException extends RuntimeException {
        public AccountDeletionRefusedException() {
            super("ยกเลิกบัญชีสินเชื่อที่ปิดแล้วไม่ได้ — ถ้าข้อมูลผิด ให้ยื่นคำขอปรับปรุงเพื่อขออนุมัติแทน");
        }
    }

    /** UC-miniloan-017: "ระบุค่าเดิมและค่าใหม่" — ENT-010's required attributes, and no more. */
    public record AdjustmentFields(
            String targetRecordId, AdjustableField fieldName, String oldValue, String newValue) {}

    /** What one filed request is, and the approver it is now waiting on. */
    public record SubmitResult(ClosedAccountAdjustment adjustment, ApproverRole approverRole) {

        /** AC-miniloan-076's first sentence, word for word, with the role that was configured. */
        public String message() {
            return "ส่งคำขอปรับปรุงบัญชีที่ปิดแล้วเรียบร้อย — รออนุมัติจาก " + approverRole;
        }
    }

    /**
     * API-016. The order of the gates is the order the criteria ask them in: who may use the feature,
     * then whether the feature is switched on at all, then the account and its own two conditions.
     */
    @Transactional
    public SubmitResult submit(UUID loanAccountId, String callerRole, AdjustmentFields fields) {
        // ACL-015 is about WHO, and it does not depend on the account existing.
        if (!OPERATIONS.equals(callerRole)) {
            throw new OperationsOnlyException();
        }

        // BR-miniloan-040@v1 · AC-miniloan-080 — "ทุกกรณี", so before any account is read.
        ApproverRole approverRole =
                approvers.currentApproverRole().orElseThrow(ApproverRoleNotSetException::new);

        LoanAccount account =
                accounts
                        .findById(loanAccountId)
                        .orElseThrow(() -> new LoanAccountNotFoundException(loanAccountId));
        // Assignment before state, the order the payoff route already uses: telling a stranger
        // whether an account is open or closed is already telling them something.
        if (!account.getAssignedOperationsId().equals(callerRole)) {
            throw new NotAssignedOperationsException();
        }
        if (account.getStatus() != LoanAccount.Status.Closed) {
            throw new AccountNotClosedException();
        }
        if (fields.fieldName() == null
                || isBlank(fields.targetRecordId())
                || isBlank(fields.oldValue())
                || isBlank(fields.newValue())) {
            throw new AdjustmentFieldsRequiredException();
        }
        requireTargetBelongsToAccount(fields, account);

        // BR-miniloan-041@v1: a row of its own. Nothing on the account is written here, and there is
        // no branch below that could — the account object is not passed on.
        ClosedAccountAdjustment filed =
                adjustments.save(
                        new ClosedAccountAdjustment(
                                loanAccountId,
                                fields.targetRecordId().trim(),
                                fields.fieldName(),
                                fields.oldValue(),
                                fields.newValue(),
                                callerRole,
                                Instant.now(clock)));

        return new SubmitResult(filed, approverRole);
    }

    /**
     * AC-miniloan-077 · AC-miniloan-081 — refused for every caller and every account, so no role gate
     * runs first: a permission check would imply somebody could pass it.
     */
    public void refuseDirectAccountEdit() {
        throw new DirectAccountEditRefusedException(approvers.currentApproverRole().isPresent());
    }

    /** AC-miniloan-078 — likewise refused outright; the adjustment path is the only way in. */
    public void refuseAccountDeletion() {
        throw new AccountDeletionRefusedException();
    }

    /**
     * The field says which entity its row lives in, so nothing here has to be told twice. A payment
     * is looked up rather than trusted: an id that parses is not an id that belongs to this account.
     */
    private void requireTargetBelongsToAccount(AdjustmentFields fields, LoanAccount account) {
        String target = fields.targetRecordId().trim();
        if (fields.fieldName().targetEntity() == TargetEntity.LoanAccount) {
            if (!account.getId().toString().equals(target)) {
                throw new TargetRecordMismatchException(
                        "รายการที่ขอแก้ต้องเป็นบัญชีที่กำลังเปิดอยู่ — "
                                + fields.fieldName().declaredName()
                                + " เป็นฟิลด์ของบัญชี");
            }
            return;
        }

        UUID paymentId;
        try {
            paymentId = UUID.fromString(target);
        } catch (IllegalArgumentException notAUuid) {
            throw new TargetRecordMismatchException("รายการชำระที่ขอแก้ไม่ใช่ id ที่ถูกต้อง: " + target);
        }
        Payment payment =
                payments
                        .findById(paymentId)
                        .orElseThrow(
                                () -> new TargetRecordMismatchException("ไม่พบรายการชำระ " + paymentId));
        if (!payment.getLoanAccountId().equals(account.getId())) {
            throw new TargetRecordMismatchException(
                    "รายการชำระ " + paymentId + " ไม่ใช่ของบัญชีสินเชื่อนี้");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
