package com.miniloan.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * How a baht figure is written. Extracted from {@code CreditAssessmentService} when
 * FE-miniloan-007 needed the same formatting for a domain message (AC-miniloan-053 quotes both the
 * requested amount and the ceiling), and the Domain layer depends on nothing (DEC-001's layering).
 * One implementation, so a number never reads two ways in two places.
 *
 * <p>Grouping and decimal symbols are pinned to {@link Locale#US} so stored text never depends on
 * the JVM's default locale.
 */
public final class Money {

    private static final MathContext WORKING = MathContext.DECIMAL128;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private Money() {}

    /**
     * The prose form the acceptance criteria use — "30,000 บาท/เดือน", "150,001 บาท" — with the
     * satang dropped when there are none.
     */
    public static String format(BigDecimal amount) {
        BigDecimal scaled = round(amount);
        return render(scaled.stripTrailingZeros().scale() <= 0 ? "#,##0" : "#,##0.00", scaled);
    }

    /**
     * The money(2) form req's golden datasets use — "150,000.00" — always two decimals. A number and
     * the sentence it appears in are not obliged to look the same.
     */
    public static String exact(BigDecimal amount) {
        return render("#,##0.00", round(amount));
    }

    /** Display only — CALC-miniloan-002@v1 forbids deciding pass/fail on this value. */
    public static String percent(BigDecimal ratio) {
        return render("#,##0.00", ratio.multiply(HUNDRED, WORKING).setScale(2, RoundingMode.HALF_UP)) + "%";
    }

    /** Money is rounded round-half-up at the point it occurs (BR-miniloan-035@v1 · CLAUDE.md). */
    public static BigDecimal round(BigDecimal amount) {
        return amount == null ? null : amount.setScale(2, RoundingMode.HALF_UP);
    }

    private static String render(String pattern, BigDecimal value) {
        return new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.US)).format(value);
    }
}
