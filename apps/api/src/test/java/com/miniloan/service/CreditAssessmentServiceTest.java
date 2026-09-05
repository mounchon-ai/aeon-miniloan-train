package com.miniloan.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.miniloan.domain.CreditAssessment.Band;
import com.miniloan.domain.LoanApplication;
import com.miniloan.service.CreditAssessmentService.DtiOutcome;
import com.miniloan.service.CreditAssessmentService.LimitedBy;
import com.miniloan.service.CreditAssessmentService.MaxApprovable;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The expected values below are the rows of req's signed golden datasets, word for word — nothing
 * here is arithmetic this unit did for itself (RQ24):
 *
 * <ul>
 *   <li>GD-miniloan-003 proves CALC-miniloan-002@v1 / BR-miniloan-002@v1 (DTI), verified by aplus191
 *       on 2026-09-01;
 *   <li>GD-miniloan-004 proves CALC-miniloan-003@v1 / BR-miniloan-003@v1 (max approvable amount),
 *       verified by aplus191 on 2026-09-01.
 * </ul>
 *
 * <p>Band boundaries come from the acceptance criteria that pin them (AC-miniloan-041/042/043/044),
 * which is where BR-miniloan-006@v1's 50% line was decided.
 */
class CreditAssessmentServiceTest {

    /**
     * A complete application. The requested amount and tenor are GD-miniloan-003 row 1's inputs
     * (100,000 baht over 12 months) so the instalment folded into every DTI here is the golden
     * 9,504.42 rather than a figure this test invented.
     */
    private static LoanApplication application(
            int age, BigDecimal monthlyIncome, int employmentMonths, BigDecimal existingMonthlyDebt) {
        LoanApplication application = new LoanApplication("ROLE-001");
        application.applyDraftFields(
                "ทดสอบ ผู้สมัคร",
                age,
                monthlyIncome,
                employmentMonths,
                existingMonthlyDebt,
                new BigDecimal("100000"),
                12);
        return application;
    }

    @Nested
    @DisplayName("GD-miniloan-003 · CALC-miniloan-002@v1 — DTI")
    class DtiGoldenRows {

        @ParameterizedTest(name = "[{index}] debt {0} / income {1} / P {2} / n {3} / rate {4}")
        @CsvSource({
            // existing_debt, income,    principal, term, rate, new_instalment, total_debt, dti_shown, pass
            "10500.00,        30000.00,  100000,    12,   0.25, '9,504.42',     '20,004.42', '66.68%', true",
            "11495.58,        30000.00,  100000,    12,   0.25, '9,504.42',     '21,000.00', '70.00%', true",
            "11495.59,        30000.00,  100000,    12,   0.25, '9,504.42',     '21,000.01', '70.00%', false",
            "3000.00,         20000.00,  120000,    12,   0,    '10,000.00',    '13,000.00', '65.00%', true",
            "500.00,          15000.00,  10000,     6,    0.25, '1,790.28',     '2,290.28',  '15.27%', true",
        })
        void matchesTheGoldenRow(
                BigDecimal existingDebt,
                BigDecimal income,
                BigDecimal principal,
                int termMonths,
                BigDecimal annualRate,
                String newInstalment,
                String totalDebt,
                String dtiShown,
                boolean pass) {
            DtiOutcome outcome =
                    CreditAssessmentService.evaluateDti(existingDebt, income, principal, termMonths, annualRate);

            assertThat(CreditAssessmentService.moneyExact(outcome.newInstalment())).isEqualTo(newInstalment);
            assertThat(CreditAssessmentService.moneyExact(outcome.totalDebt())).isEqualTo(totalDebt);
            assertThat(CreditAssessmentService.percent(outcome.ratio())).isEqualTo(dtiShown);
            assertThat(outcome.passed()).isEqualTo(pass);
            // threshold_ratio is 0.70 on every row of GD-miniloan-003.
            assertThat(CreditAssessmentService.DTI_THRESHOLD_RATIO).isEqualByComparingTo("0.70");
        }

        /**
         * Rows 2 and 3 of the dataset show the same 70.00% and disagree on pass — the reason
         * CALC-miniloan-002@v1 decides in baht (AC-miniloan-038).
         */
        @org.junit.jupiter.api.Test
        void sameShownPercentageStillSplitsOnTheBahtAmount() {
            DtiOutcome onTheLine =
                    CreditAssessmentService.evaluateDti(
                            new BigDecimal("11495.58"),
                            new BigDecimal("30000.00"),
                            new BigDecimal("100000"),
                            12,
                            new BigDecimal("0.25"));
            DtiOutcome oneSatangOver =
                    CreditAssessmentService.evaluateDti(
                            new BigDecimal("11495.59"),
                            new BigDecimal("30000.00"),
                            new BigDecimal("100000"),
                            12,
                            new BigDecimal("0.25"));

            assertThat(CreditAssessmentService.percent(onTheLine.ratio()))
                    .isEqualTo(CreditAssessmentService.percent(oneSatangOver.ratio()));
            assertThat(onTheLine.passed()).isTrue();
            assertThat(oneSatangOver.passed()).isFalse();
        }
    }

