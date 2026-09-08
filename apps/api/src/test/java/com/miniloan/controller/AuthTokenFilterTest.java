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
    // The browser sends a CORS preflight BY ITSELF and, per spec, without the Authorization header.
    // Before FE-miniloan-002's revision the token check 401'd it and the real request was never sent,
    // so every screen showed FE-miniloan-029's connection banner instead of data.
    @Test
    void answersACorsPreflightWithoutATokenAndNeverRunsTheChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("OPTIONS");
        request.addHeader("Origin", AuthTokenFilter.WEB_ORIGIN);
        request.addHeader("Access-Control-Request-Method", "GET");
        request.addHeader("Access-Control-Request-Headers", "authorization");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NO_CONTENT);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo(AuthTokenFilter.WEB_ORIGIN);
        assertThat(response.getHeader("Access-Control-Allow-Headers")).contains("Authorization");
    }

    // A rejection the browser cannot read is a network error to the page, not a 401: the header has to
    // be on the refusal too, or FE-miniloan-029's banner can never say what actually happened.
    @Test
    void putsTheAllowOriginHeaderOnTheRejectionAsWell() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo(AuthTokenFilter.WEB_ORIGIN);
    }
}
