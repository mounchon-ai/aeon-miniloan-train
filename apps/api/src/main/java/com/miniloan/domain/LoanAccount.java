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
 * <p><b>What is deliberately not here.</b> ENT-006 also declares {@code closedAt} and
 * {@code closeReason}. Closing an account is FE-miniloan-013 work, and a field arrives with the unit
 * that first needs it. ENT-006 declares no account number either, while AC-miniloan-058 renders
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

    public UUID getId() {
        return id;
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
