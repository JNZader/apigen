package com.jnzader.apigen.security.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("RateLimitTier Tests")
class RateLimitTierTest {

    @Nested
    @DisplayName("fromString")
    class FromStringTests {

        @ParameterizedTest(name = "should parse \"{0}\" as {1}")
        @CsvSource({
            "free, FREE",
            "FREE, FREE",
            "Free, FREE",
            "basic, BASIC",
            "BASIC, BASIC",
            "Basic, BASIC",
            "pro, PRO",
            "PRO, PRO",
            "Pro, PRO",
            "anonymous, ANONYMOUS",
            "ANONYMOUS, ANONYMOUS",
            "Anonymous, ANONYMOUS"
        })
        @DisplayName("should parse valid tier names")
        void shouldParseValidTierNames(String input, RateLimitTier expected) {
            assertThat(RateLimitTier.fromString(input)).isEqualTo(expected);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "invalid", "premium", "enterprise"})
        @DisplayName("should return ANONYMOUS for invalid values")
        void shouldReturnAnonymousForInvalidValues(String input) {
            assertThat(RateLimitTier.fromString(input)).isEqualTo(RateLimitTier.ANONYMOUS);
        }
    }

    @Nested
    @DisplayName("Default Values")
    class DefaultValuesTests {

        @Test
        @DisplayName("ANONYMOUS should have lowest limits")
        void anonymousShouldHaveLowestLimits() {
            RateLimitTier tier = RateLimitTier.ANONYMOUS;
            assertThat(tier.getDefaultRequestsPerSecond()).isEqualTo(10);
            assertThat(tier.getDefaultBurstCapacity()).isEqualTo(20);
        }

        @Test
        @DisplayName("FREE should have limited access")
        void freeShouldHaveLimitedAccess() {
            RateLimitTier tier = RateLimitTier.FREE;
            assertThat(tier.getDefaultRequestsPerSecond()).isEqualTo(50);
            assertThat(tier.getDefaultBurstCapacity()).isEqualTo(100);
        }

        @Test
        @DisplayName("BASIC should have moderate limits")
        void basicShouldHaveModerateLimits() {
            RateLimitTier tier = RateLimitTier.BASIC;
            assertThat(tier.getDefaultRequestsPerSecond()).isEqualTo(200);
            assertThat(tier.getDefaultBurstCapacity()).isEqualTo(400);
        }

        @Test
        @DisplayName("PRO should have highest limits")
        void proShouldHaveHighestLimits() {
            RateLimitTier tier = RateLimitTier.PRO;
            assertThat(tier.getDefaultRequestsPerSecond()).isEqualTo(1000);
            assertThat(tier.getDefaultBurstCapacity()).isEqualTo(2000);
        }

        @Test
        @DisplayName("tiers should have increasing limits")
        void tiersShouldHaveIncreasingLimits() {
            assertThat(RateLimitTier.ANONYMOUS.getDefaultRequestsPerSecond())
                    .isLessThan(RateLimitTier.FREE.getDefaultRequestsPerSecond());
            assertThat(RateLimitTier.FREE.getDefaultRequestsPerSecond())
                    .isLessThan(RateLimitTier.BASIC.getDefaultRequestsPerSecond());
            assertThat(RateLimitTier.BASIC.getDefaultRequestsPerSecond())
                    .isLessThan(RateLimitTier.PRO.getDefaultRequestsPerSecond());
        }
    }

    @Nested
    @DisplayName("getName")
    class GetNameTests {

        @Test
        @DisplayName("should return lowercase tier names")
        void shouldReturnLowercaseTierNames() {
            assertThat(RateLimitTier.ANONYMOUS.getName()).isEqualTo("anonymous");
            assertThat(RateLimitTier.FREE.getName()).isEqualTo("free");
            assertThat(RateLimitTier.BASIC.getName()).isEqualTo("basic");
            assertThat(RateLimitTier.PRO.getName()).isEqualTo("pro");
        }

        @Test
        @DisplayName("should have name different from enum name")
        void shouldHaveNameDifferentFromEnumName() {
            for (RateLimitTier tier : RateLimitTier.values()) {
                // getName() returns lowercase, name() returns uppercase
                assertThat(tier.getName()).isEqualTo(tier.name().toLowerCase());
            }
        }
    }

    @Nested
    @DisplayName("Enum Values")
    class EnumValuesTests {

        @Test
        @DisplayName("should have exactly four tiers")
        void shouldHaveFourTiers() {
            assertThat(RateLimitTier.values()).hasSize(4);
        }

        @Test
        @DisplayName("burst capacity should be 2x requests per second")
        void burstCapacityShouldBeTwiceRequestsPerSecond() {
            for (RateLimitTier tier : RateLimitTier.values()) {
                assertThat(tier.getDefaultBurstCapacity())
                        .as("Tier %s burst should be 2x rate", tier)
                        .isEqualTo(tier.getDefaultRequestsPerSecond() * 2);
            }
        }
    }
}
