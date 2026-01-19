package com.jnzader.apigen.core.infrastructure.multitenancy;

/**
 * Interface for entities that are tenant-aware.
 *
 * <p>Entities implementing this interface can be automatically filtered by tenant in repositories.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @Entity
 * @Table(name = "products")
 * public class Product extends Base implements TenantAware {
 *
 *     @Column(name = "tenant_id", nullable = false)
 *     private String tenantId;
 *
 *     @Override
 *     public String getTenantId() {
 *         return tenantId;
 *     }
 *
 *     @Override
 *     public void setTenantId(String tenantId) {
 *         this.tenantId = tenantId;
 *     }
 * }
 * }</pre>
 */
public interface TenantAware {

    /**
     * Gets the tenant ID for this entity.
     *
     * @return the tenant ID
     */
    String getTenantId();

    /**
     * Sets the tenant ID for this entity.
     *
     * @param tenantId the tenant ID
     */
    void setTenantId(String tenantId);
}
