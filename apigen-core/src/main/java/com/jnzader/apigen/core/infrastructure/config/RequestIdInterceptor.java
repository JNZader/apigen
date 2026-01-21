package com.jnzader.apigen.core.infrastructure.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Interceptor that adds a unique Request ID to each HTTP request.
 *
 * <p>The Request ID is used for: - Request traceability through logs - Error correlation in
 * distributed systems - Debugging and customer support
 *
 * <p>The ID is obtained from the X-Request-ID header if it exists, or a new one is generated. It is
 * added to the SLF4J MDC for logging and returned in the response header.
 */
@Component
public class RequestIdInterceptor implements HandlerInterceptor {

    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String MDC_REQUEST_ID_KEY = "requestId";

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Get Request ID from header or generate a new one
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = generateRequestId();
        }

        // Add to MDC for logging
        MDC.put(MDC_REQUEST_ID_KEY, requestId);

        // Add to response header
        response.setHeader(REQUEST_ID_HEADER, requestId);

        return true;
    }

    @Override
    public void postHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            ModelAndView modelAndView) {
        // No action needed
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {
        // Clean MDC at the end of the request
        MDC.remove(MDC_REQUEST_ID_KEY);
    }

    /**
     * Generates a unique Request ID.
     *
     * <p>Uses truncated UUID v4 for balance between uniqueness and readability.
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
