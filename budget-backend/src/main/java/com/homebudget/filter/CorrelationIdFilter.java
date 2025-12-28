package com.homebudget.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that generates/extracts correlation IDs and user context for request tracking.
 *
 * This filter:
 * - Generates a unique correlation ID for each request (or extracts from X-Correlation-ID header if provided)
 * - Extracts user identifier from X-Hass-User header (Home Assistant authentication)
 * - Stores both values in MDC (Mapped Diagnostic Context) for inclusion in all log entries
 * - Adds correlation ID to response headers for client-side correlation
 * - Cleans up MDC in finally block to prevent thread pool contamination
 */
public class CorrelationIdFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(CorrelationIdFilter.class);

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String USER_HEADER = "X-Hass-User";
    private static final String CORRELATION_ID_KEY = "correlation_id";
    private static final String USER_ID_KEY = "user_id";
    private static final String ANONYMOUS_USER = "anonymous";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // Generate or extract correlation ID
            String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.trim().isEmpty()) {
                correlationId = UUID.randomUUID().toString();
            }

            // Extract user ID from Home Assistant header
            String userId = httpRequest.getHeader(USER_HEADER);
            if (userId == null || userId.trim().isEmpty()) {
                userId = ANONYMOUS_USER;
            }

            // Store in MDC for logging
            MDC.put(CORRELATION_ID_KEY, correlationId);
            MDC.put(USER_ID_KEY, userId);

            // Add correlation ID to response headers
            httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);

            logger.info("Request started: {} {}", httpRequest.getMethod(), httpRequest.getRequestURI());

            // Continue the filter chain
            chain.doFilter(request, response);

        } finally {
            // Critical: Clean up MDC to prevent thread pool contamination
            MDC.clear();
        }
    }
}
