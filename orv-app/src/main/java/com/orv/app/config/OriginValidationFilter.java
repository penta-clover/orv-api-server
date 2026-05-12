package com.orv.app.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class OriginValidationFilter extends OncePerRequestFilter {
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final Set<String> allowedOrigins;

    public OriginValidationFilter(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins.stream()
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (SAFE_METHODS.contains(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestOrigin = resolveOrigin(request);
        if (requestOrigin == null) {
            // Non-browser clients and some same-origin requests may not send Origin/Referer.
            filterChain.doFilter(request, response);
            return;
        }

        if (allowedOrigins.contains(requestOrigin)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"Forbidden origin\"}");
    }

    private String resolveOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank()) {
            return origin;
        }

        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return null;
        }

        try {
            URI uri = new URI(referer);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }

            StringBuilder originBuilder = new StringBuilder()
                    .append(uri.getScheme())
                    .append("://")
                    .append(uri.getHost());
            if (uri.getPort() != -1) {
                originBuilder.append(":").append(uri.getPort());
            }

            return originBuilder.toString();
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
