package com.miniloan.service;

import com.miniloan.domain.CreditAssessment;
import com.miniloan.domain.IdempotencyKey;
import com.miniloan.domain.LoanApplication;
import com.miniloan.repository.CreditAssessmentRepository;
import com.miniloan.repository.IdempotencyKeyRepository;
import com.miniloan.repository.LoanApplicationRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * อนุมัติใบสมัคร (UC-miniloan-005 · UC-miniloan-021 · API-007). The checks run in the order the
 * acceptance criteria expect to be told about them:
 *
 * <ol>
 *   <li>state — an application still being assessed has not reached review (AC-miniloan-050), and
 *       one already approved says so and keeps its first approver (AC-miniloan-051);
 *   <li>assignment — BR-miniloan-032@v1, refused for an unassigned application (AC-miniloan-066)
 *       and for an officer it was not given to (AC-miniloan-114);
 *   <li>the ceiling — BR-miniloan-012@v1, measured against the maxApprovableAmount stored on the
 *       CreditAssessment at submission, not one recomputed now;
 *   <li>BR-miniloan-043@v1's key, claimed last so the sequential repeat still gets
 *       AC-miniloan-051's message while two simultaneous calls are still separated by the database.
 *       Submit claims its key first because AC-miniloan-133 asks for the opposite there.
 * </ol>
 *
 * <p>UC-miniloan-021 is the same command called straight at the API with no screen involved — there
 * is nothing here a browser could skip, which is what BR-miniloan-025@v1 asks for.
 */
@Service
public class LoanApplicationApprovalService {

    private final LoanApplicationRepository applications;
    private final CreditAssessmentRepository assessments;
    private final IdempotencyKeyRepository idempotencyKeys;

    public LoanApplicationApprovalService(
            LoanApplicationRepository applications,
            CreditAssessmentRepository assessments,
            IdempotencyKeyRepository idempotencyKeys) {
        this.applications = applications;
        this.assessments = assessments;
        this.idempotencyKeys = idempotencyKeys;
    }

    public static class ApplicationNotFoundException extends RuntimeException {
        public ApplicationNotFoundException(UUID id) {
            super("ไม่พบใบสมัคร " + id);
        }
    }

    /**
     * BR-miniloan-009@v1 writes one on every submission, so an application at UnderReview always has
     * one. Its absence would mean the row was made some other way, and there is then no ceiling to
     * measure BR-miniloan-012@v1 against — refusing beats guessing one.
     */
    public static class AssessmentMissingException extends RuntimeException {
        public AssessmentMissingException(UUID id) {
            super("อนุมัติไม่ได้ — ใบสมัคร " + id + " ไม่มีผลการประเมิน");
        }
    }

    public static class DuplicateCommandException extends RuntimeException {
        public DuplicateCommandException(UUID id) {
            super("ใบสมัคร " + id + " ถูกอนุมัติไปแล้ว — คำสั่งอนุมัติซ้ำถูกปฏิเสธ");
        }
    }

    @Transactional
    public LoanApplication approve(UUID id, BigDecimal approvedAmount, String loanOfficerId) {
        LoanApplication application =
                applications.findById(id).orElseThrow(() -> new ApplicationNotFoundException(id));

        if (application.getStatus() != LoanApplication.Status.UnderReview) {
            throw new LoanApplication.NotApprovableException(application);
        }
        application.requireAssignedTo(loanOfficerId);

        CreditAssessment assessment =
                assessments.findByApplicationId(id).orElseThrow(() -> new AssessmentMissingException(id));

        application.approve(approvedAmount, assessment.getMaxApprovableAmount(), loanOfficerId);
        claimApproveCommand(id);

        return applications.save(application);
    }

    private void claimApproveCommand(UUID applicationId) {
        try {
            idempotencyKeys.saveAndFlush(
                    new IdempotencyKey(
                            IdempotencyKey.CommandType.ApproveApplication,
                            applicationId.toString(),
                            applicationId.toString()));
        } catch (DataIntegrityViolationException duplicate) {
            throw new DuplicateCommandException(applicationId);
        }
    }
}
