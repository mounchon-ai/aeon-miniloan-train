package com.miniloan.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * ยอดปิดบัญชีก่อนกำหนด ตาม CALC-miniloan-004@v1 (constrains BR-miniloan-022@v1).
 *
 * <p>{@code EarlySettlementAmount = RemainingPrincipal + AccruedInterest + EarlySettlementFee} ·
 * {@code EarlySettlementFee = RemainingPrincipal × 1%} · {@code AccruedInterest = RemainingPrincipal
 * × AnnualRate × DaysElapsed / 365} · {@code DaysElapsed = ClosingDate − LastPaidDueDate} in real
 * days. The contract says in so many words that 1% and 365 are constants of the formula and not
 * inputs, so they are constants here and neither is a parameter.
 *
 * <p><b>Pure, and separate from where the numbers come from</b> — the precedent is
 * {@link InstalmentCalculator}. AC-miniloan-073 says its 50,000.00 is "ค่าที่ใช้ยืนยันสูตร … ไม่ใช่ค่าที่
 * ไล่มาจากตารางผ่อนจริงของบัญชีนี้", so the golden dataset feeds the four inputs directly and the
 * question of which row of which schedule supplies {@code remainingPrincipal} belongs to
 * {@code EarlyClosureQuoteService}, one layer up.
 *
 * <p><b>Where it rounds, and where it deliberately does not.</b> CALC-miniloan-004@v1's
 * roundingPoints names exactly two: the accrued-interest line and the fee line, each rounded once to
 * 2 places HALF_UP (BR-miniloan-035@v1). {@code remainingPrincipal} arrives already rounded from the
 * schedule's balance column and is read straight through — rounding it again would be a second
 * rounding of a number CALC-miniloan-001@v2 already settled. The total is not rounded a third time
 * either: three values that each carry two decimals sum to two decimals exactly.
 *
 * <p><b>A closing date before the last paid due date is refused before the formula</b>
 * (GD-miniloan-005 row 4, {@code computed: false}). The contract's boundaryBehavior is explicit that
 * the rejection happens ahead of the arithmetic — returning a negative accrued interest instead would
 * be an answer to a question nobody may ask.
 *
 * <p><b>A zero-day span is zero by definition, not by rounding</b> (AC-miniloan-074), and one day is
 * one day (AC-miniloan-075): the divisor is 365, never 360 and never 30 days a month, and nothing is
 * rounded up to a whole instalment or a whole month. A 0%/yr rate gives 0.00 for any span, because
 * the rate is in the numerator (CALC-miniloan-004@v1's last boundary).
 */
public final class EarlySettlementCalculator {

    private static final MathContext WORKING = MathContext.DECIMAL128;
    private static final BigDecimal FEE_RATE = new BigDecimal("0.01");
    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");

    private EarlySettlementCalculator() {}

    /**
     * GD-miniloan-005 row 4 — the input is refused before the formula is reached, and the sentence
     * names both dates because a caller who typed the wrong one has to see which.
     */
    public static class ClosingDateBeforeLastPaidDueDateException extends RuntimeException {
        public ClosingDateBeforeLastPaidDueDateException(LocalDate closingDate, LocalDate lastPaidDueDate) {
            super(
                    "วันที่ปิดบัญชี ("
                            + closingDate
                            + ") ก่อนวันครบกำหนดงวดล่าสุดที่ชำระแล้ว ("
                            + lastPaidDueDate
                            + ") — ป้อนย้อนหลัง คำนวณยอดปิดบัญชีไม่ได้");
        }
    }

    /**
     * The three lines AC-miniloan-073 requires a screen to show separately, plus the span they were
     * computed over. Kept as one record because the total is only meaningful beside its parts —
     * BR-miniloan-022@v1's whole point is that the borrower can see what they are paying for.
     */
    public record Payoff(
            BigDecimal remainingPrincipal,
            long daysElapsed,
            BigDecimal accruedInterest,
            BigDecimal earlySettlementFee,
            BigDecimal earlySettlementAmount) {}

    /**
     * @param remainingPrincipal money(2), read from the schedule's balance column per
     *     BR-miniloan-053@v1 — never recomputed from another formula
     * @param annualRate rate(10) as a FRACTION (0.25 = 25%/yr) — the version bound to the account at
     *     schedule time per BR-miniloan-036@v1 · BR-miniloan-037@v1, not today's master rate
     * @param lastPaidDueDate the due date of the latest settled instalment, or the disbursement date
     *     when nothing has been settled yet
     * @param closingDate the date the payoff is asked for
     */
    public static Payoff quote(
            BigDecimal remainingPrincipal,
            BigDecimal annualRate,
            LocalDate lastPaidDueDate,
            LocalDate closingDate) {

        if (closingDate.isBefore(lastPaidDueDate)) {
            throw new ClosingDateBeforeLastPaidDueDateException(closingDate, lastPaidDueDate);
        }

        long daysElapsed = ChronoUnit.DAYS.between(lastPaidDueDate, closingDate);

        BigDecimal accruedInterest =
                Money.round(
                        remainingPrincipal
                                .multiply(annualRate, WORKING)
                                .multiply(BigDecimal.valueOf(daysElapsed), WORKING)
                                .divide(DAYS_IN_YEAR, WORKING));
        BigDecimal fee = Money.round(remainingPrincipal.multiply(FEE_RATE, WORKING));

        return new Payoff(
                remainingPrincipal,
                daysElapsed,
                accruedInterest,
                fee,
                remainingPrincipal.add(accruedInterest).add(fee));
    }
}
