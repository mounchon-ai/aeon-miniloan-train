package com.miniloan.service;

import com.miniloan.domain.CreditAssessment;
import com.miniloan.domain.CreditAssessment.Band;
import com.miniloan.domain.InstalmentCalculator;
import com.miniloan.domain.LoanApplication;
import com.miniloan.repository.CreditAssessmentRepository;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * การประเมินอัตโนมัติ (UC-miniloan-003 · BR-miniloan-009@v1) — runs the instant an application is
 * submitted and never on a draft (AC-miniloan-035: the assessment is bound to submission, not to
 * saving).
 *
 * <p>Three rules produce the record, and each keeps its own contract's boundary:
 *
 * <ul>
 *   <li>BR-miniloan-001@v1 — age 20–60, income ≥ 15,000, employment ≥ 4 months, all bounds
 *       inclusive, and <em>every</em> criterion reported, not just the first that failed.
 *   <li>BR-miniloan-002@v1 / CALC-miniloan-002@v1 — DTI decided in baht against
 *       income × 0.70, never against the rounded percentage.
 *   <li>BR-miniloan-003@v1 / CALC-miniloan-003@v1 — MIN(5 × income, 1,000,000), and the record says
 *       which of the two limited it.
 * </ul>
 */
@Service
public class CreditAssessmentService {

    public static final int MIN_AGE = 20;
    public static final int MAX_AGE = 60;
    public static final BigDecimal MIN_MONTHLY_INCOME = new BigDecimal("15000.00");
    public static final int MIN_EMPLOYMENT_MONTHS = 4;

    /** BR-miniloan-003@v1 — 5 × income, capped at 1,000,000 baht. */
    public static final BigDecimal INCOME_MULTIPLIER = new BigDecimal("5");

    public static final BigDecimal MAX_APPROVABLE_CAP = new BigDecimal("1000000.00");

    /** BR-miniloan-002@v1 (≤ 70%) and the Band A/B split of BR-miniloan-006@v1 (≤ 50%). */
    public static final BigDecimal DTI_THRESHOLD_RATIO = new BigDecimal("0.70");

    public static final BigDecimal BAND_A_DTI_RATIO = new BigDecimal("0.50");

    /**
     * BR-miniloan-005@v1 — 25% per year, reducing balance. BR-miniloan-036@v1 makes this a versioned
     * master value with an effective date, but no unit in the build plan owns that table yet, so the
     * declared rate stands in as a constant here rather than being invented as a half-built feature.
     */
    public static final BigDecimal ANNUAL_INTEREST_RATE = new BigDecimal("0.25");

    private static final MathContext WORKING = MathContext.DECIMAL128;

    private final CreditAssessmentRepository repository;

    public CreditAssessmentService(CreditAssessmentRepository repository) {
        this.repository = repository;
    }

    // ---------------------------------------------------------------- BR-miniloan-003@v1

    /** Which of the two arms of MIN() produced the answer — AC-miniloan-022 requires saying so. */
    public enum LimitedBy {
        formula,
        cap
    }

    public record MaxApprovable(BigDecimal formulaValue, BigDecimal cap, LimitedBy limitedBy, BigDecimal value) {}

    /**
     * CALC-miniloan-003@v1. At income 200,000 the formula and the cap agree exactly; the tie is
     * reported as {@code formula} (AC-miniloan-021 — the one value a wrong-sided comparison misses).
     */
    public static MaxApprovable maxApprovableAmount(BigDecimal monthlyIncome) {
        BigDecimal formulaValue = monthlyIncome.multiply(INCOME_MULTIPLIER).setScale(2, RoundingMode.HALF_UP);
        boolean cappedByFormula = formulaValue.compareTo(MAX_APPROVABLE_CAP) <= 0;
        return new MaxApprovable(
                formulaValue,
                MAX_APPROVABLE_CAP,
                cappedByFormula ? LimitedBy.formula : LimitedBy.cap,
                cappedByFormula ? formulaValue : MAX_APPROVABLE_CAP);
    }

    // ---------------------------------------------------------------- BR-miniloan-002@v1

    public record DtiOutcome(
            BigDecimal newInstalment,
            BigDecimal totalDebt,
            BigDecimal thresholdAmount,
            BigDecimal ratio,
            boolean passed) {}

