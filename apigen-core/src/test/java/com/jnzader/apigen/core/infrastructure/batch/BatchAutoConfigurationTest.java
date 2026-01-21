package com.jnzader.apigen.core.infrastructure.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnzader.apigen.core.infrastructure.batch.BatchAutoConfiguration.BatchProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("BatchAutoConfiguration")
class BatchAutoConfigurationTest {

    @Nested
    @DisplayName("BatchProperties")
    class BatchPropertiesTests {

        @Test
        @DisplayName("should create properties with default values via defaults()")
        void shouldCreateDefaultProperties() {
            // When
            BatchProperties props = BatchProperties.defaults();

            // Then
            assertThat(props.enabled()).isTrue();
            assertThat(props.path()).isEqualTo("/api/batch");
            assertThat(props.maxOperations()).isEqualTo(100);
            assertThat(props.parallelThreshold()).isEqualTo(10);
            assertThat(props.allowNestedBatch()).isFalse();
            assertThat(props.timeoutSeconds()).isEqualTo(30);
        }

        @ParameterizedTest(name = "should use default path when path is \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t", "\n"})
        void shouldUseDefaultPathWhenPathIsNullOrBlank(String path) {
            // When
            BatchProperties props = new BatchProperties(true, path, 100, 10, false, 30);

            // Then
            assertThat(props.path()).isEqualTo(BatchProperties.DEFAULT_PATH);
        }

        @Test
        @DisplayName("should use custom path when provided")
        void shouldUseCustomPath() {
            // Given
            String customPath = "/custom/batch";

            // When
            BatchProperties props = new BatchProperties(true, customPath, 100, 10, false, 30);

            // Then
            assertThat(props.path()).isEqualTo(customPath);
        }

        @ParameterizedTest(name = "should use default maxOperations when value is {0}")
        @ValueSource(ints = {0, -1, -100})
        void shouldUseDefaultMaxOperationsWhenInvalid(int maxOperations) {
            // When
            BatchProperties props =
                    new BatchProperties(true, "/api/batch", maxOperations, 10, false, 30);

            // Then
            assertThat(props.maxOperations()).isEqualTo(100);
        }

        @Test
        @DisplayName("should use custom maxOperations when positive")
        void shouldUseCustomMaxOperations() {
            // When
            BatchProperties props = new BatchProperties(true, "/api/batch", 50, 10, false, 30);

            // Then
            assertThat(props.maxOperations()).isEqualTo(50);
        }

        @ParameterizedTest(name = "should use default parallelThreshold when value is {0}")
        @ValueSource(ints = {0, -1, -100})
        void shouldUseDefaultParallelThresholdWhenInvalid(int parallelThreshold) {
            // When
            BatchProperties props =
                    new BatchProperties(true, "/api/batch", 100, parallelThreshold, false, 30);

            // Then
            assertThat(props.parallelThreshold()).isEqualTo(10);
        }

        @Test
        @DisplayName("should use custom parallelThreshold when positive")
        void shouldUseCustomParallelThreshold() {
            // When
            BatchProperties props = new BatchProperties(true, "/api/batch", 100, 5, false, 30);

            // Then
            assertThat(props.parallelThreshold()).isEqualTo(5);
        }

        @ParameterizedTest(name = "should use default timeoutSeconds when value is {0}")
        @ValueSource(ints = {0, -1, -100})
        void shouldUseDefaultTimeoutSecondsWhenInvalid(int timeoutSeconds) {
            // When
            BatchProperties props =
                    new BatchProperties(true, "/api/batch", 100, 10, false, timeoutSeconds);

            // Then
            assertThat(props.timeoutSeconds()).isEqualTo(30);
        }

        @Test
        @DisplayName("should use custom timeoutSeconds when positive")
        void shouldUseCustomTimeoutSeconds() {
            // When
            BatchProperties props = new BatchProperties(true, "/api/batch", 100, 10, false, 60);

            // Then
            assertThat(props.timeoutSeconds()).isEqualTo(60);
        }

        @Test
        @DisplayName("should preserve allowNestedBatch setting")
        void shouldPreserveAllowNestedBatch() {
            // When
            BatchProperties propsTrue = new BatchProperties(true, "/api/batch", 100, 10, true, 30);
            BatchProperties propsFalse =
                    new BatchProperties(true, "/api/batch", 100, 10, false, 30);

            // Then
            assertThat(propsTrue.allowNestedBatch()).isTrue();
            assertThat(propsFalse.allowNestedBatch()).isFalse();
        }

        @Test
        @DisplayName("should preserve enabled setting")
        void shouldPreserveEnabled() {
            // When
            BatchProperties propsEnabled =
                    new BatchProperties(true, "/api/batch", 100, 10, false, 30);
            BatchProperties propsDisabled =
                    new BatchProperties(false, "/api/batch", 100, 10, false, 30);

            // Then
            assertThat(propsEnabled.enabled()).isTrue();
            assertThat(propsDisabled.enabled()).isFalse();
        }

        @Test
        @DisplayName("should expose DEFAULT_PATH constant")
        void shouldExposeDefaultPathConstant() {
            assertThat(BatchProperties.DEFAULT_PATH).isEqualTo("/api/batch");
        }

        @Test
        @DisplayName("should normalize all invalid values in single construction")
        void shouldNormalizeAllInvalidValues() {
            // When - all invalid values
            BatchProperties props = new BatchProperties(false, null, -1, 0, true, -5);

            // Then - all defaults applied
            assertThat(props.enabled()).isFalse();
            assertThat(props.path()).isEqualTo("/api/batch");
            assertThat(props.maxOperations()).isEqualTo(100);
            assertThat(props.parallelThreshold()).isEqualTo(10);
            assertThat(props.allowNestedBatch()).isTrue();
            assertThat(props.timeoutSeconds()).isEqualTo(30);
        }
    }
}
