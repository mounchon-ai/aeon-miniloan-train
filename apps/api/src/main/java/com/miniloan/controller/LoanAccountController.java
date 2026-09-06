package com.miniloan.controller;

import com.miniloan.controller.LoanApplicationController.ErrorResponse;
import com.miniloan.domain.LoanAccount;
import com.miniloan.service.LoanAccountScopeService;
import com.miniloan.service.LoanAccountScopeService.NotVisibleException;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API-013 (GET /loan-accounts) · API-023 (GET /loan-accounts/{id}) — the scope API-013 declares in
 * its own purpose: "เจ้าของบัญชีเห็นของตน, Operations เห็นเฉพาะที่ถูก assign" (BR-miniloan-033@v1 ·
 * BR-miniloan-054@v1 · ACL-020).
 *
 * <p><b>The list is the route that matters here.</b> AC-miniloan-137 says so outright — "ช่องโหว่
 * ของขอบเขตข้อมูลมักอยู่ที่การเรียกแบบรายการ ไม่ใช่รายบัญชี เพราะรายบัญชีมักมีการตรวจ แต่รายการมัก
 * ลืม" — so the scope is applied in the QUERY rather than to the rendering, and the criterion is
 * measured on the COUNT as well as on the contents: a list that merely contains the caller's own
 * account also passes when it contains everybody else's.
 *
 * <p>FE-miniloan-019 will extend this same surface with UC-miniloan-023's own criteria. Adding to a
 * route that exists is ordinary; what would not be ordinary is a second endpoint answering the same
 * API- id with a scope of its own.
 */
@RestController
@RequestMapping("/loan-accounts")
public class LoanAccountController {

    private final LoanAccountScopeService scopeService;

    public LoanAccountController(LoanAccountScopeService scopeService) {
        this.scopeService = scopeService;
    }

    @GetMapping
    public ResponseEntity<List<LoanAccountSummary>> list(HttpServletRequest httpRequest) {
        String role = (String) httpRequest.getAttribute(AuthTokenFilter.RESOLVED_ROLE_ATTRIBUTE);
        return ResponseEntity.ok(
                scopeService.visibleTo(role).stream().map(LoanAccountSummary::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanAccountSummary> detail(
            @PathVariable UUID id, HttpServletRequest httpRequest) {
        String role = (String) httpRequest.getAttribute(AuthTokenFilter.RESOLVED_ROLE_ATTRIBUTE);
        return ResponseEntity.ok(LoanAccountSummary.from(scopeService.visibleTo(role, id)));
    }

    /**
     * One sentence for "not yours" and for "not there", so the refusal never tells a caller that an
     * account they may not see exists — the same reasoning {@code RepaymentScheduleController} uses
     * on the schedule route.
     */
    @ExceptionHandler(NotVisibleException.class)
    public ResponseEntity<ErrorResponse> handleNotVisible(NotVisibleException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("LOAN_ACCOUNT_NOT_VISIBLE", ex.getMessage()));
    }

    /** API-023: "ข้อมูลสรุปของบัญชีสินเชื่อหนึ่งบัญชี (เงินต้น สถานะ วันที่เบิกจ่าย/ปิด)". */
    public record LoanAccountSummary(
            UUID accountNumber,
            UUID applicationId,
            String status,
            BigDecimal principalAmount,
            BigDecimal outstandingPrincipal,
            int termMonths,
            Instant disbursedAt,
            Instant closedAt,
            String closeReason) {

        static LoanAccountSummary from(LoanAccount account) {
            return new LoanAccountSummary(
                    account.getId(),
                    account.getApplicationId(),
                    account.getStatus().name(),
                    account.getPrincipalAmount(),
                    account.getOutstandingPrincipal(),
                    account.getTermMonths(),
                    account.getDisbursedAt(),
                    account.getClosedAt(),
                    account.getCloseReason() == null ? null : account.getCloseReason().name());
        }
    }
}
