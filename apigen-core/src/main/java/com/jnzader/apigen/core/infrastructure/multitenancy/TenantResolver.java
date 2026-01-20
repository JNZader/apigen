package com.jnzader.apigen.core.infrastructure.multitenancy;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves tenant ID from incoming HTTP requests.
 *
 * <p>Supports multiple resolution strategies:
 *
 * <ul>
 *   <li>HTTP Header (e.g., X-Tenant-ID)
 *   <li>Subdomain (e.g., tenant1.example.com)
 *   <li>Path prefix (e.g., /api/tenant1/products)
 *   <li>JWT claim (requires integration with security module)
 * </ul>
 */
public class TenantResolver {

    private static final Logger log = LoggerFactory.getLogger(TenantResolver.class);

    private static final String DEFAULT_TENANT_HEADER = "X-Tenant-ID";

    private final List<TenantResolutionStrategy> strategies;
    private final String defaultTenant;
    private final String tenantHeader;
    private final String pathPrefix;
    private final Pattern subdomainPattern;
    private final Pattern pathPattern;

    private TenantResolver(Builder builder) {
        this.strategies = builder.strategies;
        this.defaultTenant = builder.defaultTenant;
        this.tenantHeader = builder.tenantHeader;
        this.pathPrefix = builder.pathPrefix;
        this.subdomainPattern = Pattern.compile("^([a-zA-Z0-9-]+)\\..+");
        this.pathPattern =
                Pattern.compile("^/" + Pattern.quote(pathPrefix) + "/([a-zA-Z0-9-]+)(?:/.*)?$");
    }

    /**
     * Resolves the tenant ID from the request using configured strategies.
     *
     * @param request the HTTP request
     * @return the resolved tenant ID, or default if none found
     */
    public String resolve(HttpServletRequest request) {
        for (TenantResolutionStrategy strategy : strategies) {
            Optional<String> tenant = resolveByStrategy(request, strategy);
            if (tenant.isPresent()) {
                log.debug("Resolved tenant '{}' using strategy {}", tenant.get(), strategy);
                return tenant.get();
            }
        }

        if (defaultTenant != null) {
            log.debug("No tenant found, using default: {}", defaultTenant);
            return defaultTenant;
        }

        log.warn("No tenant could be resolved from request");
        return null;
    }

    /**
     * Resolves tenant using a specific strategy.
     *
     * @param request the HTTP request
     * @param strategy the strategy to use
     * @return the tenant ID if found
     */
    public Optional<String> resolveByStrategy(
            HttpServletRequest request, TenantResolutionStrategy strategy) {
        return switch (strategy) {
            case HEADER -> resolveFromHeader(request);
            case SUBDOMAIN -> resolveFromSubdomain(request);
            case PATH -> resolveFromPath(request);
            case JWT_CLAIM -> resolveFromJwtClaim(request);
        };
    }

    private Optional<String> resolveFromHeader(HttpServletRequest request) {
        String tenant = request.getHeader(tenantHeader);
        if (tenant == null) {
            tenant = request.getHeader(DEFAULT_TENANT_HEADER);
        }
        return Optional.ofNullable(tenant).map(String::trim).filter(t -> !t.isEmpty());
    }

    private Optional<String> resolveFromSubdomain(HttpServletRequest request) {
        String host = request.getServerName();
        if (host == null) {
            return Optional.empty();
        }

        Matcher matcher = subdomainPattern.matcher(host);
        if (matcher.matches()) {
            String subdomain = matcher.group(1);
            // Exclude common non-tenant subdomains
            if (!isReservedSubdomain(subdomain)) {
                return Optional.of(subdomain);
            }
        }
        return Optional.empty();
    }

    private boolean isReservedSubdomain(String subdomain) {
        return subdomain.equalsIgnoreCase("www")
                || subdomain.equalsIgnoreCase("api")
                || subdomain.equalsIgnoreCase("app")
                || subdomain.equalsIgnoreCase("admin")
                || subdomain.equalsIgnoreCase("localhost");
    }

    private Optional<String> resolveFromPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return Optional.empty();
        }

        Matcher matcher = pathPattern.matcher(path);
        if (matcher.matches()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

    private Optional<String> resolveFromJwtClaim(HttpServletRequest request) {
        // JWT claim resolution requires security module integration
        // Check for pre-resolved tenant from security filter
        Object tenant = request.getAttribute("tenant.id");
        if (tenant instanceof String tenantStr) {
            return Optional.of(tenantStr);
        }
        return Optional.empty();
    }

    /**
     * Validates if a tenant ID is valid.
     *
     * @param tenantId the tenant ID to validate
     * @return true if valid
     */
    public boolean isValidTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return false;
        }
        // Alphanumeric with hyphens, 2-50 chars
        return tenantId.matches("^[a-zA-Z0-9][a-zA-Z0-9-]{1,48}[a-zA-Z0-9]$");
    }

    /** Creates a new builder for TenantResolver. */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for TenantResolver. */
    public static class Builder {
        private List<TenantResolutionStrategy> strategies =
                List.of(TenantResolutionStrategy.HEADER);
        private String defaultTenant = null;
        private String tenantHeader = DEFAULT_TENANT_HEADER;
        private String pathPrefix = "tenants";

        public Builder strategies(List<TenantResolutionStrategy> strategies) {
            this.strategies = strategies;
            return this;
        }

        public Builder strategies(TenantResolutionStrategy... strategies) {
            this.strategies = List.of(strategies);
            return this;
        }

        public Builder defaultTenant(String defaultTenant) {
            this.defaultTenant = defaultTenant;
            return this;
        }

        public Builder tenantHeader(String tenantHeader) {
            this.tenantHeader = tenantHeader;
            return this;
        }

        public Builder pathPrefix(String pathPrefix) {
            this.pathPrefix = pathPrefix;
            return this;
        }

        public TenantResolver build() {
            return new TenantResolver(this);
        }
    }
}
