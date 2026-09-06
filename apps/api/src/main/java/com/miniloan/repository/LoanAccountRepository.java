package com.miniloan.repository;

import com.miniloan.domain.LoanAccount;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanAccountRepository extends JpaRepository<LoanAccount, UUID> {

    /** BR-miniloan-014@v1: one application, one account — backed by a unique constraint too. */
    Optional<LoanAccount> findByApplicationId(UUID applicationId);

    /**
     * AC-miniloan-105 · AC-miniloan-106: the moment the first account points at a rate version, that
     * version may never be deleted. The count is the answer to "{จำนวน} บัญชี" in that refusal —
     * the administrative action itself has no unit and no API yet.
     */
    List<LoanAccount> findByInterestRateVersionId(UUID interestRateVersionId);
}
