package com.miniloan.controller;

import com.miniloan.controller.LoanApplicationController.ErrorResponse;
import com.miniloan.domain.Payment;
import com.miniloan.service.PaymentService;
import com.miniloan.service.PaymentService.AccountClosedException;
import com.miniloan.service.PaymentService.AlreadyClosedException;
import com.miniloan.service.PaymentService.BoundRateVersionMissingException;
import com.miniloan.service.PaymentService.CloseOperationsOnlyException;
import com.miniloan.service.PaymentService.DirectCloseRefusedException;
import com.miniloan.service.PaymentService.DuplicateCommandException;
import com.miniloan.service.PaymentService.InstallmentNotDueException;
import com.miniloan.service.PaymentService.InstallmentNotFoundException;
import com.miniloan.service.PaymentService.LoanAccountNotFoundException;
import com.miniloan.service.PaymentService.NoCurrentScheduleException;
import com.miniloan.service.PaymentService.NotAssignedOperationsException;
import com.miniloan.service.PaymentService.OperationsOnlyException;
import com.miniloan.service.PaymentService.PartialPaymentException;
import com.miniloan.service.PaymentService.PaymentResult;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API-014 (POST /loan-accounts/{id}/payments) — ACL-011 · ACL-012 · ACL-020: the Operations person
 * the account was assigned to, while the account is Active and the instalment is Due, and nobody
 * else.
 *
 * <p><b>One route here answers to no API- id, and that is deliberate.</b> UC-miniloan-014's
 * exception flow, AC-miniloan-006, AC-miniloan-007 and AC-miniloan-136 all measure what happens when
 * somebody asks the system to CLOSE an account directly, and AC-miniloan-007 and AC-miniloan-136 say
 * in so many words that the refusal must happen "ทั้งจากหน้าจอและด้วยการเรียก API" / "ไม่ใช่แค่ไม่มี
 * ปุ่มบนหน้าจอ". {@code interfaces.json} declares no close endpoint and {@code rbac.json} — which is
 * default-deny — has no entry for UC-miniloan-014 at all, which is consistent: nobody may close an
 * account, because closing is something that HAPPENS, never something that is asked for. Leaving the
 * route out would answer those criteria with a 404 — the ABSENCE of a route, not the ENFORCEMENT of
 * a rule, and indistinguishable from a typo in the path — so {@code POST /{id}/close} is declared
 * and always refuses. It is recorded in the build report as a decision for the person reading the
 * diff, and it is NOT listed under a borrowed API- ref in the manifest. This is the same argument
 * {@code RepaymentScheduleController} makes for its two refusal routes, for the same reason.
 *
 * <p><b>Idempotency transport.</b> BR-miniloan-043@v1 requires a dedup key on every write and
 * ENT-012 answers DQ-miniloan-009 with the key's shape but not with what a RecordPayment puts in it;
 * API-014 declares no header and no body field. {@code Idempotency-Key} is therefore this unit's
 * decision, optional, with {@code <accountId>:<installmentNumber>} as the fallback — see
 * {@link PaymentService#record}.
 */
@RestController
@RequestMapping("/loan-accounts")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<PaymentResponse> recordPayment(
            @PathVariable UUID id,
            @RequestBody PaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest httpRequest) {
        String role = (String) httpRequest.getAttribute(AuthTokenFilter.RESOLVED_ROLE_ATTRIBUTE);
        return ResponseEntity.ok(
                PaymentResponse.from(
                        paymentService.record(
                                id, role, request.installmentNumber(), request.amount(), idempotencyKey)));
    }

    /**
     * AC-miniloan-006 · AC-miniloan-007 · AC-miniloan-136: the close command is refused through the
     * API exactly as it is refused on the screen, and the account keeps the status it had. This
     * method never returns normally — every path through {@link PaymentService#refuseDirectClose}
     * throws, and the handlers below turn each refusal into the sentence its criterion quotes.
     */
    @PostMapping("/{id}/close")
    public ResponseEntity<ErrorResponse> refuseDirectClose(
            @PathVariable UUID id, HttpServletRequest httpRequest) {
        String role = (String) httpRequest.getAttribute(AuthTokenFilter.RESOLVED_ROLE_ATTRIBUTE);
        paymentService.refuseDirectClose(id, role);
        throw new IllegalStateException("refuseDirectClose must always refuse");
    }

    /** AC-miniloan-070 — BR-miniloan-034@v1, refused at the API and not by a hidden button. */
    @ExceptionHandler(OperationsOnlyException.class)
    public ResponseEntity<ErrorResponse> handleOperationsOnly(OperationsOnlyException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("PAYMENT_OPERATIONS_ONLY", ex.getMessage()));
    }

    /** BR-miniloan-034@v1's closing half — its own sentence, see {@link PaymentService}. */
    @ExceptionHandler(CloseOperationsOnlyException.class)
    public ResponseEntity<ErrorResponse> handleCloseOperationsOnly(CloseOperationsOnlyException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("CLOSE_OPERATIONS_ONLY", ex.getMessage()));
    }

    /** AC-miniloan-136 — BR-miniloan-054@v1, among Operations who hold the very same role. */
    @ExceptionHandler(NotAssignedOperationsException.class)
    public ResponseEntity<ErrorResponse> handleNotAssigned(NotAssignedOperationsException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("ACCOUNT_ASSIGNED_TO_ANOTHER", ex.getMessage()));
    }

    /** AC-miniloan-088 — BR-miniloan-045@v1. */
    @ExceptionHandler(AccountClosedException.class)
    public ResponseEntity<ErrorResponse> handleAccountClosed(AccountClosedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("LOAN_ACCOUNT_CLOSED", ex.getMessage()));
    }

    /** AC-miniloan-012 · AC-miniloan-013 · AC-miniloan-014 — BR-miniloan-019@v1, no tolerance. */
    @ExceptionHandler(PartialPaymentException.class)
    public ResponseEntity<ErrorResponse> handlePartialPayment(PartialPaymentException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("PARTIAL_PAYMENT_REFUSED", ex.getMessage()));
    }

    /** AC-miniloan-006 — the direct close, refused with the count of instalments still owing. */
    @ExceptionHandler(DirectCloseRefusedException.class)
    public ResponseEntity<ErrorResponse> handleDirectClose(DirectCloseRefusedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("ACCOUNT_CLOSE_NOT_A_COMMAND", ex.getMessage()));
    }

    /** AC-miniloan-007 — Closed is a terminal state with no exit, so closing again is refused. */
    @ExceptionHandler(AlreadyClosedException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyClosed(AlreadyClosedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("LOAN_ACCOUNT_ALREADY_CLOSED", ex.getMessage()));
    }

    @ExceptionHandler(InstallmentNotDueException.class)
    public ResponseEntity<ErrorResponse> handleNotDue(InstallmentNotDueException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("INSTALLMENT_NOT_DUE", ex.getMessage()));
    }

    /** BR-miniloan-043@v1 — a repeat is turned down by name, never as a 500 and never silently. */
    @ExceptionHandler(DuplicateCommandException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateCommandException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("DUPLICATE_PAYMENT_COMMAND", ex.getMessage()));
    }

    @ExceptionHandler(LoanAccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(LoanAccountNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("LOAN_ACCOUNT_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InstallmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleInstallmentNotFound(InstallmentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("INSTALLMENT_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(NoCurrentScheduleException.class)
    public ResponseEntity<ErrorResponse> handleNoCurrentSchedule(NoCurrentScheduleException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("NO_CURRENT_SCHEDULE", ex.getMessage()));
    }

    @ExceptionHandler(BoundRateVersionMissingException.class)
    public ResponseEntity<ErrorResponse> handleBoundRateMissing(BoundRateVersionMissingException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("BOUND_RATE_VERSION_MISSING", ex.getMessage()));
    }

    /**
     * API-014 declares no request schema. The instalment number and the amount are the two things
     * UC-miniloan-012 and UC-miniloan-013 both name, and nothing else is asked for — the payment type
     * is DERIVED from the amount rather than declared by the caller, because BR-miniloan-019@v1 and
     * BR-miniloan-046@v2 decide it from the comparison, and a caller who could label its own payment
     * "overpayment" could label a short one that way too.
     */
    public record PaymentRequest(int installmentNumber, BigDecimal amount) {}

    /**
     * What the payment did, in figures.
     *
     * <p>AC-miniloan-086 renders "บันทึกการชำระงวดที่ 6 เรียบร้อย — ส่วนเกิน 20,000.00 บาท ·
     * ค่าธรรมเนียมการโปะ 200.00 บาท · ตัดเงินต้น 19,800.00 บาท", and AC-miniloan-013 renders
     * "บันทึกการชำระงวดที่ 3 เรียบร้อย". Those sentences belong to the screen (UI-miniloan-005 ·
     * ACL-025) and this unit reaches none, so what travels here is every NUMBER they are built from —
     * which is also the stronger thing to assert: AC-miniloan-086's point is that the principal fell
     * by 19,800.00 and not by 20,000.00. Refusals are the other way round: those ARE the API's
     * behaviour, so each carries the criterion's sentence word for word.
     */
    public record PaymentResponse(
            UUID paymentId,
            UUID accountNumber,
            String accountStatus,
            String closeReason,
            Instant closedAt,
            int installmentNumber,
            String installmentStatus,
            BigDecimal amount,
            String paymentType,
            BigDecimal overpaymentAmount,
            BigDecimal prepaymentFee,
            BigDecimal principalReduction,
            BigDecimal outstandingPrincipal,
            Integer reissuedRevisionNumber) {

        static PaymentResponse from(PaymentResult result) {
            Payment payment = result.payment();
            return new PaymentResponse(
                    payment.getId(),
                    result.account().getId(),
                    result.account().getStatus().name(),
                    result.account().getCloseReason() == null
                            ? null
                            : result.account().getCloseReason().name(),
                    result.account().getClosedAt(),
                    result.installment().getInstallmentNumber(),
                    result.installment().getStatus().name(),
                    payment.getAmount(),
                    payment.getPaymentType().name(),
                    payment.getOverpaymentAmount(),
                    payment.getPrepaymentFee(),
                    result.principalReduction(),
                    result.account().getOutstandingPrincipal(),
                    result.reissued().map(schedule -> schedule.getRevisionNumber()).orElse(null));
        }
    }
}
