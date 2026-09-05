package com.miniloan.repository;

import com.miniloan.domain.CreditAssessment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditAssessmentRepository extends JpaRepository<CreditAssessment, UUID> {

    /** ENT-003: one application, one latest assessment (enforced by a unique constraint too). */
    Optional<CreditAssessment> findByApplicationId(UUID applicationId);
}
