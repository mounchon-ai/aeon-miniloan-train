package com.miniloan.controller;

import com.miniloan.controller.LoanApplicationController.ErrorResponse;
import com.miniloan.domain.ClosedAccountAdjustment;
import com.miniloan.domain.ClosedAccountAdjustment.Status;
import com.miniloan.service.ClosedAccountAdjustmentDecisionService;
import com.miniloan.service.ClosedAccountAdjustmentDecisionService.AdjustedValueUnreadableException;
import com.miniloan.service.ClosedAccountAdjustmentDecisionService.AdjustmentAlreadyDecidedException;
import com.miniloan.service.ClosedAccountAdjustmentDecisionService.AdjustmentNotFoundException;
import com.miniloan.service.ClosedAccountAdjustmentDecisionService.DecisionResult;
import com.miniloan.service.ClosedAccountAdjustmentDecisionService.NotTheApproverException;
import com.miniloan.service.ClosedAccountAdjustmentDecisionService.PaymentNotFoundException;
import com.miniloan.service.ClosedAccountAdjustmentDecisionService.SelfApprovalRefusedException;
import com.miniloan.service.ClosedAccountAdjustmentService.AccountNotClosedException;
import com.miniloan.service.ClosedAccountAdjustmentService.ApproverRoleNotSetException;
import com.miniloan.service.ClosedAccountAdjustmentService.LoanAccountNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The adjustment request itself, once it has been filed — API-017, API-018, API-024 and API-025, all
 * four of which design roots at {@code /adjustments} rather than under the account.
 *
 * <p><b>Why this is a second controller.</b> {@link ClosedAccountAdjustmentController} owns
 * {@code /loan-accounts}, which is where API-016 and FE-miniloan-015's two refusal routes live.
 * Design put these four somewhere else, and moving that class's base path to make room would rewrite
 * three routes another unit has already proved. Two classes, two base paths, one aggregate.
 *
 * <p>ACL-016 is enforced in the service for all four routes, including the two reads: the queue and
 * the detail page are what the approver decides from, and {@code rbac.json} is default-deny, so a
 * second role reading them would be a permission this unit granted rather than one design did.
 */
@RestController
@RequestMapping("/adjustments")
public class AdjustmentController {

    private final ClosedAccountAdjustmentDecisionService decisions;

    public AdjustmentController(ClosedAccountAdjustmentDecisionService decisions) {
        this.decisions = decisions;
    }

    /** API-017 · AC-miniloan-076 — the second of the two steps, and the only one that changes data. */
    @PostMapping("/{id}/approve")
    public DecisionResponse approve(@PathVariable UUID id, HttpServletRequest httpRequest) {
        return DecisionResponse.from(decisions.approve(id, callerRole(httpRequest)));
    }

    /** API-018 · UC-miniloan-018's alternate flow — the old value stays. */
    @PostMapping("/{id}/reject")
    public DecisionResponse reject(@PathVariable UUID id, HttpServletRequest httpRequest) {
        return DecisionResponse.from(decisions.reject(id, callerRole(httpRequest)));
    }

    /**
     * API-024, declared as {@code GET /adjustments?status=Pending}. The parameter is kept so the
     * declared call works as written and defaults to the only value it may take — ACL-016's
     * condition is STM-miniloan-004 state Pending, so this route serves the QUEUE and not a history.
     * Asking for another status is refused rather than quietly answered: what a history of decided
     * adjustments looks like, and who may read it, is GAP-miniloan-010's question for design.
     */
    @GetMapping
    public List<AdjustmentView> list(
            @RequestParam(name = "status", defaultValue = "Pending") Status status,
            HttpServletRequest httpRequest) {
        if (status != Status.Pending) {
            throw new QueueIsPendingOnlyException(status);
        }
        return decisions.listPending(callerRole(httpRequest)).stream().map(AdjustmentView::of).toList();
    }

    /** ACL-016's condition, at the route — see {@link #list}. */
    public static class QueueIsPendingOnlyException extends RuntimeException {
        public QueueIsPendingOnlyException(Status asked) {
            super("รายการนี้แสดงเฉพาะคำขอที่รออนุมัติ — ขอสถานะ " + asked + " ไม่ได้");
        }
    }

    /** API-025 — one request, with both values side by side for the person deciding. */
    @GetMapping("/{id}")
    public AdjustmentView view(@PathVariable UUID id, HttpServletRequest httpRequest) {
        return AdjustmentView.of(decisions.view(callerRole(httpRequest), id));
    }

