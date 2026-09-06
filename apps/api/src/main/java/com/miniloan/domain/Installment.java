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
import java.time.LocalDate;
import java.util.UUID;

/**
 * งวดผ่อน (ENT-008 · STM-miniloan-003). Every figure arrives already rounded, because
 * BR-miniloan-035@v1 leaves no fuller copy anywhere for it to disagree with.
 *
 * <p><b>STM-miniloan-003 is enforced by the two mutators, not by whoever calls them.</b> A row leaves
 * {@code Due} exactly twice — {@link #markPaid} when the instalment is settled (BR-miniloan-020@v1)
 * and {@link #cancel} when early settlement retires the rest of the table (BR-miniloan-023@v1) — and
 * both refuse a row that has already left. There is no setter and no path back to {@code Due}, so a
 * paid instalment cannot be re-paid and a cancelled one cannot be revived, whatever a service does.
 */
@Entity
@Table(name = "installments")
public class Installment {

    /** STM-miniloan-003 — Due is where every instalment starts. */
    public enum Status {
        Due,
        Paid,
        Cancelled
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID repaymentScheduleId;

    @Column(nullable = false)
    private int installmentNumber;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal emiAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal interestPortion;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal principalPortion;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal remainingBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    /** ENT-008 · null for every row that has not been settled — including a cancelled one. */
    @Column
    private Instant paidAt;

    protected Installment() {
        // JPA
    }

    public Installment(
            UUID repaymentScheduleId,
            int installmentNumber,
            LocalDate dueDate,
            BigDecimal emiAmount,
            BigDecimal interestPortion,
            BigDecimal principalPortion,
            BigDecimal remainingBalance) {
        this.repaymentScheduleId = repaymentScheduleId;
        this.installmentNumber = installmentNumber;
        this.dueDate = dueDate;
        this.emiAmount = emiAmount;
        this.interestPortion = interestPortion;
        this.principalPortion = principalPortion;
        this.remainingBalance = remainingBalance;
        this.status = Status.Due;
    }

    /** BR-miniloan-020@v1 — Due → Paid, and only from Due. */
    public void markPaid(Instant at) {
        if (this.status != Status.Due) {
            throw new IllegalStateException("งวดที่ " + installmentNumber + " ไม่ได้อยู่ในสถานะค้างชำระ");
        }
        this.status = Status.Paid;
        this.paidAt = at;
    }

    /**
     * BR-miniloan-023@v1 — Due → Cancelled when the account is settled early. The figures stay
     * exactly as issued: the row is retired, not erased, so AC-miniloan-005 can still show what
     * instalments 6 to 12 would have been.
     */
    public void cancel() {
        if (this.status != Status.Due) {
            throw new IllegalStateException("งวดที่ " + installmentNumber + " ไม่ได้อยู่ในสถานะค้างชำระ");
        }
        this.status = Status.Cancelled;
    }

    public UUID getId() {
        return id;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public UUID getRepaymentScheduleId() {
        return repaymentScheduleId;
    }

    public int getInstallmentNumber() {
        return installmentNumber;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public BigDecimal getEmiAmount() {
        return emiAmount;
    }

    public BigDecimal getInterestPortion() {
        return interestPortion;
    }

    public BigDecimal getPrincipalPortion() {
        return principalPortion;
    }

    public BigDecimal getRemainingBalance() {
        return remainingBalance;
    }

    public Status getStatus() {
        return status;
    }
}
