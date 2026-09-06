package com.miniloan.repository;

import com.miniloan.domain.ClosedAccountAdjustment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClosedAccountAdjustmentRepository
        extends JpaRepository<ClosedAccountAdjustment, UUID> {

    /**
     * Every adjustment an account has, oldest first. AC-miniloan-080 and AC-miniloan-081 are measured
     * against this list being EMPTY — a refusal that left a row behind would be the "แก้แล้วรออนุมัติ
     * ย้อนหลัง" state BR-miniloan-040@v1 says the system must have no way to reach.
     */
    List<ClosedAccountAdjustment> findByLoanAccountIdOrderByRequestedAtAsc(UUID loanAccountId);
}
