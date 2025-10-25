package com.homebudget.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter to extract and process the X-Hass-User header for authentication.
 *
 * In production, this filter reads the X-Hass-User header added by the
 * Home Assistant nginx proxy, which contains the authenticated username.
 *
 * Security Model:
 * - The application trusts that requests come through Home Assistant's nginx
 * - The X-Hass-User header is considered authoritative for user identity
 * - Direct external access to this service should be blocked at the network level
 *
 * Development Mode:
 * - When running locally without Home Assistant, the header may be missing
 * - For development testing, you can manually add the header via curl or Postman
 * - Example: curl -H "X-Hass-User: testuser" http://localhost:8081/api/health
 */
@Component
public class HassUserHeaderFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(HassUserHeaderFilter.class);
    private static final String HASS_USER_HEADER = "X-Hass-User";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest httpRequest) {
            String hassUser = httpRequest.getHeader(HASS_USER_HEADER);

            if (hassUser != null && !hassUser.trim().isEmpty()) {
                log.debug("Request authenticated for user: {}", hassUser);
                // TODO: In future, store user in SecurityContext or ThreadLocal
                // For now, just log the user for verification
                request.setAttribute("hassUser", hassUser);
            } else {
                log.debug("No {} header found - request not authenticated", HASS_USER_HEADER);
                // In development without Home Assistant, this is expected
                // In production, all requests should have this header
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("HassUserHeaderFilter initialized - will process {} header", HASS_USER_HEADER);
    }

    @Override
    public void destroy() {
        log.info("HassUserHeaderFilter destroyed");
    }
}
