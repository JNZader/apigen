package com.jnzader.apigen.gateway.route;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RouteBuilder Tests")
class RouteBuilderTest {

    @Nested
    @DisplayName("Basic Configuration")
    class BasicConfigurationTests {

        @Test
        @DisplayName("should build route with id and uri")
        void shouldBuildRouteWithIdAndUri() {
            RouteBuilder.RouteDefinition definition =
                    RouteBuilder.route("test-route").uri("http://localhost:8080").buildDefinition();

            assertThat(definition.id()).isEqualTo("test-route");
            assertThat(definition.uri()).isEqualTo(URI.create("http://localhost:8080"));
        }

        @Test
        @DisplayName("should build route with URI object")
        void shouldBuildRouteWithUriObject() {
            URI uri = URI.create("http://backend-service:8080");
            RouteBuilder.RouteDefinition definition =
                    RouteBuilder.route("service-route").uri(uri).buildDefinition();

            assertThat(definition.uri()).isEqualTo(uri);
        }
    }

    @Nested
    @DisplayName("Predicates")
    class PredicatesTests {

        @Test
        @DisplayName("should configure path predicates")
        void shouldConfigurePathPredicates() {
            RouteBuilder.RouteDefinition definition =
                    RouteBuilder.route("path-route")
                            .uri("http://localhost:8080")
                            .path("/api/**", "/v1/**")
                            .buildDefinition();

            assertThat(definition.pathPredicates()).containsExactly("/api/**", "/v1/**");
            assertThat(definition.hasPathPredicates()).isTrue();
        }

        @Test
        @DisplayName("should configure method predicates")
        void shouldConfigureMethodPredicates() {
            RouteBuilder.RouteDefinition definition =
                    RouteBuilder.route("method-route")
                            .uri("http://localhost:8080")
                            .method("GET", "POST")
                            .buildDefinition();

            assertThat(definition.methodPredicates()).containsExactly("GET", "POST");
            assertThat(definition.hasMethodPredicates()).isTrue();
        }

        @Test
        @DisplayName("should configure header predicates with regex")
        void shouldConfigureHeaderPredicatesWithRegex() {
            RouteBuilder.RouteDefinition definition =
                    RouteBuilder.route("header-route")
                            .uri("http://localhost:8080")
                            .header("X-Custom-Header", "value.*")
                            .buildDefinition();

            assertThat(definition.headerPredicates()).containsExactly("X-Custom-Header,value.*");
            assertThat(definition.hasHeaderPredicates()).isTrue();
        }

        @Test
        @DisplayName("should configure header predicates without regex")
        void shouldConfigureHeaderPredicatesWithoutRegex() {
            RouteBuilder.RouteDefinition definition =
                    RouteBuilder.route("header-exists-route")
                            .uri("http://localhost:8080")
                            .header("X-Required-Header")
                            .buildDefinition();

            assertThat(definition.headerPredicates()).containsExactly("X-Required-Header");
        }
    }

    @Nested
    @DisplayName("Filters")
    class FiltersTests {

        @Test
        @DisplayName("should configure strip prefix")
        void shouldConfigureStripPrefix() {
            RouteBuilder.RouteDefinition definition =
                    RouteBuilder.route("strip-route")
                            .uri("http://localhost:8080")
                            .stripPrefix(2)
                            .buildDefinition();

            assertThat(definition.stripPrefix()).isTrue();
            assertThat(definition.stripPrefixParts()).isEqualTo(2);
        }

        @Test
        @DisplayName("should configure rewrite path")
        void shouldConfigureRewritePath() {
            RouteBuilder.RouteDefinition definition =
                    RouteBuilder.route("rewrite-route")
                            .uri("http://localhost:8080")
                            .rewritePath("/api/(?<segment>.*)", "/${segment}")
                            .buildDefinition();

            assertThat(definition.hasRewritePath()).isTrue();
            assertThat(definition.rewritePath()).isEqualTo("/api/(?<segment>.*)");
            assertThat(definition.rewritePathReplacement()).isEqualTo("/${segment}");
        }

        @Test
        @DisplayName("should configure add request header")
        void shouldConfigureAddRequestHeader() {
            RouteBuilder.RouteDefinition definition =
                    RouteBuilder.route("header-add-route")
                            .uri("http://localhost:8080")
                            .addRequestHeader("X-Gateway", "true")
                            .buildDefinition();

            assertThat(definition.addRequestHeader()).isTrue();
            assertThat(definition.requestHeaderName()).isEqualTo("X-Gateway");
            assertThat(definition.requestHeaderValue()).isEqualTo("true");
        }
    }

