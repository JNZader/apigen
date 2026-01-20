package com.jnzader.apigen.grpc.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("HealthCheck Tests")
class HealthCheckTest {

    @Nested
    @DisplayName("Result Factory Methods")
    class ResultFactoryTests {

        @Test
        @DisplayName("should create healthy result with defaults")
        void shouldCreateHealthyWithDefaults() {
            HealthCheck.Result result = HealthCheck.Result.healthy();

            assertThat(result.isHealthy()).isTrue();
            assertThat(result.message()).isEqualTo("OK");
            assertThat(result.details()).isEmpty();
        }

        @Test
        @DisplayName("should create healthy result with message")
        void shouldCreateHealthyWithMessage() {
            HealthCheck.Result result = HealthCheck.Result.healthy("All systems operational");

            assertThat(result.isHealthy()).isTrue();
            assertThat(result.message()).isEqualTo("All systems operational");
            assertThat(result.details()).isEmpty();
        }

        @Test
        @DisplayName("should create healthy result with details")
        void shouldCreateHealthyWithDetails() {
            Map<String, String> details = Map.of("version", "1.0", "uptime", "3600s");
            HealthCheck.Result result = HealthCheck.Result.healthy("Healthy", details);

            assertThat(result.isHealthy()).isTrue();
            assertThat(result.message()).isEqualTo("Healthy");
            assertThat(result.details()).containsEntry("version", "1.0");
            assertThat(result.details()).containsEntry("uptime", "3600s");
        }

        @Test
        @DisplayName("should create unhealthy result with message")
        void shouldCreateUnhealthyWithMessage() {
            HealthCheck.Result result = HealthCheck.Result.unhealthy("Database connection failed");

            assertThat(result.isHealthy()).isFalse();
            assertThat(result.message()).isEqualTo("Database connection failed");
            assertThat(result.details()).isEmpty();
        }

        @Test
        @DisplayName("should create unhealthy result with details")
        void shouldCreateUnhealthyWithDetails() {
            Map<String, String> details = Map.of("error", "Connection refused", "host", "db:5432");
            HealthCheck.Result result =
                    HealthCheck.Result.unhealthy("Database unavailable", details);

            assertThat(result.isHealthy()).isFalse();
            assertThat(result.message()).isEqualTo("Database unavailable");
            assertThat(result.details()).containsEntry("error", "Connection refused");
            assertThat(result.details()).containsEntry("host", "db:5432");
        }
    }
}
