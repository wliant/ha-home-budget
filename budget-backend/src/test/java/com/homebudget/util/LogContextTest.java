package com.homebudget.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class LogContextTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Nested
    @DisplayName("correlationId")
    class CorrelationId {

        @Test
        @DisplayName("should set and get correlation ID")
        void setAndGetCorrelationId() {
            LogContext.setCorrelationId("test-corr-123");
            assertThat(LogContext.getCorrelationId()).isEqualTo("test-corr-123");
        }

        @Test
        @DisplayName("should return null when not set")
        void returnsNullWhenNotSet() {
            assertThat(LogContext.getCorrelationId()).isNull();
        }

        @Test
        @DisplayName("should not set blank correlation ID")
        void ignoresBlankCorrelationId() {
            LogContext.setCorrelationId("  ");
            assertThat(LogContext.getCorrelationId()).isNull();
        }

        @Test
        @DisplayName("should not set null correlation ID")
        void ignoresNullCorrelationId() {
            LogContext.setCorrelationId(null);
            assertThat(LogContext.getCorrelationId()).isNull();
        }
    }

    @Nested
    @DisplayName("userId")
    class UserId {

        @Test
        @DisplayName("should set and get user ID")
        void setAndGetUserId() {
            LogContext.setUserId("testuser");
            assertThat(LogContext.getUserId()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("should return anonymous when not set")
        void returnsAnonymousWhenNotSet() {
            assertThat(LogContext.getUserId()).isEqualTo("anonymous");
        }

        @Test
        @DisplayName("should set anonymous for null user ID")
        void setsAnonymousForNull() {
            LogContext.setUserId(null);
            assertThat(LogContext.getUserId()).isEqualTo("anonymous");
        }
    }

    @Nested
    @DisplayName("clear")
    class Clear {

        @Test
        @DisplayName("should remove both correlationId and userId from MDC")
        void clearsAll() {
            LogContext.setCorrelationId("corr-1");
            LogContext.setUserId("user-1");

            LogContext.clear();

            assertThat(LogContext.getCorrelationId()).isNull();
            assertThat(LogContext.getUserId()).isEqualTo("anonymous");
        }
    }

    @Nested
    @DisplayName("isContextInitialized")
    class IsContextInitialized {

        @Test
        @DisplayName("should return true after correlationId is set")
        void returnsTrueWhenSet() {
            LogContext.setCorrelationId("corr-1");
            assertThat(LogContext.isContextInitialized()).isTrue();
        }

        @Test
        @DisplayName("should return false when correlationId is not set")
        void returnsFalseWhenNotSet() {
            assertThat(LogContext.isContextInitialized()).isFalse();
        }

        @Test
        @DisplayName("should return false after clear")
        void returnsFalseAfterClear() {
            LogContext.setCorrelationId("corr-1");
            LogContext.clear();
            assertThat(LogContext.isContextInitialized()).isFalse();
        }
    }

    @Nested
    @DisplayName("getContextInfo")
    class GetContextInfo {

        @Test
        @DisplayName("should return formatted string with both values")
        void returnsFormattedString() {
            LogContext.setCorrelationId("corr-abc");
            LogContext.setUserId("testuser");

            String info = LogContext.getContextInfo();
            assertThat(info).contains("corr-abc");
            assertThat(info).contains("testuser");
        }

        @Test
        @DisplayName("should show null and anonymous when not set")
        void showsDefaultValues() {
            String info = LogContext.getContextInfo();
            assertThat(info).contains("null");
            assertThat(info).contains("anonymous");
        }
    }
}
