package com.miniloan.controller;

import com.miniloan.controller.LoanApplicationController.ErrorResponse;
import com.miniloan.service.DashboardSummaryService;
import com.miniloan.service.DashboardSummaryService.DashboardNotPermittedException;
import com.miniloan.service.DashboardSummaryService.DashboardSummary;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API-020 (GET /dashboard) — "นับใบสมัครแยกตามสถานะและบัญชีสินเชื่อแยกตามสถานะสำหรับแสดงบนแดชบอร์ด"
 * (UC-miniloan-020 · BR-miniloan-024@v1 · ACL-018).
 *
 * <p>One read, five numbers, no parameter — API-020 declares none, and a range or a role filter
 * added on the way past would be a surface design never asked for. The role that reaches the service
 * is the one {@link AuthTokenFilter} resolved from the mock bearer token, exactly as every other
 * controller in this app reads it.
 *
 * <p>This is the {@code api} half of ACL-018's {@code enforceAt: [api, domain]}; the decision itself
 * lives in {@link DashboardSummaryService}, so a caller who reaches the numbers by another route
 * meets the same refusal. 403 rather than 404: the dashboard is a fixed resource whose existence is
 * not a secret — what is refused is this role's permission to read it.
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardSummaryService summaryService;

    public DashboardController(DashboardSummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping
    public ResponseEntity<DashboardSummary> summary(HttpServletRequest httpRequest) {
        String role = (String) httpRequest.getAttribute(AuthTokenFilter.RESOLVED_ROLE_ATTRIBUTE);
        return ResponseEntity.ok(summaryService.summaryFor(role));
    }

    @ExceptionHandler(DashboardNotPermittedException.class)
    public ResponseEntity<ErrorResponse> handleNotPermitted(DashboardNotPermittedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("DASHBOARD_NOT_PERMITTED", ex.getMessage()));
    }
}
