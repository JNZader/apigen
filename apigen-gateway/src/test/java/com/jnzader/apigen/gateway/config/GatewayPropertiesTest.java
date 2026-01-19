package com.jnzader.apigen.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GatewayProperties Tests")
class GatewayPropertiesTest {

    private GatewayProperties properties;

    @BeforeEach
    void setUp() {
        properties = new GatewayProperties();
    }

    @Nested
    @DisplayName("Default Values")
    class DefaultValuesTests {

        @Test
        @DisplayName("should have disabled by default")
        void shouldHaveDisabledByDefault() {
            assertThat(properties.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("should have rate limit enabled by default")
        void shouldHaveRateLimitEnabledByDefault() {
            assertThat(properties.getRateLimit().isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should have circuit breaker enabled by default")
        void shouldHaveCircuitBreakerEnabledByDefault() {
            assertThat(properties.getCircuitBreaker().isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should have auth enabled by default")
        void shouldHaveAuthEnabledByDefault() {
            assertThat(properties.getAuth().isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should have logging enabled by default")
        void shouldHaveLoggingEnabledByDefault() {
            assertThat(properties.getLogging().isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should have CORS enabled by default")
        void shouldHaveCorsEnabledByDefault() {
            assertThat(properties.getCors().isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Rate Limit Properties")
    class RateLimitPropertiesTests {

        @Test
        @DisplayName("should have default replenish rate")
        void shouldHaveDefaultReplenishRate() {
            assertThat(properties.getRateLimit().getDefaultReplenishRate()).isEqualTo(100);
        }

        @Test
        @DisplayName("should have default burst capacity")
        void shouldHaveDefaultBurstCapacity() {
            assertThat(properties.getRateLimit().getDefaultBurstCapacity()).isEqualTo(200);
        }

        @Test
        @DisplayName("should configure route-specific limits")
        void shouldConfigureRouteSpecificLimits() {
            GatewayProperties.RouteRateLimit routeLimit = new GatewayProperties.RouteRateLimit();
            routeLimit.setReplenishRate(10);
            routeLimit.setBurstCapacity(20);

            properties.getRateLimit().getRoutes().put("auth-route", routeLimit);

            assertThat(properties.getRateLimit().getRoutes()).containsKey("auth-route");
            assertThat(properties.getRateLimit().getRoutes().get("auth-route").getReplenishRate())
                    .isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("Circuit Breaker Properties")
    class CircuitBreakerPropertiesTests {

        @Test
        @DisplayName("should have default timeout")
        void shouldHaveDefaultTimeout() {
            assertThat(properties.getCircuitBreaker().getTimeout())
                    .isEqualTo(Duration.ofSeconds(10));
        }

        @Test
        @DisplayName("should have default failure rate threshold")
        void shouldHaveDefaultFailureRateThreshold() {
            assertThat(properties.getCircuitBreaker().getFailureRateThreshold()).isEqualTo(50);
        }

        @Test
        @DisplayName("should configure wait duration")
        void shouldConfigureWaitDuration() {
            properties.getCircuitBreaker().setWaitDurationInOpenState(Duration.ofMinutes(1));

            assertThat(properties.getCircuitBreaker().getWaitDurationInOpenState())
                    .isEqualTo(Duration.ofMinutes(1));
        }
    }

    @Nested
    @DisplayName("Auth Properties")
    class AuthPropertiesTests {

        @Test
        @DisplayName("should have default header name")
        void shouldHaveDefaultHeaderName() {
            assertThat(properties.getAuth().getHeaderName()).isEqualTo("Authorization");
        }

        @Test
        @DisplayName("should have default token prefix")
        void shouldHaveDefaultTokenPrefix() {
            assertThat(properties.getAuth().getTokenPrefix()).isEqualTo("Bearer ");
        }

        @Test
        @DisplayName("should configure excluded paths")
        void shouldConfigureExcludedPaths() {
            properties.getAuth().setExcludedPaths(List.of("/public/**", "/health"));

            assertThat(properties.getAuth().getExcludedPaths())
                    .containsExactly("/public/**", "/health");
        }
    }

    @Nested
    @DisplayName("Logging Properties")
    class LoggingPropertiesTests {

        @Test
        @DisplayName("should not include headers by default")
        void shouldNotIncludeHeadersByDefault() {
            assertThat(properties.getLogging().isIncludeHeaders()).isFalse();
        }

        @Test
        @DisplayName("should not include body by default")
        void shouldNotIncludeBodyByDefault() {
            assertThat(properties.getLogging().isIncludeBody()).isFalse();
        }
    }

    @Nested
    @DisplayName("CORS Properties")
    class CorsPropertiesTests {

        @Test
        @DisplayName("should have default allowed origins")
        void shouldHaveDefaultAllowedOrigins() {
            assertThat(properties.getCors().getAllowedOrigins()).containsExactly("*");
        }

        @Test
        @DisplayName("should have default allowed methods")
        void shouldHaveDefaultAllowedMethods() {
            assertThat(properties.getCors().getAllowedMethods())
                    .containsExactlyInAnyOrder("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
        }

        @Test
        @DisplayName("should have default max age")
        void shouldHaveDefaultMaxAge() {
            assertThat(properties.getCors().getMaxAge()).isEqualTo(Duration.ofHours(1));
        }

        @Test
        @DisplayName("should not allow credentials by default")
        void shouldNotAllowCredentialsByDefault() {
            assertThat(properties.getCors().isAllowCredentials()).isFalse();
        }
    }

    @Nested
    @DisplayName("Route Definition")
    class RouteDefinitionTests {

        @Test
        @DisplayName("should configure route definition")
        void shouldConfigureRouteDefinition() {
            GatewayProperties.RouteDefinition route = new GatewayProperties.RouteDefinition();
            route.setId("user-service");
            route.setUri("http://user-service:8080");
            route.setPredicates(List.of("Path=/api/users/**"));
            route.setFilters(List.of("StripPrefix=1"));
            route.setOrder(10);
            route.setMetadata(Map.of("service", "users"));

            properties.getRoutes().add(route);

            assertThat(properties.getRoutes()).hasSize(1);
            GatewayProperties.RouteDefinition configured = properties.getRoutes().get(0);
            assertThat(configured.getId()).isEqualTo("user-service");
            assertThat(configured.getUri()).isEqualTo("http://user-service:8080");
            assertThat(configured.getPredicates()).containsExactly("Path=/api/users/**");
            assertThat(configured.getFilters()).containsExactly("StripPrefix=1");
            assertThat(configured.getOrder()).isEqualTo(10);
            assertThat(configured.getMetadata()).containsEntry("service", "users");
        }
    }

    @Nested
    @DisplayName("Setters")
    class SettersTests {

        @Test
        @DisplayName("should set enabled")
        void shouldSetEnabled() {
            properties.setEnabled(true);
            assertThat(properties.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should set rate limit properties")
        void shouldSetRateLimitProperties() {
            GatewayProperties.RateLimitProperties rateLimit =
                    new GatewayProperties.RateLimitProperties();
            rateLimit.setDefaultReplenishRate(50);
            properties.setRateLimit(rateLimit);

            assertThat(properties.getRateLimit().getDefaultReplenishRate()).isEqualTo(50);
        }

        @Test
        @DisplayName("should set circuit breaker properties")
        void shouldSetCircuitBreakerProperties() {
            GatewayProperties.CircuitBreakerProperties cb =
                    new GatewayProperties.CircuitBreakerProperties();
            cb.setTimeout(Duration.ofSeconds(30));
            properties.setCircuitBreaker(cb);

            assertThat(properties.getCircuitBreaker().getTimeout())
                    .isEqualTo(Duration.ofSeconds(30));
        }
    }
}
