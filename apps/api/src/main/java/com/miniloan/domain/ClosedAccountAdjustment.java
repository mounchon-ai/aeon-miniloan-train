package com.miniloan.domain;

import jakarta.persistence.Column;
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
 * <p><b>{@code fieldName} is a free string because ENT-010 says {@code type: string}.</b> No enum,
 * no whitelist, and no mapping to a {@link LoanAccount} attribute exists anywhere in the design — so
 * nothing is validated against a list this unit would have had to invent. What that leaves open for
 * the unit that APPLIES an approved change is raised in FE-miniloan-015's build report.
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

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID loanAccountId;

    @Column(nullable = false)
    private String fieldName;

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
            String fieldName,
            String oldValue,
            String newValue,
            String requestedBy,
            Instant requestedAt) {
        this.loanAccountId = loanAccountId;
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

    public String getFieldName() {
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