    @Nested
    @DisplayName("Circuit Breaker")
    class CircuitBreakerTests {

        @Test
        @DisplayName("should configure circuit breaker")
        void shouldConfigureCircuitBreaker() {
            RouteBuilder.RouteDefinition definition =
                    RouteBuilder.route("cb-route")
                            .uri("http://localhost:8080")
                            .circuitBreaker("backend-cb")
                            .buildDefinition();

            assertThat(definition.hasCircuitBreaker()).isTrue();
            assertThat(definition.circuitBreakerId()).isEqualTo("backend-cb");
        }

        @Test
        @DisplayName("should configure timeout")
        void shouldConfigureTimeout() {
            RouteBuilder.RouteDefinition definition =
                    RouteBuilder.route("timeout-route")
                            .uri("http://localhost:8080")
                            .timeout(Duration.ofSeconds(30))
                            .buildDefinition();

            assertThat(definition.hasTimeout()).isTrue();
            assertThat(definition.timeout()).isEqualTo(Duration.ofSeconds(30));
        }
    }

    @Nested
    @DisplayName("Metadata")
    class MetadataTests {

        @Test
        @DisplayName("should configure metadata")
        void shouldConfigureMetadata() {
            RouteBuilder.RouteDefinition definition =
                    RouteBuilder.route("meta-route")
                            .uri("http://localhost:8080")
                            .metadata("service", "product-service")
                            .metadata("version", "v2")
                            .buildDefinition();

            assertThat(definition.metadata())
                    .containsEntry("service", "product-service")
                    .containsEntry("version", "v2");
        }

        @Test
        @DisplayName("should configure order")
        void shouldConfigureOrder() {
            RouteBuilder.RouteDefinition definition =
                    RouteBuilder.route("ordered-route")
                            .uri("http://localhost:8080")
                            .order(100)
                            .buildDefinition();

            assertThat(definition.order()).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("Fluent API")
    class FluentApiTests {

        @Test
        @DisplayName("should chain all configurations")
        void shouldChainAllConfigurations() {
            RouteBuilder.RouteDefinition definition =
                    RouteBuilder.route("full-route")
                            .uri("http://backend:8080")
                            .path("/api/v1/**")
                            .method("GET", "POST", "PUT", "DELETE")
                            .header("X-Version", "1")
                            .stripPrefix(1)
                            .addRequestHeader("X-Gateway-Route", "full-route")
                            .circuitBreaker("full-cb")
                            .timeout(Duration.ofSeconds(15))
                            .metadata("env", "production")
                            .order(50)
                            .buildDefinition();

            assertThat(definition.id()).isEqualTo("full-route");
            assertThat(definition.uri().toString()).isEqualTo("http://backend:8080");
            assertThat(definition.pathPredicates()).containsExactly("/api/v1/**");
            assertThat(definition.methodPredicates())
                    .containsExactly("GET", "POST", "PUT", "DELETE");
            assertThat(definition.headerPredicates()).containsExactly("X-Version,1");
            assertThat(definition.stripPrefix()).isTrue();
            assertThat(definition.addRequestHeader()).isTrue();
            assertThat(definition.hasCircuitBreaker()).isTrue();
            assertThat(definition.hasTimeout()).isTrue();
            assertThat(definition.metadata()).containsEntry("env", "production");
            assertThat(definition.order()).isEqualTo(50);
        }
    }

    @Nested
    @DisplayName("RouteBuilder Accessors")
    class AccessorsTests {

        @Test
        @DisplayName("should provide access to id")
        void shouldProvideAccessToId() {
            RouteBuilder builder = RouteBuilder.route("test-id");
            assertThat(builder.getId()).isEqualTo("test-id");
        }

        @Test
        @DisplayName("should provide access to uri")
        void shouldProvideAccessToUri() {
            RouteBuilder builder = RouteBuilder.route("test").uri("http://localhost:8080");
            assertThat(builder.getUri()).isEqualTo(URI.create("http://localhost:8080"));
        }

        @Test
        @DisplayName("should provide access to path predicates")
        void shouldProvideAccessToPathPredicates() {
            RouteBuilder builder = RouteBuilder.route("test").path("/api/**");
            assertThat(builder.getPathPredicates()).containsExactly("/api/**");
        }

        @Test
        @DisplayName("should provide access to method predicates")
        void shouldProvideAccessToMethodPredicates() {
            RouteBuilder builder = RouteBuilder.route("test").method("GET");
            assertThat(builder.getMethodPredicates()).containsExactly("GET");
        }
    }
}
