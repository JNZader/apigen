package com.jnzader.apigen.core.infrastructure.versioning;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("VersionContext Tests")
class VersionContextTest {

    @AfterEach
    void cleanup() {
        VersionContext.clear();
    }

    @Nested
    @DisplayName("Set and Get Version")
    class SetAndGetTests {

        @Test
        @DisplayName("should set and get version")
        void shouldSetAndGetVersion() {
            VersionContext.setVersion("2.0");

            assertThat(VersionContext.getVersion()).isEqualTo("2.0");
        }

        @Test
        @DisplayName("should return null when not set")
        void shouldReturnNullWhenNotSet() {
            assertThat(VersionContext.getVersion()).isNull();
        }

        @Test
        @DisplayName("should return default when not set")
        void shouldReturnDefaultWhenNotSet() {
            assertThat(VersionContext.getVersionOrDefault("1.0")).isEqualTo("1.0");
        }

        @Test
        @DisplayName("should return version instead of default when set")
        void shouldReturnVersionInsteadOfDefault() {
            VersionContext.setVersion("3.0");

            assertThat(VersionContext.getVersionOrDefault("1.0")).isEqualTo("3.0");
        }

        @Test
        @DisplayName("should clear version")
        void shouldClearVersion() {
            VersionContext.setVersion("2.0");
            VersionContext.clear();

            assertThat(VersionContext.getVersion()).isNull();
        }
    }

    @Nested
    @DisplayName("Version Checking")
    class VersionCheckingTests {

        @Test
        @DisplayName("should check exact version match")
        void shouldCheckExactVersionMatch() {
            VersionContext.setVersion("2.0");

            assertThat(VersionContext.isVersion("2.0")).isTrue();
            assertThat(VersionContext.isVersion("1.0")).isFalse();
        }

        @Test
        @DisplayName("should check major version match")
        void shouldCheckMajorVersionMatch() {
            VersionContext.setVersion("2.0");

            assertThat(VersionContext.isVersion("2")).isTrue();
        }

        @Test
        @DisplayName("should return false when no version set")
        void shouldReturnFalseWhenNoVersionSet() {
            assertThat(VersionContext.isVersion("1.0")).isFalse();
        }

        @Test
        @DisplayName("should return false for null expected version")
        void shouldReturnFalseForNullExpectedVersion() {
            VersionContext.setVersion("2.0");

            assertThat(VersionContext.isVersion(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("Version Comparison")
    class VersionComparisonTests {

        @Test
        @DisplayName("should check if at least minimum version")
        void shouldCheckIfAtLeastMinimumVersion() {
            VersionContext.setVersion("2.0");

            assertThat(VersionContext.isAtLeast("1.0")).isTrue();
            assertThat(VersionContext.isAtLeast("2.0")).isTrue();
            assertThat(VersionContext.isAtLeast("3.0")).isFalse();
        }

        @Test
        @DisplayName("should return false when no version set for isAtLeast")
        void shouldReturnFalseWhenNoVersionSetForIsAtLeast() {
            assertThat(VersionContext.isAtLeast("1.0")).isFalse();
        }

        @Test
        @DisplayName("should compare versions correctly")
        void shouldCompareVersionsCorrectly() {
            assertThat(VersionContext.compareVersions("1.0", "2.0")).isLessThan(0);
            assertThat(VersionContext.compareVersions("2.0", "1.0")).isGreaterThan(0);
            assertThat(VersionContext.compareVersions("1.0", "1.0")).isEqualTo(0);
        }

        @Test
        @DisplayName("should compare versions with different segment counts")
        void shouldCompareVersionsWithDifferentSegmentCounts() {
            assertThat(VersionContext.compareVersions("1", "1.0")).isEqualTo(0);
            assertThat(VersionContext.compareVersions("1.0", "1.0.1")).isLessThan(0);
            assertThat(VersionContext.compareVersions("2", "1.9.9")).isGreaterThan(0);
        }

        @Test
        @DisplayName("should compare versions with suffixes")
        void shouldCompareVersionsWithSuffixes() {
            assertThat(VersionContext.compareVersions("1.0-beta", "1.0")).isEqualTo(0);
            assertThat(VersionContext.compareVersions("2.0-alpha", "1.0")).isGreaterThan(0);
        }

        @Test
        @DisplayName("should compare multi-segment versions")
        void shouldCompareMultiSegmentVersions() {
            assertThat(VersionContext.compareVersions("1.2.3", "1.2.4")).isLessThan(0);
            assertThat(VersionContext.compareVersions("1.2.10", "1.2.9")).isGreaterThan(0);
            assertThat(VersionContext.compareVersions("1.10.0", "1.9.0")).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Thread Isolation")
    class ThreadIsolationTests {

        @Test
        @DisplayName("should isolate version per thread")
        void shouldIsolateVersionPerThread() throws InterruptedException {
            VersionContext.setVersion("1.0");

            Thread otherThread =
                    new Thread(
                            () -> {
                                VersionContext.setVersion("2.0");
                                assertThat(VersionContext.getVersion()).isEqualTo("2.0");
                            });

            otherThread.start();
            otherThread.join();

            // Main thread should still have 1.0
            assertThat(VersionContext.getVersion()).isEqualTo("1.0");
        }
    }
}
