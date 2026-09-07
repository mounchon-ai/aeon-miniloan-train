package com.miniloan.repository;

import com.miniloan.domain.Installment;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstallmentRepository extends JpaRepository<Installment, UUID> {

    /** AC-miniloan-094: the whole table is there on the very next request, in instalment order. */
    List<Installment> findByRepaymentScheduleIdOrderByInstallmentNumberAsc(UUID repaymentScheduleId);

    /**
     * UI-miniloan-011's next-due-installment, for a whole page of accounts in one query
     * (FE-miniloan-027). Ordered by instalment number so the FIRST row per schedule is the next one
     * owing: BR-miniloan-020@v1 retires instalments in order, so "the next Due" is the
     * lowest-numbered Due row and never a later one that happened to be found first.
     */
    List<Installment> findByRepaymentScheduleIdInAndStatusOrderByInstallmentNumberAsc(
            Collection<UUID> repaymentScheduleIds, Installment.Status status);
}
