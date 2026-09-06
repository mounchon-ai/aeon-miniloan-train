package com.miniloan.controller;

import com.miniloan.controller.DisbursementController.InstallmentResponse;
import com.miniloan.controller.LoanApplicationController.ErrorResponse;
import com.miniloan.service.RepaymentScheduleReissueService;
import com.miniloan.service.RepaymentScheduleReissueService.AccountClosedException;
import com.miniloan.service.RepaymentScheduleReissueService.BoundRateVersionMissingException;
import com.miniloan.service.RepaymentScheduleReissueService.ConcurrentReissueException;
import com.miniloan.service.RepaymentScheduleReissueService.LoanAccountNotFoundException;
import com.miniloan.service.RepaymentScheduleReissueService.NoCurrentScheduleException;
import com.miniloan.service.RepaymentScheduleReissueService.NotAssignedOperationsException;
import com.miniloan.service.RepaymentScheduleReissueService.NothingLeftToRescheduleException;
import com.miniloan.service.RepaymentScheduleReissueService.Reissue;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API-011 (POST /loan-accounts/{id}/reschedule) — ACL-009: the Operations person the account is
 * assigned to, while the account is Active, and nobody else.
 *
 * <p><b>Two routes here answer to no API- id, and that is deliberate.</b> BR-miniloan-044@v1 names
 * the surface it fences — "แก้แถวในฉบับเดิมไม่ได้ทั้งจากหน้าจอและ API" — and AC-miniloan-009 and
 * AC-miniloan-010 each measure the refusal on BOTH sides. {@code interfaces.json} declares no
 * endpoint for editing an instalment or deleting a revision, which would leave those two criteria
 * answered by a 404: the ABSENCE of a route, not the ENFORCEMENT of a rule, and indistinguishable
 * from a typo in the path. BR-miniloan-025@v1 requires the rule itself to be enforced at the API, so
 * both are declared and both refuse, with the sentence the acceptance criterion quotes. They are
 * recorded in the build report as a decision for the person reading the diff, and they are NOT
 * listed under a borrowed API- ref in the manifest.
 *
 * <p>BR-miniloan-042@v1: nothing queues or retries. A second deliberate reissue is legitimate
 * (AC-miniloan-011 issues revision 3 over revision 2), so what the database refuses is two writes
 * racing for the SAME revision number, not a repeat.
 */
@RestController
@RequestMapping("/loan-accounts")
public class RepaymentScheduleController {

    /** FE-miniloan-002 mock scheme: one identity per role, so the Operations role is the person. */
    private static final String OPERATIONS = "ROLE-004";

    private static final String ONLY_OPERATIONS_CODE = "OPERATIONS_ONLY";
    private static final String ONLY_OPERATIONS_MESSAGE = "ไม่มีสิทธิ์ออกตารางผ่อนฉบับใหม่ทับ";

    /** AC-miniloan-009, word for word. */
    private static final String ROW_IMMUTABLE_MESSAGE =
            "แก้ตารางผ่อนรายงวดไม่ได้ — ถ้าต้องเปลี่ยน ให้ออกตารางผ่อนฉบับใหม่ทับทั้งฉบับ";

    /** AC-miniloan-010, word for word. */
    private static final String REVISION_RETAINED_MESSAGE =
            "ลบตารางผ่อนฉบับเก่าไม่ได้ — ฉบับที่ถูกแทนที่ต้องเก็บไว้ให้ดูย้อนหลังได้";

    private final RepaymentScheduleReissueService reissueService;

    public RepaymentScheduleController(RepaymentScheduleReissueService reissueService) {
        this.reissueService = reissueService;
    }

    @PostMapping("/{id}/reschedule")
    public ResponseEntity<RescheduleResponse> reschedule(
            @PathVariable UUID id, HttpServletRequest httpRequest) {
        String operationsId = requireOperations(httpRequest);
        return ResponseEntity.ok(RescheduleResponse.from(reissueService.reissue(id, operationsId)));
    }

