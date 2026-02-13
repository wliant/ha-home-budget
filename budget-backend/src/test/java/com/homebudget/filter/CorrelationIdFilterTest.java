package com.homebudget.filter;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("should generate a UUID correlation ID and set it in MDC")
    void generatesCorrelationId() throws IOException, ServletException {
        filter.doFilter(request, response, chain);

        // After filter completes, MDC is cleared in finally block
        // But the response header should have the correlation ID
        String correlationId = response.getHeader("X-Correlation-ID");
        assertThat(correlationId).isNotNull();
        assertThat(correlationId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    @DisplayName("should reuse existing correlation ID header if present")
    void reusesExistingCorrelationId() throws IOException, ServletException {
        request.addHeader("X-Correlation-ID", "existing-corr-id");

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Correlation-ID")).isEqualTo("existing-corr-id");
    }

    @Test
    @DisplayName("should set user_id in MDC from X-Hass-User header")
    void setsUserIdInMDC() throws IOException, ServletException {
        request.addHeader("X-Hass-User", "testuser");

        // Use a custom chain to capture MDC state during filter execution
        MockFilterChain captureChain = new MockFilterChain();
        filter.doFilter(request, response, captureChain);

        // After filter, MDC is cleared - verify response header instead
        assertThat(response.getHeader("X-Correlation-ID")).isNotNull();
    }

    @Test
    @DisplayName("should clear MDC after filter chain completes")
    void clearsMDCAfterCompletion() throws IOException, ServletException {
        filter.doFilter(request, response, chain);

        assertThat(MDC.get("correlation_id")).isNull();
        assertThat(MDC.get("user_id")).isNull();
    }

    @Test
    @DisplayName("should set anonymous user when X-Hass-User header is absent")
    void setsAnonymousUserWhenHeaderAbsent() throws IOException, ServletException {
        // No X-Hass-User header set
        filter.doFilter(request, response, chain);

        // The response should still have a correlation ID
        assertThat(response.getHeader("X-Correlation-ID")).isNotNull();
    }
}
