package com.miniloan.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * รายการชำระเงิน (ENT-009). One row per payment the system actually accepted — and only those.
 *
 * <p><b>A refused payment leaves nothing behind</b> (BR-miniloan-019@v1 · AC-miniloan-014). Two half
 * payments of instalment 3 are turned down separately and nothing accumulates, so there is no
 * partially-paid row here, no "pending" flag, and no sum for a later call to find. That is why this
 * entity is written at the END of a successful command rather than at the start of an attempted one.
 *
 * <p>{@code installmentId} is optional in ENT-009 because an early-settlement payment settles the
 * account rather than one instalment. {@code overpaymentAmount} and {@code prepaymentFee} are
 * likewise only present on an {@code InstallmentOverpayment}, and both arrive already rounded
 * (BR-miniloan-035@v1).
 *
 * <p><b>The fee is stored, so the principal cut is the remainder.</b> BR-miniloan-046@v2 describes
 * the mechanism twice — "หักออกจากส่วนเกินก่อนนำไปตัดเงินต้น" and "เงินต้นลดลง = ส่วนเกิน × 99%" —
 * and the two agree at AC-miniloan-086's 20,000.00 and AC-miniloan-087's 0.01 but not everywhere.
 * This column is why the first reading is the one implemented: the fee exists as a rounded figure of
 * its own before any principal is cut, so the cut is what is left of the overage after it, never a
 * second independent rounding of the same overage. BR-miniloan-050@v1, which BR-miniloan-046@v2
 * names as the place that pins this, is not in req's contract at all — see PaymentService.
 */
@Entity
@Table(name = "payments")
public class Payment {

    /** ENT-009's three values. */
    public enum PaymentType {
        InstallmentExact,
        InstallmentOverpayment,
        EarlySettlement
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID loanAccountId;

    @Column
    private UUID installmentId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType paymentType;

    @Column(precision = 19, scale = 2)
    private BigDecimal overpaymentAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal prepaymentFee;

    /** BR-miniloan-034@v1 — Operations, and the mock scheme makes the role the person. */
    @Column(nullable = false)
    private String recordedBy;

    @Column(nullable = false)
    private Instant recordedAt;

    protected Payment() {
        // JPA
    }

    public Payment(
            UUID loanAccountId,
            UUID installmentId,
            BigDecimal amount,
            PaymentType paymentType,
            BigDecimal overpaymentAmount,
            BigDecimal prepaymentFee,
            String recordedBy,
            Instant recordedAt) {
        this.loanAccountId = loanAccountId;
        this.installmentId = installmentId;
        this.amount = Money.round(amount);
        this.paymentType = paymentType;
        this.overpaymentAmount = Money.round(overpaymentAmount);
        this.prepaymentFee = Money.round(prepaymentFee);
        this.recordedBy = recordedBy;
        this.recordedAt = recordedAt;
    }

    /**
     * ENT-010's two {@code Payment.*} adjustable fields, applied by FE-miniloan-016 after approval.
     *
     * <p><b>This is not a second way to record a payment.</b> BR-miniloan-034@v1 keeps the recording
     * of a payment with Operations and {@link PaymentService} is still the only place a row is
     * created; these two correct a row that already exists, and only because BR-miniloan-038@v1's
     * approved request said so. The caller checks the approval and the account's Closed state —
     * neither is knowable from here, since a payment holds no status of its own — and what this
     * class can still enforce, it does: the amount goes through {@link Money#round} exactly as the
     * constructor's did (BR-miniloan-035@v1), so a corrected figure never keeps more decimals than
     * the original was allowed.
     *
     * <p>{@code recordedBy} is deliberately NOT adjustable. ENT-010 lists five fields and it is not
     * one of them — who recorded a payment is a fact about what happened, not a value to correct.
     */
    public void applyApprovedAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("ยอดชำระที่อนุมัติแล้วเป็นค่าว่างไม่ได้");
        }
        // Deliberately no sign or range check: the CONSTRUCTOR has none either, and a corrected
        // amount refused by a rule the original was never held to would be this unit inventing a
        // requirement — the same shape as FE-miniloan-017's ApproverRoleRequiredException, which is
        // now GAP-miniloan-008. What a payment amount may be is req's to say.
        this.amount = Money.round(amount);
    }

    /** The time the payment was taken — corrected, never blanked. */
    public void applyApprovedRecordedAt(Instant recordedAt) {
        if (recordedAt == null) {
            throw new IllegalArgumentException("เวลาที่บันทึกการชำระที่อนุมัติแล้วเป็นค่าว่างไม่ได้");
        }
        this.recordedAt = recordedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLoanAccountId() {
        return loanAccountId;
    }

    public UUID getInstallmentId() {
        return installmentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public BigDecimal getOverpaymentAmount() {
        return overpaymentAmount;
    }

    public BigDecimal getPrepaymentFee() {
        return prepaymentFee;
    }

    public String getRecordedBy() {
        return recordedBy;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
