package com.homebudget.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.*;

/**
 * HttpServletRequestWrapper that allows adding custom headers to the request.
 * Used by AuthHeaderInterceptor to inject default X-Hass-User header in development mode.
 */
public class HeaderModifyingRequestWrapper extends HttpServletRequestWrapper {

    private final Map<String, String> customHeaders;

    /**
     * Create a request wrapper that adds a custom header.
     *
     * @param request the original request
     * @param headerName the header name to add
     * @param headerValue the header value to add
     */
    public HeaderModifyingRequestWrapper(HttpServletRequest request, String headerName, String headerValue) {
        super(request);
        this.customHeaders = new HashMap<>();
        this.customHeaders.put(headerName, headerValue);
    }

    /**
     * Get header value, checking custom headers first before delegating to original request.
     *
     * @param name header name
     * @return header value from custom headers if present, otherwise from original request
     */
    @Override
    public String getHeader(String name) {
        String headerValue = customHeaders.get(name);
        if (headerValue != null) {
            return headerValue;
        }
        return super.getHeader(name);
    }

    /**
     * Get all values for a header, checking custom headers first.
     *
     * @param name header name
     * @return enumeration of header values
     */
    @Override
    public Enumeration<String> getHeaders(String name) {
        if (customHeaders.containsKey(name)) {
            return Collections.enumeration(Collections.singletonList(customHeaders.get(name)));
        }
        return super.getHeaders(name);
    }

    /**
     * Get all header names, including custom headers.
     *
     * @return enumeration of all header names
     */
    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> headerNames = new HashSet<>();

        // Add custom header names
        headerNames.addAll(customHeaders.keySet());

        // Add original header names
        Enumeration<String> originalHeaders = super.getHeaderNames();
        if (originalHeaders != null) {
            while (originalHeaders.hasMoreElements()) {
                headerNames.add(originalHeaders.nextElement());
            }
        }

        return Collections.enumeration(headerNames);
    }
}