    /**
     * AC-miniloan-009: editing one row of the revision in force is refused through the API exactly as
     * it is refused on the screen, and the row keeps every value it had.
     */
    @PatchMapping("/{id}/installments/{installmentNumber}")
    public ResponseEntity<ErrorResponse> refuseRowEdit(
            @PathVariable UUID id, @PathVariable int installmentNumber) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("SCHEDULE_ROW_IMMUTABLE", ROW_IMMUTABLE_MESSAGE));
    }

    /** AC-miniloan-010: a superseded revision is kept, so deleting one is refused. */
    @DeleteMapping("/{id}/schedules/{revisionNumber}")
    public ResponseEntity<ErrorResponse> refuseRevisionDelete(
            @PathVariable UUID id, @PathVariable int revisionNumber) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("SUPERSEDED_SCHEDULE_RETAINED", REVISION_RETAINED_MESSAGE));
    }

    private String requireOperations(HttpServletRequest httpRequest) {
        Object role = httpRequest.getAttribute(AuthTokenFilter.RESOLVED_ROLE_ATTRIBUTE);
        if (!OPERATIONS.equals(role)) {
            throw new ForbiddenRoleException();
        }
        return (String) role;
    }

    @ExceptionHandler(ForbiddenRoleException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenRole() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(ONLY_OPERATIONS_CODE, ONLY_OPERATIONS_MESSAGE));
    }

    @ExceptionHandler(LoanAccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(LoanAccountNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("LOAN_ACCOUNT_NOT_FOUND", ex.getMessage()));
    }

    /** AC-miniloan-107 · AC-miniloan-108 · AC-miniloan-109 — BR-miniloan-045@v1. */
    @ExceptionHandler(AccountClosedException.class)
    public ResponseEntity<ErrorResponse> handleAccountClosed(AccountClosedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("LOAN_ACCOUNT_CLOSED", ex.getMessage()));
    }

    @ExceptionHandler(NotAssignedOperationsException.class)
    public ResponseEntity<ErrorResponse> handleNotAssigned(NotAssignedOperationsException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("ACCOUNT_ASSIGNED_TO_ANOTHER", ex.getMessage()));
    }

    @ExceptionHandler(NoCurrentScheduleException.class)
    public ResponseEntity<ErrorResponse> handleNoCurrentSchedule(NoCurrentScheduleException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("NO_CURRENT_SCHEDULE", ex.getMessage()));
    }

    @ExceptionHandler(NothingLeftToRescheduleException.class)
    public ResponseEntity<ErrorResponse> handleNothingLeft(NothingLeftToRescheduleException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("NOTHING_LEFT_TO_RESCHEDULE", ex.getMessage()));
    }

    /** The loser of a race for the same revision number — named, not a 500. */
    @ExceptionHandler(ConcurrentReissueException.class)
    public ResponseEntity<ErrorResponse> handleConcurrentReissue(ConcurrentReissueException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("CONCURRENT_RESCHEDULE", ex.getMessage()));
    }

    @ExceptionHandler(BoundRateVersionMissingException.class)
    public ResponseEntity<ErrorResponse> handleBoundRateMissing(BoundRateVersionMissingException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("BOUND_RATE_VERSION_MISSING", ex.getMessage()));
    }

    static class ForbiddenRoleException extends RuntimeException {}

    /**
     * The revision that is now in force, its rows, and the list of every revision the account has
     * held. {@code InstallmentResponse} is DisbursementController's — one row shape, so a table never
     * reads two ways depending on which command produced it.
     */
    public record RescheduleResponse(
            UUID accountNumber,
            String accountStatus,
            int revisionNumber,
            Instant issuedAt,
            BigDecimal totalPrincipal,
            List<RevisionResponse> revisions,
            List<InstallmentResponse> installments) {

        static RescheduleResponse from(Reissue reissued) {
            return new RescheduleResponse(
                    reissued.account().getId(),
                    reissued.account().getStatus().name(),
                    reissued.schedule().getRevisionNumber(),
                    reissued.schedule().getIssuedAt(),
                    reissued.schedule().getTotalPrincipal(),
                    reissued.revisions().stream().map(RevisionResponse::from).toList(),
                    reissued.installments().stream().map(InstallmentResponse::from).toList());
        }
    }

    /**
     * AC-miniloan-008 renders "ฉบับที่ 1 — ถูกแทนที่เมื่อ {วันที่ออกฉบับใหม่}", so
     * {@code supersededAt} travels with the revision. It is derived from the next revision's issue
     * time rather than stored, because ENT-007 declares no such attribute.
     */
    public record RevisionResponse(
            int revisionNumber, Instant issuedAt, Instant supersededAt, boolean current, BigDecimal totalPrincipal) {

        static RevisionResponse from(RepaymentScheduleReissueService.Revision revision) {
            return new RevisionResponse(
                    revision.schedule().getRevisionNumber(),
                    revision.schedule().getIssuedAt(),
                    revision.supersededAt(),
                    revision.schedule().isCurrent(),
                    revision.schedule().getTotalPrincipal());
        }
    }
}
