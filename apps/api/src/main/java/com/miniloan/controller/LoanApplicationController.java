package com.miniloan.controller;

import com.miniloan.domain.CreditAssessment;
import com.miniloan.domain.LoanApplication;
import com.miniloan.service.CreditAssessmentService;
import com.miniloan.service.LoanApplicationDraftService;
import com.miniloan.service.LoanApplicationDraftService.DraftFields;
import com.miniloan.service.LoanApplicationDraftService.DraftNotEditableException;
import com.miniloan.service.LoanApplicationDraftService.DraftNotFoundException;
import com.miniloan.service.LoanApplicationSubmitService;
import com.miniloan.service.LoanApplicationSubmitService.ApplicationNotFoundException;
import com.miniloan.service.LoanApplicationSubmitService.DuplicateCommandException;
import com.miniloan.service.LoanApplicationSubmitService.IncompleteApplicationException;
import com.miniloan.service.LoanApplicationSubmitService.NotSubmittableException;
import com.miniloan.service.LoanApplicationSubmitService.OutOfRangeException;
import com.miniloan.service.LoanApplicationSubmitService.SubmitResult;
import com.miniloan.service.LoanApplicationSubmitService.ViewNotPermittedException;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API-001 (POST /applications) · API-002 (PUT /applications/{id}) — ACL-001, ROLE-001 only ·
 * API-003 (POST /applications/{id}/submit) — ACL-002, ROLE-001 own · API-004
 * (GET /applications/{id}) — ACL-024 own / ACL-031 all.
 *
 * <p>BR-miniloan-025@v1: the rules are refused here as well as on screen, so calling the endpoint
 * directly gets the same answer the UI would have given (AC-miniloan-031).
 *
 * <p>There is no endpoint for Submitted → UnderReview anywhere in this controller, and that is the
 * point of AC-miniloan-063 — the edge belongs to the System, so no role can be given a button for
 * something no route exposes.
 */
@RestController
@RequestMapping("/applications")
public class LoanApplicationController {

    private static final String ONLY_APPLICANT_CODE = "APPLICANT_ONLY";
    private static final String ONLY_APPLICANT_MESSAGE = "เฉพาะผู้สมัครเท่านั้นที่บันทึกร่างใบสมัครได้";

    private final LoanApplicationDraftService draftService;
    private final LoanApplicationSubmitService submitService;

    public LoanApplicationController(
            LoanApplicationDraftService draftService, LoanApplicationSubmitService submitService) {
        this.draftService = draftService;
        this.submitService = submitService;
    }

