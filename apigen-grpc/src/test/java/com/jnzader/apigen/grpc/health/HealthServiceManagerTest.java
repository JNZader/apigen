package com.jnzader.apigen.grpc.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("HealthServiceManager Tests")
class HealthServiceManagerTest {

    private HealthServiceManager manager;

    @BeforeEach
    void setUp() {
        manager = new HealthServiceManager();
    }

    @Nested
    @DisplayName("Registration")
    class RegistrationTests {

        @Test
        @DisplayName("should register health check")
        void shouldRegisterHealthCheck() {
            HealthCheck check = new SimpleHealthCheck("db", true);

            manager.register(check);

            assertThat(manager.getRegisteredChecks()).contains("db");
        }

        @Test
        @DisplayName("should unregister health check")
        void shouldUnregisterHealthCheck() {
            HealthCheck check = new SimpleHealthCheck("db", true);
            manager.register(check);

            manager.unregister("db");

            assertThat(manager.getRegisteredChecks()).doesNotContain("db");
        }
    }

    @Nested
    @DisplayName("Single Component Check")
    class SingleComponentCheckTests {

        @Test
        @DisplayName("should check healthy component")
        void shouldCheckHealthyComponent() {
            manager.register(new SimpleHealthCheck("db", true));

            HealthCheck.Result result = manager.checkComponent("db");

            assertThat(result).isNotNull();
            assertThat(result.isHealthy()).isTrue();
        }

        @Test
        @DisplayName("should check unhealthy component")
        void shouldCheckUnhealthyComponent() {
            manager.register(new SimpleHealthCheck("db", false));

            HealthCheck.Result result = manager.checkComponent("db");

            assertThat(result).isNotNull();
            assertThat(result.isHealthy()).isFalse();
        }

        @Test
        @DisplayName("should return null for unknown component")
        void shouldReturnNullForUnknownComponent() {
            HealthCheck.Result result = manager.checkComponent("unknown");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should handle exception during check")
        void shouldHandleExceptionDuringCheck() {
            manager.register(new FailingHealthCheck("db"));

            HealthCheck.Result result = manager.checkComponent("db");

            assertThat(result).isNotNull();
            assertThat(result.isHealthy()).isFalse();
            assertThat(result.message()).contains("Health check failed");
        }
    }

    @Nested
    @DisplayName("Aggregated Check")
    class AggregatedCheckTests {

        @Test
        @DisplayName("should return healthy when all components healthy")
        void shouldReturnHealthyWhenAllHealthy() {
            manager.register(new SimpleHealthCheck("db", true));
            manager.register(new SimpleHealthCheck("cache", true));

            HealthServiceManager.AggregatedHealth result = manager.checkAll();

            assertThat(result.healthy()).isTrue();
            assertThat(result.components()).hasSize(2);
        }

        @Test
        @DisplayName("should return unhealthy when any component unhealthy")
        void shouldReturnUnhealthyWhenAnyUnhealthy() {
            manager.register(new SimpleHealthCheck("db", true));
            manager.register(new SimpleHealthCheck("cache", false));

            HealthServiceManager.AggregatedHealth result = manager.checkAll();

            assertThat(result.healthy()).isFalse();
            assertThat(result.components()).hasSize(2);
        }

        @Test
        @DisplayName("should return healthy when no components registered")
        void shouldReturnHealthyWhenNoComponents() {
            HealthServiceManager.AggregatedHealth result = manager.checkAll();

            assertThat(result.healthy()).isTrue();
            assertThat(result.components()).isEmpty();
        }

        @Test
        @DisplayName("should handle exception during aggregated check")
        void shouldHandleExceptionDuringAggregatedCheck() {
            manager.register(new SimpleHealthCheck("db", true));
            manager.register(new FailingHealthCheck("cache"));

            HealthServiceManager.AggregatedHealth result = manager.checkAll();

            assertThat(result.healthy()).isFalse();
            assertThat(result.components()).hasSize(2);
        }
    }

    // Test helper implementations
    private static class SimpleHealthCheck implements HealthCheck {
        private final String name;
        private final boolean healthy;

        SimpleHealthCheck(String name, boolean healthy) {
            this.name = name;
            this.healthy = healthy;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Result check() {
            return healthy
                    ? Result.healthy("OK", Map.of())
                    : Result.unhealthy("Component unhealthy", Map.of());
        }
    }

    private static class FailingHealthCheck implements HealthCheck {
        private final String name;

        FailingHealthCheck(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Result check() {
            throw new RuntimeException("Check failed");
        }
    }
}
