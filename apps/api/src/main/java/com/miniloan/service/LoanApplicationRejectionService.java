package com.miniloan.service;

import com.miniloan.domain.IdempotencyKey;
import com.miniloan.domain.LoanApplication;
import com.miniloan.repository.IdempotencyKeyRepository;
import com.miniloan.repository.LoanApplicationRepository;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ปฏิเสธใบสมัคร (UC-miniloan-006 · API-008 · BR-miniloan-013@v1). The same three checks approval
 * runs, in the same order, minus the one that has no counterpart here:
 *
 * <ol>
 *   <li>state — only UnderReview has an edge to Rejected (STM-miniloan-001), and an application
 *       already rejected names the first refusal instead of claiming it never reached review;
 *   <li>assignment — BR-miniloan-032@v1, the same guard on the aggregate that approval calls
 *       (AC-miniloan-065 · AC-miniloan-066);
 *   <li>the reason — BR-miniloan-013@v1, checked inside {@link LoanApplication#reject} so nothing is
 *       written before it passes and AC-miniloan-056's "ยังเป็น UnderReview" holds.
 * </ol>
 *
 * <p>There is no ceiling to measure and therefore no CreditAssessment to load: a refusal does not
 * grant an amount. BR-miniloan-043@v1's key is still claimed, last, for the same reason approval
 * claims it last — the sequential repeat is already answered by the state guard with the better
 * message, and the key is what separates two calls arriving at the same instant.
 */
@Service
public class LoanApplicationRejectionService {

    private final LoanApplicationRepository applications;
    private final IdempotencyKeyRepository idempotencyKeys;

    public LoanApplicationRejectionService(
            LoanApplicationRepository applications, IdempotencyKeyRepository idempotencyKeys) {
        this.applications = applications;
        this.idempotencyKeys = idempotencyKeys;
    }

    public static class ApplicationNotFoundException extends RuntimeException {
        public ApplicationNotFoundException(UUID id) {
            super("ไม่พบใบสมัคร " + id);
        }
    }

    public static class DuplicateCommandException extends RuntimeException {
        public DuplicateCommandException(UUID id) {
            super("ใบสมัคร " + id + " ถูกปฏิเสธไปแล้ว — คำสั่งปฏิเสธซ้ำถูกปฏิเสธ");
        }
    }

    @Transactional
    public LoanApplication reject(UUID id, String reason, String loanOfficerId) {
        LoanApplication application =
                applications.findById(id).orElseThrow(() -> new ApplicationNotFoundException(id));

        if (application.getStatus() != LoanApplication.Status.UnderReview) {
            throw new LoanApplication.NotRejectableException(application);
        }
        application.requireAssignedTo(loanOfficerId);

        application.reject(reason, loanOfficerId);
        claimRejectCommand(id);

        return applications.save(application);
    }

    private void claimRejectCommand(UUID applicationId) {
        try {
            idempotencyKeys.saveAndFlush(
                    new IdempotencyKey(
                            IdempotencyKey.CommandType.RejectApplication,
                            applicationId.toString(),
                            applicationId.toString()));
        } catch (DataIntegrityViolationException duplicate) {
            throw new DuplicateCommandException(applicationId);
        }
    }
}
