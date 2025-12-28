package com.homebudget.util;

import org.slf4j.MDC;

/**
 * Utility class for managing logging context (MDC - Mapped Diagnostic Context).
 *
 * This class provides helper methods to:
 * - Retrieve correlation ID and user ID from MDC
 * - Manually set/update MDC values (for testing or background tasks)
 * - Check if logging context is properly initialized
 *
 * MDC is populated by CorrelationIdFilter for all HTTP requests.
 */
public class LogContext {

    private static final String CORRELATION_ID_KEY = "correlation_id";
    private static final String USER_ID_KEY = "user_id";
    private static final String ANONYMOUS_USER = "anonymous";

    private LogContext() {
        // Utility class, prevent instantiation
    }

    /**
     * Get the current correlation ID from MDC.
     *
     * @return correlation ID, or null if not set
     */
    public static String getCorrelationId() {
        return MDC.get(CORRELATION_ID_KEY);
    }

    /**
     * Get the current user ID from MDC.
     *
     * @return user ID, or "anonymous" if not set
     */
    public static String getUserId() {
        String userId = MDC.get(USER_ID_KEY);
        return userId != null ? userId : ANONYMOUS_USER;
    }

    /**
     * Manually set the correlation ID in MDC.
     * Use this for background tasks or async operations that don't have HTTP context.
     *
     * @param correlationId the correlation ID to set
     */
    public static void setCorrelationId(String correlationId) {
        if (correlationId != null && !correlationId.trim().isEmpty()) {
            MDC.put(CORRELATION_ID_KEY, correlationId);
        }
    }

    /**
     * Manually set the user ID in MDC.
     * Use this for background tasks or async operations that don't have HTTP context.
     *
     * @param userId the user ID to set
     */
    public static void setUserId(String userId) {
        MDC.put(USER_ID_KEY, userId != null ? userId : ANONYMOUS_USER);
    }

    /**
     * Check if MDC context is properly initialized with correlation ID.
     *
     * @return true if correlation ID is set, false otherwise
     */
    public static boolean isContextInitialized() {
        return getCorrelationId() != null;
    }

    /**
     * Clear all MDC context.
     * This should be called after completing background tasks to prevent thread pool contamination.
     */
    public static void clear() {
        MDC.clear();
    }

    /**
     * Get all MDC context as a formatted string for debugging.
     *
     * @return formatted string with correlation_id and user_id
     */
    public static String getContextInfo() {
        return String.format("correlation_id=%s, user_id=%s",
                getCorrelationId(), getUserId());
    }
}
