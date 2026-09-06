package com.miniloan.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * เวอร์ชันอัตราดอกเบี้ย (ENT-005 · BR-miniloan-036@v1 · BR-miniloan-037@v1).
 *
 * <p>A rate is master data with a version and an effective date, and a loan account REFERS to the
 * version it was built with rather than copying the number onto itself (BR-miniloan-037@v1) — which
 * is what lets an account opened last January still read its own rate today.
 *
 * <p><b>What this unit does NOT build.</b> Publishing a version and deleting one are an
 * administrator's actions (AC-miniloan-102 · AC-miniloan-105), and no API in {@code interfaces.json}
 * and no screen in {@code sitemap.json} exposes either — nor does any unit in the build plan own
 * them. Only the reading side is here: which version is in force on a disbursement date, and the
 * account's reference to it. The rest is a gap raised through {@code /dev:plan}.
 */
@Entity
@Table(name = "interest_rate_versions")
public class InterestRateVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Per cent per year, as ENT-005 names it — 25 means 25%/year, not 0.25. */
    @Column(nullable = false, precision = 9, scale = 4)
    private BigDecimal annualRatePercent;

    /**
     * BR-miniloan-036@v1: the day the version starts to apply, counted INCLUSIVE — disbursing on
     * exactly this date uses this version and not the one before it (AC-miniloan-101).
     */
    @Column(nullable = false)
    private LocalDate effectiveFrom;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean archived;

    protected InterestRateVersion() {
        // JPA
    }

    public InterestRateVersion(BigDecimal annualRatePercent, LocalDate effectiveFrom, String createdBy) {
        this.annualRatePercent = annualRatePercent;
        this.effectiveFrom = effectiveFrom;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
        this.archived = false;
    }

    /** The calculator takes a rate per year as a fraction; ENT-005 stores it as a percentage. */
    public BigDecimal annualRateFraction() {
        return annualRatePercent.divide(new BigDecimal("100"));
    }

    public UUID getId() {
        return id;
    }

    public BigDecimal getAnnualRatePercent() {
        return annualRatePercent;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isArchived() {
        return archived;
    }
}
