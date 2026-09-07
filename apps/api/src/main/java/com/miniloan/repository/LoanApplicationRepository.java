package com.miniloan.repository;

import com.miniloan.domain.LoanApplication;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, UUID> {

    /** Scopes lookups to the caller's own applications (ACL-001, scope=own). */
    Optional<LoanApplication> findByIdAndApplicantId(UUID id, String applicantId);

    /** The same scope as a list — used to reach the accounts an Applicant owns (API-013). */
    List<LoanApplication> findByApplicantId(String applicantId);

    /**
     * FE-miniloan-018 · BR-miniloan-024@v1 — one bucket of the dashboard, counted in the database
     * rather than by loading rows and sizing a list. {@code Disbursed} is deliberately never asked
     * for here: AC-miniloan-112 says that state has no square of its own, and the account it
     * produced is what the "ใช้งานอยู่" square counts.
     */
    long countByStatus(LoanApplication.Status status);

    /**
     * FE-miniloan-019 · AC-miniloan-128 — the scope of a LIST, applied in the query. Oldest first so
     * the order is the order they were filed, not whatever the database returned.
     */
    List<LoanApplication> findByApplicantIdOrderByCreatedAtAsc(String applicantId);

    /** ACL-003 · ACL-031 give ROLE-003 scope: all — the same rows, in the same order. */
    List<LoanApplication> findAllByOrderByCreatedAtAsc();
}
