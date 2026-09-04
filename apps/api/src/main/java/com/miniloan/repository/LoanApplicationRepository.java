package com.miniloan.repository;

import com.miniloan.domain.LoanApplication;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, UUID> {

    /** Scopes lookups to the caller's own applications (ACL-001, scope=own). */
    Optional<LoanApplication> findByIdAndApplicantId(UUID id, String applicantId);
}
