package com.miniloan.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * ใบสมัครสินเชื่อ (ENT-002 · STM-miniloan-001). This unit (FE-miniloan-004) only ever writes
 * {@link Status#Draft} rows — every other status, and the fields that go with it
 * (assignedLoanOfficerId, approvedAmount, rejectionReason, ...), belongs to the unit that first
 * needs it (FE-miniloan-005 onward) and is added to this same entity there.
 *
 * <p>ENT-001 (Applicant) is not a persisted entity anywhere in the build plan — the mock-auth
 * scheme (FE-miniloan-002) has exactly one demo identity per role, so {@code applicantId} is
 * that resolved role, and the applicant's entered name is kept directly on this row instead of
 * a separate table no unit builds.
 */
@Entity
@Table(name = "loan_applications")
public class LoanApplication {

    /**
     * BR-miniloan-004@v1's range, as the machine-readable/Thai-message contract UC-miniloan-022
     * requires the API to enforce (AC-miniloan-115). Draft save deliberately never calls this
     * (BR-miniloan-008@v1) — FE-miniloan-005's submit flow is what wires it in.
     */
    public static final BigDecimal MIN_REQUESTED_AMOUNT = new BigDecimal("10000.00");

    public static final BigDecimal MAX_REQUESTED_AMOUNT = new BigDecimal("1000000.00");
    public static final String AMOUNT_OUT_OF_RANGE_CODE = "LOAN_AMOUNT_OUT_OF_RANGE";
    public static final String AMOUNT_OUT_OF_RANGE_MESSAGE =
            "จำนวนเงินกู้ที่ขอต้องอยู่ระหว่าง 10,000 – 1,000,000 บาท";

    public enum Status {
        Draft,
        Submitted,
        UnderReview,
        Approved,
        Rejected,
        Disbursed,
        Cancelled
    }

    public record RangeViolation(String code, String message) {}

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String applicantId;

    private String fullName;

    private Integer age;

    @Column(precision = 19, scale = 2)
    private BigDecimal monthlyIncome;

    private Integer currentEmploymentMonths;

    @Column(precision = 19, scale = 2)
    private BigDecimal existingMonthlyDebt;

    @Column(precision = 19, scale = 2)
    private BigDecimal requestedAmount;

    private Integer requestedTermMonths;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.Draft;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant submittedAt;

    protected LoanApplication() {
        // JPA
    }

    public LoanApplication(String applicantId) {
        this.applicantId = applicantId;
        this.status = Status.Draft;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** AC-miniloan-115: below the minimum or above the maximum, in either direction. */
    public static Optional<RangeViolation> validateRequestedAmount(BigDecimal amount) {
        if (amount == null) {
            return Optional.empty();
        }
        if (amount.compareTo(MIN_REQUESTED_AMOUNT) < 0 || amount.compareTo(MAX_REQUESTED_AMOUNT) > 0) {
            return Optional.of(new RangeViolation(AMOUNT_OUT_OF_RANGE_CODE, AMOUNT_OUT_OF_RANGE_MESSAGE));
        }
        return Optional.empty();
    }

    public void applyDraftFields(
            String fullName,
            Integer age,
            BigDecimal monthlyIncome,
            Integer currentEmploymentMonths,
            BigDecimal existingMonthlyDebt,
            BigDecimal requestedAmount,
            Integer requestedTermMonths) {
        this.fullName = fullName;
        this.age = age;
        this.monthlyIncome = roundMoney(monthlyIncome);
        this.currentEmploymentMonths = currentEmploymentMonths;
        this.existingMonthlyDebt = roundMoney(existingMonthlyDebt);
        this.requestedAmount = roundMoney(requestedAmount);
        this.requestedTermMonths = requestedTermMonths;
        this.updatedAt = Instant.now();
    }

    // Money is rounded round-half-up at the point it occurs (CLAUDE.md) — never store an
    // unrounded value and round only for display.
    private static BigDecimal roundMoney(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    public UUID getId() {
        return id;
    }

    public String getApplicantId() {
        return applicantId;
    }

    public String getFullName() {
        return fullName;
    }

    public Integer getAge() {
        return age;
    }

    public BigDecimal getMonthlyIncome() {
        return monthlyIncome;
    }

    public Integer getCurrentEmploymentMonths() {
        return currentEmploymentMonths;
    }

    public BigDecimal getExistingMonthlyDebt() {
        return existingMonthlyDebt;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public Integer getRequestedTermMonths() {
        return requestedTermMonths;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }
}
