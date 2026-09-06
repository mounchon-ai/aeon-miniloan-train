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
     * BR-miniloan-054@v1 · AC-miniloan-137: the scope of a LIST, which is where a scope hole
     * actually lives — "รายบัญชีมักมีการตรวจ แต่รายการมักลืม". Filtering happens in the query, so
     * there is no moment at which every account has been loaded and only the rendering is scoped.
     */
    List<LoanAccount> findByAssignedOperationsIdOrderByDisbursedAtAsc(String assignedOperationsId);

    /** The Applicant half of API-013's scope — the accounts behind that person's applications. */
    List<LoanAccount> findByApplicationIdInOrderByDisbursedAtAsc(List<UUID> applicationIds);

    /**
     * AC-miniloan-105 · AC-miniloan-106: the moment the first account points at a rate version, that
     * version may never be deleted. The count is the answer to "{จำนวน} บัญชี" in that refusal —
     * the administrative action itself has no unit and no API yet.
     */
    List<LoanAccount> findByInterestRateVersionId(UUID interestRateVersionId);
}
