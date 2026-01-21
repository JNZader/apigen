package com.jnzader.apigen.security.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.jnzader.apigen.security.infrastructure.config.SecurityProperties;
import com.jnzader.apigen.security.infrastructure.config.SecurityProperties.RateLimitProperties;
import com.jnzader.apigen.security.infrastructure.config.SecurityProperties.RateLimitProperties.StorageMode;
import com.jnzader.apigen.security.infrastructure.config.SecurityProperties.RateLimitProperties.TierConfig;
import com.jnzader.apigen.security.infrastructure.config.SecurityProperties.RateLimitProperties.TiersConfig;
import io.github.bucket4j.ConsumptionProbe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitService")
class RateLimitServiceTest {

    @Mock private RedisConnectionFactory redisConnectionFactory;

    private SecurityProperties securityProperties;
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        securityProperties = new SecurityProperties();
        RateLimitProperties rateLimitProperties = new RateLimitProperties();
        rateLimitProperties.setEnabled(true);
        rateLimitProperties.setStorageMode(StorageMode.IN_MEMORY);
        rateLimitProperties.setRequestsPerSecond(100);
        rateLimitProperties.setBurstCapacity(150);
        rateLimitProperties.setAuthRequestsPerMinute(10);
        rateLimitProperties.setAuthBurstCapacity(15);
        rateLimitProperties.setRedisKeyPrefix("rate-limit:");
        securityProperties.setRateLimit(rateLimitProperties);

