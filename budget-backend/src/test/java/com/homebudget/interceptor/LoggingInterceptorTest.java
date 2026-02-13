package com.homebudget.interceptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingInterceptorTest {

    private LoggingInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new LoggingInterceptor();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Nested
    @DisplayName("preHandle")
    class PreHandle {

        @Test
        @DisplayName("should return true to continue request processing")
        void returnsTrue() {
            request.setMethod("GET");
            request.setRequestURI("/api/budgets");

            boolean result = interceptor.preHandle(request, response, new Object());

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should set requestStartTime attribute")
        void setsStartTime() {
            request.setMethod("POST");
            request.setRequestURI("/api/expenses");

            interceptor.preHandle(request, response, new Object());

            assertThat(request.getAttribute("requestStartTime")).isNotNull();
            assertThat(request.getAttribute("requestStartTime")).isInstanceOf(Long.class);
        }
    }

    @Nested
    @DisplayName("afterCompletion")
    class AfterCompletion {

        @Test
        @DisplayName("should log response status and duration")
        void logsCompletionWithDuration() {
            request.setAttribute("requestStartTime", System.currentTimeMillis());
            response.setStatus(200);

            // Should not throw
            interceptor.afterCompletion(request, response, new Object(), null);
        }

        @Test
        @DisplayName("should handle exception parameter")
        void handlesException() {
            request.setAttribute("requestStartTime", System.currentTimeMillis());
            response.setStatus(500);
            RuntimeException ex = new RuntimeException("Server error");

            // Should not throw
            interceptor.afterCompletion(request, response, new Object(), ex);
        }

        @Test
        @DisplayName("should handle missing start time gracefully")
        void handlesMissingStartTime() {
            response.setStatus(200);

            // Should not throw even without start time
            interceptor.afterCompletion(request, response, new Object(), null);
        }
    }
}
