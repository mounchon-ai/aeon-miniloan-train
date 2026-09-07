package com.miniloan.controller;

import com.miniloan.controller.LoanApplicationController.ErrorResponse;
import com.miniloan.domain.Installment;
import com.miniloan.domain.LoanAccount;
import com.miniloan.domain.RepaymentSchedule;
import com.miniloan.repository.InstallmentRepository;
import com.miniloan.repository.RepaymentScheduleRepository;
import com.miniloan.service.LoanAccountScopeService;
import com.miniloan.service.LoanAccountScopeService.NotVisibleException;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final RepaymentScheduleRepository schedules;
    private final InstallmentRepository installments;

    public LoanAccountController(
            LoanAccountScopeService scopeService,
            RepaymentScheduleRepository schedules,
            InstallmentRepository installments) {
        this.scopeService = scopeService;
        this.schedules = schedules;
        this.installments = installments;
    }

    @GetMapping
    public ResponseEntity<List<LoanAccountSummary>> list(HttpServletRequest httpRequest) {
        String role = (String) httpRequest.getAttribute(AuthTokenFilter.RESOLVED_ROLE_ATTRIBUTE);
        List<LoanAccount> visible = scopeService.visibleTo(role);
        Map<UUID, Integer> nextDue = nextDueInstallments(visible);
        return ResponseEntity.ok(
                visible.stream()
                        .map(account -> LoanAccountSummary.from(account, nextDue.get(account.getId())))
                        .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanAccountSummary> detail(
            @PathVariable UUID id, HttpServletRequest httpRequest) {
        String role = (String) httpRequest.getAttribute(AuthTokenFilter.RESOLVED_ROLE_ATTRIBUTE);
        LoanAccount account = scopeService.visibleTo(role, id);
        return ResponseEntity.ok(
                LoanAccountSummary.from(account, nextDueInstallments(List.of(account)).get(id)));
    }

    /**
     * UI-miniloan-011's next-due-installment. Mock named the field in {@code conventions.json}
     * {@code fieldMap[]} (gate 45), so it is a declared control with no entity behind it, and
     * nothing on ENT-006 could answer it.
     *
     * <p><b>Two queries for a whole page, not two per row.</b> The same shape
     * {@code ScopedListingService#bandsFor} already uses for UI-miniloan-006's band: the current
     * schedule of every visible account, then the Due instalments of those schedules, then one map.
     * An account with nothing owing — every instalment paid, or a Closed account whose remaining
     * rows were cancelled by BR-miniloan-023@v1 — is simply absent from the map and travels as
     * null, which the screen renders as a dash. A zero would read as instalment number zero.
     */
    private Map<UUID, Integer> nextDueInstallments(List<LoanAccount> visible) {
        if (visible.isEmpty()) {
            return Map.of();
        }
        Map<UUID, UUID> accountOfSchedule = new HashMap<>();
        for (LoanAccount account : visible) {
            schedules
                    .findByLoanAccountIdAndCurrentIsTrue(account.getId())
                    .map(RepaymentSchedule::getId)
                    .ifPresent(scheduleId -> accountOfSchedule.put(scheduleId, account.getId()));
        }
        if (accountOfSchedule.isEmpty()) {
            return Map.of();
        }

        Map<UUID, Integer> nextDue = new HashMap<>();
        for (Installment row :
                installments.findByRepaymentScheduleIdInAndStatusOrderByInstallmentNumberAsc(
                        accountOfSchedule.keySet(), Installment.Status.Due)) {
            // The query is in instalment order, so the first row seen per account is the lowest.
            nextDue.putIfAbsent(
                    accountOfSchedule.get(row.getRepaymentScheduleId()), row.getInstallmentNumber());
        }
        return nextDue;
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
            String closeReason,
            Integer nextDueInstallmentNumber) {

        static LoanAccountSummary from(LoanAccount account, Integer nextDueInstallmentNumber) {
            return new LoanAccountSummary(
                    account.getId(),
                    account.getApplicationId(),
                    account.getStatus().name(),
                    account.getPrincipalAmount(),
                    account.getOutstandingPrincipal(),
                    account.getTermMonths(),
                    account.getDisbursedAt(),
                    account.getClosedAt(),
                    account.getCloseReason() == null ? null : account.getCloseReason().name(),
                    nextDueInstallmentNumber);
        }
    }
}
