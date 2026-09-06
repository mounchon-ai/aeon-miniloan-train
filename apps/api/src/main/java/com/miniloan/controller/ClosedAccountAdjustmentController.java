package com.miniloan.controller;

import com.miniloan.controller.LoanApplicationController.ErrorResponse;
import com.miniloan.domain.ClosedAccountAdjustment;
import com.miniloan.service.ClosedAccountAdjustmentService;
import com.miniloan.service.ClosedAccountAdjustmentService.AccountDeletionRefusedException;
import com.miniloan.service.ClosedAccountAdjustmentService.AccountNotClosedException;
import com.miniloan.service.ClosedAccountAdjustmentService.AdjustmentFields;
import com.miniloan.service.ClosedAccountAdjustmentService.AdjustmentFieldsRequiredException;
import com.miniloan.service.ClosedAccountAdjustmentService.ApproverRoleNotSetException;
import com.miniloan.service.ClosedAccountAdjustmentService.DirectAccountEditRefusedException;
import com.miniloan.service.ClosedAccountAdjustmentService.LoanAccountNotFoundException;
import com.miniloan.service.ClosedAccountAdjustmentService.NotAssignedOperationsException;
import com.miniloan.service.ClosedAccountAdjustmentService.OperationsOnlyException;
import com.miniloan.service.ClosedAccountAdjustmentService.SubmitResult;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API-016 (POST /loan-accounts/{id}/adjustments) — ACL-015: the Operations person the account is
 * assigned to, on an account that is Closed, and nobody else.
 *
 * <p><b>Two more routes here answer to no API- id, and that is deliberate.</b> BR-miniloan-038@v1
 * says the only way to change a closed account is a request that gets approved, and AC-miniloan-077,
 * AC-miniloan-078 and AC-miniloan-081 each measure the refusal on BOTH sides — "ทั้งจากหน้าจอและ
 * ด้วยการเรียก API แก้ไขบัญชีโดยตรง". {@code interfaces.json} declares no endpoint for editing or
 * deleting a loan account, which would leave those three answered by a 404: the ABSENCE of a route,
 * not the ENFORCEMENT of a rule. BR-miniloan-025@v1 requires the rule itself at the API, so both are
 * declared, both refuse, and neither is listed under a borrowed API- ref in the manifest — the same
 * decision {@code RepaymentScheduleController} recorded for its two.
 *
 * <p>AC-miniloan-108 belongs to this use case too and is enforced where the schedule lives:
 * {@code RepaymentScheduleReissueService} refuses a Closed account before anything else, approved
 * adjustment or not (BR-miniloan-045@v1). Nothing here reopens that door, and no code in this unit
 * touches it.
 */
@RestController
@RequestMapping("/loan-accounts")
public class ClosedAccountAdjustmentController {

    private final ClosedAccountAdjustmentService adjustments;

    public ClosedAccountAdjustmentController(ClosedAccountAdjustmentService adjustments) {
        this.adjustments = adjustments;
    }

