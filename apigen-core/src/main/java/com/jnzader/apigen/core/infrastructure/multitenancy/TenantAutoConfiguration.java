package com.jnzader.apigen.core.infrastructure.multitenancy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Auto-configuration for multi-tenancy support.
 *
 * <p>Enable with:
 *
 * <pre>{@code
 * apigen.multitenancy.enabled=true
 * }</pre>
 *
 * <p>Configuration options:
 *
 * <pre>{@code
 * apigen:
 *   multitenancy:
 *     enabled: true
 *     require-tenant: true
 *     default-tenant: "default"
 *     strategies:
 *       - HEADER
 *       - SUBDOMAIN
 *     header-name: "X-Tenant-ID"
 *     path-prefix: "tenants"
 *     excluded-paths:
 *       - /actuator/**
 *       - /swagger-ui/**
 *       - /v3/api-docs/**
 * }</pre>
 */
@Configuration
@ConditionalOnProperty(prefix = "apigen.multitenancy", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(TenantAutoConfiguration.MultitenancyProperties.class)
public class TenantAutoConfiguration {

    private final MultitenancyProperties properties;

    public TenantAutoConfiguration(MultitenancyProperties properties) {
        this.properties = properties;
    }

    @Bean
    public TenantResolver tenantResolver() {
        return TenantResolver.builder()
                .strategies(properties.getStrategies())
                .defaultTenant(properties.getDefaultTenant())
                .tenantHeader(properties.getHeaderName())
                .pathPrefix(properties.getPathPrefix())
                .build();
    }

    @Bean
    public TenantFilter tenantFilter(TenantResolver tenantResolver) {
        return new TenantFilter(
                tenantResolver,
                properties.isRequireTenant(),
                new HashSet<>(properties.getExcludedPaths()));
    }

    @Bean
    public FilterRegistrationBean<TenantFilter> tenantFilterRegistration(TenantFilter filter) {
        FilterRegistrationBean<TenantFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.addUrlPatterns("/*");
        registration.setName("tenantFilter");
        return registration;
    }

    /** Configuration properties for multi-tenancy. */
    @ConfigurationProperties(prefix = "apigen.multitenancy")
    public static class MultitenancyProperties {

        /** Whether multi-tenancy is enabled. Default: false. */
        private boolean enabled = false;

        /** Whether to require a tenant for all requests. Default: true. */
        private boolean requireTenant = true;

        /** Default tenant when none is specified. Default: null (no default). */
        private String defaultTenant = null;

        /** Tenant resolution strategies in order of precedence. Default: [HEADER]. */
        private List<TenantResolutionStrategy> strategies =
                List.of(TenantResolutionStrategy.HEADER);

        /** Header name for tenant. Default: "X-Tenant-ID". */
        private String headerName = "X-Tenant-ID";

        /** Path prefix for path-based resolution. Default: "tenants". */
        private String pathPrefix = "tenants";

        /** Paths to exclude from tenant resolution. */
        private Set<String> excludedPaths =
                Set.of(
                        "/actuator/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/health",
                        "/info");

        // Getters and setters

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isRequireTenant() {
            return requireTenant;
        }

        public void setRequireTenant(boolean requireTenant) {
            this.requireTenant = requireTenant;
        }

        public String getDefaultTenant() {
            return defaultTenant;
        }

        public void setDefaultTenant(String defaultTenant) {
            this.defaultTenant = defaultTenant;
        }

        public List<TenantResolutionStrategy> getStrategies() {
            return strategies;
        }

        public void setStrategies(List<TenantResolutionStrategy> strategies) {
            this.strategies = strategies;
        }

        public String getHeaderName() {
            return headerName;
        }

        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }

        public String getPathPrefix() {
            return pathPrefix;
        }

        public void setPathPrefix(String pathPrefix) {
            this.pathPrefix = pathPrefix;
        }

        public Set<String> getExcludedPaths() {
            return excludedPaths;
        }

        public void setExcludedPaths(Set<String> excludedPaths) {
            this.excludedPaths = excludedPaths;
        }
    }
}
