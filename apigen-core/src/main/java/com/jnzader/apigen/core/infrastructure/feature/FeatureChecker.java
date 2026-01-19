package com.jnzader.apigen.core.infrastructure.feature;

import org.springframework.stereotype.Component;
import org.togglz.core.manager.FeatureManager;

/**
 * Utility component for checking feature flag status.
 *
 * <p>Provides a Spring-managed way to check features, useful for injection into services.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @Service
 * public class MyService {
 *     private final FeatureChecker features;
 *
 *     public void doSomething() {
 *         if (features.isActive(ApigenFeatures.CACHING)) {
 *             // Use caching
 *         }
 *     }
 * }
 * }</pre>
 */
@Component
public class FeatureChecker {

    private final FeatureManager featureManager;

    public FeatureChecker(FeatureManager featureManager) {
        this.featureManager = featureManager;
    }

    /**
     * Checks if a feature is currently active.
     *
     * @param feature the feature to check
     * @return true if the feature is enabled
     */
    public boolean isActive(ApigenFeatures feature) {
        return featureManager.isActive(feature);
    }

    /**
     * Checks if caching is enabled.
     *
     * @return true if CACHING feature is active
     */
    public boolean isCachingEnabled() {
        return isActive(ApigenFeatures.CACHING);
    }

    /**
     * Checks if circuit breaker is enabled.
     *
     * @return true if CIRCUIT_BREAKER feature is active
     */
    public boolean isCircuitBreakerEnabled() {
        return isActive(ApigenFeatures.CIRCUIT_BREAKER);
    }

    /**
     * Checks if rate limiting is enabled.
     *
     * @return true if RATE_LIMITING feature is active
     */
    public boolean isRateLimitingEnabled() {
        return isActive(ApigenFeatures.RATE_LIMITING);
    }

    /**
     * Checks if cursor pagination is enabled.
     *
     * @return true if CURSOR_PAGINATION feature is active
     */
    public boolean isCursorPaginationEnabled() {
        return isActive(ApigenFeatures.CURSOR_PAGINATION);
    }

    /**
     * Checks if ETag support is enabled.
     *
     * @return true if ETAG_SUPPORT feature is active
     */
    public boolean isEtagEnabled() {
        return isActive(ApigenFeatures.ETAG_SUPPORT);
    }

    /**
     * Checks if soft delete is enabled.
     *
     * @return true if SOFT_DELETE feature is active
     */
    public boolean isSoftDeleteEnabled() {
        return isActive(ApigenFeatures.SOFT_DELETE);
    }

    /**
     * Checks if domain events are enabled.
     *
     * @return true if DOMAIN_EVENTS feature is active
     */
    public boolean isDomainEventsEnabled() {
        return isActive(ApigenFeatures.DOMAIN_EVENTS);
    }

    /**
     * Checks if HATEOAS is enabled.
     *
     * @return true if HATEOAS feature is active
     */
    public boolean isHateoasEnabled() {
        return isActive(ApigenFeatures.HATEOAS);
    }

    /**
     * Checks if audit logging is enabled.
     *
     * @return true if AUDIT_LOGGING feature is active
     */
    public boolean isAuditLoggingEnabled() {
        return isActive(ApigenFeatures.AUDIT_LOGGING);
    }

    /**
     * Checks if SSE updates are enabled.
     *
     * @return true if SSE_UPDATES feature is active
     */
    public boolean isSseEnabled() {
        return isActive(ApigenFeatures.SSE_UPDATES);
    }

    /**
     * Checks if tracing is enabled.
     *
     * @return true if TRACING feature is active
     */
    public boolean isTracingEnabled() {
        return isActive(ApigenFeatures.TRACING);
    }

    /**
     * Checks if metrics collection is enabled.
     *
     * @return true if METRICS feature is active
     */
    public boolean isMetricsEnabled() {
        return isActive(ApigenFeatures.METRICS);
    }

    /**
     * Checks if advanced filtering is enabled.
     *
     * @return true if ADVANCED_FILTERING feature is active
     */
    public boolean isAdvancedFilteringEnabled() {
        return isActive(ApigenFeatures.ADVANCED_FILTERING);
    }

    /**
     * Checks if batch operations are enabled.
     *
     * @return true if BATCH_OPERATIONS feature is active
     */
    public boolean isBatchOperationsEnabled() {
        return isActive(ApigenFeatures.BATCH_OPERATIONS);
    }
}
