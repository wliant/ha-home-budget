package com.homebudget.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor for logging HTTP request entry and exit with performance metrics.
 *
 * This interceptor:
 * - Logs request start with HTTP method and path at INFO level
 * - Records request start time for duration calculation
 * - Logs request completion with status code and duration at INFO level
 * - Logs slow requests (>1000ms) at WARN level for performance monitoring
 *
 * All logs include correlation_id and user_id from MDC (set by CorrelationIdFilter).
 */
public class LoggingInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);
    private static final String REQUEST_START_TIME = "requestStartTime";
    private static final long SLOW_REQUEST_THRESHOLD_MS = 1000L;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Store request start time for duration calculation
        request.setAttribute(REQUEST_START_TIME, System.currentTimeMillis());

        // Log request start (INFO level)
        logger.info("Request started: {} {}", request.getMethod(), request.getRequestURI());

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // Calculate request duration
        Long startTime = (Long) request.getAttribute(REQUEST_START_TIME);
        if (startTime == null) {
            return;
        }

        long duration = System.currentTimeMillis() - startTime;
        int status = response.getStatus();

        // Log request completion with duration
        if (duration > SLOW_REQUEST_THRESHOLD_MS) {
            // Slow request - log at WARN level for alerting
            logger.warn("Request completed: {} {} - Status: {} - Duration: {}ms (SLOW)",
                    request.getMethod(),
                    request.getRequestURI(),
                    status,
                    duration);
        } else {
            // Normal request - log at INFO level
            logger.info("Request completed: {} {} - Status: {} - Duration: {}ms",
                    request.getMethod(),
                    request.getRequestURI(),
                    status,
                    duration);
        }
    }
}
