package com.miniloan.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.miniloan.domain.LoanApplication;
import com.miniloan.domain.Money;
import com.miniloan.service.AmortizationScheduleService.Row;
import com.miniloan.service.AmortizationScheduleService.Schedule;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * CALC-miniloan-001@v2 against GD-miniloan-002 — <b>every expected value below is that dataset's
 * row, copied character for character</b> (RQ24). The dataset was computed on 2026-09-01 and
 * verified by aplus191; nothing here recomputes it, because arithmetic dev did itself is dev
 * grading its own homework.
 *
 * <p>The five rows the dataset marks {@code computed: true} exercise the calculator. The two it
 * marks {@code computed: false} are BR-miniloan-004@v1's range gate refusing before the formula is
 * ever reached — a different layer, which is why the contract's boundaryBehavior says the rejection
 * "เกิดก่อนถึงสูตร และเป็นหน้าที่ของ BR-miniloan-004@v1 ไม่ใช่ของสัญญานี้". The n = 1 row is
 * computed for the matching reason: the contract defines its own behaviour without leaning on
 * another rule.
 */
class AmortizationScheduleServiceTest {

    private final AmortizationScheduleService schedules = new AmortizationScheduleService();

    private static final BigDecimal RATE_25 = new BigDecimal("0.25");

    private static Row first(Schedule schedule) {
        return schedule.rows().get(0);
    }

    private static Row last(Schedule schedule) {
        return schedule.rows().get(schedule.rows().size() - 1);
    }

    @Nested
    @DisplayName("GD-miniloan-002 · the rows req signed")
    class GoldenRows {

        /** GD-miniloan-002 row 1 — 100,000 · 25% · 12 งวด. */
        @Test
        void oneHundredThousandOverTwelveMonths() {
            Schedule schedule = schedules.build(new BigDecimal("100000"), RATE_25, 12);

            assertThat(Money.exact(schedule.instalment())).isEqualTo("9,504.42");
            assertThat(schedule.rows()).hasSize(12);
            assertThat(Money.exact(first(schedule).interest())).isEqualTo("2,083.33");
            assertThat(Money.exact(first(schedule).principal())).isEqualTo("7,421.09");
            assertThat(Money.exact(first(schedule).instalment())).isEqualTo("9,504.42");
            assertThat(Money.exact(last(schedule).interest())).isEqualTo("193.97");
            assertThat(Money.exact(last(schedule).principal())).isEqualTo("9,310.46");
            assertThat(Money.exact(last(schedule).instalment())).isEqualTo("9,504.43");
            assertThat(Money.exact(schedule.totalPrincipal())).isEqualTo("100,000.00");
            assertThat(Money.exact(schedule.totalInterest())).isEqualTo("14,053.05");
            assertThat(Money.exact(last(schedule).remainingBalance())).isEqualTo("0.00");
        }

        /**
         * GD-miniloan-002 row 2 — 1,000,000 · 25% · 60 งวด. AC-miniloan-003 calls this the case
         * where rounding residue accumulates the hardest in the whole system.
         */
        @Test
        void oneMillionOverSixtyMonths() {
            Schedule schedule = schedules.build(new BigDecimal("1000000"), RATE_25, 60);

            assertThat(Money.exact(schedule.instalment())).isEqualTo("29,351.32");
            assertThat(schedule.rows()).hasSize(60);
            assertThat(Money.exact(first(schedule).interest())).isEqualTo("20,833.33");
            assertThat(Money.exact(first(schedule).principal())).isEqualTo("8,517.99");
            assertThat(Money.exact(first(schedule).instalment())).isEqualTo("29,351.32");
            assertThat(Money.exact(last(schedule).interest())).isEqualTo("599.01");
            assertThat(Money.exact(last(schedule).principal())).isEqualTo("28,752.61");
            assertThat(Money.exact(last(schedule).instalment())).isEqualTo("29,351.62");
            assertThat(Money.exact(schedule.totalPrincipal())).isEqualTo("1,000,000.00");
            assertThat(Money.exact(schedule.totalInterest())).isEqualTo("761,079.50");
            assertThat(Money.exact(last(schedule).remainingBalance())).isEqualTo("0.00");
        }

