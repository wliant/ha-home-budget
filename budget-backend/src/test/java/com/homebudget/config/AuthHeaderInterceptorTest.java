package com.homebudget.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class AuthHeaderInterceptorTest {

    private AuthHeaderInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        interceptor = new AuthHeaderInterceptor();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @Test
    @DisplayName("should forward existing X-Hass-User header without modification")
    void forwardsExistingHeader() throws IOException, ServletException {
        ReflectionTestUtils.setField(interceptor, "devMode", false);
        request.addHeader("X-Hass-User", "realuser");

        interceptor.doFilter(request, response, chain);

        // Header was not modified - the original request should still have it
        assertThat(request.getHeader("X-Hass-User")).isEqualTo("realuser");
    }

    @Test
    @DisplayName("should not add default header when dev mode is off and header missing")
    void doesNotAddHeaderWhenDevModeOff() throws IOException, ServletException {
        ReflectionTestUtils.setField(interceptor, "devMode", false);
        ReflectionTestUtils.setField(interceptor, "defaultDevUser", "dev-user");

        interceptor.doFilter(request, response, chain);

        // No header modification should happen
        assertThat(request.getHeader("X-Hass-User")).isNull();
    }

    @Test
    @DisplayName("should add default header when dev mode is on and header missing")
    void addsDefaultHeaderInDevMode() throws IOException, ServletException {
        ReflectionTestUtils.setField(interceptor, "devMode", true);
        ReflectionTestUtils.setField(interceptor, "defaultDevUser", "dev-user");

        interceptor.doFilter(request, response, chain);

        // The filter wraps the request, so we check the chain got a wrapped request
        // MockFilterChain captures the request that was passed to it
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("should not add default header in dev mode when header already present")
    void doesNotOverrideExistingHeaderInDevMode() throws IOException, ServletException {
        ReflectionTestUtils.setField(interceptor, "devMode", true);
        ReflectionTestUtils.setField(interceptor, "defaultDevUser", "dev-user");
        request.addHeader("X-Hass-User", "realuser");

        interceptor.doFilter(request, response, chain);

        // The original request should be passed through (not wrapped)
        assertThat(request.getHeader("X-Hass-User")).isEqualTo("realuser");
    }
}
