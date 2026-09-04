package com.miniloan.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.miniloan.service.MockTokenService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthTokenFilterTest {

    private final AuthTokenFilter filter = new AuthTokenFilter(new MockTokenService());

    // AC-miniloan-123: a valid token passes through, and the resolved role is available downstream.
    @Test
    void letsTheRequestThroughAndRecordsTheRoleForAValidToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer mock-role-002");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(request.getAttribute(AuthTokenFilter.RESOLVED_ROLE_ATTRIBUTE)).isEqualTo("ROLE-002");
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    // AC-miniloan-124: no Authorization header at all — rejected before the chain runs, no data touched.
    @Test
    void rejectsBeforeTheChainWhenNoTokenIsAttached() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getContentAsString()).contains(AuthTokenFilter.UNAUTHORIZED_MESSAGE);
    }

    // AC-miniloan-125: a fake/unrecognized token is rejected exactly like no token at all.
    @Test
    void rejectsBeforeTheChainWhenTheTokenIsFake() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer not-a-real-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getContentAsString()).contains(AuthTokenFilter.UNAUTHORIZED_MESSAGE);
    }
}
