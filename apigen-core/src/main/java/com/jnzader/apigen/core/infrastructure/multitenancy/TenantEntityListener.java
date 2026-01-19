package com.jnzader.apigen.core.infrastructure.multitenancy;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JPA entity listener that automatically sets tenant ID on entities.
 *
 * <p>Use with {@code @EntityListeners(TenantEntityListener.class)} on your entities.
 *
 * <p>Example:
 *
 * <pre>{@code
 * @Entity
 * @EntityListeners(TenantEntityListener.class)
 * public class Product extends Base implements TenantAware {
 *     // ...
 * }
 * }</pre>
 *
 * <p>Or apply globally via orm.xml:
 *
 * <pre>{@code
 * <entity-listeners>
 *     <entity-listener class="com.jnzader.apigen.core.infrastructure.multitenancy.TenantEntityListener"/>
 * </entity-listeners>
 * }</pre>
 */
public class TenantEntityListener {

    private static final Logger log = LoggerFactory.getLogger(TenantEntityListener.class);

    /**
     * Sets the tenant ID before persisting a new entity.
     *
     * @param entity the entity being persisted
     */
    @PrePersist
    public void setTenantOnCreate(Object entity) {
        if (entity instanceof TenantAware tenantAware) {
            if (tenantAware.getTenantId() == null) {
                String tenantId = TenantContext.getTenantId();
                if (tenantId != null) {
                    tenantAware.setTenantId(tenantId);
                    log.debug(
                            "Set tenant ID '{}' on new entity: {}",
                            tenantId,
                            entity.getClass().getSimpleName());
                } else {
                    log.warn(
                            "No tenant context available when creating entity: {}",
                            entity.getClass().getSimpleName());
                }
            }
        }
    }

    /**
     * Validates tenant ID before updating an entity.
     *
     * @param entity the entity being updated
     */
    @PreUpdate
    public void validateTenantOnUpdate(Object entity) {
        if (entity instanceof TenantAware tenantAware) {
            String currentTenant = TenantContext.getTenantId();
            String entityTenant = tenantAware.getTenantId();

            if (currentTenant != null && entityTenant != null && !currentTenant.equals(entityTenant)) {
                log.error(
                        "Tenant mismatch on update! Entity tenant: {}, Context tenant: {}",
                        entityTenant,
                        currentTenant);
                throw new TenantMismatchException(currentTenant, entityTenant);
            }
        }
    }
}
