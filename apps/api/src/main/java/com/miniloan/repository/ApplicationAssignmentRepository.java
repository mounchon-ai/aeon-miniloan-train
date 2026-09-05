package com.miniloan.repository;

import com.miniloan.domain.ApplicationAssignment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationAssignmentRepository extends JpaRepository<ApplicationAssignment, UUID> {

    /** ENT-013 is a history: oldest first, so a re-assignment reads as the story it is. */
    List<ApplicationAssignment> findByApplicationIdOrderByAssignedAtAsc(UUID applicationId);
}
