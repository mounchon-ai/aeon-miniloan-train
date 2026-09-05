package com.miniloan.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * ประวัติการมอบหมายใบสมัคร (ENT-013 · BR-miniloan-032@v1). A log, not a current-state table: every
 * assignment appends a row and none is ever overwritten, which is the half of DQ-miniloan-003 that
 * ENT-013 answers. Who may act on the application <em>now</em> is read from
 * {@code LoanApplication.assignedLoanOfficerId}, never from this history.
 *
 * <p>There is deliberately no unique constraint here, and no {@code IdempotencyKey} guarding the
 * assign command: ENT-012's {@code CommandType} list is closed at six values and names none for
 * assignment, and re-assigning the same application is a legitimate repeat that has to produce a
 * second row. BR-miniloan-043@v1 speaks of every write command, so the tension is real — it is
 * design's list to extend, not this unit's.
 */
@Entity
@Table(name = "application_assignments")
public class ApplicationAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID applicationId;

    /**
     * ENT-004 (Staff) has no unit in the build plan that persists it, so this is the id the
     * supervisor named and nothing validates it against a roster. That limit is the plan's, not a
     * shortcut taken here.
     */
    @Column(nullable = false)
    private String loanOfficerId;

    @Column(nullable = false)
    private String assignedBy;

    @Column(nullable = false)
    private Instant assignedAt;

    protected ApplicationAssignment() {
        // JPA
    }

    public ApplicationAssignment(UUID applicationId, String loanOfficerId, String assignedBy) {
        this.applicationId = applicationId;
        this.loanOfficerId = loanOfficerId;
        this.assignedBy = assignedBy;
        this.assignedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public String getLoanOfficerId() {
        return loanOfficerId;
    }

    public String getAssignedBy() {
        return assignedBy;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }
}
