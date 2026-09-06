package com.miniloan.repository;

import com.miniloan.domain.ApproverRoleSetting;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApproverRoleSettingRepository extends JpaRepository<ApproverRoleSetting, UUID> {

    /**
     * The single row, looked up by the key it is written under rather than by a hard-coded id.
     * Empty is BR-miniloan-040@v1's "ยังไม่ได้ตั้ง" — the state the system starts in, and the one
     * AC-miniloan-080 and AC-miniloan-081 are measured against.
     */
    Optional<ApproverRoleSetting> findBySingletonKey(String singletonKey);
}
