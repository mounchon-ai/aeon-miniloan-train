package com.miniloan.repository;

import com.miniloan.domain.IdempotencyKey;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * BR-miniloan-043@v1 — deliberately offers no "does this key exist?" query. A read-then-write check
 * is exactly the in-memory dedup AC-miniloan-134 rejects: the write itself is the check, and the
 * unique constraint on (commandType, requestId) is what fails.
 */
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {}
