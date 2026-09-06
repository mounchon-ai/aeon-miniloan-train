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
import java.time.LocalDate;
import java.util.UUID;

/**
 * งวดผ่อน (ENT-008 · STM-miniloan-003). Every figure arrives already rounded, because
 * BR-miniloan-035@v1 leaves no fuller copy anywhere for it to disagree with.
 *
 * <p>{@code paidAt} is ENT-008 too, and belongs to FE-miniloan-013 — the unit that records a payment
 * is the one that first needs it.
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

    public UUID getId() {
        return id;
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
