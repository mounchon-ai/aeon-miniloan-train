package com.miniloan.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * ผลการประเมินสินเชื่อ (ENT-003 · BR-miniloan-009@v1). Created the moment an application is
 * submitted, for every outcome including a rejection — AC-miniloan-036 requires the record to exist
 * even for Band C, otherwise nobody can answer what the system decided on and why.
 *
 * <p>"หนึ่งใบสมัครมีผลประเมินล่าสุดหนึ่งชุด" (ENT-003) is a unique constraint on
 * {@code application_id}, not a convention — the same reason BR-miniloan-043@v1 puts idempotency in
 * the database.
 */
@Entity
@Table(
        name = "credit_assessments",
        uniqueConstraints = @UniqueConstraint(name = "uk_credit_assessment_application", columnNames = "applicationId"))
public class CreditAssessment {

    public enum Band {
        A,
        B,
        C
    }

    /** Bands A and B are the two the system may move on by itself (STM-miniloan-001). */
    public boolean movesToUnderReview() {
        return band == Band.A || band == Band.B;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Band band;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal maxApprovableAmount;

    /**
     * The ratio itself, not the percentage shown on screen. CALC-miniloan-002@v1 is explicit that
     * the pass/fail decision is made in baht and that the percentage is a display value — storing
     * the ratio keeps the two from being confused for one another.
     */
    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal dtiRatio;

    @Column(nullable = false, length = 4000)
    private String reasons;

    @Column(nullable = false)
    private Instant assessedAt;

    protected CreditAssessment() {
        // JPA
    }

    public CreditAssessment(
            UUID applicationId, Band band, BigDecimal maxApprovableAmount, BigDecimal dtiRatio, List<String> reasons) {
        this.applicationId = applicationId;
        this.band = band;
        this.maxApprovableAmount = maxApprovableAmount;
        this.dtiRatio = dtiRatio;
        this.reasons = String.join("\n", reasons);
        this.assessedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public Band getBand() {
        return band;
    }

    public BigDecimal getMaxApprovableAmount() {
        return maxApprovableAmount;
    }

    public BigDecimal getDtiRatio() {
        return dtiRatio;
    }

    public String getReasons() {
        return reasons;
    }

    /** AC-miniloan-034: the caller shows every criterion's verdict, not only the ones that failed. */
    public List<String> getReasonLines() {
        return List.of(reasons.split("\n"));
    }

    public Instant getAssessedAt() {
        return assessedAt;
    }
}
