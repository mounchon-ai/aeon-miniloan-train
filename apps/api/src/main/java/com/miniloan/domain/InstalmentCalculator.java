package com.miniloan.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * ค่างวดรายเดือน (EMI) ตาม CALC-miniloan-001@v2 — เฉพาะ "ค่างวดที่แสดง" = round(EMI, 2).
 *
 * <p>This unit (FE-miniloan-005) needs exactly one number out of that contract: the instalment
 * CALC-miniloan-002@v1 feeds into DTI ("ไม่มีสูตรประมาณแยกอีกชุด" — one formula, not two). The
 * amortization table, its per-row interest/principal split and the last-row residual policy belong
 * to the schedule units (FE-miniloan-010/011) and are deliberately NOT written here.
 *
 * <p>{@code annualRate} is a parameter, not a constant, because CALC-miniloan-001@v2 defines a
 * behaviour at 0% and BR-miniloan-036@v1 makes the rate a versioned master value. No unit in the
 * build plan owns that rate-version table yet, so the 25%/yr of BR-miniloan-005@v1 lives at the
 * call site (see {@code CreditAssessmentService}).
 */
public final class InstalmentCalculator {

    /** Enough working precision that the 2-decimal result never depends on where r was truncated. */
    private static final MathContext WORKING = MathContext.DECIMAL128;

    private static final BigDecimal MONTHS_PER_YEAR = new BigDecimal("12");

    private InstalmentCalculator() {}

    /**
     * EMI = P × r × (1+r)^n / ((1+r)^n − 1), rounded HALF_UP to 2 decimals at the point it occurs
     * (BR-miniloan-035@v1). At r = 0 the denominator is zero and the contract switches to P / n.
     */
    public static BigDecimal monthlyInstalment(BigDecimal principal, BigDecimal annualRate, int termMonths) {
        BigDecimal monthlyRate = annualRate.divide(MONTHS_PER_YEAR, WORKING);
        BigDecimal term = new BigDecimal(termMonths);

        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(term, 2, RoundingMode.HALF_UP);
        }

        BigDecimal growth = BigDecimal.ONE.add(monthlyRate).pow(termMonths, WORKING);
        BigDecimal numerator = principal.multiply(monthlyRate, WORKING).multiply(growth, WORKING);
        BigDecimal denominator = growth.subtract(BigDecimal.ONE);
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }
}
