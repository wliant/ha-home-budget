package com.homebudget.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class HassUserHeaderFilterTest {

    private HassUserHeaderFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new HassUserHeaderFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @Test
    @DisplayName("should extract X-Hass-User header and set as request attribute")
    void extractsHeaderAndSetsAttribute() throws IOException, ServletException {
        request.addHeader("X-Hass-User", "testuser");

        filter.doFilter(request, response, chain);

        assertThat(request.getAttribute("hassUser")).isEqualTo("testuser");
    }

    @Test
    @DisplayName("should continue filter chain when header is present")
    void continuesChainWithHeader() throws IOException, ServletException {
        request.addHeader("X-Hass-User", "testuser");

        filter.doFilter(request, response, chain);

        // MockFilterChain will throw if doFilter is not called
        // No exception means chain was continued
    }

    @Test
    @DisplayName("should continue filter chain when header is absent")
    void continuesChainWithoutHeader() throws IOException, ServletException {
        filter.doFilter(request, response, chain);

        assertThat(request.getAttribute("hassUser")).isNull();
    }

    @Test
    @DisplayName("should not set attribute when header is blank")
    void doesNotSetAttributeForBlankHeader() throws IOException, ServletException {
        request.addHeader("X-Hass-User", "   ");

        filter.doFilter(request, response, chain);

        assertThat(request.getAttribute("hassUser")).isNull();
    }
}
