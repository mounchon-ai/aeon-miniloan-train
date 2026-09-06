package com.miniloan.service.config;

import com.miniloan.domain.InterestRateVersion;
import com.miniloan.repository.InterestRateVersionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * The one interest-rate version this project runs on, written once if the table is empty.
 *
 * <p><b>Why a bootstrap rather than an administrator.</b> BR-miniloan-036@v1 makes the rate
 * versioned master data that somebody publishes, and AC-miniloan-102 · AC-miniloan-105 describe that
 * person doing it. No API in {@code interfaces.json}, no screen in {@code sitemap.json} and no unit
 * in the build plan exposes either action — so disbursement would have nothing to read and the
 * feature could not run at all. This is the smallest thing that makes it run, and it is raised
 * through {@code /dev:plan} rather than quietly standing in for the missing unit.
 *
 * <p><b>The numbers are not invented.</b> 25% per year is the rate the requirement source declares
 * and every row of GD-miniloan-002 is computed at; 2026-01-01 is the effective date AC-miniloan-100
 * names for the version in force. Nothing here publishes a second version, and the back-dating rule
 * BR-miniloan-036@v1 sets is about an administrator publishing — this row is the starting state, not
 * a publication.
 */
@Component
public class InterestRateBootstrap implements ApplicationRunner {

    static final BigDecimal DECLARED_ANNUAL_RATE_PERCENT = new BigDecimal("25.0000");
    static final LocalDate DECLARED_EFFECTIVE_FROM = LocalDate.of(2026, 1, 1);
    private static final String BOOTSTRAP_AUTHOR = "system";

    private final InterestRateVersionRepository rateVersions;

    public InterestRateBootstrap(InterestRateVersionRepository rateVersions) {
        this.rateVersions = rateVersions;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (rateVersions.count() > 0) {
            return;
        }
        rateVersions.save(
                new InterestRateVersion(
                        DECLARED_ANNUAL_RATE_PERCENT, DECLARED_EFFECTIVE_FROM, BOOTSTRAP_AUTHOR));
    }
}
