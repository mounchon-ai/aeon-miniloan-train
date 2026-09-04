package com.miniloan.controller;

import com.miniloan.domain.LoanApplication;
import com.miniloan.service.LoanApplicationDraftService;
import com.miniloan.service.LoanApplicationDraftService.DraftFields;
import com.miniloan.service.LoanApplicationDraftService.DraftNotEditableException;
import com.miniloan.service.LoanApplicationDraftService.DraftNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API-001 (POST /applications) · API-002 (PUT /applications/{id}) — ACL-001, ROLE-001 only. */
@RestController
@RequestMapping("/applications")
public class LoanApplicationController {

    private static final String ONLY_APPLICANT_CODE = "APPLICANT_ONLY";
    private static final String ONLY_APPLICANT_MESSAGE = "เฉพาะผู้สมัครเท่านั้นที่บันทึกร่างใบสมัครได้";

    private final LoanApplicationDraftService draftService;

    public LoanApplicationController(LoanApplicationDraftService draftService) {
        this.draftService = draftService;
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

    public record ErrorResponse(String code, String message) {}
}
