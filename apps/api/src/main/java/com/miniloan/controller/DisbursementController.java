package com.miniloan.controller;

import com.miniloan.controller.LoanApplicationController.ErrorResponse;
import com.miniloan.domain.Installment;
import com.miniloan.domain.LoanApplication;
import com.miniloan.service.DisbursementService;
import com.miniloan.service.DisbursementService.AlreadyDisbursedException;
import com.miniloan.service.DisbursementService.ApplicationNotFoundException;
import com.miniloan.service.DisbursementService.Disbursement;
import com.miniloan.service.DisbursementService.DuplicateCommandException;
import com.miniloan.service.DisbursementService.NoEffectiveRateException;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API-010 (POST /applications/{id}/disburse) — ACL-008: the Loan Officer the application was
 * assigned to, and only from Approved.
 *
 * <p>It sits on its own controller rather than beside approve/reject/cancel because those three end
 * a review and this one starts a loan: everything it returns belongs to ENT-006/007/008, and the
 * units that read those back (FE-miniloan-012's API-012, FE-miniloan-013's API-014) will hang off
 * the same side of the model.
 *
 * <p>BR-miniloan-042@v1 · AC-miniloan-131: nothing here queues, waits or retries. A failure is a
 * failure the caller sees at once and re-issues by hand, and AC-miniloan-132's promise that the
 * re-issue cannot produce a second account is kept by the database, not by this method.
 */
@RestController
@RequestMapping("/applications")
public class DisbursementController {

    private static final String ONLY_LOAN_OFFICER_CODE = "LOAN_OFFICER_ONLY";
    private static final String ONLY_LOAN_OFFICER_MESSAGE = "ไม่มีสิทธิ์สั่งเบิกจ่าย";

    /** AC-miniloan-114's shape, said by the action that hit the shared guard. */
    private static final String ASSIGNED_TO_ANOTHER_MESSAGE = "เบิกจ่ายไม่ได้ — ใบสมัครนี้มอบหมายให้ผู้พิจารณาคนอื่น";

    private final DisbursementService disbursementService;

    public DisbursementController(DisbursementService disbursementService) {
        this.disbursementService = disbursementService;
    }

    @PostMapping("/{id}/disburse")
    public ResponseEntity<DisbursementResponse> disburse(
            @PathVariable UUID id, HttpServletRequest httpRequest) {
        String loanOfficerId = requireLoanOfficer(httpRequest);
        return ResponseEntity.ok(DisbursementResponse.from(disbursementService.disburse(id, loanOfficerId)));
    }

    private String requireLoanOfficer(HttpServletRequest httpRequest) {
        Object role = httpRequest.getAttribute(AuthTokenFilter.RESOLVED_ROLE_ATTRIBUTE);
        if (!"ROLE-002".equals(role)) {
            throw new ForbiddenRoleException();
        }
        return (String) role;
    }

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

    /** AC-miniloan-059 — approved first, and nothing was created. */
    @ExceptionHandler(LoanApplication.NotDisbursableException.class)
    public ResponseEntity<ErrorResponse> handleNotDisbursable(LoanApplication.NotDisbursableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("APPLICATION_NOT_DISBURSABLE", ex.getMessage()));
    }

    /** AC-miniloan-060 — one application, one account, and the first one says when. */
    @ExceptionHandler(AlreadyDisbursedException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyDisbursed(AlreadyDisbursedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("APPLICATION_ALREADY_DISBURSED", ex.getMessage()));
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

    @ExceptionHandler(NoEffectiveRateException.class)
    public ResponseEntity<ErrorResponse> handleNoEffectiveRate(NoEffectiveRateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("NO_EFFECTIVE_INTEREST_RATE", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateCommandException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateCommandException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("DUPLICATE_DISBURSE", ex.getMessage()));
    }

    static class ForbiddenRoleException extends RuntimeException {}

    /**
     * ENT-006 declares no account number, so the account's id is the number AC-miniloan-058 and
     * AC-miniloan-093 render — the web builds the sentence, this returns the parts.
     *
     * <p>{@code interestRateVersionId} and {@code interestRateEffectiveFrom} travel together for
     * AC-miniloan-104: the account refers to a version, and the screen has to be able to say WHICH
     * version rather than show a bare number.
     */
    public record DisbursementResponse(
            UUID accountNumber,
            String applicationStatus,
            String accountStatus,
            BigDecimal principalAmount,
            int termMonths,
            Instant disbursedAt,
            UUID interestRateVersionId,
            BigDecimal interestRateAnnualPercent,
            LocalDate interestRateEffectiveFrom,
            BigDecimal totalPrincipal,
            List<InstallmentResponse> installments) {

        static DisbursementResponse from(Disbursement disbursed) {
            return new DisbursementResponse(
                    disbursed.account().getId(),
                    LoanApplication.Status.Disbursed.name(),
                    disbursed.account().getStatus().name(),
                    disbursed.account().getPrincipalAmount(),
                    disbursed.account().getTermMonths(),
                    disbursed.account().getDisbursedAt(),
                    disbursed.rateVersion().getId(),
                    disbursed.rateVersion().getAnnualRatePercent(),
                    disbursed.rateVersion().getEffectiveFrom(),
                    disbursed.schedule().getTotalPrincipal(),
                    disbursed.installments().stream().map(InstallmentResponse::from).toList());
        }
    }

    public record InstallmentResponse(
            int number,
            LocalDate dueDate,
            BigDecimal emiAmount,
            BigDecimal interestPortion,
            BigDecimal principalPortion,
            BigDecimal remainingBalance,
            String status) {

        static InstallmentResponse from(Installment installment) {
            return new InstallmentResponse(
                    installment.getInstallmentNumber(),
                    installment.getDueDate(),
                    installment.getEmiAmount(),
                    installment.getInterestPortion(),
                    installment.getPrincipalPortion(),
                    installment.getRemainingBalance(),
                    installment.getStatus().name());
        }
    }
}
