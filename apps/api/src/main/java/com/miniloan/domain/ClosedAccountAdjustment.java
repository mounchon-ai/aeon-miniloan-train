package com.miniloan.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * คำขอปรับปรุงบัญชีที่ปิดแล้ว (ENT-010 · STM-miniloan-004 · BR-miniloan-041@v1).
 *
 * <p><b>The adjustment is a row of its own, and that is the whole rule.</b> BR-miniloan-041@v1 asks
 * for the change to be recorded "แยกจากตัวบัญชี — ค่าเดิมของบัญชีคงไว้ไม่ถูกทับ", so this entity
 * carries {@code oldValue} and {@code newValue} side by side and {@link LoanAccount} is not touched
 * when one is filed. AC-miniloan-084 reads the five fields back off this row long after the account
 * shows the new value; a design that overwrote the account and kept an audit note would have nothing
 * to read.
 *
 * <p><b>Pending is where it starts and the only place it can leave from</b> (STM-miniloan-004).
 * Approved and Rejected are both final, so {@link #approve} and {@link #reject} refuse anything that
 * has already been decided — the same shape {@link LoanAccount#close} uses, and for the same reason:
 * an invariant enforced on the object cannot be walked around by a second caller.
 *
 * <p><b>Four eyes is guarded here, not only at the route</b> (BR-miniloan-049@v1). ENT-010 puts the
 * constraint on the attribute itself — {@code approvedBy} "ต้องเป็นคนละคนกับ requestedBy" — and
 * STM-miniloan-004 lists the rule on the Pending→Approved transition, so it belongs to whoever holds
 * the object. AC-miniloan-079's sentence is a different thing and belongs to FE-miniloan-016, which
 * refuses the caller before it ever gets this far; what is here is the floor under that.
 *
 * <p><b>{@code fieldName} is a closed list design declares, and {@code targetRecordId} names the row
 * it applies to</b> (ADR-006, answering GAP-miniloan-006). It used to be a free string with nothing
 * behind it, which left the unit that APPLIES an approved change with no way to apply one. The scope
 * is the account AND that account's payments, because AC-miniloan-076's own example — "ยอดชำระงวด
 * สุดท้ายถูกบันทึกผิด" — lives on {@link Payment} and on no {@link LoanAccount} attribute at all.
 *
 * <p><b>ENT-008's instalments are absent from that list on purpose</b>: BR-miniloan-045@v1
 * locks a closed account's schedule permanently and AC-miniloan-108 proves an approved adjustment
 * does not unlock it. The list simply has no way to name one, so the fence is the type rather than a
 * check somebody has to remember.
 */
@Entity
@Table(name = "closed_account_adjustments")
public class ClosedAccountAdjustment {

    /** STM-miniloan-004 — Pending is initial, and both of the others are final. */
    public enum Status {
        Pending,
        Approved,
        Rejected
    }

    /** Which entity the row named by {@code targetRecordId} belongs to. */
    public enum TargetEntity {
        LoanAccount,
        Payment
    }

    /**
     * ENT-010's {@code fieldName} value list, verbatim (ADR-006). The constant names are Java's; the
     * strings are design's, and {@link #declaredName()} is the only form that crosses the wire or
     * reaches the database — see {@link AdjustableFieldConverter}.
     *
     * <p>Each value carries the entity it names, so {@code targetRecordId} can be checked against it
     * without a second field repeating what this one already says.
     */
    public enum AdjustableField {
        LOAN_ACCOUNT_CLOSED_AT("LoanAccount.closedAt", TargetEntity.LoanAccount),
        LOAN_ACCOUNT_CLOSE_REASON("LoanAccount.closeReason", TargetEntity.LoanAccount),
        LOAN_ACCOUNT_ASSIGNED_OPERATIONS_ID(
                "LoanAccount.assignedOperationsId", TargetEntity.LoanAccount),
        PAYMENT_AMOUNT("Payment.amount", TargetEntity.Payment),
        PAYMENT_RECORDED_AT("Payment.recordedAt", TargetEntity.Payment);

        private final String declaredName;
        private final TargetEntity targetEntity;

        AdjustableField(String declaredName, TargetEntity targetEntity) {
            this.declaredName = declaredName;
            this.targetEntity = targetEntity;
        }

        @JsonValue
        public String declaredName() {
            return declaredName;
        }

        public TargetEntity targetEntity() {
            return targetEntity;
        }

        /** Anything outside ENT-010's list is refused here rather than stored and puzzled over later. */
        @JsonCreator
        public static AdjustableField ofDeclaredName(String declaredName) {
            for (AdjustableField candidate : values()) {
                if (candidate.declaredName.equals(declaredName)) {
                    return candidate;
                }
            }
            throw new IllegalArgumentException("ไม่รู้จักฟิลด์ที่ขอแก้: " + declaredName);
        }
    }

    /**
     * Persists the DECLARED name, not the Java constant. ENT-010's list is what a reader of the
     * database is entitled to see, and a column holding {@code LOAN_ACCOUNT_CLOSED_AT} would be a
     * second spelling of a value design already spelled once.
     */
    @Converter
    public static class AdjustableFieldConverter
            implements AttributeConverter<AdjustableField, String> {

        @Override
        public String convertToDatabaseColumn(AdjustableField attribute) {
            return attribute == null ? null : attribute.declaredName();
        }

        @Override
        public AdjustableField convertToEntityAttribute(String dbData) {
            return dbData == null ? null : AdjustableField.ofDeclaredName(dbData);
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID loanAccountId;

    /** ADR-006 — the row the change applies to; equals loanAccountId for a LoanAccount.* field. */
    @Column(nullable = false)
    private String targetRecordId;

    @Convert(converter = AdjustableFieldConverter.class)
    @Column(nullable = false)
    private AdjustableField fieldName;

    @Column(nullable = false)
    private String oldValue;

    @Column(nullable = false)
    private String newValue;

    /** BR-miniloan-049@v1's "ผู้ขอแก้" — the mock scheme makes the role the person. */
    @Column(nullable = false)
    private String requestedBy;

    @Column(nullable = false)
    private Instant requestedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    /** Optional in ENT-010 — AC-miniloan-085: an undecided request has no approver, and no stand-in. */
    @Column
    private String approvedBy;

    @Column
    private Instant approvedAt;

    protected ClosedAccountAdjustment() {
        // JPA
    }

    public ClosedAccountAdjustment(
            UUID loanAccountId,
            String targetRecordId,
            AdjustableField fieldName,
            String oldValue,
            String newValue,
            String requestedBy,
            Instant requestedAt) {
        this.loanAccountId = loanAccountId;
        this.targetRecordId = targetRecordId;
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.requestedBy = requestedBy;
        this.requestedAt = requestedAt;
        this.status = Status.Pending;
    }

    /**
     * STM-miniloan-004's Pending→Approved. BR-miniloan-049@v1 is checked here as well as at the
     * route, so the invariant holds for any caller that reaches this object.
     */
    public void approve(String approvedBy, Instant approvedAt) {
        decide(Status.Approved, approvedBy, approvedAt);
    }

    /** STM-miniloan-004's Pending→Rejected — the same transition guard and the same four-eyes rule. */
    public void reject(String approvedBy, Instant approvedAt) {
        decide(Status.Rejected, approvedBy, approvedAt);
    }

    private void decide(Status decision, String approvedBy, Instant approvedAt) {
        if (this.status != Status.Pending) {
            throw new IllegalStateException("คำขอปรับปรุงนี้ถูกพิจารณาไปแล้ว — พิจารณาซ้ำไม่ได้");
        }
        if (this.requestedBy.equals(approvedBy)) {
            throw new IllegalStateException("ผู้อนุมัติต้องเป็นคนละคนกับผู้ขอแก้");
        }
        this.status = decision;
        this.approvedBy = approvedBy;
        this.approvedAt = approvedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLoanAccountId() {
        return loanAccountId;
    }

    public String getTargetRecordId() {
        return targetRecordId;
    }

    public AdjustableField getFieldName() {
        return fieldName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Status getStatus() {
        return status;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }
}
