package com.homebudget.config;

import com.homebudget.util.HeaderModifyingRequestWrapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter that adds default X-Hass-User header in development mode when the header is missing.
 * This improves developer experience by eliminating authentication configuration during local development.
 */
@Component
@Order(1) // Execute before other filters
public class AuthHeaderInterceptor implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(AuthHeaderInterceptor.class);
    private static final String HASS_USER_HEADER = "X-Hass-User";

    @Value("${app.dev-mode:false}")
    private boolean devMode;

    @Value("${app.default-dev-user:dev-user}")
    private String defaultDevUser;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String hassUser = httpRequest.getHeader(HASS_USER_HEADER);

        // In development mode, add default header if missing or empty
        if (devMode && (hassUser == null || hassUser.trim().isEmpty())) {
            logger.debug("Development mode: Adding default X-Hass-User header: {}", defaultDevUser);
            httpRequest = new HeaderModifyingRequestWrapper(httpRequest, HASS_USER_HEADER, defaultDevUser);
        }

        chain.doFilter(httpRequest, response);
    }
}
