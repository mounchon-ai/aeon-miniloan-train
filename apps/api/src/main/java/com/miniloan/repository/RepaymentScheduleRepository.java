package com.miniloan.repository;

import com.miniloan.domain.RepaymentSchedule;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, UUID> {

    /**
     * ENT-007: an account may hold several revisions (FE-miniloan-011 issues them), and exactly one
     * of them is current.
     */
    Optional<RepaymentSchedule> findByLoanAccountIdAndCurrentIsTrue(UUID loanAccountId);
}
