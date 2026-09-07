package com.miniloan.controller;

import com.miniloan.controller.LoanApplicationController.ErrorResponse;
import com.miniloan.service.CreditPreviewService;
import com.miniloan.service.CreditPreviewService.PreviewNotPermittedException;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API-021 (POST /credit-assessments/preview) — "คำนวณวงเงินอนุมัติสูงสุดจากรายได้ที่กรอกระหว่างกรอก
 * ใบสมัคร โดยยังไม่ต้องมีใบสมัครอยู่ในระบบ" (UC-miniloan-028 · BR-miniloan-027@v1 · ACL-021).
 *
 * <p><b>This unit delivers the API half of AC-miniloan-117 and AC-miniloan-118, and only that half.</b>
 * Both criteria are written about the browser — that a request goes out every time the income
 * changes, and that a failed call shows BR-miniloan-028@v1's message rather than a guessed number.
 * Neither can be asserted from a Spring test, and neither is possible at all without a route to
 * call: what is built here is the thing the screen must ask, and the absence of any second source
 * for the number is what makes "ไม่แสดงตัวเลขที่คำนวณเอง" enforceable. FE-miniloan-021 and
 * FE-miniloan-029 own the browser halves.
 *
 * <p><b>One number back, matching what the assessment record already exposes.</b> {@code
 * CreditAssessmentResponse} hands a client {@code maxApprovableAmount} and not the arm that limited
 * it; AC-miniloan-022's obligation to say which arm won belongs to the stored ENT-003 record, and
 * this preview stores nothing. UI-miniloan-001 has one field for this, "วงเงินอนุมัติสูงสุด
 * โดยประมาณ", so the response carries one value.
 *
 * <p>POST rather than GET because design wrote POST, and because the income is a body field the
 * applicant is still typing — not an identifier of anything.
 */
@RestController
@RequestMapping("/credit-assessments/preview")
public class CreditPreviewController {

    static final String INCOME_REQUIRED_CODE = "MONTHLY_INCOME_REQUIRED";
    static final String INCOME_REQUIRED_MESSAGE = "ต้องระบุรายได้ต่อเดือนเพื่อประเมินวงเงิน";

    private final CreditPreviewService previewService;

    public CreditPreviewController(CreditPreviewService previewService) {
        this.previewService = previewService;
    }

    @PostMapping
    public ResponseEntity<PreviewResponse> preview(
            @RequestBody PreviewRequest request, HttpServletRequest httpRequest) {
        String role = (String) httpRequest.getAttribute(AuthTokenFilter.RESOLVED_ROLE_ATTRIBUTE);
        if (request == null || request.monthlyIncome() == null) {
            throw new MonthlyIncomeRequiredException();
        }
        return ResponseEntity.ok(
                new PreviewResponse(previewService.preview(role, request.monthlyIncome()).value()));
    }

    /**
     * A request with no income is malformed, not a business decision — 400, and the refusal never
     * reaches the calculation. No range is checked here: BR-miniloan-001@v1's income floor is an
     * eligibility rule and applying it would make the preview disagree with the approval path, which
     * is what AC-miniloan-117 exists to prevent.
     */
    static class MonthlyIncomeRequiredException extends RuntimeException {}

    @ExceptionHandler(MonthlyIncomeRequiredException.class)
    public ResponseEntity<ErrorResponse> handleIncomeRequired() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(INCOME_REQUIRED_CODE, INCOME_REQUIRED_MESSAGE));
    }

    @ExceptionHandler(PreviewNotPermittedException.class)
    public ResponseEntity<ErrorResponse> handleNotPermitted(PreviewNotPermittedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("CREDIT_PREVIEW_FORBIDDEN", ex.getMessage()));
    }

    public record PreviewRequest(BigDecimal monthlyIncome) {}

    /** BR-miniloan-003@v1's answer, as UI-miniloan-001 renders it — one figure, already rounded. */
    public record PreviewResponse(BigDecimal maxApprovableAmount) {}
}
