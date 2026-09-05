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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ใบสมัครสินเชื่อ (ENT-002 · STM-miniloan-001). FE-miniloan-004 only ever wrote
 * {@link Status#Draft} rows; FE-miniloan-005 adds the two transitions out of it —
 * {@link #submit()} (Draft → Submitted, the applicant's own) and
 * {@link #moveToUnderReview()} (Submitted → UnderReview, the system's). Every other status, and the
 * fields that go with it (assignedLoanOfficerId, approvedAmount, rejectionReason, ...), belongs to
 * the unit that first needs it and is added to this same entity there.
 *
 * <p>BR-miniloan-010@v1: status changes happen through methods on this aggregate and nowhere else —
 * there is no setter, and no path back.
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

    /** The other half of BR-miniloan-004@v1's range — monthly instalments, 6 to 60 of them. */
    public static final int MIN_TERM_MONTHS = 6;

    public static final int MAX_TERM_MONTHS = 60;
    public static final String TERM_OUT_OF_RANGE_CODE = "LOAN_TERM_OUT_OF_RANGE";
    public static final String TERM_OUT_OF_RANGE_MESSAGE = "จำนวนงวดต้องอยู่ระหว่าง 6 – 60 งวด";

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

    /**
     * The Loan Officer responsible right now (BR-miniloan-032@v1). Null until a supervisor assigns
     * one, and that null is the normal state of a freshly assessed application, not a fault
     * (AC-miniloan-066). The assignment history lives in ENT-013; this is the only field anything
     * asks when deciding who may act.
     */
    private String assignedLoanOfficerId;

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

    /** AC-miniloan-115 for the tenor half of BR-miniloan-004@v1's range. */
    public static Optional<RangeViolation> validateRequestedTermMonths(Integer termMonths) {
        if (termMonths == null) {
            return Optional.empty();
        }
        if (termMonths < MIN_TERM_MONTHS || termMonths > MAX_TERM_MONTHS) {
            return Optional.of(new RangeViolation(TERM_OUT_OF_RANGE_CODE, TERM_OUT_OF_RANGE_MESSAGE));
        }
        return Optional.empty();
    }

    /**
     * BR-miniloan-007@v1 — the six fields submission requires, reported in full and in the order the
     * form asks for them. AC-miniloan-030 is explicit that naming only the first missing field is
     * wrong: the applicant fixes one round of corrections, not six.
     */
    public List<String> missingRequiredFields() {
        List<String> missing = new ArrayList<>();
        if (fullName == null || fullName.isBlank()) {
            missing.add("ชื่อ");
        }
        if (age == null) {
            missing.add("อายุ");
        }
        if (monthlyIncome == null) {
            missing.add("รายได้");
        }
        if (currentEmploymentMonths == null) {
            missing.add("อายุงาน");
        }
        if (requestedAmount == null) {
            missing.add("จำนวนเงินกู้");
        }
        if (requestedTermMonths == null) {
            missing.add("จำนวนงวด");
        }
        return List.copyOf(missing);
    }

    /**
     * Draft → Submitted (STM-miniloan-001). BR-miniloan-031@v2 puts this edge in the hands of the
     * owning applicant alone; the caller has already resolved that, this method guards the state.
     */
    public void submit() {
        if (status != Status.Draft) {
            throw new IllegalStateTransitionException(status, Status.Submitted);
        }
        this.status = Status.Submitted;
        Instant now = Instant.now();
        this.submittedAt = now;
        this.updatedAt = now;
    }

    /**
     * Submitted → UnderReview (STM-miniloan-001). AC-miniloan-063: no role has this edge — it is the
     * assessment finishing on Band A or B, which is why no endpoint anywhere exposes it and no
     * screen carries a button for it.
     */
    public void moveToUnderReview() {
        if (status != Status.Submitted) {
            throw new IllegalStateTransitionException(status, Status.UnderReview);
        }
        this.status = Status.UnderReview;
        this.updatedAt = Instant.now();
    }

    public static class IllegalStateTransitionException extends RuntimeException {
        public IllegalStateTransitionException(Status from, Status to) {
            super("ใบสมัครสถานะ " + from + " เดินไปสถานะ " + to + " ไม่ได้");
        }
    }

    /**
     * BR-miniloan-032@v1 — a supervisor hands the application to one Loan Officer; the system never
     * distributes work itself and an officer cannot pick one up. Re-assigning is allowed and simply
     * moves this pointer: ENT-013 keeps the row that says who held it before, and UC-miniloan-004's
     * "not yet assigned" precondition describes where the main flow starts rather than an invariant
     * (decided with the project owner on 2026-09-05).
     */
    public void assignTo(String loanOfficerId) {
        if (status != Status.UnderReview) {
            throw new NotAssignableException(status);
        }
        this.assignedLoanOfficerId = loanOfficerId;
        this.updatedAt = Instant.now();
    }

    /**
     * The guard every officer-only action goes through (BR-miniloan-032@v1). Holding the same role
     * is not enough — AC-miniloan-065 turns down a second Loan Officer with identical rights — and
     * an unassigned application turns everyone down (AC-miniloan-066). The units that expose those
     * actions are FE-miniloan-007 (approve) and FE-miniloan-008 (reject); this is the rule they
     * call, kept on the aggregate so no caller can route around it.
     */
    public void requireAssignedTo(String loanOfficerId) {
        if (assignedLoanOfficerId == null) {
            throw new NotAssignedException();
        }
        if (!assignedLoanOfficerId.equals(loanOfficerId)) {
            throw new AssignedToAnotherOfficerException(assignedLoanOfficerId);
        }
    }

    public static class NotAssignableException extends RuntimeException {
        public NotAssignableException(Status status) {
            super("มอบหมายไม่ได้ — ใบสมัครนี้อยู่สถานะ " + status + " ไม่ใช่ UnderReview");
        }
    }

    public static class NotAssignedException extends RuntimeException {
        public NotAssignedException() {
            super("ดำเนินการไม่ได้ — ใบสมัครนี้ยังไม่ถูกมอบหมายให้ผู้พิจารณา");
        }
    }

    /**
     * Carries the assignee so the acting unit can phrase its own refusal — AC-miniloan-065's exact
     * wording ("อนุมัติไม่ได้ — ...") belongs to the approve action, and reject and cancel each name
     * themselves differently.
     */
    public static class AssignedToAnotherOfficerException extends RuntimeException {
        private final String assignedLoanOfficerId;

        public AssignedToAnotherOfficerException(String assignedLoanOfficerId) {
            super("ดำเนินการไม่ได้ — ใบสมัครนี้มอบหมายให้ " + assignedLoanOfficerId + " เป็นผู้พิจารณา");
            this.assignedLoanOfficerId = assignedLoanOfficerId;
        }

        public String getAssignedLoanOfficerId() {
            return assignedLoanOfficerId;
        }
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

    public String getAssignedLoanOfficerId() {
        return assignedLoanOfficerId;
    }
}
