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
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * การตั้งค่า role ผู้อนุมัติการปรับปรุงบัญชีที่ปิดแล้ว (ENT-011 · BR-miniloan-039@v1).
 *
 * <p><b>A setting, not a constant.</b> BR-miniloan-039@v1 asks for a value that can be changed later
 * "โดยไม่ต้องแก้โปรแกรม", and AC-miniloan-089 measures exactly that — change the role and the next
 * request answers to the new one, with no redeploy and no restart. That is why this is a table row
 * read on every call rather than a field in {@code application.yml} or a constant in a service.
 *
 * <p><b>There is one row, and the database is what says so.</b> {@code singletonKey} carries the
 * same literal on every instance under a unique constraint, so a second settings row cannot be
 * written even by two callers racing — the fence CLAUDE.md puts on every retryable write, applied to
 * a table whose whole meaning is that it holds one answer.
 *
 * <p><b>Unset is a real state, and it is the state the system starts in</b> (BR-miniloan-040@v1).
 * ENT-011 declares all three attributes optional, and no row at all is how "ยังไม่ได้ตั้ง" is
 * represented here: nothing is seeded, so there is no default approver for a caller to inherit by
 * accident. {@link #approverRoleValue()} folds the two shapes of unset — no row, and a row whose
 * {@code approverRole} is null — into one empty answer, so a reader cannot accidentally treat one as
 * set.
 *
 * <p><b>{@link ApproverRole} is ENT-011's value list verbatim.</b> The three staff roles ENT-004
 * declares, and nothing else: neither Applicant nor a "none" constant is a member, because an
 * approver that is nobody is the absence of a row rather than a fourth value.
 */
@Entity
@Table(
        name = "approver_role_settings",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_approver_role_setting_singleton",
                        columnNames = {"singletonKey"}))
public class ApproverRoleSetting {

    /** ENT-011's three values — the same list ENT-004's {@code role} carries. */
    public enum ApproverRole {
        LoanOfficer,
        Supervisor,
        Operations
    }

    /** The one key this table's single row is written under. */
    public static final String SINGLETON_KEY = "closed-account-approver-role";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String singletonKey = SINGLETON_KEY;

    /** Optional in ENT-011 — null is "ยังไม่ได้ตั้ง", and there is no default. */
    @Enumerated(EnumType.STRING)
    @Column
    private ApproverRole approverRole;

    /** BR-miniloan-048@v1 — a Loan Officer, and the mock scheme makes the role the person. */
    @Column
    private String updatedBy;

    @Column
    private Instant updatedAt;

    protected ApproverRoleSetting() {
        // JPA
    }

    public ApproverRoleSetting(ApproverRole approverRole, String updatedBy, Instant updatedAt) {
        this.singletonKey = SINGLETON_KEY;
        this.approverRole = approverRole;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    /**
     * AC-miniloan-089: the change is the whole mechanism. Who set it and when are overwritten with
     * it, because BR-miniloan-039@v1 asks for the value in force and nothing asks for its history —
     * unlike ENT-013, where the assignment history is the point.
     */
    public void changeTo(ApproverRole approverRole, String updatedBy, Instant updatedAt) {
        this.approverRole = approverRole;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getSingletonKey() {
        return singletonKey;
    }

    /** Raw, and nullable — {@link #approverRoleValue()} is what a caller deciding on it should ask. */
    public ApproverRole getApproverRole() {
        return approverRole;
    }

    /** BR-miniloan-040@v1's question, asked so it cannot be answered by a stray null. */
    public Optional<ApproverRole> approverRoleValue() {
        return Optional.ofNullable(approverRole);
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
