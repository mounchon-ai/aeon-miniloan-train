package com.miniloan.service;

import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Resolves the mock bearer token attached by apps/web's mock-token interceptor
 * (BR-miniloan-030@v1) to the role it stands for. Real auth is out of scope
 * this round (CLAUDE.md) — there is no login flow, no expiry, and no issuing
 * authority: the 5 role/token pairs are a fixed constant that must match
 * apps/web/src/app/core/services/current-role.service.ts exactly.
 */
@Service
public class MockTokenService {

    private static final Map<String, String> ROLE_BY_TOKEN = Map.of(
            "mock-role-001", "ROLE-001",
            "mock-role-002", "ROLE-002",
            "mock-role-003", "ROLE-003",
            "mock-role-004", "ROLE-004",
            "mock-role-005", "ROLE-005");

    /** Empty for a missing, fake, or otherwise unrecognized token (AC-miniloan-124/125). */
    public Optional<String> resolveRole(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ROLE_BY_TOKEN.get(token));
    }
}
