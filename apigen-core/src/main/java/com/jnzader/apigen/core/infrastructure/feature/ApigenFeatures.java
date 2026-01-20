package com.jnzader.apigen.core.infrastructure.feature;

import org.togglz.core.Feature;
import org.togglz.core.annotation.EnabledByDefault;
import org.togglz.core.annotation.Label;
import org.togglz.core.context.FeatureContext;

/**
 * Feature flags for APiGen.
 *
 * <p>These flags allow runtime control of features without restarting the application. Features can
 * be enabled/disabled via:
 *
 * <ul>
 *   <li>Application properties: {@code togglz.features.CACHING.enabled=true}
 *   <li>Togglz console: {@code /togglz-console}
 *   <li>Actuator endpoint: {@code /actuator/togglz}
 * </ul>
 */
public enum ApigenFeatures implements Feature {

    /** Enable caching for entities and queries. */
    @EnabledByDefault
    @Label("Caching")
    CACHING,

    /** Enable circuit breaker for external service calls. */
    @EnabledByDefault
    @Label("Circuit Breaker")
    CIRCUIT_BREAKER,

    /** Enable rate limiting for API endpoints. */
    @EnabledByDefault
    @Label("Rate Limiting")
    RATE_LIMITING,

    /** Enable cursor-based pagination. When disabled, uses offset pagination. */
    @EnabledByDefault
    @Label("Cursor Pagination")
    CURSOR_PAGINATION,

    /** Enable ETag support for conditional requests. */
    @EnabledByDefault
    @Label("ETag Support")
    ETAG_SUPPORT,

    /** Enable soft delete functionality. When disabled, performs hard delete. */
    @EnabledByDefault
    @Label("Soft Delete")
    SOFT_DELETE,

    /** Enable domain event publishing. */
    @EnabledByDefault
    @Label("Domain Events")
    DOMAIN_EVENTS,

    /** Enable HATEOAS links in responses. */
    @EnabledByDefault
    @Label("HATEOAS Links")
    HATEOAS,

    /** Enable audit logging. */
    @EnabledByDefault
    @Label("Audit Logging")
    AUDIT_LOGGING,

    /** Enable Server-Sent Events for real-time updates. */
    @Label("SSE Updates")
    SSE_UPDATES,

    /** Enable OpenTelemetry tracing. */
    @EnabledByDefault
    @Label("Tracing")
    TRACING,

    /** Enable metrics collection. */
    @EnabledByDefault
    @Label("Metrics")
    METRICS,

    /** Enable advanced filtering with complex operators. */
    @EnabledByDefault
    @Label("Advanced Filtering")
    ADVANCED_FILTERING,

    /** Enable batch operations for bulk create/update/delete. */
    @Label("Batch Operations")
    BATCH_OPERATIONS;

    /**
     * Checks if this feature is currently active.
     *
     * @return true if the feature is enabled
     */
    @Override
    public boolean isActive() {
        return FeatureContext.getFeatureManager().isActive(this);
    }
}
