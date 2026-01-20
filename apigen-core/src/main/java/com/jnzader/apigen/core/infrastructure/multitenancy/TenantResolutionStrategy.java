package com.jnzader.apigen.core.infrastructure.multitenancy;

/**
 * Strategies for resolving tenant ID from requests.
 *
 * <p>Multiple strategies can be combined for fallback behavior.
 */
public enum TenantResolutionStrategy {

    /**
     * Resolve tenant from HTTP header.
     *
     * <p>Supported headers:
     *
     * <ul>
     *   <li>{@code X-Tenant-ID}
     *   <li>Custom header (configurable)
     * </ul>
     */
    HEADER,

    /**
     * Resolve tenant from subdomain.
     *
     * <p>Example: {@code tenant1.example.com} resolves to "tenant1".
     *
     * <p>Reserved subdomains (www, api, app, admin) are excluded.
     */
    SUBDOMAIN,

    /**
     * Resolve tenant from URL path prefix.
     *
     * <p>Example: {@code /tenants/tenant1/products} resolves to "tenant1".
     *
     * <p>The path prefix is configurable (default: "tenants").
     */
    PATH,

    /**
     * Resolve tenant from JWT claim.
     *
     * <p>Requires integration with security module. The tenant ID is extracted from a JWT claim
     * (typically "tenant_id" or "tid").
     */
    JWT_CLAIM
}
