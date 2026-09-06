package com.miniloan.controller;

import com.miniloan.controller.LoanApplicationController.ErrorResponse;
import com.miniloan.domain.LoanApplication;
import com.miniloan.service.LoanApplicationApprovalService;
import com.miniloan.service.LoanApplicationApprovalService.ApplicationNotFoundException;
import com.miniloan.service.LoanApplicationApprovalService.AssessmentMissingException;
import com.miniloan.service.LoanApplicationApprovalService.DuplicateCommandException;
import com.miniloan.service.LoanApplicationRejectionService;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API-007 (POST /applications/{id}/approve) — ACL-004 · API-008 (POST /applications/{id}/reject) —
 * ACL-005: the Loan Officer the application was assigned to, and only from UnderReview. The two
 * outcomes of one review sit on one controller because they share every guard and differ only in
 * what they record.
 *
 * <p>Every refusal here is the API's own (BR-miniloan-025@v1 · AC-miniloan-062 · AC-miniloan-113/114):
 * calling the route directly with curl gets exactly what pressing the button gets, and nothing is
 * left to the screen to enforce.
 *
 * <p><b>Which officer is calling.</b> The mock scheme resolves one identity per role
 * (FE-miniloan-002), so the demo Loan Officer's staff id is the resolved role, "ROLE-002" — the same
 * stand-in FE-miniloan-004/005 made for the applicant and FE-miniloan-006 for the supervisor. A
 * supervisor who assigns to any other staff id has assigned to someone who cannot sign in, and this
 * route then refuses the demo officer exactly as AC-miniloan-114 requires. That is enough to enforce
 * BR-miniloan-032@v1; it is not enough to demonstrate two officers signed in at once, and no unit in
 * the plan builds the ENT-004 roster that would be needed for it.
 */
@RestController
@RequestMapping("/applications")
public class LoanApplicationDecisionController {

    private static final String ONLY_LOAN_OFFICER_CODE = "LOAN_OFFICER_ONLY";
    private static final String ONLY_LOAN_OFFICER_MESSAGE = "ไม่มีสิทธิ์อนุมัติใบสมัคร";

    private static final String ONLY_LOAN_OFFICER_REJECT_MESSAGE = "ไม่มีสิทธิ์ปฏิเสธใบสมัคร";

    /** AC-miniloan-114's wording — the approve action names itself, the domain guard does not. */
    private static final String ASSIGNED_TO_ANOTHER_MESSAGE = "อนุมัติไม่ได้ — ใบสมัครนี้มอบหมายให้ผู้พิจารณาคนอื่น";

    /** The same refusal, named by the action that hit it — reject is not approve. */
    private static final String REJECT_ASSIGNED_TO_ANOTHER_MESSAGE =
            "ปฏิเสธไม่ได้ — ใบสมัครนี้มอบหมายให้ผู้พิจารณาคนอื่น";

    private final LoanApplicationApprovalService approvalService;
    private final LoanApplicationRejectionService rejectionService;

    public LoanApplicationDecisionController(
            LoanApplicationApprovalService approvalService,
            LoanApplicationRejectionService rejectionService) {
        this.approvalService = approvalService;
        this.rejectionService = rejectionService;
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApprovalResponse> approve(
            @PathVariable UUID id,
            @RequestBody(required = false) ApproveRequest request,
            HttpServletRequest httpRequest) {
        String loanOfficerId = requireLoanOfficer(httpRequest, ForbiddenRoleException::new);
        BigDecimal amount = request == null ? null : request.approvedAmount();
        return ResponseEntity.ok(ApprovalResponse.from(approvalService.approve(id, amount, loanOfficerId)));
    }

    /**
     * API-008 — ACL-005, the same officer and the same state as approve. AC-miniloan-057 is the
     * boundary the two routes share: once this one has run, the approve route refuses.
     *
     * <p>The one refusal that has to be re-phrased here is BR-miniloan-032@v1's: the domain guard is
     * shared with approve and opens with "ดำเนินการไม่ได้ — …", while the acceptance criteria give
     * each action its own opening. Catching it at the action is what keeps a caller who pressed
     * reject from being told they cannot approve.
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<RejectionResponse> reject(
            @PathVariable UUID id,
            @RequestBody(required = false) RejectRequest request,
            HttpServletRequest httpRequest) {
        String loanOfficerId = requireLoanOfficer(httpRequest, RejectForbiddenRoleException::new);
        String reason = request == null ? null : request.reason();
        try {
            return ResponseEntity.ok(
                    RejectionResponse.from(rejectionService.reject(id, reason, loanOfficerId)));
        } catch (LoanApplication.AssignedToAnotherOfficerException ex) {
            throw new RejectAssignedToAnotherException();
        }
    }

    private String requireLoanOfficer(
            HttpServletRequest httpRequest, Supplier<ForbiddenRoleException> refusal) {
        Object role = httpRequest.getAttribute(AuthTokenFilter.RESOLVED_ROLE_ATTRIBUTE);
        if (!"ROLE-002".equals(role)) {
            throw refusal.get();
        }
        return (String) role;
    }

    /** AC-miniloan-062: an applicant calling the route directly is refused here, not on a screen. */
    @ExceptionHandler(ForbiddenRoleException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenRole() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(ONLY_LOAN_OFFICER_CODE, ONLY_LOAN_OFFICER_MESSAGE));
    }

