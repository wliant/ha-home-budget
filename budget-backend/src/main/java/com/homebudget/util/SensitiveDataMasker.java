package com.homebudget.util;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Utility class for masking sensitive data in log messages.
 *
 * This class uses regex patterns to detect and mask sensitive fields like:
 * - password
 * - token
 * - secret
 * - apiKey
 * - authorization
 * - creditCard
 *
 * Masked values are replaced with "***MASKED***" to prevent sensitive data leakage in logs.
 */
public class SensitiveDataMasker {

    private static final String MASK_VALUE = "***MASKED***";

    // Default sensitive field patterns (case-insensitive)
    private static final List<String> DEFAULT_SENSITIVE_PATTERNS = List.of(
            "password",
            "token",
            "secret",
            "apiKey",
            "authorization",
            "creditCard"
    );

    private final List<Pattern> compiledPatterns;

    /**
     * Create a SensitiveDataMasker with default sensitive field patterns.
     */
    public SensitiveDataMasker() {
        this(DEFAULT_SENSITIVE_PATTERNS);
    }

    /**
     * Create a SensitiveDataMasker with custom sensitive field patterns.
     *
     * @param sensitiveFields list of field names to mask (case-insensitive)
     */
    public SensitiveDataMasker(List<String> sensitiveFields) {
        this.compiledPatterns = sensitiveFields.stream()
                .map(field -> Pattern.compile(
                        "\\b" + field + "\\s*[=:]\\s*['\"]?([^'\"\\s,}]+)['\"]?",
                        Pattern.CASE_INSENSITIVE
                ))
                .toList();
    }

    /**
     * Mask sensitive data in a log message.
     *
     * This method scans the message for patterns like:
     * - password=value
     * - token: value
     * - secret="value"
     *
     * And replaces the value portion with ***MASKED***
     *
     * @param message the log message to mask
     * @return masked log message
     */
    public String mask(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        String maskedMessage = message;

        for (Pattern pattern : compiledPatterns) {
            Matcher matcher = pattern.matcher(maskedMessage);
            StringBuffer result = new StringBuffer();

            while (matcher.find()) {
                // Replace the captured group (value) with MASK_VALUE
                String replacement = matcher.group(0).substring(0, matcher.start(1) - matcher.start())
                        + MASK_VALUE;
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(result);
            maskedMessage = result.toString();
        }

        return maskedMessage;
    }

    /**
     * Check if a message contains sensitive data patterns.
     *
     * @param message the message to check
     * @return true if message contains sensitive field patterns, false otherwise
     */
    public boolean containsSensitiveData(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }

        for (Pattern pattern : compiledPatterns) {
            if (pattern.matcher(message).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get the list of sensitive field patterns being used.
     *
     * @return list of sensitive field names
     */
    public List<String> getSensitivePatterns() {
        return DEFAULT_SENSITIVE_PATTERNS;
    }
}
