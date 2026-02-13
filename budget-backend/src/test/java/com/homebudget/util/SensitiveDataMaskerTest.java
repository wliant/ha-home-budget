package com.homebudget.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataMaskerTest {

    private SensitiveDataMasker masker;

    @BeforeEach
    void setUp() {
        masker = new SensitiveDataMasker();
    }

    @Nested
    @DisplayName("mask")
    class Mask {

        @Test
        @DisplayName("should replace password values with masked text")
        void masksPassword() {
            String input = "password=secret123";
            String result = masker.mask(input);
            assertThat(result).doesNotContain("secret123");
            assertThat(result).contains("***MASKED***");
        }

        @Test
        @DisplayName("should replace token values with masked text")
        void masksToken() {
            String input = "token=abc-def-123";
            String result = masker.mask(input);
            assertThat(result).doesNotContain("abc-def-123");
            assertThat(result).contains("***MASKED***");
        }

        @Test
        @DisplayName("should replace secret values with masked text")
        void masksSecret() {
            String input = "secret: mySecretValue";
            String result = masker.mask(input);
            assertThat(result).doesNotContain("mySecretValue");
            assertThat(result).contains("***MASKED***");
        }

        @Test
        @DisplayName("should leave non-sensitive data unchanged")
        void leavesNonSensitiveUnchanged() {
            String input = "username=testuser, budgetId=42";
            String result = masker.mask(input);
            assertThat(result).isEqualTo(input);
        }

        @Test
        @DisplayName("should return null for null input")
        void returnsNullForNull() {
            assertThat(masker.mask(null)).isNull();
        }

        @Test
        @DisplayName("should return empty string for empty input")
        void returnsEmptyForEmpty() {
            assertThat(masker.mask("")).isEmpty();
        }

        @Test
        @DisplayName("should mask multiple sensitive fields in same message")
        void masksMultipleFields() {
            String input = "password=abc token=xyz";
            String result = masker.mask(input);
            assertThat(result).doesNotContain("abc");
            assertThat(result).doesNotContain("xyz");
        }
    }

    @Nested
    @DisplayName("containsSensitiveData")
    class ContainsSensitiveData {

        @Test
        @DisplayName("should return true for strings with password pattern")
        void detectsPassword() {
            assertThat(masker.containsSensitiveData("password=secret")).isTrue();
        }

        @Test
        @DisplayName("should return true for strings with token pattern")
        void detectsToken() {
            assertThat(masker.containsSensitiveData("token: abc123")).isTrue();
        }

        @Test
        @DisplayName("should return false for safe strings")
        void returnsFalseForSafe() {
            assertThat(masker.containsSensitiveData("username=testuser")).isFalse();
        }

        @Test
        @DisplayName("should return false for null")
        void returnsFalseForNull() {
            assertThat(masker.containsSensitiveData(null)).isFalse();
        }

        @Test
        @DisplayName("should return false for empty string")
        void returnsFalseForEmpty() {
            assertThat(masker.containsSensitiveData("")).isFalse();
        }
    }

    @Nested
    @DisplayName("constructor and getSensitivePatterns")
    class ConstructorAndPatterns {

        @Test
        @DisplayName("should use default patterns")
        void defaultPatterns() {
            List<String> patterns = masker.getSensitivePatterns();
            assertThat(patterns).contains("password", "token", "secret", "apiKey", "authorization", "creditCard");
        }

        @Test
        @DisplayName("should support custom patterns")
        void customPatterns() {
            SensitiveDataMasker customMasker = new SensitiveDataMasker(List.of("ssn", "dob"));
            String input = "ssn=123-45-6789";
            String result = customMasker.mask(input);
            assertThat(result).doesNotContain("123-45-6789");
            assertThat(result).contains("***MASKED***");
        }
    }
}
