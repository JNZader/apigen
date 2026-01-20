package com.jnzader.apigen.core.infrastructure.multitenancy;

/**
 * Exception thrown when there is a tenant mismatch during entity operations.
 *
 * <p>This typically occurs when attempting to update or delete an entity belonging to a different
 * tenant than the current context.
 */
public class TenantMismatchException extends RuntimeException {

    private final String expectedTenant;
    private final String actualTenant;

    /**
     * Creates a new TenantMismatchException.
     *
     * @param expectedTenant the expected tenant (from context)
     * @param actualTenant the actual tenant (from entity)
     */
    public TenantMismatchException(String expectedTenant, String actualTenant) {
        super(
                String.format(
                        "Tenant mismatch: expected '%s' but found '%s'",
                        expectedTenant, actualTenant));
        this.expectedTenant = expectedTenant;
        this.actualTenant = actualTenant;
    }

    public String getExpectedTenant() {
        return expectedTenant;
    }

    public String getActualTenant() {
        return actualTenant;
    }
}
