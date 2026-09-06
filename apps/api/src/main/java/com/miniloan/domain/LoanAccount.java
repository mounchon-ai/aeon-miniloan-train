package com.miniloan.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * บัญชีสินเชื่อ (ENT-006 · STM-miniloan-002 · BR-miniloan-014@v1).
 *
 * <p><b>One application, one account.</b> The unique constraint on {@code applicationId} is that
 * rule (AC-miniloan-060 · AC-miniloan-132) — the database refuses the second account rather than a
 * disabled button doing it, which is the fence CLAUDE.md puts on every retryable write.
 *
 * <p>{@code interestRateVersionId} is a REFERENCE, not a copied number (BR-miniloan-037@v1 ·
 * AC-miniloan-104): an account opened under an old version still reads that version rate after two
 * more have been published.
 *
 * <p><b>Closing is two doors and no others</b> (BR-miniloan-021@v1). {@link #close} is the only way
 * the status leaves Active, it demands a {@link CloseReason}, and {@link CloseReason} has exactly
 * the two values ENT-006 declares — so a third way to close cannot be expressed here at all, let
 * alone reached. Closed is final: {@link #close} on a closed account throws rather than moving the
 * timestamp, which is AC-miniloan-007's "ปิดซ้ำไม่ได้" at the layer that owns the invariant.
 *
 * <p><b>What is deliberately not here.</b> ENT-006 declares no account number, while AC-miniloan-058
 * renders
 * "เลขบัญชีเลขที่ {เลขบัญชี}" — the account id IS that number, raised for design with the other
 * enumeration gaps rather than answered with a second identifier nobody asked for.
 */
@Entity
@Table(
        name = "loan_accounts",
        uniqueConstraints =
                @UniqueConstraint(name = "uk_loan_account_application", columnNames = {"applicationId"}))
public class LoanAccount {

    /** STM-miniloan-002 — two states, and Closed is final. */
    public enum Status {
        Active,
        Closed
    }

    /**
     * ENT-006's two values, which are BR-miniloan-021@v1's two doors: every instalment paid, or the
     * early-settlement amount paid in full. There is no third constant because there is no third
     * way, and AC-miniloan-006 refuses the direct close precisely because it belongs to neither.
     */
    public enum CloseReason {
        FullyPaid,
        EarlySettlement
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID applicationId;

    @Column(nullable = false)
    private UUID interestRateVersionId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal principalAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingPrincipal;

    @Column(nullable = false)
    private int termMonths;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    /**
     * ENT-006 requires it and no rule says who is chosen at disbursement. FE-miniloan-002 mock
     * scheme resolves one identity per role, so the Operations role IS the Operations person here —
     * the same stand-in {@code applicantId} and {@code approvedBy} already use.
     */
    @Column(nullable = false)
    private String assignedOperationsId;

    @Column(nullable = false)
    private Instant disbursedAt;

    @Column
    private Instant closedAt;

    @Enumerated(EnumType.STRING)
    @Column
    private CloseReason closeReason;

    protected LoanAccount() {
        // JPA
    }

    public LoanAccount(
            UUID applicationId,
            UUID interestRateVersionId,
            BigDecimal principalAmount,
            int termMonths,
            String assignedOperationsId) {
        this.applicationId = applicationId;
        this.interestRateVersionId = interestRateVersionId;
        this.principalAmount = Money.round(principalAmount);
        this.outstandingPrincipal = this.principalAmount;
        this.termMonths = termMonths;
        this.assignedOperationsId = assignedOperationsId;
        this.status = Status.Active;
        this.disbursedAt = Instant.now();
    }

    /**
     * BR-miniloan-020@v1: the balance falls by the PRINCIPAL of what was paid, never by the amount
     * handed over — the interest portion of an instalment was never owed as principal. Already
     * rounded on the way in, because BR-miniloan-035@v1 leaves no fuller copy to disagree with.
     */
    public void reducePrincipal(BigDecimal amount) {
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("ยอดตัดเงินต้นติดลบไม่ได้");
        }
        BigDecimal reduced = Money.round(this.outstandingPrincipal.subtract(Money.round(amount)));
        if (reduced.signum() < 0) {
            throw new IllegalArgumentException("ตัดเงินต้นเกินยอดคงเหลือไม่ได้");
        }
        this.outstandingPrincipal = reduced;
    }

    /**
     * BR-miniloan-021@v1 · AC-miniloan-007. The caller names which of the two doors it came through,
     * and a closed account refuses to be closed again here rather than only at the route — a second
     * close must not be able to move {@code closedAt}, whichever caller reaches this object.
     */
    public void close(CloseReason reason, Instant at) {
        if (this.status == Status.Closed) {
            throw new IllegalStateException("บัญชีนี้ปิดแล้ว — ปิดซ้ำไม่ได้");
        }
        this.status = Status.Closed;
        this.closeReason = reason;
        this.closedAt = at;
    }

    /**
     * ENT-010's three {@code LoanAccount.*} adjustable fields, applied by FE-miniloan-016 AFTER a
     * request has been approved (BR-miniloan-038@v1 · BR-miniloan-041@v1). They are not general
     * setters and must not become any: each one refuses an account that is not Closed, because the
     * only door design opened to them is a closed account's approved adjustment. An Active account
     * changes through {@link #reducePrincipal} and {@link #close} exactly as it always did.
     *
     * <p>They are deliberately three narrow methods rather than one that takes a field name: the
     * closed list of what may be adjusted is ENT-010's, and it is already expressed as a type there.
     * A method per field means an unlisted field has nothing here to call.
     */
    public void applyApprovedClosedAt(Instant closedAt) {
        requireClosedForAdjustment();
        if (closedAt == null) {
            throw new IllegalArgumentException("เวลาปิดบัญชีที่อนุมัติแล้วเป็นค่าว่างไม่ได้");
        }
        this.closedAt = closedAt;
    }

    /** ENT-006's two doors stay two — a reason outside {@link CloseReason} cannot be expressed. */
    public void applyApprovedCloseReason(CloseReason reason) {
        requireClosedForAdjustment();
        if (reason == null) {
            throw new IllegalArgumentException("เหตุผลการปิดบัญชีที่อนุมัติแล้วเป็นค่าว่างไม่ได้");
        }
        this.closeReason = reason;
    }

    /** BR-miniloan-054@v1's owner of the account — reassigned only through an approved request. */
    public void applyApprovedAssignedOperationsId(String assignedOperationsId) {
        requireClosedForAdjustment();
        if (assignedOperationsId == null || assignedOperationsId.isBlank()) {
            throw new IllegalArgumentException("ผู้ดูแลบัญชีที่อนุมัติแล้วเป็นค่าว่างไม่ได้");
        }
        this.assignedOperationsId = assignedOperationsId;
    }

    private void requireClosedForAdjustment() {
        if (this.status != Status.Closed) {
            throw new IllegalStateException(
                    "บัญชีนี้ยังไม่ปิด — การปรับปรุงที่อนุมัติแล้วใช้กับบัญชีที่ปิดแล้วเท่านั้น");
        }
    }

    public UUID getId() {
        return id;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public CloseReason getCloseReason() {
        return closeReason;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public UUID getInterestRateVersionId() {
        return interestRateVersionId;
    }

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public BigDecimal getOutstandingPrincipal() {
        return outstandingPrincipal;
    }

    public int getTermMonths() {
        return termMonths;
    }

    public Status getStatus() {
        return status;
    }

    public String getAssignedOperationsId() {
        return assignedOperationsId;
    }

    public Instant getDisbursedAt() {
        return disbursedAt;
    }
}
