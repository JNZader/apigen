package com.jnzader.apigen.core.infrastructure.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.togglz.core.manager.FeatureManager;

/**
 * Tests for FeatureChecker.
 *
 * <p>Verifies:
 *
 * <ul>
 *   <li>Feature status checking works correctly
 *   <li>Convenience methods return expected values
 * </ul>
 */
@DisplayName("FeatureChecker Tests")
@ExtendWith(MockitoExtension.class)
class FeatureCheckerTest {

    @Mock private FeatureManager featureManager;

    private FeatureChecker featureChecker;

    @BeforeEach
    void setUp() {
        featureChecker = new FeatureChecker(featureManager);
    }

    @Nested
    @DisplayName("isActive")
    class IsActiveTests {

        @Test
        @DisplayName("should return true when feature is active")
        void shouldReturnTrueWhenFeatureIsActive() {
            when(featureManager.isActive(ApigenFeatures.CACHING)).thenReturn(true);

            assertThat(featureChecker.isActive(ApigenFeatures.CACHING)).isTrue();
        }

        @Test
        @DisplayName("should return false when feature is inactive")
        void shouldReturnFalseWhenFeatureIsInactive() {
            when(featureManager.isActive(ApigenFeatures.SSE_UPDATES)).thenReturn(false);

            assertThat(featureChecker.isActive(ApigenFeatures.SSE_UPDATES)).isFalse();
        }
    }

    @Nested
    @DisplayName("Convenience Methods")
    class ConvenienceMethodsTests {

        @Test
        @DisplayName("isCachingEnabled should check CACHING feature")
        void isCachingEnabledShouldCheckCachingFeature() {
            when(featureManager.isActive(ApigenFeatures.CACHING)).thenReturn(true);

            assertThat(featureChecker.isCachingEnabled()).isTrue();
        }

        @Test
        @DisplayName("isCircuitBreakerEnabled should check CIRCUIT_BREAKER feature")
        void isCircuitBreakerEnabledShouldCheckCircuitBreakerFeature() {
            when(featureManager.isActive(ApigenFeatures.CIRCUIT_BREAKER)).thenReturn(true);

            assertThat(featureChecker.isCircuitBreakerEnabled()).isTrue();
        }

        @Test
        @DisplayName("isRateLimitingEnabled should check RATE_LIMITING feature")
        void isRateLimitingEnabledShouldCheckRateLimitingFeature() {
            when(featureManager.isActive(ApigenFeatures.RATE_LIMITING)).thenReturn(false);

            assertThat(featureChecker.isRateLimitingEnabled()).isFalse();
        }

        @Test
        @DisplayName("isCursorPaginationEnabled should check CURSOR_PAGINATION feature")
        void isCursorPaginationEnabledShouldCheckCursorPaginationFeature() {
            when(featureManager.isActive(ApigenFeatures.CURSOR_PAGINATION)).thenReturn(true);

            assertThat(featureChecker.isCursorPaginationEnabled()).isTrue();
        }

        @Test
        @DisplayName("isEtagEnabled should check ETAG_SUPPORT feature")
        void isEtagEnabledShouldCheckEtagSupportFeature() {
            when(featureManager.isActive(ApigenFeatures.ETAG_SUPPORT)).thenReturn(true);

            assertThat(featureChecker.isEtagEnabled()).isTrue();
        }

        @Test
        @DisplayName("isSoftDeleteEnabled should check SOFT_DELETE feature")
        void isSoftDeleteEnabledShouldCheckSoftDeleteFeature() {
            when(featureManager.isActive(ApigenFeatures.SOFT_DELETE)).thenReturn(true);

            assertThat(featureChecker.isSoftDeleteEnabled()).isTrue();
        }

        @Test
        @DisplayName("isDomainEventsEnabled should check DOMAIN_EVENTS feature")
        void isDomainEventsEnabledShouldCheckDomainEventsFeature() {
            when(featureManager.isActive(ApigenFeatures.DOMAIN_EVENTS)).thenReturn(false);

            assertThat(featureChecker.isDomainEventsEnabled()).isFalse();
        }

        @Test
        @DisplayName("isHateoasEnabled should check HATEOAS feature")
        void isHateoasEnabledShouldCheckHateoasFeature() {
            when(featureManager.isActive(ApigenFeatures.HATEOAS)).thenReturn(true);

            assertThat(featureChecker.isHateoasEnabled()).isTrue();
        }

        @Test
        @DisplayName("isAuditLoggingEnabled should check AUDIT_LOGGING feature")
        void isAuditLoggingEnabledShouldCheckAuditLoggingFeature() {
            when(featureManager.isActive(ApigenFeatures.AUDIT_LOGGING)).thenReturn(true);

            assertThat(featureChecker.isAuditLoggingEnabled()).isTrue();
        }

        @Test
        @DisplayName("isSseEnabled should check SSE_UPDATES feature")
        void isSseEnabledShouldCheckSseUpdatesFeature() {
            when(featureManager.isActive(ApigenFeatures.SSE_UPDATES)).thenReturn(false);

            assertThat(featureChecker.isSseEnabled()).isFalse();
        }

        @Test
        @DisplayName("isTracingEnabled should check TRACING feature")
        void isTracingEnabledShouldCheckTracingFeature() {
            when(featureManager.isActive(ApigenFeatures.TRACING)).thenReturn(true);

            assertThat(featureChecker.isTracingEnabled()).isTrue();
        }

        @Test
        @DisplayName("isMetricsEnabled should check METRICS feature")
        void isMetricsEnabledShouldCheckMetricsFeature() {
            when(featureManager.isActive(ApigenFeatures.METRICS)).thenReturn(true);

            assertThat(featureChecker.isMetricsEnabled()).isTrue();
        }

        @Test
        @DisplayName("isAdvancedFilteringEnabled should check ADVANCED_FILTERING feature")
        void isAdvancedFilteringEnabledShouldCheckAdvancedFilteringFeature() {
            when(featureManager.isActive(ApigenFeatures.ADVANCED_FILTERING)).thenReturn(true);

            assertThat(featureChecker.isAdvancedFilteringEnabled()).isTrue();
        }

        @Test
        @DisplayName("isBatchOperationsEnabled should check BATCH_OPERATIONS feature")
        void isBatchOperationsEnabledShouldCheckBatchOperationsFeature() {
            when(featureManager.isActive(ApigenFeatures.BATCH_OPERATIONS)).thenReturn(false);

            assertThat(featureChecker.isBatchOperationsEnabled()).isFalse();
        }
    }
}