    /**
     * API-016 · AC-miniloan-076: the request is filed, it waits, and the account is untouched — the
     * response says which role it is waiting on because that is what the criterion shows the user.
     */
    @PostMapping("/{id}/adjustments")
    public ResponseEntity<AdjustmentResponse> submit(
            @PathVariable UUID id,
            @RequestBody AdjustmentRequest request,
            HttpServletRequest httpRequest) {
        String role = (String) httpRequest.getAttribute(AuthTokenFilter.RESOLVED_ROLE_ATTRIBUTE);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        AdjustmentResponse.from(
                                adjustments.submit(
                                        id,
                                        role,
                                        new AdjustmentFields(
                                                request.fieldName(), request.oldValue(), request.newValue()))));
    }

    /**
     * AC-miniloan-077 · AC-miniloan-081: editing the account itself is refused through the API
     * exactly as it is refused on the screen, the account keeps every value it had, and a request
     * already filed stays where it was.
     */
    @PatchMapping("/{id}")
    public void refuseDirectEdit(@PathVariable UUID id) {
        adjustments.refuseDirectAccountEdit();
    }

    /** AC-miniloan-078: the shortcut of deleting the account and rebuilding it is closed off too. */
    @DeleteMapping("/{id}")
    public void refuseDeletion(@PathVariable UUID id) {
        adjustments.refuseAccountDeletion();
    }

    // ── refusals ────────────────────────────────────────────────────────────

    /** ACL-015 · rbac.json is default-deny, so no other role reaches the filing route. */
    @ExceptionHandler(OperationsOnlyException.class)
    public ResponseEntity<ErrorResponse> handleOperationsOnly(OperationsOnlyException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("ADJUSTMENT_OPERATIONS_ONLY", ex.getMessage()));
    }

    /** BR-miniloan-054@v1 · ACL-015's {@code scope: own}. */
    @ExceptionHandler(NotAssignedOperationsException.class)
    public ResponseEntity<ErrorResponse> handleNotAssigned(NotAssignedOperationsException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("ACCOUNT_ASSIGNED_TO_ANOTHER", ex.getMessage()));
    }

    /** BR-miniloan-040@v1 · AC-miniloan-080 — refused at the filing step, with nothing created. */
    @ExceptionHandler(ApproverRoleNotSetException.class)
    public ResponseEntity<ErrorResponse> handleApproverRoleNotSet(ApproverRoleNotSetException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("APPROVER_ROLE_NOT_SET", ex.getMessage()));
    }

    /** AC-miniloan-077 · AC-miniloan-081 — the account is refused a direct edit, whoever asks. */
    @ExceptionHandler(DirectAccountEditRefusedException.class)
    public ResponseEntity<ErrorResponse> handleDirectEdit(DirectAccountEditRefusedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("LOAN_ACCOUNT_DIRECT_EDIT_REFUSED", ex.getMessage()));
    }

    /** AC-miniloan-078. */
    @ExceptionHandler(AccountDeletionRefusedException.class)
    public ResponseEntity<ErrorResponse> handleDeletion(AccountDeletionRefusedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("LOAN_ACCOUNT_DELETION_REFUSED", ex.getMessage()));
    }

    @ExceptionHandler(AccountNotClosedException.class)
    public ResponseEntity<ErrorResponse> handleNotClosed(AccountNotClosedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("LOAN_ACCOUNT_NOT_CLOSED", ex.getMessage()));
    }

    @ExceptionHandler(AdjustmentFieldsRequiredException.class)
    public ResponseEntity<ErrorResponse> handleFieldsRequired(AdjustmentFieldsRequiredException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("ADJUSTMENT_FIELDS_REQUIRED", ex.getMessage()));
    }

    @ExceptionHandler(LoanAccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(LoanAccountNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("LOAN_ACCOUNT_NOT_FOUND", ex.getMessage()));
    }

    // ── wire shapes ─────────────────────────────────────────────────────────

    /** ENT-010's three required attributes — one field per request, see the service comment. */
    public record AdjustmentRequest(String fieldName, String oldValue, String newValue) {}

    /**
     * AC-miniloan-085: an undecided request has no approver and no approval time, so both travel as
     * null rather than as a placeholder somebody could mistake for a decision.
     */
    public record AdjustmentResponse(
            UUID id,
            UUID loanAccountId,
            String fieldName,
            String oldValue,
            String newValue,
            String requestedBy,
            Instant requestedAt,
            String status,
            String approverRole,
            String message) {

        static AdjustmentResponse from(SubmitResult result) {
            ClosedAccountAdjustment filed = result.adjustment();
            return new AdjustmentResponse(
                    filed.getId(),
                    filed.getLoanAccountId(),
                    filed.getFieldName(),
                    filed.getOldValue(),
                    filed.getNewValue(),
                    filed.getRequestedBy(),
                    filed.getRequestedAt(),
                    filed.getStatus().name(),
                    result.approverRole().name(),
                    result.message());
        }
    }
}