        rateLimitService = new RateLimitService(securityProperties, redisConnectionFactory);
        rateLimitService.init();
    }

    @Nested
    @DisplayName("tryConsume - API rate limiting")
    class TryConsumeTests {

        @Test
        @DisplayName("should allow requests within limit")
        void shouldAllowRequestsWithinLimit() {
            // When
            boolean result = rateLimitService.tryConsume("192.168.1.1");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should allow multiple requests within burst capacity")
        void shouldAllowMultipleRequestsWithinBurstCapacity() {
            // When - consume multiple tokens
            String key = "192.168.1.2";
            for (int i = 0; i < 50; i++) {
                assertThat(rateLimitService.tryConsume(key)).isTrue();
            }

            // Then - should still have capacity
            assertThat(rateLimitService.getAvailableTokens(key, false)).isGreaterThan(0);
        }

        @Test
        @DisplayName("should block requests when limit exceeded")
        void shouldBlockRequestsWhenLimitExceeded() {
            // Given - exhaust the bucket
            String key = "192.168.1.3";
            for (int i = 0; i < 150; i++) {
                rateLimitService.tryConsume(key);
            }

            // When - try one more
            boolean result = rateLimitService.tryConsume(key);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should use separate buckets for different keys")
        void shouldUseSeparateBucketsForDifferentKeys() {
            // Given
            String key1 = "192.168.1.4";
            String key2 = "192.168.1.5";

            // Exhaust first key
            for (int i = 0; i < 150; i++) {
                rateLimitService.tryConsume(key1);
            }

            // When
            boolean result1 = rateLimitService.tryConsume(key1);
            boolean result2 = rateLimitService.tryConsume(key2);

            // Then
            assertThat(result1).isFalse();
            assertThat(result2).isTrue();
        }
    }

    @Nested
    @DisplayName("tryConsume - Auth endpoint rate limiting")
    class TryConsumeAuthTests {

        @Test
        @DisplayName("should have more restrictive limits for auth endpoints")
        void shouldHaveMoreRestrictiveLimitsForAuth() {
            // Given
            String key = "192.168.1.10";

            // When - exhaust auth bucket (burstCapacity = 15)
            for (int i = 0; i < 15; i++) {
                assertThat(rateLimitService.tryConsume(key, true)).isTrue();
            }

            // Then - next request should be blocked
            assertThat(rateLimitService.tryConsume(key, true)).isFalse();
        }

        @Test
        @DisplayName("should use separate buckets for auth and API")
        void shouldUseSeparateBucketsForAuthAndApi() {
            // Given
            String key = "192.168.1.11";

            // Exhaust auth bucket
            for (int i = 0; i < 15; i++) {
                rateLimitService.tryConsume(key, true);
            }

            // When
            boolean authResult = rateLimitService.tryConsume(key, true);
            boolean apiResult = rateLimitService.tryConsume(key, false);

            // Then - auth blocked but API still works
            assertThat(authResult).isFalse();
            assertThat(apiResult).isTrue();
        }
    }

    @Nested
    @DisplayName("tryConsumeAndReturnRemaining")
    class TryConsumeAndReturnRemainingTests {

        @Test
        @DisplayName("should return consumption probe with remaining tokens")
        void shouldReturnProbeWithRemainingTokens() {
            // Given
            String key = "192.168.1.20";

            // When
            ConsumptionProbe probe = rateLimitService.tryConsumeAndReturnRemaining(key, false);

            // Then
            assertThat(probe.isConsumed()).isTrue();
            assertThat(probe.getRemainingTokens()).isLessThan(150); // Started with 150
        }

        @Test
        @DisplayName("should return probe with wait time when limit exceeded")
        void shouldReturnProbeWithWaitTimeWhenExceeded() {
            // Given - exhaust the bucket
            String key = "192.168.1.21";
            for (int i = 0; i < 150; i++) {
                rateLimitService.tryConsume(key);
            }

            // When
            ConsumptionProbe probe = rateLimitService.tryConsumeAndReturnRemaining(key, false);

            // Then
            assertThat(probe.isConsumed()).isFalse();
            assertThat(probe.getNanosToWaitForRefill()).isPositive();
        }
    }

    @Nested
    @DisplayName("getAvailableTokens")
    class GetAvailableTokensTests {

        @Test
        @DisplayName("should return full capacity for new key")
        void shouldReturnFullCapacityForNewKey() {
            // Given
            String key = "192.168.1.30";

            // When
            long tokens = rateLimitService.getAvailableTokens(key, false);

            // Then
            assertThat(tokens).isEqualTo(150); // burstCapacity
        }

        @Test
        @DisplayName("should return reduced tokens after consumption")
        void shouldReturnReducedTokensAfterConsumption() {
            // Given
            String key = "192.168.1.31";
            rateLimitService.tryConsume(key);
            rateLimitService.tryConsume(key);
            rateLimitService.tryConsume(key);

            // When
            long tokens = rateLimitService.getAvailableTokens(key, false);

            // Then
            assertThat(tokens).isEqualTo(147); // 150 - 3
        }
    }

    @Nested
    @DisplayName("Tier-based rate limiting")
    class TierBasedRateLimitingTests {

        @BeforeEach
        void setUpTiers() {
            RateLimitProperties rateLimitProperties = securityProperties.getRateLimit();
            rateLimitProperties.setTiersEnabled(true);

            TiersConfig tiersConfig = new TiersConfig();
            // Set up default tier configurations
            tiersConfig.setAnonymous(new TierConfig(10, 20));
            tiersConfig.setFree(new TierConfig(50, 100));
            tiersConfig.setBasic(new TierConfig(200, 400));
            tiersConfig.setPro(new TierConfig(1000, 2000));
            rateLimitProperties.setTiers(tiersConfig);
        }

        @Test
        @DisplayName("should consume from tier bucket")
        void shouldConsumeFromTierBucket() {
            // Given
            String userIdentifier = "user:123";
            RateLimitTier tier = RateLimitTier.FREE;

            // When
            boolean result = rateLimitService.tryConsumeForTier(userIdentifier, tier);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should respect tier limits")
        void shouldRespectTierLimits() {
            // Given
            String userIdentifier = "user:456";
            RateLimitTier tier = RateLimitTier.ANONYMOUS;

            // Exhaust anonymous tier bucket (burstCapacity = 20)
            for (int i = 0; i < 20; i++) {
                assertThat(rateLimitService.tryConsumeForTier(userIdentifier, tier)).isTrue();
            }

            // When
            boolean result = rateLimitService.tryConsumeForTier(userIdentifier, tier);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return consumption probe for tier")
        void shouldReturnConsumptionProbeForTier() {
            // Given
            String userIdentifier = "user:789";
            RateLimitTier tier = RateLimitTier.BASIC;

            // When
            ConsumptionProbe probe =
                    rateLimitService.tryConsumeForTierAndReturnRemaining(userIdentifier, tier);

            // Then
            assertThat(probe.isConsumed()).isTrue();
            assertThat(probe.getRemainingTokens()).isEqualTo(399); // 400 - 1
        }

        @Test
        @DisplayName("should get available tokens for tier")
        void shouldGetAvailableTokensForTier() {
            // Given
            String userIdentifier = "user:101";
            RateLimitTier tier = RateLimitTier.PRO;

            // When
            long tokens = rateLimitService.getAvailableTokensForTier(userIdentifier, tier);

            // Then
            assertThat(tokens).isEqualTo(2000); // PRO burstCapacity
        }

        @Test
        @DisplayName("should use separate buckets per tier")
        void shouldUseSeparateBucketsPerTier() {
            // Given
            String userIdentifier = "user:102";

            // Exhaust FREE tier
            for (int i = 0; i < 100; i++) {
                rateLimitService.tryConsumeForTier(userIdentifier, RateLimitTier.FREE);
            }

            // When
            boolean freeResult =
                    rateLimitService.tryConsumeForTier(userIdentifier, RateLimitTier.FREE);
            boolean basicResult =
                    rateLimitService.tryConsumeForTier(userIdentifier, RateLimitTier.BASIC);

            // Then - FREE is blocked but BASIC still works
            assertThat(freeResult).isFalse();
            assertThat(basicResult).isTrue();
        }

        @Test
        @DisplayName("should get tier config")
        void shouldGetTierConfig() {
            // When
            TierConfig config = rateLimitService.getTierConfig(RateLimitTier.FREE);

            // Then
            assertThat(config.getRequestsPerSecond()).isEqualTo(50);
            assertThat(config.getBurstCapacity()).isEqualTo(100);
        }

        @Test
        @DisplayName("should report tiers enabled status")
        void shouldReportTiersEnabledStatus() {
            assertThat(rateLimitService.isTiersEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("clearLocalBuckets")
    class ClearLocalBucketsTests {

        @Test
        @DisplayName("should reset all buckets")
        void shouldResetAllBuckets() {
            // Given - exhaust a bucket
            String key = "192.168.1.40";
            for (int i = 0; i < 150; i++) {
                rateLimitService.tryConsume(key);
            }
            assertThat(rateLimitService.tryConsume(key)).isFalse();

            // When
            rateLimitService.clearLocalBuckets();

            // Then - bucket should be reset
            assertThat(rateLimitService.tryConsume(key)).isTrue();
        }
    }

    @Nested
    @DisplayName("isUsingRedis")
    class IsUsingRedisTests {

        @Test
        @DisplayName("should return false when in-memory mode")
        void shouldReturnFalseWhenInMemoryMode() {
            assertThat(rateLimitService.isUsingRedis()).isFalse();
        }
    }

    @Nested
    @DisplayName("cleanup")
    class CleanupTests {

        @Test
        @DisplayName("should cleanup without error when no Redis connections")
        void shouldCleanupWithoutErrorWhenNoRedisConnections() {
            // When/Then - should not throw
            assertThatCode(() -> rateLimitService.cleanup()).doesNotThrowAnyException();
        }
    }
}