    /**
     * CALC-miniloan-002@v1. The new instalment is the <em>displayed</em> EMI — round(EMI, 2) from
     * CALC-miniloan-001@v2 — computed from the amount and tenor the applicant asked for, not from
     * whatever a loan officer later approves (BR-miniloan-012@v1 does not re-open this number).
     *
     * <p>The verdict compares (existing debt + instalment) to (income × 0.70) in baht. Deciding on
     * the displayed percentage instead would pass 21,001 baht against a 21,000 baht ceiling, since
     * both round to 70.00% (AC-miniloan-038).
     */
    public static DtiOutcome evaluateDti(
            BigDecimal existingMonthlyDebt,
            BigDecimal monthlyIncome,
            BigDecimal requestedPrincipal,
            int requestedTermMonths,
            BigDecimal annualRate) {
        BigDecimal newInstalment =
                InstalmentCalculator.monthlyInstalment(requestedPrincipal, annualRate, requestedTermMonths);
        BigDecimal totalDebt = existingMonthlyDebt.add(newInstalment).setScale(2, RoundingMode.HALF_UP);
        BigDecimal thresholdAmount = monthlyIncome.multiply(DTI_THRESHOLD_RATIO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal ratio = totalDebt.divide(monthlyIncome, 6, RoundingMode.HALF_UP);
        return new DtiOutcome(
                newInstalment, totalDebt, thresholdAmount, ratio, totalDebt.compareTo(thresholdAmount) <= 0);
    }

    // ---------------------------------------------------------------- BR-miniloan-006/009@v1

    public record Outcome(
            Band band,
            MaxApprovable maxApprovable,
            DtiOutcome dti,
            boolean eligibilityPassed,
            List<String> reasons) {}

    /**
     * BR-miniloan-006@v1. One failed eligibility criterion is Band C however good the DTI is
     * (AC-miniloan-044); a DTI over 70% is Band C as well, not B (AC-miniloan-038); and a DTI of
     * exactly 50% falls on the A side of the line (AC-miniloan-042).
     */
    public static Outcome assess(LoanApplication application) {
        List<String> reasons = new ArrayList<>();

        boolean ageOk = withinAge(application.getAge());
        boolean incomeOk = application.getMonthlyIncome().compareTo(MIN_MONTHLY_INCOME) >= 0;
        boolean employmentOk = application.getCurrentEmploymentMonths() >= MIN_EMPLOYMENT_MONTHS;

        reasons.add(
                ageOk
                        ? "อายุ " + application.getAge() + " ปี ✓ (เกณฑ์ " + MIN_AGE + "–" + MAX_AGE + " ปี)"
                        : "อายุ " + application.getAge() + " ปี ✗ ต้องอยู่ระหว่าง " + MIN_AGE + "–" + MAX_AGE + " ปี");
        reasons.add(
                incomeOk
                        ? "รายได้ " + money(application.getMonthlyIncome()) + " บาท/เดือน ✓ (เกณฑ์ ไม่น้อยกว่า "
                                + money(MIN_MONTHLY_INCOME) + " บาท)"
                        : "รายได้ " + money(application.getMonthlyIncome()) + " บาท/เดือน ✗ ต้องไม่น้อยกว่า "
                                + money(MIN_MONTHLY_INCOME) + " บาท");
        reasons.add(
                employmentOk
                        ? "อายุงาน " + application.getCurrentEmploymentMonths() + " เดือน ✓ (เกณฑ์ ไม่น้อยกว่า "
                                + MIN_EMPLOYMENT_MONTHS + " เดือน)"
                        : "อายุงาน " + application.getCurrentEmploymentMonths() + " เดือน ✗ ต้องไม่น้อยกว่า "
                                + MIN_EMPLOYMENT_MONTHS + " เดือน");

        DtiOutcome dti =
                evaluateDti(
                        application.getExistingMonthlyDebt(),
                        application.getMonthlyIncome(),
                        application.getRequestedAmount(),
                        application.getRequestedTermMonths(),
                        ANNUAL_INTEREST_RATE);
        reasons.add(
                dti.passed()
                        ? "ภาระหนี้ต่อรายได้ (DTI) " + percent(dti.ratio()) + " ✓ (เกณฑ์ ไม่เกิน 70%)"
                        : "ภาระหนี้ต่อรายได้ (DTI) เกินเกณฑ์ ✗ ภาระหนี้รวม " + money(dti.totalDebt())
                                + " บาท เกินเพดาน " + money(dti.thresholdAmount()) + " บาท (70% ของรายได้ "
                                + money(application.getMonthlyIncome()) + " บาท/เดือน)");
        // AC-miniloan-040: the DTI stays the one computed at submission, so the record has to say
        // which amount it was computed from or a later reduction makes it look wrong.
        reasons.add(
                "DTI คำนวณจากจำนวนเงินกู้ที่ขอ " + money(application.getRequestedAmount())
                        + " บาท ณ วันยื่น (ค่างวดใหม่ " + money(dti.newInstalment()) + " บาท/เดือน)");

        MaxApprovable maxApprovable = maxApprovableAmount(application.getMonthlyIncome());
        reasons.add(
                maxApprovable.limitedBy() == LimitedBy.formula
                        ? "วงเงินที่อนุมัติได้ " + money(maxApprovable.value()) + " บาท (5 เท่าของรายได้ "
                                + money(application.getMonthlyIncome()) + " บาท/เดือน)"
                        : "วงเงินที่อนุมัติได้ " + money(maxApprovable.value()) + " บาท (ถูกจำกัดด้วยเพดาน "
                                + money(maxApprovable.cap()) + " บาท ไม่ใช่ 5 เท่าของรายได้)");

        boolean eligibilityPassed = ageOk && incomeOk && employmentOk;
        Band band = band(eligibilityPassed, dti.totalDebt(), application.getMonthlyIncome());
        return new Outcome(band, maxApprovable, dti, eligibilityPassed, List.copyOf(reasons));
    }

    /**
     * The three branches of BR-miniloan-006@v1, decided on the same baht totals as the DTI rule
     * itself. The acceptance criteria state these boundaries as a total monthly debt against an
     * income (AC-miniloan-042: "ภาระหนี้รวม 15,000 บาทพอดี"), which is the shape this takes.
     */
    public static Band band(boolean eligibilityPassed, BigDecimal totalMonthlyDebt, BigDecimal monthlyIncome) {
        BigDecimal dtiCeiling = monthlyIncome.multiply(DTI_THRESHOLD_RATIO).setScale(2, RoundingMode.HALF_UP);
        if (!eligibilityPassed || totalMonthlyDebt.compareTo(dtiCeiling) > 0) {
            return Band.C;
        }
        BigDecimal bandACeiling = monthlyIncome.multiply(BAND_A_DTI_RATIO).setScale(2, RoundingMode.HALF_UP);
        return totalMonthlyDebt.compareTo(bandACeiling) <= 0 ? Band.A : Band.B;
    }

    /** BR-miniloan-009@v1: the record is written for every outcome, a Band C included. */
    public CreditAssessment record(LoanApplication application, Outcome outcome) {
        return repository.save(
                new CreditAssessment(
                        application.getId(),
                        outcome.band(),
                        outcome.maxApprovable().value(),
                        outcome.dti().ratio(),
                        outcome.reasons()));
    }

    private static boolean withinAge(int age) {
        return age >= MIN_AGE && age <= MAX_AGE;
    }

    /**
     * Comma-grouped baht, with the satang dropped when there are none — the prose form the
     * acceptance criteria use ("รายได้ 30,000 บาท/เดือน"), for the reason lines a person reads.
     * Grouping and decimal symbols are pinned to {@link Locale#US} so the text of a stored reason
     * never depends on the JVM's default locale.
     */
    public static String money(BigDecimal amount) {
        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
        String pattern = scaled.stripTrailingZeros().scale() <= 0 ? "#,##0" : "#,##0.00";
        return format(pattern, scaled);
    }

    /**
     * The same amount as a money(2) figure — always two decimals, which is how req's golden
     * datasets write every baht value ("150,000.00"). Separate from {@link #money(BigDecimal)}
     * because a number and the sentence it appears in are not obliged to look the same.
     */
    public static String moneyExact(BigDecimal amount) {
        return format("#,##0.00", amount.setScale(2, RoundingMode.HALF_UP));
    }

    /** Display only — CALC-miniloan-002@v1 forbids deciding pass/fail on this value. */
    public static String percent(BigDecimal ratio) {
        BigDecimal shown = ratio.multiply(new BigDecimal("100"), WORKING).setScale(2, RoundingMode.HALF_UP);
        return format("#,##0.00", shown) + "%";
    }

    private static String format(String pattern, BigDecimal value) {
        return new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.US)).format(value);
    }
}
