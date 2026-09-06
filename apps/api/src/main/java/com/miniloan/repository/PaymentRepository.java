package com.miniloan.repository;

import com.miniloan.domain.Payment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * Every payment an account has taken, oldest first. AC-miniloan-014 is measured against this
     * list being EMPTY after two refused half-payments — a refusal that left a row behind would be
     * the accumulation the criterion rules out.
     */
    List<Payment> findByLoanAccountIdOrderByRecordedAtAsc(UUID loanAccountId);
}