    @Nested
    @DisplayName("GD-miniloan-004 · CALC-miniloan-003@v1 — วงเงินอนุมัติสูงสุด")
    class MaxApprovableGoldenRows {

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
        void matchesTheGoldenRow(
                BigDecimal monthlyIncome,
                String formulaValue,
                String cap,
                LimitedBy limitedBy,
                String maxApprovable) {
            MaxApprovable outcome = CreditAssessmentService.maxApprovableAmount(monthlyIncome);

            assertThat(CreditAssessmentService.moneyExact(outcome.formulaValue())).isEqualTo(formulaValue);
            assertThat(CreditAssessmentService.moneyExact(outcome.cap())).isEqualTo(cap);
            assertThat(outcome.limitedBy()).isEqualTo(limitedBy);
            assertThat(CreditAssessmentService.moneyExact(outcome.value())).isEqualTo(maxApprovable);
        }
    }

    @Nested
    @DisplayName("BR-miniloan-006@v1 — Credit Band")
    class Banding {

        /**
         * AC-miniloan-041/042/043: 50% falls on the A side, one baht past it is B, and past 70% the
         * application is C rather than B (AC-miniloan-038).
         */
        @ParameterizedTest(name = "[{index}] income {0} · total debt {1} → Band {2}")
        @CsvSource({
            "30000.00, 10500.00, A", // AC-miniloan-041 — DTI 35%
            "30000.00, 15000.00, A", // AC-miniloan-042 — 50% exactly, the boundary Q-miniloan-003 settled
            "30000.00, 15001.00, B", // AC-miniloan-043 — one baht over 50%
            "30000.00, 21000.00, B", // 70% exactly is still inside the DTI rule
            "30000.00, 21001.00, C", // AC-miniloan-038 — over 70% is C, not B
        })
        void followsTheDtiBoundaries(BigDecimal income, BigDecimal totalDebt, Band expected) {
            assertThat(CreditAssessmentService.band(true, totalDebt, income)).isEqualTo(expected);
        }

        /** AC-miniloan-044: one failed eligibility criterion is Band C however good the DTI is. */
        @org.junit.jupiter.api.Test
        void failedEligibilityIsBandCEvenWithALowDti() {
            var application = application(19, new BigDecimal("30000.00"), 24, new BigDecimal("1000.00"));

            var outcome = CreditAssessmentService.assess(application);

            assertThat(outcome.band()).isEqualTo(Band.C);
            assertThat(outcome.dti().passed()).isTrue();
            assertThat(outcome.reasons()).anyMatch(r -> r.contains("อายุ 19 ปี ✗"));
        }
    }

    @Nested
    @DisplayName("BR-miniloan-001@v1 — เกณฑ์คุณสมบัติ")
    class Eligibility {

        /** AC-miniloan-016/018: every bound is inclusive, the upper age bound included. */
        @ParameterizedTest(name = "[{index}] age {0} · income {1} · employment {2} → eligible {3}")
        @CsvSource({
            "35, 30000.00, 24, true",
            "20, 15000.00, 4,  true", // all three lower bounds hit exactly
            "60, 30000.00, 24, true", // the upper age bound, the one that runs the other way
            "61, 30000.00, 24, false",
            "19, 14999.00, 3,  false",
        })
        void boundsAreInclusive(int age, BigDecimal income, int employmentMonths, boolean eligible) {
            var application = application(age, income, employmentMonths, new BigDecimal("1000.00"));
            assertThat(CreditAssessmentService.assess(application).eligibilityPassed()).isEqualTo(eligible);
        }

        /** AC-miniloan-017: all three reasons, not just the first that failed. */
        @org.junit.jupiter.api.Test
        void reportsEveryFailedCriterionNotOnlyTheFirst() {
            var application = application(19, new BigDecimal("14999.00"), 3, new BigDecimal("1000.00"));

            var reasons = CreditAssessmentService.assess(application).reasons();

            assertThat(reasons).anyMatch(r -> r.contains("อายุ 19 ปี ✗ ต้องอยู่ระหว่าง 20–60 ปี"));
            assertThat(reasons).anyMatch(r -> r.contains("รายได้ 14,999 บาท/เดือน ✗ ต้องไม่น้อยกว่า 15,000 บาท"));
            assertThat(reasons).anyMatch(r -> r.contains("อายุงาน 3 เดือน ✗ ต้องไม่น้อยกว่า 4 เดือน"));
        }

        /** AC-miniloan-022: the record says the cap was the limit, not just what the number is. */
        @org.junit.jupiter.api.Test
        void namesTheCapAsTheLimitWhenItIsOne() {
            var application = application(35, new BigDecimal("250000.00"), 24, new BigDecimal("1000.00"));

            var reasons = CreditAssessmentService.assess(application).reasons();

            assertThat(reasons)
                    .anyMatch(r -> r.contains("ถูกจำกัดด้วยเพดาน 1,000,000 บาท ไม่ใช่ 5 เท่าของรายได้"));
        }
    }
}
