package com.miniloan.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * ตารางผ่อน หนึ่งฉบับ (ENT-007 · BR-miniloan-015@v1).
 *
 * <p>A schedule is issued as a whole and versioned as a whole: {@code revisionNumber} with
 * {@code current} is what lets FE-miniloan-011 issue a replacement over the top without erasing what
 * the borrower was shown before. Disbursement issues revision 1.
 *
 * <p>{@code totalPrincipal} is stored rather than summed on read because BR-miniloan-017@v1 makes it
 * an assertion the table has to satisfy — it equals the principal exactly, and a stored figure that
 * disagreed with the rows would say so.
 */
@Entity
@Table(name = "repayment_schedules")
public class RepaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID loanAccountId;

    @Column(nullable = false)
    private int revisionNumber;

    @Column(nullable = false)
    private boolean current;

    @Column(nullable = false)
    private Instant issuedAt;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPrincipal;

    protected RepaymentSchedule() {
        // JPA
    }

    public RepaymentSchedule(UUID loanAccountId, int revisionNumber, BigDecimal totalPrincipal) {
        this.loanAccountId = loanAccountId;
        this.revisionNumber = revisionNumber;
        this.totalPrincipal = Money.round(totalPrincipal);
        this.current = true;
        this.issuedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getLoanAccountId() {
        return loanAccountId;
    }

    public int getRevisionNumber() {
        return revisionNumber;
    }

    public boolean isCurrent() {
        return current;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public BigDecimal getTotalPrincipal() {
        return totalPrincipal;
    }
}
