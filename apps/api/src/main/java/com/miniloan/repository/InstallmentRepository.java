package com.miniloan.repository;

import com.miniloan.domain.Installment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstallmentRepository extends JpaRepository<Installment, UUID> {

    /** AC-miniloan-094: the whole table is there on the very next request, in instalment order. */
    List<Installment> findByRepaymentScheduleIdOrderByInstallmentNumberAsc(UUID repaymentScheduleId);
}
