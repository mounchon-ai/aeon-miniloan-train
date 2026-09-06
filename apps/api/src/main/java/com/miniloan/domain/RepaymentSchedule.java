package com.miniloan.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
 *
 * <p><b>The revision number is the retry fence</b> (CLAUDE.md: every write that can be retried is
 * guarded by a database unique constraint). Two reissues racing on the same account both compute
 * {@code n + 1}, and {@code uk_repayment_schedule_revision} is what turns the loser into a failed
 * write instead of a second revision 2. A DELIBERATE second reissue is a different thing and is
 * allowed — AC-miniloan-011 issues revision 3 over revision 2 on purpose — so there is no
 * idempotency key here: the constraint separates a duplicate from a repeat, and a key could not.
 *
 * <p>{@code supersededAt} is NOT a column. ENT-007 declares four attributes and that is not one of
 * them, and it does not need to be: revision n was replaced at the moment revision n+1 was issued,
 * so AC-miniloan-008's "ถูกแทนที่เมื่อ {วันที่ออกฉบับใหม่}" is read off the next revision's
 * {@code issuedAt} rather than written twice and left to disagree.
 */
@Entity
@Table(
        name = "repayment_schedules",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_repayment_schedule_revision",
                        columnNames = {"loanAccountId", "revisionNumber"}))
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

    /**
     * BR-miniloan-044@v1: a revision is replaced, never edited and never removed. The row keeps
     * every figure it was issued with — all this changes is which revision the account reads as
     * current, and ENT-007 allows exactly one of those at a time (AC-miniloan-011).
     */
    public void supersede() {
        this.current = false;
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
