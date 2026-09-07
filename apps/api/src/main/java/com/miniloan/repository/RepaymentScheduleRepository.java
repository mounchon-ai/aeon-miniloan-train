package com.miniloan.repository;

import com.miniloan.domain.RepaymentSchedule;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, UUID> {

    /**
     * ENT-007: an account may hold several revisions (FE-miniloan-011 issues them), and exactly one
     * of them is current.
     */
    Optional<RepaymentSchedule> findByLoanAccountIdAndCurrentIsTrue(UUID loanAccountId);

    /**
     * The same question for a whole page of accounts (FE-miniloan-027 · UI-miniloan-011). Exactly one
     * revision per account is current, so this returns at most one row per id — the batched twin of
     * the single read above, and the reason API-013 costs two queries however many rows it lists.
     */
    List<RepaymentSchedule> findByLoanAccountIdInAndCurrentIsTrue(Collection<UUID> loanAccountIds);

    /**
     * AC-miniloan-011: every revision an account has ever been issued, oldest first — nothing is
     * removed when it is replaced (BR-miniloan-044@v1), so this list only ever grows.
     *
     * <p>Ordered by revision number rather than by {@code issuedAt}: the number is what
     * BR-miniloan-044@v1 increments, and the only field guaranteed to separate two revisions issued
     * inside the same clock tick.
     */
    List<RepaymentSchedule> findByLoanAccountIdOrderByRevisionNumberAsc(UUID loanAccountId);
}
