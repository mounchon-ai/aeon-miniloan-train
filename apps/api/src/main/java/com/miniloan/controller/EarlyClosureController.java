package com.miniloan.controller;

import com.miniloan.controller.LoanApplicationController.ErrorResponse;
import com.miniloan.domain.EarlySettlementCalculator.ClosingDateBeforeLastPaidDueDateException;
import com.miniloan.domain.EarlySettlementCalculator.Payoff;
import com.miniloan.domain.Installment;
import com.miniloan.domain.Money;
import com.miniloan.service.EarlyClosureQuoteService;
import com.miniloan.service.EarlyClosureQuoteService.AccountNotActiveException;
import com.miniloan.service.EarlyClosureQuoteService.ApplicantOnlyException;
import com.miniloan.service.EarlyClosureQuoteService.BoundRateVersionMissingException;
import com.miniloan.service.EarlyClosureQuoteService.NoCurrentScheduleException;
import com.miniloan.service.EarlyClosureQuoteService.NotAccountOwnerException;
import com.miniloan.service.EarlyClosureSettlementService;
import com.miniloan.service.EarlyClosureSettlementService.DuplicateCommandException;
import com.miniloan.service.EarlyClosureSettlementService.NotAssignedOperationsException;
import com.miniloan.service.EarlyClosureSettlementService.OperationsOnlyException;
import com.miniloan.service.EarlyClosureSettlementService.PayoffAmountMismatchException;
import com.miniloan.service.EarlyClosureSettlementService.SettlementResult;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API-015 (GET /loan-accounts/{id}/payoff-quote) และ API-026
 * (POST /loan-accounts/{id}/payoff-settlement) — the two halves of closing early, and they answer to
 * two different permission rows on purpose.
 *
 * <p><b>ACL-013 guards the quote and ACL-014 guards the settlement</b>, so a route each: the
 * Applicant may ask what it would cost on their own account, and the assigned Operations may record
 * the payment that closes it. AC-miniloan-072 is the line between them, and it is enforced by the
 * quote route having no close in it at all rather than by hiding a button.
 *
 * <p><b>API-026 was declared by design before this unit was built</b> (ADR-005, after
 * GAP-miniloan-005): {@code interfaces.json} carried no endpoint for UC-miniloan-016 while ACL-014
 * demanded {@code enforceAt: [api, domain]}, so the api layer had nowhere to stand. Nothing here
 * borrows an API- id, and the route path is the one design wrote.
 *
 * <p><b>Every figure crosses the wire as a rendered money(2) string.</b> BR-miniloan-035@v1 keeps
 * money rounded at the point it occurs, and {@link Money#exact} is the same renderer req's golden
 * dataset writes its expected values in — one representation, so the number the client displays and
 * the number the dataset signed cannot drift apart.
 */
@RestController
@RequestMapping("/loan-accounts")
public class EarlyClosureController {

    private final EarlyClosureQuoteService quotes;
    private final EarlyClosureSettlementService settlements;

    public EarlyClosureController(
            EarlyClosureQuoteService quotes, EarlyClosureSettlementService settlements) {
        this.quotes = quotes;
        this.settlements = settlements;
    }

    /**
     * API-015. AC-miniloan-071: showing the figure changes nothing — no status moves and no payment
     * row is created, which is why this is a GET and why the service is {@code readOnly}.
     */
    @GetMapping("/{id}/payoff-quote")
    public ResponseEntity<PayoffQuoteResponse> quote(
            @PathVariable UUID id,
            @RequestParam("closingDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate closingDate,
            HttpServletRequest httpRequest) {
        String role = (String) httpRequest.getAttribute(AuthTokenFilter.RESOLVED_ROLE_ATTRIBUTE);
        return ResponseEntity.ok(
                PayoffQuoteResponse.from(quotes.quoteFor(id, role, closingDate), closingDate));
    }

    /** API-026. AC-miniloan-005: the account closes and the instalments still Due are retired. */
    @PostMapping("/{id}/payoff-settlement")
    public ResponseEntity<SettlementResponse> settle(
            @PathVariable UUID id,
            @RequestBody SettlementRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest httpRequest) {
        String role = (String) httpRequest.getAttribute(AuthTokenFilter.RESOLVED_ROLE_ATTRIBUTE);
        return ResponseEntity.ok(
                SettlementResponse.from(
                        settlements.settle(id, role, request.closingDate(), request.amount(), idempotencyKey)));
    }

    // ── refusals ────────────────────────────────────────────────────────────

    /** ACL-013 · rbac.json is default-deny, so no other role reaches the quote. */
    @ExceptionHandler(ApplicantOnlyException.class)
    public ResponseEntity<ErrorResponse> handleApplicantOnly(ApplicantOnlyException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("PAYOFF_QUOTE_APPLICANT_ONLY", ex.getMessage()));
    }

    /** ACL-014 — AC-miniloan-072's "ไม่มีสิทธิ์ปิดบัญชีสินเชื่อ" for anybody who is not Operations. */
    @ExceptionHandler(OperationsOnlyException.class)
    public ResponseEntity<ErrorResponse> handleOperationsOnly(OperationsOnlyException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("PAYOFF_SETTLEMENT_OPERATIONS_ONLY", ex.getMessage()));
    }

    /** BR-miniloan-054@v1 · AC-miniloan-136, among Operations who hold the very same role. */
    @ExceptionHandler(NotAssignedOperationsException.class)
    public ResponseEntity<ErrorResponse> handleNotAssigned(NotAssignedOperationsException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("ACCOUNT_ASSIGNED_TO_ANOTHER", ex.getMessage()));
    }

    /** BR-miniloan-033@v1 · ACL-013's {@code scope: own}. */
    @ExceptionHandler(NotAccountOwnerException.class)
    public ResponseEntity<ErrorResponse> handleNotOwner(NotAccountOwnerException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("LOAN_ACCOUNT_NOT_OWNED", ex.getMessage()));
    }

    @ExceptionHandler(AccountNotActiveException.class)
    public ResponseEntity<ErrorResponse> handleNotActive(AccountNotActiveException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("LOAN_ACCOUNT_CLOSED", ex.getMessage()));
    }

    @ExceptionHandler(EarlyClosureSettlementService.AccountClosedException.class)
    public ResponseEntity<ErrorResponse> handleAccountClosed(
            EarlyClosureSettlementService.AccountClosedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("LOAN_ACCOUNT_CLOSED", ex.getMessage()));
    }

    /** CALC-miniloan-004@v1's boundary — GD-miniloan-005 row 4, refused before the formula. */
    @ExceptionHandler(ClosingDateBeforeLastPaidDueDateException.class)
    public ResponseEntity<ErrorResponse> handleBackdated(ClosingDateBeforeLastPaidDueDateException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("PAYOFF_CLOSING_DATE_BACKDATED", ex.getMessage()));
    }

    /** Short and over are the same refusal, and both leave the account exactly as it was. */
    @ExceptionHandler(PayoffAmountMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMismatch(PayoffAmountMismatchException ex) {
        return ResponseEntity.unprocessableEntity()
                .body(new ErrorResponse("PAYOFF_AMOUNT_MISMATCH", ex.getMessage()));
    }

    /** BR-miniloan-043@v1 — surfaced immediately, and the caller re-issues it (REQ-miniloan-006). */
    @ExceptionHandler(DuplicateCommandException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateCommandException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("DUPLICATE_COMMAND", ex.getMessage()));
    }

    @ExceptionHandler(EarlyClosureQuoteService.LoanAccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleQuoteNotFound(
            EarlyClosureQuoteService.LoanAccountNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("LOAN_ACCOUNT_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(EarlyClosureSettlementService.LoanAccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSettlementNotFound(
            EarlyClosureSettlementService.LoanAccountNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("LOAN_ACCOUNT_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(NoCurrentScheduleException.class)
    public ResponseEntity<ErrorResponse> handleNoSchedule(NoCurrentScheduleException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("NO_CURRENT_SCHEDULE", ex.getMessage()));
    }

    @ExceptionHandler(BoundRateVersionMissingException.class)
    public ResponseEntity<ErrorResponse> handleRateMissing(BoundRateVersionMissingException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("RATE_VERSION_MISSING", ex.getMessage()));
    }

    // ── wire shapes ─────────────────────────────────────────────────────────

    public record SettlementRequest(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate closingDate, BigDecimal amount) {}

    /**
     * AC-miniloan-073 requires three lines and a total, and {@code daysElapsed} travels beside the
     * interest line because the criterion quotes it — "ดอกเบี้ยค้างจ่าย 10 วัน" is part of what the
     * borrower is shown, not a detail of the arithmetic.
     */
    public record PayoffQuoteResponse(
            LocalDate closingDate,
            String remainingPrincipal,
            long daysElapsed,
            String accruedInterest,
            String earlySettlementFee,
            String earlySettlementAmount) {

        static PayoffQuoteResponse from(Payoff payoff, LocalDate closingDate) {
            return new PayoffQuoteResponse(
                    closingDate,
                    Money.exact(payoff.remainingPrincipal()),
                    payoff.daysElapsed(),
                    Money.exact(payoff.accruedInterest()),
                    Money.exact(payoff.earlySettlementFee()),
                    Money.exact(payoff.earlySettlementAmount()));
        }
    }

    /** AC-miniloan-005: the status, when it closed, and which instalments were retired. */
    public record SettlementResponse(
            UUID paymentId,
            UUID loanAccountId,
            String status,
            String closeReason,
            Instant closedAt,
            String amountPaid,
            String earlySettlementFee,
            List<Integer> cancelledInstallments) {

        static SettlementResponse from(SettlementResult result) {
            return new SettlementResponse(
                    result.payment().getId(),
                    result.account().getId(),
                    result.account().getStatus().name(),
                    result.account().getCloseReason().name(),
                    result.account().getClosedAt(),
                    Money.exact(result.payment().getAmount()),
                    Money.exact(result.payoff().earlySettlementFee()),
                    result.cancelled().stream().map(Installment::getInstallmentNumber).toList());
        }
    }
}
