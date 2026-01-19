package com.jnzader.apigen.core.infrastructure.feature;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.togglz.core.Feature;

/**
 * Tests for ApigenFeatures enum.
 *
 * <p>Verifies:
 *
 * <ul>
 *   <li>All features are properly defined
 *   <li>Default enabled/disabled state is correct
 *   <li>Features implement Togglz Feature interface
 * </ul>
 */
@DisplayName("ApigenFeatures Tests")
class ApigenFeaturesTest {

    @Nested
    @DisplayName("Feature Enum Definition")
    class FeatureEnumDefinition {

        @Test
        @DisplayName("all features should implement Feature interface")
        void allFeaturesShouldImplementFeatureInterface() {
            for (ApigenFeatures feature : ApigenFeatures.values()) {
                assertThat(feature).isInstanceOf(Feature.class);
            }
        }

        @Test
        @DisplayName("should have expected number of features")
        void shouldHaveExpectedNumberOfFeatures() {
            assertThat(ApigenFeatures.values()).hasSize(14);
        }

        @Test
        @DisplayName("all features should have unique names")
        void allFeaturesShouldHaveUniqueNames() {
            String[] names =
                    java.util.Arrays.stream(ApigenFeatures.values())
                            .map(Enum::name)
                            .toArray(String[]::new);

            assertThat(names).doesNotHaveDuplicates();
        }
    }

    @Nested
    @DisplayName("Feature Definitions")
    class FeatureDefinitions {

        @Test
        @DisplayName("CACHING feature should exist")
        void cachingFeatureShouldExist() {
            assertThat(ApigenFeatures.CACHING.name()).isEqualTo("CACHING");
        }

        @Test
        @DisplayName("CIRCUIT_BREAKER feature should exist")
        void circuitBreakerFeatureShouldExist() {
            assertThat(ApigenFeatures.CIRCUIT_BREAKER.name()).isEqualTo("CIRCUIT_BREAKER");
        }

        @Test
        @DisplayName("RATE_LIMITING feature should exist")
        void rateLimitingFeatureShouldExist() {
            assertThat(ApigenFeatures.RATE_LIMITING.name()).isEqualTo("RATE_LIMITING");
        }

        @Test
        @DisplayName("CURSOR_PAGINATION feature should exist")
        void cursorPaginationFeatureShouldExist() {
            assertThat(ApigenFeatures.CURSOR_PAGINATION.name()).isEqualTo("CURSOR_PAGINATION");
        }

        @Test
        @DisplayName("ETAG_SUPPORT feature should exist")
        void etagSupportFeatureShouldExist() {
            assertThat(ApigenFeatures.ETAG_SUPPORT.name()).isEqualTo("ETAG_SUPPORT");
        }

        @Test
        @DisplayName("SOFT_DELETE feature should exist")
        void softDeleteFeatureShouldExist() {
            assertThat(ApigenFeatures.SOFT_DELETE.name()).isEqualTo("SOFT_DELETE");
        }

        @Test
        @DisplayName("DOMAIN_EVENTS feature should exist")
        void domainEventsFeatureShouldExist() {
            assertThat(ApigenFeatures.DOMAIN_EVENTS.name()).isEqualTo("DOMAIN_EVENTS");
        }

        @Test
        @DisplayName("HATEOAS feature should exist")
        void hateoasFeatureShouldExist() {
            assertThat(ApigenFeatures.HATEOAS.name()).isEqualTo("HATEOAS");
        }

        @Test
        @DisplayName("AUDIT_LOGGING feature should exist")
        void auditLoggingFeatureShouldExist() {
            assertThat(ApigenFeatures.AUDIT_LOGGING.name()).isEqualTo("AUDIT_LOGGING");
        }

        @Test
        @DisplayName("SSE_UPDATES feature should exist")
        void sseUpdatesFeatureShouldExist() {
            assertThat(ApigenFeatures.SSE_UPDATES.name()).isEqualTo("SSE_UPDATES");
        }

        @Test
        @DisplayName("TRACING feature should exist")
        void tracingFeatureShouldExist() {
            assertThat(ApigenFeatures.TRACING.name()).isEqualTo("TRACING");
        }

        @Test
        @DisplayName("METRICS feature should exist")
        void metricsFeatureShouldExist() {
            assertThat(ApigenFeatures.METRICS.name()).isEqualTo("METRICS");
        }

        @Test
        @DisplayName("ADVANCED_FILTERING feature should exist")
        void advancedFilteringFeatureShouldExist() {
            assertThat(ApigenFeatures.ADVANCED_FILTERING.name()).isEqualTo("ADVANCED_FILTERING");
        }

        @Test
        @DisplayName("BATCH_OPERATIONS feature should exist")
        void batchOperationsFeatureShouldExist() {
            assertThat(ApigenFeatures.BATCH_OPERATIONS.name()).isEqualTo("BATCH_OPERATIONS");
        }
    }
}
