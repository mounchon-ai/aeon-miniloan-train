package com.miniloan.service;

import com.miniloan.domain.Money;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * ตารางผ่อน (CALC-miniloan-001@v2 · BR-miniloan-016@v1 · BR-miniloan-017@v1 · BR-miniloan-035@v1).
 *
 * <p>A pure calculator: it reads nothing and writes nothing, so the arithmetic can be measured
 * against req's signed rows on its own (GD-miniloan-002). Who may disburse, which rate version
 * applies and when the instalments fall due are all somebody else's questions.
 *
 * <p><b>Rounding happens where it happens</b> (BR-miniloan-035@v1). There is no full-precision
 * ledger running alongside a display copy — the contract is explicit that one column exists and
 * every rule reads it:
 *
 * <ul>
 *   <li>{@code EMI = round(P × r × (1+r)^n / ((1+r)^n − 1), 2)}, once, for the whole table;
 *   <li>row interest {@code = round(balance before this row × r, 2)};
 *   <li>row principal {@code = EMI − that rounded interest} — subtracted, not rounded again
 *       (AC-miniloan-130 measures the ORDER, not the value);
 *   <li>balance after the row {@code = balance before − row principal}.
 * </ul>
 *
 * <p><b>The last row absorbs the residue</b> (CALC-miniloan-001@v2's residualPolicy): its principal
 * is {@code P − Σ principal of rows 1..n−1}, which makes the column sum exactly P and the closing
 * balance exactly 0 (BR-miniloan-017@v1). Its instalment is therefore its own interest plus that
 * residue and may differ from every other row by a satang or two — GD-miniloan-002 records exactly
 * that in every row it computes, so AC-miniloan-096's "equal every instalment" is measured over
 * rows 1..n−1 and the last row is the named exception, not a failure.
 *
 * <p><b>r = 0 is reachable</b>, because the rate is versioned master data (BR-miniloan-036@v1) and
 * nothing stops a 0% version from being declared. The EMI formula divides by zero there, so the
 * contract switches to {@code EMI = P / n} with no interest at all.
 *
 * <p><b>Range is not this class's job.</b> P outside 10,000–1,000,000 and n outside 6–60 are
 * BR-miniloan-004@v1's to refuse, before anything reaches here — which is why the contract defines
 * n = 1 anyway: "สัญญาต้องนิยามพฤติกรรมของตัวเองได้โดยไม่พึ่งกฎอื่น".
 */
@Service
public class AmortizationScheduleService {

    /** The contract divides the annual rate by 12 itself; callers pass the annual figure. */
    private static final BigDecimal MONTHS_PER_YEAR = new BigDecimal("12");

    /**
     * CALC-miniloan-001@v2 types {@code annual_rate} as {@code rate(10)}, and the monthly rate the
     * contract derives from it is carried at that same scale. This is not a detail: 0.25 / 12 does
     * not terminate, and how many digits survive the division decides where a row's interest lands
     * on a rounding tie. GD-miniloan-002's 60-instalment row settles it — ten places reproduce its
     * residual principal of 28,752.61 exactly, while full precision gives 28,752.64 and IEEE double
     * gives 28,752.67. The signed rows are the specification of this constant.
     */
    private static final int RATE_SCALE = 10;

    /**
     * Working precision for the power — NOT for money. Every baht figure leaves this class through
     * {@link Money#round}; this only keeps {@code (1+r)^n} from losing digits that would move the
     * rounded EMI.
     */
    private static final MathContext WORKING = MathContext.DECIMAL128;

    /** One row of the table, every figure already rounded (ENT-008). */
    public record Row(
            int number,
            BigDecimal instalment,
            BigDecimal interest,
            BigDecimal principal,
            BigDecimal remainingBalance) {}

    /**
     * @param instalment the EMI every row but the last carries — CALC-miniloan-001@v2's
     *     {@code instalment_shown}
     */
    public record Schedule(
            BigDecimal instalment, List<Row> rows, BigDecimal totalPrincipal, BigDecimal totalInterest) {}

    public Schedule build(BigDecimal principal, BigDecimal annualRate, int termMonths) {
        if (termMonths < 1) {
            throw new IllegalArgumentException("จำนวนงวดต้องอย่างน้อย 1 งวด");
        }
        BigDecimal monthlyRate = annualRate.divide(MONTHS_PER_YEAR, RATE_SCALE, RoundingMode.HALF_UP);
        BigDecimal instalment = emiOf(principal, monthlyRate, termMonths);

        List<Row> rows = new ArrayList<>(termMonths);
        BigDecimal balance = Money.round(principal);
        BigDecimal principalSoFar = BigDecimal.ZERO.setScale(2);
        BigDecimal interestSoFar = BigDecimal.ZERO.setScale(2);

        for (int number = 1; number <= termMonths; number++) {
            BigDecimal interest = Money.round(balance.multiply(monthlyRate, WORKING));
            BigDecimal rowPrincipal =
                    number < termMonths
                            ? instalment.subtract(interest)
                            : Money.round(principal).subtract(principalSoFar);
            balance = balance.subtract(rowPrincipal);
            principalSoFar = principalSoFar.add(rowPrincipal);
            interestSoFar = interestSoFar.add(interest);
            rows.add(new Row(number, interest.add(rowPrincipal), interest, rowPrincipal, balance));
        }

        return new Schedule(instalment, List.copyOf(rows), principalSoFar, interestSoFar);
    }

    /** BR-miniloan-016@v1, with the r = 0 branch CALC-miniloan-001@v2 names. */
    private static BigDecimal emiOf(BigDecimal principal, BigDecimal monthlyRate, int termMonths) {
        if (monthlyRate.signum() == 0) {
            return Money.round(principal.divide(new BigDecimal(termMonths), WORKING));
        }
        BigDecimal growth = BigDecimal.ONE.add(monthlyRate).pow(termMonths, WORKING);
        return Money.round(
                principal
                        .multiply(monthlyRate, WORKING)
                        .multiply(growth, WORKING)
                        .divide(growth.subtract(BigDecimal.ONE), WORKING));
    }
}
