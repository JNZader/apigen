package com.jnzader.apigen.core.infrastructure.multitenancy;

import java.util.Optional;

/**
 * Thread-local holder for the current tenant identifier.
 *
 * <p>Provides access to the tenant ID throughout the request lifecycle. The tenant is typically set
 * by a filter or interceptor at the beginning of the request and cleared at the end.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // In a filter
 * TenantContext.setTenantId("acme-corp");
 *
 * // In service layer
 * String tenantId = TenantContext.getTenantId();
 * repository.findByTenant(tenantId);
 *
 * // At request end
 * TenantContext.clear();
 * }</pre>
 *
 * <p>For virtual threads, consider using {@link #withTenant(String, Runnable)} to ensure proper
 * cleanup.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new InheritableThreadLocal<>();

    private TenantContext() {
        // Utility class
    }

    /**
     * Sets the current tenant ID.
     *
     * @param tenantId the tenant identifier
     * @throws IllegalArgumentException if tenantId is null or blank
     */
    public static void setTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or blank");
        }
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Gets the current tenant ID.
     *
     * @return the tenant ID, or null if not set
     */
    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    /**
     * Gets the current tenant ID as an Optional.
     *
     * @return Optional containing the tenant ID, or empty if not set
     */
    public static Optional<String> getCurrentTenant() {
        return Optional.ofNullable(CURRENT_TENANT.get());
    }

    /**
     * Gets the current tenant ID or throws if not set.
     *
     * @return the tenant ID
     * @throws IllegalStateException if no tenant is set
     */
    public static String requireTenantId() {
        String tenantId = CURRENT_TENANT.get();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant context available");
        }
        return tenantId;
    }

    /**
     * Gets the current tenant ID or a default value.
     *
     * @param defaultTenantId the default tenant ID
     * @return the current tenant ID or the default
     */
    public static String getTenantIdOrDefault(String defaultTenantId) {
        String tenantId = CURRENT_TENANT.get();
        return tenantId != null ? tenantId : defaultTenantId;
    }

    /**
     * Checks if a tenant context is currently set.
     *
     * @return true if a tenant is set
     */
    public static boolean hasTenant() {
        return CURRENT_TENANT.get() != null;
    }

    /**
     * Checks if the current tenant matches the expected tenant.
     *
     * @param expectedTenantId the expected tenant ID
     * @return true if the current tenant matches
     */
    public static boolean isTenant(String expectedTenantId) {
        String current = CURRENT_TENANT.get();
        return current != null && current.equals(expectedTenantId);
    }

    /** Clears the current tenant. Should be called at the end of request processing. */
    public static void clear() {
        CURRENT_TENANT.remove();
    }

    /**
     * Executes a runnable with a specific tenant context.
     *
     * <p>The tenant is set before execution and cleared after, ensuring proper cleanup even if an
     * exception occurs.
     *
     * @param tenantId the tenant ID to use
     * @param runnable the code to execute
     */
    public static void withTenant(String tenantId, Runnable runnable) {
        String previousTenant = CURRENT_TENANT.get();
        try {
            setTenantId(tenantId);
            runnable.run();
        } finally {
            if (previousTenant != null) {
                CURRENT_TENANT.set(previousTenant);
            } else {
                CURRENT_TENANT.remove();
            }
        }
    }

    /**
     * Executes a supplier with a specific tenant context.
     *
     * @param tenantId the tenant ID to use
     * @param supplier the code to execute
     * @param <T> the return type
     * @return the result of the supplier
     */
    public static <T> T withTenant(String tenantId, java.util.function.Supplier<T> supplier) {
        String previousTenant = CURRENT_TENANT.get();
        try {
            setTenantId(tenantId);
            return supplier.get();
        } finally {
            if (previousTenant != null) {
                CURRENT_TENANT.set(previousTenant);
            } else {
                CURRENT_TENANT.remove();
            }
        }
    }
}
