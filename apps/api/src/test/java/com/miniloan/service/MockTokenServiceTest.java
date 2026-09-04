package com.miniloan.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

class MockTokenServiceTest {

    private final MockTokenService service = new MockTokenService();

    // AC-miniloan-123: a valid mock token resolves to the role it was issued for.
    @ParameterizedTest
    @MethodSource("validTokens")
    void resolvesTheRoleForEachValidToken(String token, String expectedRole) {
        assertThat(service.resolveRole(token)).contains(expectedRole);
    }

    private static Stream<Arguments> validTokens() {
        return Stream.of(
                Arguments.of("mock-role-001", "ROLE-001"),
                Arguments.of("mock-role-002", "ROLE-002"),
                Arguments.of("mock-role-003", "ROLE-003"),
                Arguments.of("mock-role-004", "ROLE-004"),
                Arguments.of("mock-role-005", "ROLE-005"));
    }

    // AC-miniloan-124: no token at all resolves to nothing.
    @Test
    void resolvesNothingForANullToken() {
        assertThat(service.resolveRole(null)).isEmpty();
    }

    @Test
    void resolvesNothingForABlankToken() {
        assertThat(service.resolveRole("   ")).isEmpty();
    }

    // AC-miniloan-125: a fake or unrecognized token resolves to nothing, exactly like no token —
    // having *a* token is not enough, it must be one that passes the check.
    @Test
    void resolvesNothingForAFakeToken() {
        assertThat(service.resolveRole("mock-role-999")).isEmpty();
    }

    @Test
    void resolvesNothingForARealJwtLookingToken() {
        assertThat(service.resolveRole("eyJhbGciOiJIUzI1NiJ9.fake.signature")).isEmpty();
    }
}
