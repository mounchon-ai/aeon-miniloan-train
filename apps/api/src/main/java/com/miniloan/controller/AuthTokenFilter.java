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
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * First gate of every endpoint (BR-miniloan-030@v1): rejects before any
 * handler runs unless the request carries the mock bearer token apps/web's
 * interceptor attaches. Scheme decided while building FE-miniloan-001/002 —
 * "Authorization: Bearer &lt;token&gt;" — since no design doc pinned one and
 * no login screen exists in sitemap.json.
 *
 * <p>Applies to every request (no exempt paths) with one exception the
 * browser forces: a CORS preflight. REQ-miniloan-006 keeps apps/web and
 * apps/api as two separate apps, and docker/docker-compose.yml publishes
 * them on two ports, so every call the browser makes to the 26 endpoints
 * interfaces.json declares is cross-origin. A preflight is an OPTIONS
 * request the browser sends BY ITSELF and deliberately without the
 * Authorization header, so the token check below would 401 it and the real
 * request would never be sent — which is exactly what it did.
 *
 * <p>The allowed origin is a constant here rather than a new config class:
 * the api's declared layers are Domain / Repository / Service / Controller
 * (features.json structure.apps), so a com.miniloan.config package would sit
 * outside every one of them and DV15 refuses that. It is the mirror of
 * apps/web's core/api-base-url.ts — one seam, in a declared layer, for
 * whoever wires real origins later.
 *
 * <p>A later unit adding actuator health checks or similar must add its own
 * exemption here rather than assuming one exists.
 */
@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    static final String UNAUTHORIZED_MESSAGE = "ไม่ได้รับอนุญาต — กรุณาเข้าสู่ระบบใหม่";
    static final String RESOLVED_ROLE_ATTRIBUTE = "miniloan.role";

    /** Where apps/web is served from, as the browser sees it (docker-compose publishes web on 3000). */
    static final String WEB_ORIGIN = "http://localhost:3000";

    private final MockTokenService mockTokenService;

    public AuthTokenFilter(MockTokenService mockTokenService) {
        this.mockTokenService = mockTokenService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Every response carries it, including the 401 below: without the header the browser refuses
        // to hand the body to the page, so even the rejection would read as a network error.
        response.setHeader("Access-Control-Allow-Origin", WEB_ORIGIN);
        response.addHeader("Vary", "Origin");

        if (CorsUtils.isPreFlightRequest(request)) {
            response.setHeader(
                    "Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
            response.setHeader("Access-Control-Max-Age", "3600");
            // Answered here and never passed on: a preflight asks permission, it touches no data,
            // and it carries no token to check.
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }

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
