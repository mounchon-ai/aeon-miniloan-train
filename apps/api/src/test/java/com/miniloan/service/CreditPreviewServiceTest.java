package com.miniloan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miniloan.service.CreditAssessmentService.LimitedBy;
import com.miniloan.service.CreditAssessmentService.MaxApprovable;
import com.miniloan.service.CreditPreviewService.PreviewNotPermittedException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * ประเมินวงเงินอนุมัติสูงสุดล่วงหน้า (UC-miniloan-028 · BR-miniloan-027@v1 · BR-miniloan-003@v1) —
 * the API half of AC-miniloan-117 and AC-miniloan-118, at the domain layer ACL-021 names alongside
 * the API.
 *
 * <p><b>The expected figures are GD-miniloan-004's rows, word for word.</b> Nothing is computed
 * here: the six rows below are the signed answer key for CALC-miniloan-003@v1, verified by aplus191
 * on 2026-09-01, and they are quoted rather than derived. They are the same six rows {@code
 * CreditAssessmentServiceTest$MaxApprovableGoldenRows} pins on the approval path — and that is the
 * measurement, not a duplication: AC-miniloan-117 says the preview must never disagree with the
 * number used at approval, so proving both against one answer key is how "ไม่มีวันไม่ตรงกัน"
 * becomes something a test can fail on.
 *
 * <p><b>What this file deliberately does not assert.</b> AC-miniloan-117's "หน้าจอเรียก API ทุกครั้ง"
 * and AC-miniloan-118's error message are browser behaviour; no Spring test can see whether a
 * request left a browser or what a page rendered when one failed. FE-miniloan-021 and
 * FE-miniloan-029 own those. What is provable here is that the number the API serves is the signed
 * one, so a screen that shows it cannot be showing a different answer from the approval path.
 */
class CreditPreviewServiceTest {

    private static final String APPLICANT = "ROLE-001";

    private final CreditPreviewService previewService = new CreditPreviewService();

    /**
     * GD-miniloan-004 — every column of every row, through the preview rather than through the
     * assessment. 200,000.00 is the tie where the formula and the cap agree and the answer key says
     * {@code formula}; 250,000.00 is the first row past it where the cap wins.
     */
    @ParameterizedTest(name = "[{index}] income {0}")
    @CsvSource({
        // monthly_income, formula_value,  cap,            limited_by, max_approvable
        "30000.00,         '150,000.00',   '1,000,000.00', formula,    '150,000.00'",
        "199999.00,        '999,995.00',   '1,000,000.00', formula,    '999,995.00'",
        "200000.00,        '1,000,000.00', '1,000,000.00', formula,    '1,000,000.00'",
        "250000.00,        '1,250,000.00', '1,000,000.00', cap,        '1,000,000.00'",
        "15000.00,         '75,000.00',    '1,000,000.00', formula,    '75,000.00'",
        "45678.33,         '228,391.65',   '1,000,000.00', formula,    '228,391.65'",
    })
    void thePreviewMatchesTheGoldenRow(
            BigDecimal monthlyIncome,
            String formulaValue,
            String cap,
            LimitedBy limitedBy,
            String maxApprovable) {
        MaxApprovable outcome = previewService.preview(APPLICANT, monthlyIncome);

        assertThat(CreditAssessmentService.moneyExact(outcome.formulaValue())).isEqualTo(formulaValue);
        assertThat(CreditAssessmentService.moneyExact(outcome.cap())).isEqualTo(cap);
        assertThat(outcome.limitedBy()).isEqualTo(limitedBy);
        assertThat(CreditAssessmentService.moneyExact(outcome.value())).isEqualTo(maxApprovable);
    }

    /**
     * AC-miniloan-117's last clause, measured directly: for each answer-key input the preview and the
     * approval path return the identical record. A second implementation that happened to agree on
     * the six rows today would still fail this the moment either side changed alone.
     */
    @ParameterizedTest(name = "[{index}] income {0}")
    @CsvSource({"30000.00", "199999.00", "200000.00", "250000.00", "15000.00", "45678.33"})
    void thePreviewAndTheApprovalPathAreTheSameAnswer(BigDecimal monthlyIncome) {
        assertThat(previewService.preview(APPLICANT, monthlyIncome))
                .isEqualTo(CreditAssessmentService.maxApprovableAmount(monthlyIncome));
    }

    /**
     * BR-miniloan-001@v1's ≥ 15,000 บาท floor is an eligibility rule and is not applied here — the
     * answer key's own 15,000.00 row proves the ceiling is defined at the floor, and refusing an
     * income below it would make the preview disagree with the approval path. 14,999.99 is one
     * satang under, and it answers with the ceiling rather than a refusal.
     */
    @Test
    void anIncomeBelowTheEligibilityFloorStillGetsACeiling() {
        MaxApprovable outcome = previewService.preview(APPLICANT, new BigDecimal("14999.99"));

        assertThat(CreditAssessmentService.moneyExact(outcome.value())).isEqualTo("74,999.95");
        assertThat(outcome.limitedBy()).isEqualTo(LimitedBy.formula);
    }

    /**
     * ACL-021 names ROLE-001 and rbac.json's default effect is deny, so no other role may ask — the
     * refusal is here at the domain half of {@code enforceAt: [api, domain]}, not only on the route.
     */
    @Test
    void aRoleWithNoEntryForThisUseCaseIsRefused() {
        for (String role : new String[] {"ROLE-002", "ROLE-003", "ROLE-004", "ROLE-005"}) {
            assertThatThrownBy(() -> previewService.preview(role, new BigDecimal("30000.00")))
                    .isInstanceOf(PreviewNotPermittedException.class);
        }
    }
}
