package com.miniloan.controller;

import com.miniloan.domain.ApplicationAssignment;
import com.miniloan.domain.LoanApplication;
import com.miniloan.service.ApplicationAssignmentService;
import com.miniloan.service.ApplicationAssignmentService.ApplicationNotFoundException;
import com.miniloan.service.ApplicationAssignmentService.AssignResult;
import com.miniloan.service.ApplicationAssignmentService.LoanOfficerRequiredException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API-006 (POST /applications/{id}/assign) — ACL-003: the supervisor (ROLE-003), scope=all, and only
 * while the application is UnderReview.
 *
 * <p>BR-miniloan-032@v1 refuses a Loan Officer here on purpose: an officer taking work off the queue
 * themselves is exactly what the rule forbids, so ROLE-002 gets a 403 from this route rather than a
 * hidden button (BR-miniloan-025@v1 — the refusal is the API's, not the screen's).
 */
@RestController
@RequestMapping("/applications")
public class ApplicationAssignmentController {

    private static final String ONLY_SUPERVISOR_CODE = "SUPERVISOR_ONLY";
    private static final String ONLY_SUPERVISOR_MESSAGE =
            "เฉพาะหัวหน้าเจ้าหน้าที่สินเชื่อเท่านั้นที่มอบหมายใบสมัครได้";

    private final ApplicationAssignmentService assignmentService;

    public ApplicationAssignmentController(ApplicationAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<AssignmentResponse> assign(
            @PathVariable UUID id, @RequestBody AssignRequest request, HttpServletRequest httpRequest) {
        String supervisorId = requireSupervisor(httpRequest);
        return ResponseEntity.ok(
                AssignmentResponse.from(assignmentService.assign(id, request.loanOfficerId(), supervisorId)));
    }

    private String requireSupervisor(HttpServletRequest httpRequest) {
        Object role = httpRequest.getAttribute(AuthTokenFilter.RESOLVED_ROLE_ATTRIBUTE);
        if (!"ROLE-003".equals(role)) {
            throw new ForbiddenRoleException();
        }
        // Mock auth has one demo identity per role (FE-miniloan-002), so the resolved role stands in
        // for "which supervisor" — the same stand-in FE-miniloan-004/005 made for the applicant.
        return (String) role;
    }

    @ExceptionHandler(ForbiddenRoleException.class)
    public ResponseEntity<LoanApplicationController.ErrorResponse> handleForbiddenRole() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new LoanApplicationController.ErrorResponse(ONLY_SUPERVISOR_CODE, ONLY_SUPERVISOR_MESSAGE));
    }

    @ExceptionHandler(ApplicationNotFoundException.class)
    public ResponseEntity<LoanApplicationController.ErrorResponse> handleNotFound(ApplicationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new LoanApplicationController.ErrorResponse("APPLICATION_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(LoanOfficerRequiredException.class)
    public ResponseEntity<LoanApplicationController.ErrorResponse> handleMissingOfficer(
            LoanOfficerRequiredException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new LoanApplicationController.ErrorResponse("LOAN_OFFICER_REQUIRED", ex.getMessage()));
    }

    /** AC-miniloan-066's "not yet assigned" is normal; assigning outside UnderReview is not. */
    @ExceptionHandler(LoanApplication.NotAssignableException.class)
    public ResponseEntity<LoanApplicationController.ErrorResponse> handleNotAssignable(
            LoanApplication.NotAssignableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new LoanApplicationController.ErrorResponse("APPLICATION_NOT_ASSIGNABLE", ex.getMessage()));
    }

    static class ForbiddenRoleException extends RuntimeException {}

    public record AssignRequest(String loanOfficerId) {}

    /** AC-miniloan-064: the application shows who holds it, who handed it over, and when. */
    public record AssignmentResponse(
            UUID applicationId,
            String status,
            String assignedLoanOfficerId,
            String assignedBy,
            Instant assignedAt) {

        static AssignmentResponse from(AssignResult result) {
            ApplicationAssignment assignment = result.assignment();
            return new AssignmentResponse(
                    result.application().getId(),
                    result.application().getStatus().name(),
                    result.application().getAssignedLoanOfficerId(),
                    assignment.getAssignedBy(),
                    assignment.getAssignedAt());
        }
    }
}
