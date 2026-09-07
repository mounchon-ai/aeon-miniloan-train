package com.miniloan.repository;

import com.miniloan.domain.CreditAssessment;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditAssessmentRepository extends JpaRepository<CreditAssessment, UUID> {

    /** ENT-003: one application, one latest assessment (enforced by a unique constraint too). */
    Optional<CreditAssessment> findByApplicationId(UUID applicationId);

    /**
     * FE-miniloan-023 — UI-miniloan-006 puts ENT-003's band on every row of the officer's queue, so
     * the list route needs the assessments of the rows it is about to return. One read for the whole
     * page rather than one per row: a queue that issued a call per application would make its own
     * length the number of requests.
     */
    List<CreditAssessment> findByApplicationIdIn(Collection<UUID> applicationIds);
}
