package com.smartlearn.platform.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Adds OWASP-recommended security headers to every response.
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Prevent clickjacking
        response.setHeader("X-Frame-Options", "DENY");
        // Prevent MIME type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");
        // Enable XSS protection
        response.setHeader("X-XSS-Protection", "0");
        // Strict Transport Security (1 year, include subdomains)
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        // Referrer Policy
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        // Content Security Policy (restrictive default)
        response.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self'");
        // Permissions Policy
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        // Remove server header
        response.setHeader("X-Powered-By", "");

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip for static assets
        return path.startsWith("/assets/") || path.endsWith(".js") || path.endsWith(".css");
    }
}
