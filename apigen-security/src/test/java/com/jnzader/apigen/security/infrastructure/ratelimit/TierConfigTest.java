package com.jnzader.apigen.security.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnzader.apigen.security.infrastructure.config.SecurityProperties.RateLimitProperties.TierConfig;
import com.jnzader.apigen.security.infrastructure.config.SecurityProperties.RateLimitProperties.TiersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("TierConfig Tests")
class TierConfigTest {

    @Nested
    @DisplayName("TierConfig")
    class TierConfigTests {

        @Test
        @DisplayName("should create with constructor parameters")
        void shouldCreateWithConstructorParameters() {
            TierConfig config = new TierConfig(100, 200);

            assertThat(config.getRequestsPerSecond()).isEqualTo(100);
            assertThat(config.getBurstCapacity()).isEqualTo(200);
        }

        @Test
        @DisplayName("should create with default constructor and setters")
        void shouldCreateWithDefaultConstructorAndSetters() {
            TierConfig config = new TierConfig();
            config.setRequestsPerSecond(50);
            config.setBurstCapacity(75);

            assertThat(config.getRequestsPerSecond()).isEqualTo(50);
            assertThat(config.getBurstCapacity()).isEqualTo(75);
        }
    }

    @Nested
    @DisplayName("TiersConfig")
    class TiersConfigTests {

        private TiersConfig tiersConfig;

        @BeforeEach
        void setUp() {
            tiersConfig = new TiersConfig();
        }

        @Test
        @DisplayName("should have default values for all tiers")
        void shouldHaveDefaultValuesForAllTiers() {
            assertThat(tiersConfig.getAnonymous()).isNotNull();
            assertThat(tiersConfig.getAnonymous().getRequestsPerSecond()).isEqualTo(10);
            assertThat(tiersConfig.getAnonymous().getBurstCapacity()).isEqualTo(20);

            assertThat(tiersConfig.getFree()).isNotNull();
            assertThat(tiersConfig.getFree().getRequestsPerSecond()).isEqualTo(50);
            assertThat(tiersConfig.getFree().getBurstCapacity()).isEqualTo(100);

            assertThat(tiersConfig.getBasic()).isNotNull();
            assertThat(tiersConfig.getBasic().getRequestsPerSecond()).isEqualTo(200);
            assertThat(tiersConfig.getBasic().getBurstCapacity()).isEqualTo(400);

            assertThat(tiersConfig.getPro()).isNotNull();
            assertThat(tiersConfig.getPro().getRequestsPerSecond()).isEqualTo(1000);
            assertThat(tiersConfig.getPro().getBurstCapacity()).isEqualTo(2000);
        }

        @ParameterizedTest(name = "getForTier(\"{0}\") should return {1} tier config")
        @CsvSource({"anonymous, 10", "free, 50", "basic, 200", "pro, 1000"})
        @DisplayName("should return correct tier for getForTier")
        void shouldReturnCorrectTierForGetForTier(String tierName, int expectedRps) {
            TierConfig config = tiersConfig.getForTier(tierName);
            assertThat(config.getRequestsPerSecond()).isEqualTo(expectedRps);
        }

        @ParameterizedTest
        @ValueSource(strings = {"invalid", "premium", "enterprise", "UNKNOWN", ""})
        @DisplayName("should return anonymous tier for invalid tier names")
        void shouldReturnAnonymousTierForInvalidTierNames(String invalidTier) {
            TierConfig config = tiersConfig.getForTier(invalidTier);
            assertThat(config.getRequestsPerSecond())
                    .isEqualTo(tiersConfig.getAnonymous().getRequestsPerSecond());
        }

        @Test
        @DisplayName("should allow setting custom tier configs")
        void shouldAllowSettingCustomTierConfigs() {
            TierConfig customPro = new TierConfig(5000, 10000);
            tiersConfig.setPro(customPro);

            assertThat(tiersConfig.getPro().getRequestsPerSecond()).isEqualTo(5000);
            assertThat(tiersConfig.getPro().getBurstCapacity()).isEqualTo(10000);
        }

        @Test
        @DisplayName("getForTier should be case insensitive")
        void getForTierShouldBeCaseInsensitive() {
            assertThat(tiersConfig.getForTier("FREE").getRequestsPerSecond())
                    .isEqualTo(tiersConfig.getForTier("free").getRequestsPerSecond());
            assertThat(tiersConfig.getForTier("PRO").getRequestsPerSecond())
                    .isEqualTo(tiersConfig.getForTier("pro").getRequestsPerSecond());
            assertThat(tiersConfig.getForTier("Basic").getRequestsPerSecond())
                    .isEqualTo(tiersConfig.getForTier("basic").getRequestsPerSecond());
        }
    }
}
