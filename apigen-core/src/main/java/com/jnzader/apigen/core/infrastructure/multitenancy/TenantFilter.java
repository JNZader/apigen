package com.jnzader.apigen.core.infrastructure.multitenancy;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet filter that resolves and sets the tenant context for each request.
 *
 * <p>This filter should be registered early in the filter chain to ensure tenant context is
 * available for all downstream processing.
 *
 * <p>Behavior:
 *
 * <ul>
 *   <li>Resolves tenant ID from the request
 *   <li>Sets the tenant in {@link TenantContext}
 *   <li>Adds X-Tenant-ID header to the response
 *   <li>Clears the context after request processing
 * </ul>
 */
public class TenantFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TenantFilter.class);

    private static final String TENANT_HEADER = "X-Tenant-ID";

    private final TenantResolver tenantResolver;
    private final boolean requireTenant;
    private final Set<String> excludedPaths;

    /**
     * Creates a new TenantFilter.
     *
     * @param tenantResolver the resolver to use
     * @param requireTenant whether to require a tenant (returns 400 if not found)
     * @param excludedPaths paths to exclude from tenant resolution
     */
    public TenantFilter(
            TenantResolver tenantResolver, boolean requireTenant, Set<String> excludedPaths) {
        this.tenantResolver = tenantResolver;
        this.requireTenant = requireTenant;
        this.excludedPaths = excludedPaths;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Check if path should be excluded
        if (isExcludedPath(httpRequest.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String tenantId = tenantResolver.resolve(httpRequest);

            if (tenantId == null) {
                if (requireTenant) {
                    log.warn(
                            "Tenant required but not found for request: {}",
                            httpRequest.getRequestURI());
                    sendTenantRequiredError(httpResponse);
                    return;
                }
                // Continue without tenant
                chain.doFilter(request, response);
                return;
            }

            // Validate tenant ID
            if (!tenantResolver.isValidTenantId(tenantId)) {
                log.warn("Invalid tenant ID: {}", tenantId);
                sendInvalidTenantError(httpResponse, tenantId);
                return;
            }

            // Set tenant context
            TenantContext.setTenantId(tenantId);

            // Add tenant header to response
            httpResponse.setHeader(TENANT_HEADER, tenantId);

            log.debug("Tenant context set to: {}", tenantId);

            chain.doFilter(request, response);

        } finally {
            TenantContext.clear();
        }
    }

    private boolean isExcludedPath(String path) {
        if (excludedPaths == null || excludedPaths.isEmpty()) {
            return false;
        }
        for (String excluded : excludedPaths) {
            if (excluded.endsWith("/**")) {
                String prefix = excluded.substring(0, excluded.length() - 3);
                if (path.startsWith(prefix)) {
                    return true;
                }
            } else if (path.equals(excluded)) {
                return true;
            }
        }
        return false;
    }

    private void sendTenantRequiredError(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/problem+json");
        response.getWriter()
                .write(
                        """
                        {
                            "type": "https://api.example.com/problems/tenant-required",
                            "title": "Tenant Required",
                            "status": 400,
                            "detail": "A tenant identifier is required for this request. Provide it via X-Tenant-ID header."
                        }
                        """);
    }

    private void sendInvalidTenantError(HttpServletResponse response, String tenantId)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/problem+json");
        response.getWriter()
                .write(
                        String.format(
                                """
                                {
                                    "type": "https://api.example.com/problems/invalid-tenant",
                                    "title": "Invalid Tenant",
                                    "status": 400,
                                    "detail": "The tenant identifier '%s' is invalid. Use alphanumeric characters and hyphens."
                                }
                                """,
                                tenantId));
    }
}
