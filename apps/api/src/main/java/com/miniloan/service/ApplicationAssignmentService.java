package com.miniloan.service;

import com.miniloan.domain.ApplicationAssignment;
import com.miniloan.domain.LoanApplication;
import com.miniloan.repository.ApplicationAssignmentRepository;
import com.miniloan.repository.LoanApplicationRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * มอบหมายใบสมัครให้เจ้าหน้าที่สินเชื่อ (UC-miniloan-004 · API-006 · BR-miniloan-032@v1).
 *
 * <p>ACL-003 puts this in the supervisor's hands alone, over every application (scope=all), and only
 * while the application is UnderReview. Two rows are written per assignment and they answer
 * different questions: {@code LoanApplication.assignedLoanOfficerId} says who may act now, and an
 * {@link ApplicationAssignment} row says who was handed it and when.
 *
 * <p>Nothing in this unit lets an officer act on the application — approve and reject arrive with
 * FE-miniloan-007/008 and call {@link LoanApplication#requireAssignedTo(String)}, which is the rule
 * AC-miniloan-064/065/066 actually describe.
 */
@Service
public class ApplicationAssignmentService {

    private final LoanApplicationRepository applications;
    private final ApplicationAssignmentRepository assignments;

    public ApplicationAssignmentService(
            LoanApplicationRepository applications, ApplicationAssignmentRepository assignments) {
        this.applications = applications;
        this.assignments = assignments;
    }

    public record AssignResult(LoanApplication application, ApplicationAssignment assignment) {}

    public static class ApplicationNotFoundException extends RuntimeException {
        public ApplicationNotFoundException(UUID id) {
            super("ไม่พบใบสมัคร " + id);
        }
    }

    /** BR-miniloan-032@v1: the supervisor names one officer, so an empty name is not a command. */
    public static class LoanOfficerRequiredException extends RuntimeException {
        public LoanOfficerRequiredException() {
            super("มอบหมายไม่ได้ — ต้องระบุเจ้าหน้าที่สินเชื่อหนึ่งคน");
        }
    }

    @Transactional
    public AssignResult assign(UUID applicationId, String loanOfficerId, String assignedBy) {
        if (loanOfficerId == null || loanOfficerId.isBlank()) {
            throw new LoanOfficerRequiredException();
        }

        LoanApplication application =
                applications.findById(applicationId).orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        application.assignTo(loanOfficerId);

        ApplicationAssignment assignment =
                assignments.save(new ApplicationAssignment(applicationId, loanOfficerId, assignedBy));
        return new AssignResult(applications.save(application), assignment);
    }

    /** ประวัติการมอบหมาย (ENT-013) — every round, oldest first, nothing overwritten. */
    @Transactional(readOnly = true)
    public List<ApplicationAssignment> history(UUID applicationId) {
        return assignments.findByApplicationIdOrderByAssignedAtAsc(applicationId);
    }

    /**
     * The round in force, for AC-miniloan-064's "ผู้รับผิดชอบ: ก. (มอบหมายโดย … เมื่อ …)" line — the
     * officer is on the application itself, but who handed it over and when live only here.
     */
    @Transactional(readOnly = true)
    public Optional<ApplicationAssignment> latest(UUID applicationId) {
        List<ApplicationAssignment> rounds = history(applicationId);
        return rounds.isEmpty() ? Optional.empty() : Optional.of(rounds.get(rounds.size() - 1));
    }
}