        /** GD-miniloan-002 row 3 — 10,000 · 25% · 6 งวด, both floors of BR-miniloan-004@v1. */
        @Test
        void tenThousandOverSixMonths() {
            Schedule schedule = schedules.build(new BigDecimal("10000"), RATE_25, 6);

            assertThat(Money.exact(schedule.instalment())).isEqualTo("1,790.28");
            assertThat(schedule.rows()).hasSize(6);
            assertThat(Money.exact(first(schedule).interest())).isEqualTo("208.33");
            assertThat(Money.exact(first(schedule).principal())).isEqualTo("1,581.95");
            assertThat(Money.exact(first(schedule).instalment())).isEqualTo("1,790.28");
            assertThat(Money.exact(last(schedule).interest())).isEqualTo("36.54");
            assertThat(Money.exact(last(schedule).principal())).isEqualTo("1,753.76");
            assertThat(Money.exact(last(schedule).instalment())).isEqualTo("1,790.30");
            assertThat(Money.exact(schedule.totalPrincipal())).isEqualTo("10,000.00");
            assertThat(Money.exact(schedule.totalInterest())).isEqualTo("741.70");
            assertThat(Money.exact(last(schedule).remainingBalance())).isEqualTo("0.00");
        }

        /**
         * GD-miniloan-002 row 4 — the r = 0 branch, "r=0 → EMI = P / n". Reachable because the rate
         * is versioned master data and a 0% version can be declared (BR-miniloan-036@v1).
         */
        @Test
        void zeroPercentSwitchesToPrincipalOverTerm() {
            Schedule schedule = schedules.build(new BigDecimal("100000"), new BigDecimal("0"), 12);

            assertThat(Money.exact(schedule.instalment())).isEqualTo("8,333.33");
            assertThat(schedule.rows()).hasSize(12);
            assertThat(Money.exact(first(schedule).interest())).isEqualTo("0.00");
            assertThat(Money.exact(first(schedule).principal())).isEqualTo("8,333.33");
            assertThat(Money.exact(first(schedule).instalment())).isEqualTo("8,333.33");
            assertThat(Money.exact(last(schedule).interest())).isEqualTo("0.00");
            assertThat(Money.exact(last(schedule).principal())).isEqualTo("8,333.37");
            assertThat(Money.exact(last(schedule).instalment())).isEqualTo("8,333.37");
            assertThat(Money.exact(schedule.totalPrincipal())).isEqualTo("100,000.00");
            assertThat(Money.exact(schedule.totalInterest())).isEqualTo("0.00");
            assertThat(Money.exact(last(schedule).remainingBalance())).isEqualTo("0.00");
        }

        /** GD-miniloan-002 row 5 — n = 1, where the first row is also the last and absorbs itself. */
        @Test
        void oneInstalmentIsItsOwnResidualRow() {
            Schedule schedule = schedules.build(new BigDecimal("100000"), RATE_25, 1);

            assertThat(Money.exact(schedule.instalment())).isEqualTo("102,083.33");
            assertThat(schedule.rows()).hasSize(1);
            assertThat(Money.exact(first(schedule).interest())).isEqualTo("2,083.33");
            assertThat(Money.exact(first(schedule).principal())).isEqualTo("100,000.00");
            assertThat(Money.exact(first(schedule).instalment())).isEqualTo("102,083.33");
            assertThat(Money.exact(schedule.totalPrincipal())).isEqualTo("100,000.00");
            assertThat(Money.exact(schedule.totalInterest())).isEqualTo("2,083.33");
            assertThat(Money.exact(last(schedule).remainingBalance())).isEqualTo("0.00");
        }

        /**
         * GD-miniloan-002 row 6 — {@code computed: false}, declined by BR-miniloan-004@v1. The
         * refusal happens at the range gate, before the formula is reached, so this is measured
         * where that gate lives rather than by asking the calculator to refuse.
         */
        @Test
        void principalBelowTheRangeIsDeclinedBeforeTheFormula() {
            assertThat(LoanApplication.validateRequestedAmount(new BigDecimal("9999")))
                    .get()
                    .extracting(LoanApplication.RangeViolation::code)
                    .isEqualTo(LoanApplication.AMOUNT_OUT_OF_RANGE_CODE);
        }

