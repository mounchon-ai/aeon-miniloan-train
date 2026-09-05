package com.miniloan.service;

import com.miniloan.domain.CreditAssessment;
import com.miniloan.domain.IdempotencyKey;
import com.miniloan.domain.LoanApplication;
import com.miniloan.domain.LoanApplication.RangeViolation;
import com.miniloan.repository.CreditAssessmentRepository;
import com.miniloan.repository.IdempotencyKeyRepository;
import com.miniloan.repository.LoanApplicationRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ยื่นใบสมัคร (UC-miniloan-002 · API-003) and the automatic assessment it starts
 * (UC-miniloan-003). One command, in this order:
 *
 * <ol>
 *   <li>the application has to exist and belong to the caller (ACL-002, scope=own);
 *   <li>the SubmitApplication command is claimed in the database (BR-miniloan-043@v1) — a repeat
 *       fails here, on the unique constraint, before anything else is looked at;
 *   <li>every required field is present (BR-miniloan-007@v1) and the amount and tenor are inside
 *       BR-miniloan-004@v1's range — the two checks draft save deliberately skipped;
 *   <li>Draft → Submitted, edits locked (BR-miniloan-031@v2, applicant's own edge);
 *   <li>the assessment runs and is stored for every outcome (BR-miniloan-009@v1);
 *   <li>Band A or B moves on to UnderReview by itself (BR-miniloan-031@v2, System's edge). Band C
 *       stays Submitted — nothing here rejects an application, a person does that.
 * </ol>
 *
 * <p>Steps 3 onward can each refuse the command, and refusing rolls the whole transaction back,
 * including the claimed key. An applicant who submits an incomplete draft can fix it and submit
 * again; only a submission that actually succeeded leaves a key behind.
 */
@Service
public class LoanApplicationSubmitService {

    private final LoanApplicationRepository applications;
    private final CreditAssessmentRepository assessments;
    private final IdempotencyKeyRepository idempotencyKeys;
    private final CreditAssessmentService assessmentService;

    public LoanApplicationSubmitService(
            LoanApplicationRepository applications,
            CreditAssessmentRepository assessments,
            IdempotencyKeyRepository idempotencyKeys,
            CreditAssessmentService assessmentService) {
        this.applications = applications;
        this.assessments = assessments;
        this.idempotencyKeys = idempotencyKeys;
        this.assessmentService = assessmentService;
    }

    public record SubmitResult(LoanApplication application, CreditAssessment assessment) {}

    public static class ApplicationNotFoundException extends RuntimeException {
        public ApplicationNotFoundException(UUID id) {
            super("ไม่พบใบสมัคร " + id);
        }
    }

    /** AC-miniloan-030 — names every missing field, so one round of corrections is enough. */
    public static class IncompleteApplicationException extends RuntimeException {
        private final List<String> missingFields;

        public IncompleteApplicationException(List<String> missingFields) {
            super("ยื่นใบสมัครไม่ได้ — ยังกรอกไม่ครบ: " + String.join(", ", missingFields));
            this.missingFields = missingFields;
        }

        public List<String> getMissingFields() {
            return missingFields;
        }
    }

    /** BR-miniloan-004@v1, wired in here because draft save (BR-miniloan-008@v1) accepts anything. */
    public static class OutOfRangeException extends RuntimeException {
        private final String code;

        public OutOfRangeException(RangeViolation violation) {
            super(violation.message());
            this.code = violation.code();
        }

        public String getCode() {
            return code;
        }
    }

    /** AC-miniloan-133 — the repeat is refused out loud, not answered with the first result. */
    public static class DuplicateCommandException extends RuntimeException {
        public DuplicateCommandException(UUID id) {
            super("ใบสมัคร " + id + " ถูกยื่นไปแล้ว — คำสั่งยื่นซ้ำถูกปฏิเสธ");
        }
    }

    /** AC-miniloan-031: a submitted application is locked, whether the caller is a screen or curl. */
    public static class NotSubmittableException extends RuntimeException {
        public NotSubmittableException(LoanApplication.Status status) {
            super("ใบสมัครนี้อยู่สถานะ " + status + " แล้ว ยื่นไม่ได้");
        }
    }

    @Transactional
    public SubmitResult submit(UUID id, String applicantId) {
        LoanApplication application =
                applications.findByIdAndApplicantId(id, applicantId).orElseThrow(() -> new ApplicationNotFoundException(id));

        claimSubmitCommand(id);

        if (application.getStatus() != LoanApplication.Status.Draft) {
            throw new NotSubmittableException(application.getStatus());
        }

        List<String> missing = application.missingRequiredFields();
        if (!missing.isEmpty()) {
            throw new IncompleteApplicationException(missing);
        }

        requireInRange(LoanApplication.validateRequestedAmount(application.getRequestedAmount()));
        requireInRange(LoanApplication.validateRequestedTermMonths(application.getRequestedTermMonths()));

        application.submit();

        CreditAssessmentService.Outcome outcome = CreditAssessmentService.assess(application);
        CreditAssessment assessment = assessmentService.record(application, outcome);

        if (assessment.movesToUnderReview()) {
            application.moveToUnderReview();
        }

        return new SubmitResult(applications.save(application), assessment);
    }

    /** No ACL grants this role a view of an application — rbac.json's default effect is deny. */
    public static class ViewNotPermittedException extends RuntimeException {
        public ViewNotPermittedException() {
            super("บทบาทนี้ไม่มีสิทธิ์ดูรายละเอียดใบสมัคร");
        }
    }

    /**
     * ดูรายละเอียดใบสมัคร (API-004) — the assessment comes along when there is one, and is absent
     * on a draft (AC-miniloan-035).
     *
     * <p>Scope comes straight from rbac.json: the applicant sees their own (ACL-024, scope=own), the
     * supervisor sees all (ACL-031, scope=all). ACL-028 gives a Loan Officer scope=own, meaning the
     * applications assigned to them — {@code assignedLoanOfficerId} is BR-miniloan-032@v1's field and
     * FE-miniloan-006 is the unit that adds it, so that branch belongs there and is refused rather
     * than widened to "any application" here.
     */
    @Transactional(readOnly = true)
    public SubmitResult findDetail(UUID id, String role) {
        LoanApplication application =
                switch (role) {
                    case "ROLE-001" -> applications
                            .findByIdAndApplicantId(id, role)
                            .orElseThrow(() -> new ApplicationNotFoundException(id));
                    case "ROLE-003" -> applications.findById(id).orElseThrow(() -> new ApplicationNotFoundException(id));
                    default -> throw new ViewNotPermittedException();
                };
        return new SubmitResult(application, assessments.findByApplicationId(id).orElse(null));
    }

    /**
     * Writes the key and lets the database answer. Asking "is it already there?" first would be the
     * in-memory check AC-miniloan-134 rules out, and would still let two simultaneous calls both
     * through; {@code saveAndFlush} sends the INSERT now so the constraint decides.
     */
    private void claimSubmitCommand(UUID applicationId) {
        try {
            idempotencyKeys.saveAndFlush(
                    new IdempotencyKey(
                            IdempotencyKey.CommandType.SubmitApplication,
                            applicationId.toString(),
                            applicationId.toString()));
        } catch (DataIntegrityViolationException duplicate) {
            throw new DuplicateCommandException(applicationId);
        }
    }

    private static void requireInRange(Optional<RangeViolation> violation) {
        violation.ifPresent(
                found -> {
                    throw new OutOfRangeException(found);
                });
    }
}