    @PostMapping
    public ResponseEntity<LoanApplicationResponse> saveDraft(
            @RequestBody DraftRequest request, HttpServletRequest httpRequest) {
        String applicantId = requireApplicant(httpRequest);
        LoanApplication saved = draftService.saveNewDraft(applicantId, request.toFields());
        return ResponseEntity.status(HttpStatus.CREATED).body(LoanApplicationResponse.from(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LoanApplicationResponse> updateDraft(
            @PathVariable UUID id, @RequestBody DraftRequest request, HttpServletRequest httpRequest) {
        String applicantId = requireApplicant(httpRequest);
        LoanApplication saved = draftService.updateDraft(id, applicantId, request.toFields());
        return ResponseEntity.ok(LoanApplicationResponse.from(saved));
    }

    /** API-003 — ACL-002: the owning applicant, and only from Draft. */
    @PostMapping("/{id}/submit")
    public ResponseEntity<ApplicationDetailResponse> submit(
            @PathVariable UUID id, HttpServletRequest httpRequest) {
        String applicantId = requireApplicant(httpRequest);
        return ResponseEntity.ok(ApplicationDetailResponse.from(submitService.submit(id, applicantId)));
    }

    /** API-004 — scope is decided per role in the service, from rbac.json. */
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationDetailResponse> detail(
            @PathVariable UUID id, HttpServletRequest httpRequest) {
        String role = (String) httpRequest.getAttribute(AuthTokenFilter.RESOLVED_ROLE_ATTRIBUTE);
        return ResponseEntity.ok(ApplicationDetailResponse.from(submitService.findDetail(id, role)));
    }

    private String requireApplicant(HttpServletRequest httpRequest) {
        Object role = httpRequest.getAttribute(AuthTokenFilter.RESOLVED_ROLE_ATTRIBUTE);
        if (!"ROLE-001".equals(role)) {
            throw new ForbiddenRoleException();
        }
        // Mock auth has one demo identity per role (FE-miniloan-002) — the resolved role stands
        // in for "which applicant" until a real identity system exists.
        return (String) role;
    }

    @ExceptionHandler(DraftNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(DraftNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("DRAFT_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(DraftNotEditableException.class)
    public ResponseEntity<ErrorResponse> handleNotEditable(DraftNotEditableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("DRAFT_NOT_EDITABLE", ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenRoleException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenRole() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(ONLY_APPLICANT_CODE, ONLY_APPLICANT_MESSAGE));
    }

    @ExceptionHandler(ApplicationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleApplicationNotFound(ApplicationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("APPLICATION_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ViewNotPermittedException.class)
    public ResponseEntity<ErrorResponse> handleViewNotPermitted(ViewNotPermittedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("APPLICATION_VIEW_FORBIDDEN", ex.getMessage()));
    }

    /** AC-miniloan-030: the missing fields ride along so the caller can mark all of them at once. */
    @ExceptionHandler(IncompleteApplicationException.class)
    public ResponseEntity<IncompleteResponse> handleIncomplete(IncompleteApplicationException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new IncompleteResponse("APPLICATION_INCOMPLETE", ex.getMessage(), ex.getMissingFields()));
    }

    @ExceptionHandler(OutOfRangeException.class)
    public ResponseEntity<ErrorResponse> handleOutOfRange(OutOfRangeException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
    }

    /** AC-miniloan-133: a visible refusal, not a silent replay of the first result. */
    @ExceptionHandler(DuplicateCommandException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateCommandException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("DUPLICATE_SUBMIT", ex.getMessage()));
    }

    @ExceptionHandler(NotSubmittableException.class)
    public ResponseEntity<ErrorResponse> handleNotSubmittable(NotSubmittableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("APPLICATION_NOT_SUBMITTABLE", ex.getMessage()));
    }

    static class ForbiddenRoleException extends RuntimeException {}

    public record DraftRequest(
            String fullName,
            Integer age,
            BigDecimal monthlyIncome,
            Integer currentEmploymentMonths,
            BigDecimal existingMonthlyDebt,
            BigDecimal requestedAmount,
            Integer requestedTermMonths) {

        DraftFields toFields() {
            return new DraftFields(
                    fullName,
                    age,
                    monthlyIncome,
                    currentEmploymentMonths,
                    existingMonthlyDebt,
                    requestedAmount,
                    requestedTermMonths);
        }
    }

    public record LoanApplicationResponse(
            UUID id,
            String status,
            String fullName,
            Integer age,
            BigDecimal monthlyIncome,
            Integer currentEmploymentMonths,
            BigDecimal existingMonthlyDebt,
            BigDecimal requestedAmount,
            Integer requestedTermMonths,
            Instant createdAt,
            Instant updatedAt) {

        static LoanApplicationResponse from(LoanApplication application) {
            return new LoanApplicationResponse(
                    application.getId(),
                    application.getStatus().name(),
                    application.getFullName(),
                    application.getAge(),
                    application.getMonthlyIncome(),
                    application.getCurrentEmploymentMonths(),
                    application.getExistingMonthlyDebt(),
                    application.getRequestedAmount(),
                    application.getRequestedTermMonths(),
                    application.getCreatedAt(),
                    application.getUpdatedAt());
        }
    }

    /**
     * ผลการประเมิน (ENT-003) as the client reads it. {@code reasons} carries every criterion's
     * verdict, passes included (AC-miniloan-034), and {@code dtiShown} is the display percentage —
     * the pass/fail it belongs to was already decided in baht, on the API side (BR-miniloan-025@v1).
     */
    public record CreditAssessmentResponse(
            String band,
            BigDecimal maxApprovableAmount,
            BigDecimal dtiRatio,
            String dtiShown,
            List<String> reasons,
            Instant assessedAt) {

        static CreditAssessmentResponse from(CreditAssessment assessment) {
            return new CreditAssessmentResponse(
                    assessment.getBand().name(),
                    assessment.getMaxApprovableAmount(),
                    assessment.getDtiRatio(),
                    CreditAssessmentService.percent(assessment.getDtiRatio()),
                    assessment.getReasonLines(),
                    assessment.getAssessedAt());
        }
    }

    /** AC-miniloan-035: a draft has no assessment, and the note says why rather than leaving a hole. */
    public static final String NO_ASSESSMENT_NOTE = "ยังไม่มีผลการประเมิน — ใบสมัครนี้ยังไม่ได้ยื่น";

    public record ApplicationDetailResponse(
            LoanApplicationResponse application, CreditAssessmentResponse assessment, String assessmentNote) {

        static ApplicationDetailResponse from(SubmitResult result) {
            boolean assessed = result.assessment() != null;
            return new ApplicationDetailResponse(
                    LoanApplicationResponse.from(result.application()),
                    assessed ? CreditAssessmentResponse.from(result.assessment()) : null,
                    assessed ? null : NO_ASSESSMENT_NOTE);
        }
    }

    public record ErrorResponse(String code, String message) {}

    public record IncompleteResponse(String code, String message, List<String> missingFields) {}
}