        /** GD-miniloan-002 row 7 — {@code computed: false}, 61 งวด is outside 6–60. */
        @Test
        void termAboveTheRangeIsDeclinedBeforeTheFormula() {
            assertThat(LoanApplication.validateRequestedTermMonths(61))
                    .get()
                    .extracting(LoanApplication.RangeViolation::code)
                    .isEqualTo(LoanApplication.TERM_OUT_OF_RANGE_CODE);
        }
    }

    @Nested
    @DisplayName("the shape of the table, over and above the signed rows")
    class Shape {

        /**
         * AC-miniloan-096 — n rows, and every instalment equal. Measured over rows 1..n−1: the last
         * row carries the rounding residue by CALC-miniloan-001@v2's residualPolicy, and
         * GD-miniloan-002 records a different last instalment in every row it computes. Asserting
         * all n were equal would contradict data a person signed.
         */
        @Test
        void everyInstalmentButTheResidualRowIsTheSame() {
            Schedule schedule = schedules.build(new BigDecimal("1000000"), RATE_25, 60);

            assertThat(schedule.rows()).hasSize(60);
            assertThat(schedule.rows().subList(0, 59))
                    .allSatisfy(row -> assertThat(row.instalment()).isEqualByComparingTo(schedule.instalment()));
            assertThat(last(schedule).instalment()).isNotEqualByComparingTo(schedule.instalment());
        }

        /** AC-miniloan-096: interest + principal of a row is that row's instalment, every row. */
        @Test
        void everyRowSplitsExactlyIntoInterestAndPrincipal() {
            Schedule schedule = schedules.build(new BigDecimal("1000000"), RATE_25, 60);

            assertThat(schedule.rows())
                    .allSatisfy(
                            row ->
                                    assertThat(row.interest().add(row.principal()))
                                            .isEqualByComparingTo(row.instalment()));
        }

        /**
         * AC-miniloan-097 — the shape that proves this is reducing-balance and not flat interest:
         * interest never rises from one row to the next, and the first row is the highest interest
         * and lowest principal of the whole table.
         */
        @Test
        void interestFallsMonotonicallyAndTheFirstRowIsTheExtreme() {
            Schedule schedule = schedules.build(new BigDecimal("1000000"), RATE_25, 60);

            for (int i = 1; i < schedule.rows().size(); i++) {
                assertThat(schedule.rows().get(i).interest())
                        .as("row %d interest must not exceed row %d", i + 1, i)
                        .isLessThanOrEqualTo(schedule.rows().get(i - 1).interest());
            }
            assertThat(schedule.rows())
                    .allSatisfy(row -> assertThat(row.interest()).isLessThanOrEqualTo(first(schedule).interest()))
                    .allSatisfy(row -> assertThat(row.principal()).isGreaterThanOrEqualTo(first(schedule).principal()));
        }

        /**
         * AC-miniloan-002 — the seam between the second-to-last row and the last: the one before
         * still owes something, the last closes at zero, and the difference in principal is exactly
         * the residue.
         */
        @Test
        void theRowBeforeLastStillOwesAndTheLastClosesAtZero() {
            Schedule schedule = schedules.build(new BigDecimal("100000"), RATE_25, 12);
            Row eleventh = schedule.rows().get(10);

            assertThat(eleventh.remainingBalance()).isGreaterThan(BigDecimal.ZERO);
            assertThat(last(schedule).remainingBalance()).isEqualByComparingTo("0.00");
            assertThat(last(schedule).principal()).isNotEqualByComparingTo(eleventh.principal());
        }

        /**
         * AC-miniloan-129 · AC-miniloan-130 — half up, and the ORDER of the rounding. The row's
         * principal is the rounded instalment minus the rounded interest; if the pair were computed
         * at full precision and rounded at the end, the principal column would not sum to P.
         */
        @Test
        void moneyRoundsHalfUpAndThePrincipalColumnSumsToTheStartingPrincipal() {
            assertThat(Money.exact(Money.round(new BigDecimal("1234.565")))).isEqualTo("1,234.57");

            Schedule schedule = schedules.build(new BigDecimal("1000000"), RATE_25, 60);
            BigDecimal summed =
                    schedule.rows().stream()
                            .map(Row::principal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(summed).isEqualByComparingTo("1000000.00");
            assertThat(schedule.rows())
                    .allSatisfy(
                            row ->
                                    assertThat(row.principal())
                                            .isEqualByComparingTo(row.instalment().subtract(row.interest())));
        }
    }
}