    private static String callerRole(HttpServletRequest httpRequest) {
        return (String) httpRequest.getAttribute(AuthTokenFilter.RESOLVED_ROLE_ATTRIBUTE);
    }

    // ── refusals ────────────────────────────────────────────────────────────

    /** ACL-016 · UC-miniloan-018's actor — the holder of the configured approver role, nobody else. */
    @ExceptionHandler(NotTheApproverException.class)
    public ResponseEntity<ErrorResponse> handleNotTheApprover(NotTheApproverException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("ADJUSTMENT_NOT_THE_APPROVER", ex.getMessage()));
    }

    /**
     * AC-miniloan-079 · BR-miniloan-025@v1 — the rule refuses at the API, not only on the screen, and
     * the request is still Pending when this is thrown.
     */
    @ExceptionHandler(SelfApprovalRefusedException.class)
    public ResponseEntity<ErrorResponse> handleSelfApproval(SelfApprovalRefusedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("ADJUSTMENT_SELF_APPROVAL_REFUSED", ex.getMessage()));
    }

    /** ACL-016's condition: STM-miniloan-004 state Pending. */
    @ExceptionHandler(AdjustmentAlreadyDecidedException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyDecided(AdjustmentAlreadyDecidedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("ADJUSTMENT_ALREADY_DECIDED", ex.getMessage()));
    }

    /** BR-miniloan-040@v1 — with no approver named there is nobody who may decide either. */
    @ExceptionHandler(ApproverRoleNotSetException.class)
    public ResponseEntity<ErrorResponse> handleApproverRoleNotSet(ApproverRoleNotSetException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("APPROVER_ROLE_NOT_SET", ex.getMessage()));
    }

    @ExceptionHandler(AccountNotClosedException.class)
    public ResponseEntity<ErrorResponse> handleNotClosed(AccountNotClosedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("LOAN_ACCOUNT_NOT_CLOSED", ex.getMessage()));
    }

    /** The text of {@code newValue} does not read as the field it is for — see the service comment. */
    @ExceptionHandler(AdjustedValueUnreadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(AdjustedValueUnreadableException ex) {
        return ResponseEntity.unprocessableEntity()
                .body(new ErrorResponse("ADJUSTMENT_VALUE_UNREADABLE", ex.getMessage()));
    }

    @ExceptionHandler(QueueIsPendingOnlyException.class)
    public ResponseEntity<ErrorResponse> handlePendingOnly(QueueIsPendingOnlyException ex) {
        return ResponseEntity.unprocessableEntity()
                .body(new ErrorResponse("ADJUSTMENT_QUEUE_PENDING_ONLY", ex.getMessage()));
    }

    @ExceptionHandler(AdjustmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(AdjustmentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("ADJUSTMENT_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotFound(PaymentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("PAYMENT_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(LoanAccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(LoanAccountNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("LOAN_ACCOUNT_NOT_FOUND", ex.getMessage()));
    }

    // ── wire shapes ─────────────────────────────────────────────────────────

    /**
     * ENT-010 as it stands, decided or not. {@code approvedBy} and {@code approvedAt} are null while
     * the request is Pending (AC-miniloan-085) and carry the four-eyes record once it is not —
     * the two values AC-miniloan-084 wants alongside the old one, the new one and the requester.
     */
    public record AdjustmentView(
            UUID id,
            UUID loanAccountId,
            String targetRecordId,
            String fieldName,
            String oldValue,
            String newValue,
            String requestedBy,
            Instant requestedAt,
            String status,
            String approvedBy,
            Instant approvedAt) {

        static AdjustmentView of(ClosedAccountAdjustment adjustment) {
            return new AdjustmentView(
                    adjustment.getId(),
                    adjustment.getLoanAccountId(),
                    adjustment.getTargetRecordId(),
                    adjustment.getFieldName().declaredName(),
                    adjustment.getOldValue(),
                    adjustment.getNewValue(),
                    adjustment.getRequestedBy(),
                    adjustment.getRequestedAt(),
                    adjustment.getStatus().name(),
                    adjustment.getApprovedBy(),
                    adjustment.getApprovedAt());
        }
    }

    /** The decided request plus the sentence the criterion shows the person who decided it. */
    public record DecisionResponse(AdjustmentView adjustment, String message) {

        static DecisionResponse from(DecisionResult result) {
            return new DecisionResponse(AdjustmentView.of(result.adjustment()), result.message());
        }
    }
}
