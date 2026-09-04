package com.miniloan.controller;

import com.miniloan.service.MockTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * First gate of every endpoint (BR-miniloan-030@v1): rejects before any
 * handler runs unless the request carries the mock bearer token apps/web's
 * interceptor attaches. Scheme decided while building FE-miniloan-001/002 —
 * "Authorization: Bearer &lt;token&gt;" — since no design doc pinned one and
 * no login screen exists in sitemap.json.
 *
 * <p>Applies to every request (no exempt paths). A later unit adding
 * actuator health checks or similar must add its own exemption here rather
 * than assuming one exists.
 */
@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    static final String UNAUTHORIZED_MESSAGE = "ไม่ได้รับอนุญาต — กรุณาเข้าสู่ระบบใหม่";
    static final String RESOLVED_ROLE_ATTRIBUTE = "miniloan.role";

    private final MockTokenService mockTokenService;

    public AuthTokenFilter(MockTokenService mockTokenService) {
        this.mockTokenService = mockTokenService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Optional<String> role = mockTokenService.resolveRole(extractBearerToken(request));

        if (role.isEmpty()) {
            // AC-miniloan-124/125: reject before touching any data — filterChain.doFilter is never called.
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"message\":\"" + UNAUTHORIZED_MESSAGE + "\"}");
            return;
        }

        request.setAttribute(RESOLVED_ROLE_ATTRIBUTE, role.get());
        filterChain.doFilter(request, response);
    }

    private static String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring("Bearer ".length()).trim();
    }
}
