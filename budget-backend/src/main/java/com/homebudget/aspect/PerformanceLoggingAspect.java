package com.homebudget.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Performance Logging Aspect for Service Layer Methods.
 *
 * Implements User Story 4: Request and Performance Logging
 * - Measures execution time for all service layer methods
 * - Logs at DEBUG level for all methods with duration
 * - Logs at WARN level for slow methods (>100ms)
 * - Includes method name, masked arguments, and duration
 *
 * Uses Spring AOP to intercept service method calls without modifying business logic.
 */
@Aspect
@Component
public class PerformanceLoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceLoggingAspect.class);

    /**
     * Slow query threshold in milliseconds.
     * Methods taking longer than this will be logged at WARN level.
     */
    private static final long SLOW_METHOD_THRESHOLD_MS = 100;

    /**
     * Pointcut for all service layer methods.
     * Matches: com.homebudget.service.*.* (all methods in all classes in service package)
     */
    @Pointcut("execution(* com.homebudget.service..*.*(..))")
    public void serviceMethods() {
        // Pointcut definition
    }

    /**
     * Around advice to measure and log method execution time.
     *
     * @param joinPoint the method execution join point
     * @return the method return value
     * @throws Throwable if the method throws an exception
     */
    @Around("serviceMethods()")
    public Object logMethodPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String maskedArgs = maskArguments(joinPoint.getArgs());

        long startTime = System.currentTimeMillis();

        try {
            // Execute the actual method
            Object result = joinPoint.proceed();

            long duration = System.currentTimeMillis() - startTime;

            // Log performance metrics
            logPerformance(className, methodName, maskedArgs, duration, null);

            return result;
        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - startTime;

            // Log performance even for failed methods
            logPerformance(className, methodName, maskedArgs, duration, ex.getClass().getSimpleName());

            throw ex;
        }
    }

    /**
     * Log method performance at appropriate level.
     *
     * @param className the class name
     * @param methodName the method name
     * @param maskedArgs the masked arguments
     * @param duration the execution duration in milliseconds
     * @param exceptionType the exception type if method failed, null if successful
     */
    private void logPerformance(String className, String methodName, String maskedArgs,
                                long duration, String exceptionType) {
        String status = (exceptionType != null) ? "FAILED" : "SUCCESS";
        String exceptionInfo = (exceptionType != null) ? ", exception=" + exceptionType : "";

        if (duration > SLOW_METHOD_THRESHOLD_MS) {
            // Slow method - log at WARN level
            logger.warn("SLOW METHOD: {}.{}({}) - Status: {}, Duration: {}ms{}",
                    className, methodName, maskedArgs, status, duration, exceptionInfo);
        } else if (logger.isDebugEnabled()) {
            // Normal method - log at DEBUG level only if DEBUG is enabled
            logger.debug("Method execution: {}.{}({}) - Status: {}, Duration: {}ms{}",
                    className, methodName, maskedArgs, status, duration, exceptionInfo);
        }
    }

    /**
     * Mask sensitive arguments in method parameters.
     * Replaces actual values with type information to avoid logging sensitive data.
     *
     * @param args the method arguments
     * @return masked string representation of arguments
     */
    private String maskArguments(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }

            Object arg = args[i];
            if (arg == null) {
                sb.append("null");
            } else {
                // Show type instead of value to avoid logging sensitive data
                String typeName = arg.getClass().getSimpleName();
                sb.append(typeName);

                // For primitive wrappers and strings, show the value only if not sensitive
                if (isPrimitiveOrWrapper(arg) && !isSensitiveParameter(arg)) {
                    sb.append("=").append(arg);
                }
            }
        }

        return sb.toString();
    }

    /**
     * Check if argument is a primitive type or wrapper.
     */
    private boolean isPrimitiveOrWrapper(Object arg) {
        return arg instanceof Number || arg instanceof Boolean || arg instanceof Character;
    }

    /**
     * Check if parameter value might be sensitive.
     * Simple heuristic to avoid logging potential passwords, tokens, etc.
     */
    private boolean isSensitiveParameter(Object arg) {
        if (!(arg instanceof String)) {
            return false;
        }

        String str = arg.toString().toLowerCase();
        return str.contains("password") || str.contains("token") ||
               str.contains("secret") || str.contains("apikey");
    }
}