    @ExceptionHandler(ApplicationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ApplicationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("APPLICATION_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(LoanApplication.NotAssignedException.class)
    public ResponseEntity<ErrorResponse> handleNotAssigned(LoanApplication.NotAssignedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("APPLICATION_NOT_ASSIGNED", ex.getMessage()));
    }

    @ExceptionHandler(LoanApplication.AssignedToAnotherOfficerException.class)
    public ResponseEntity<ErrorResponse> handleAssignedToAnother() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("ASSIGNED_TO_ANOTHER_OFFICER", ASSIGNED_TO_ANOTHER_MESSAGE));
    }

    @ExceptionHandler(LoanApplication.NotApprovableException.class)
    public ResponseEntity<ErrorResponse> handleNotApprovable(LoanApplication.NotApprovableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("APPLICATION_NOT_APPROVABLE", ex.getMessage()));
    }

    @ExceptionHandler(LoanApplication.AmountExceedsMaxApprovableException.class)
    public ResponseEntity<ErrorResponse> handleAmountExceeded(
            LoanApplication.AmountExceedsMaxApprovableException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("AMOUNT_EXCEEDS_MAX_APPROVABLE", ex.getMessage()));
    }

    @ExceptionHandler(AssessmentMissingException.class)
    public ResponseEntity<ErrorResponse> handleAssessmentMissing(AssessmentMissingException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("ASSESSMENT_MISSING", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateCommandException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateCommandException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("DUPLICATE_APPROVE", ex.getMessage()));
    }

    /** Spring resolves the most specific handler, so reject's refusal names reject. */
    @ExceptionHandler(RejectForbiddenRoleException.class)
    public ResponseEntity<ErrorResponse> handleRejectForbiddenRole() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(ONLY_LOAN_OFFICER_CODE, ONLY_LOAN_OFFICER_REJECT_MESSAGE));
    }

    @ExceptionHandler(RejectAssignedToAnotherException.class)
    public ResponseEntity<ErrorResponse> handleRejectAssignedToAnother() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("ASSIGNED_TO_ANOTHER_OFFICER", REJECT_ASSIGNED_TO_ANOTHER_MESSAGE));
    }

    @ExceptionHandler(LoanApplication.NotRejectableException.class)
    public ResponseEntity<ErrorResponse> handleNotRejectable(LoanApplication.NotRejectableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("APPLICATION_NOT_REJECTABLE", ex.getMessage()));
    }

    /** AC-miniloan-056: the command is turned down and the application has not moved. */
    @ExceptionHandler(LoanApplication.RejectionReasonRequiredException.class)
    public ResponseEntity<ErrorResponse> handleReasonRequired(
            LoanApplication.RejectionReasonRequiredException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("REJECTION_REASON_REQUIRED", ex.getMessage()));
    }

    @ExceptionHandler(LoanApplicationRejectionService.ApplicationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRejectNotFound(
            LoanApplicationRejectionService.ApplicationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("APPLICATION_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(LoanApplicationRejectionService.DuplicateCommandException.class)
    public ResponseEntity<ErrorResponse> handleRejectDuplicate(
            LoanApplicationRejectionService.DuplicateCommandException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("DUPLICATE_REJECT", ex.getMessage()));
    }

    static class ForbiddenRoleException extends RuntimeException {}

    static class RejectForbiddenRoleException extends ForbiddenRoleException {}

    static class RejectAssignedToAnotherException extends RuntimeException {}

    /** API-007 carries no body in interfaces.json; the amount is optional and omitting it approves
     * at the amount that was requested (AC-miniloan-054 is the officer lowering it). */
    public record ApproveRequest(BigDecimal approvedAmount) {}

    /** API-008 declares no body in interfaces.json; BR-miniloan-013@v1 is what makes one required. */
    public record RejectRequest(String reason) {}

    public record RejectionResponse(
            UUID id, String status, String rejectionReason, String rejectedBy, Instant rejectedAt) {

        static RejectionResponse from(LoanApplication application) {
            return new RejectionResponse(
                    application.getId(),
                    application.getStatus().name(),
                    application.getRejectionReason(),
                    application.getRejectedBy(),
                    application.getRejectedAt());
        }
    }

    public record ApprovalResponse(
            UUID id, String status, BigDecimal approvedAmount, String approvedBy, Instant approvedAt) {

        static ApprovalResponse from(LoanApplication application) {
            return new ApprovalResponse(
                    application.getId(),
                    application.getStatus().name(),
                    application.getApprovedAmount(),
                    application.getApprovedBy(),
                    application.getApprovedAt());
        }
    }
}
