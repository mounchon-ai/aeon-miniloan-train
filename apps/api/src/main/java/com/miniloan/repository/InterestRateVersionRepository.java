package com.miniloan.repository;

import com.miniloan.domain.InterestRateVersion;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterestRateVersionRepository extends JpaRepository<InterestRateVersion, UUID> {

    /**
     * BR-miniloan-036@v1 — the version in force on a given day. {@code LessThanEqual} is the
     * inclusive boundary AC-miniloan-101 turns on: disbursing on exactly the effective date uses the
     * new version, and the day before still uses the old one.
     */
    Optional<InterestRateVersion> findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
            LocalDate on);
}
