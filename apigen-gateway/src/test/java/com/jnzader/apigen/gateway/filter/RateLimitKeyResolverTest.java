package com.jnzader.apigen.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

@DisplayName("RateLimitKeyResolver Tests")
class RateLimitKeyResolverTest {

    @Nested
    @DisplayName("IP Strategy")
    class IpStrategyTests {

        @Test
        @DisplayName("should resolve by IP address")
        void shouldResolveByIpAddress() {
            RateLimitKeyResolver resolver =
                    new RateLimitKeyResolver(RateLimitKeyResolver.KeyResolutionStrategy.IP);

            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .remoteAddress(new InetSocketAddress("192.168.1.100", 8080))
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(resolver.resolve(exchange))
                    .expectNext("ip:192.168.1.100")
                    .verifyComplete();
        }

        @Test
        @DisplayName("should use X-Forwarded-For header when present")
        void shouldUseXForwardedForHeader() {
            RateLimitKeyResolver resolver =
                    new RateLimitKeyResolver(RateLimitKeyResolver.KeyResolutionStrategy.IP);

            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .header("X-Forwarded-For", "10.0.0.1, 192.168.1.1")
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(resolver.resolve(exchange))
                    .expectNext("ip:10.0.0.1")
                    .verifyComplete();
        }

        @Test
        @DisplayName("should use X-Real-IP header when present")
        void shouldUseXRealIpHeader() {
            RateLimitKeyResolver resolver =
                    new RateLimitKeyResolver(RateLimitKeyResolver.KeyResolutionStrategy.IP);

            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test").header("X-Real-IP", "10.0.0.2").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(resolver.resolve(exchange))
                    .expectNext("ip:10.0.0.2")
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("User ID Strategy")
    class UserIdStrategyTests {

        @Test
        @DisplayName("should resolve by user ID header")
        void shouldResolveByUserIdHeader() {
            RateLimitKeyResolver resolver =
                    new RateLimitKeyResolver(RateLimitKeyResolver.KeyResolutionStrategy.USER_ID);

            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .header(AuthenticationGatewayFilter.USER_ID_HEADER, "user123")
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(resolver.resolve(exchange))
                    .expectNext("user:user123")
                    .verifyComplete();
        }

        @Test
        @DisplayName("should fall back to IP when no user ID")
        void shouldFallBackToIpWhenNoUserId() {
            RateLimitKeyResolver resolver =
                    new RateLimitKeyResolver(RateLimitKeyResolver.KeyResolutionStrategy.USER_ID);

            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .remoteAddress(new InetSocketAddress("192.168.1.50", 8080))
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(resolver.resolve(exchange))
                    .expectNext("ip:192.168.1.50")
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("API Key Strategy")
    class ApiKeyStrategyTests {

        @Test
        @DisplayName("should resolve by API key header")
        void shouldResolveByApiKeyHeader() {
            RateLimitKeyResolver resolver =
                    new RateLimitKeyResolver(
                            RateLimitKeyResolver.KeyResolutionStrategy.API_KEY, "X-API-Key");

            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .header("X-API-Key", "api-key-123")
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(resolver.resolve(exchange))
                    .expectNext("apikey:api-key-123")
                    .verifyComplete();
        }

        @Test
        @DisplayName("should fall back to IP when no API key")
        void shouldFallBackToIpWhenNoApiKey() {
            RateLimitKeyResolver resolver =
                    new RateLimitKeyResolver(
                            RateLimitKeyResolver.KeyResolutionStrategy.API_KEY, "X-API-Key");

            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .remoteAddress(new InetSocketAddress("192.168.1.75", 8080))
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(resolver.resolve(exchange))
                    .expectNext("ip:192.168.1.75")
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Composite Strategy")
    class CompositeStrategyTests {

        @Test
        @DisplayName("should resolve composite key")
        void shouldResolveCompositeKey() {
            RateLimitKeyResolver resolver =
                    new RateLimitKeyResolver(RateLimitKeyResolver.KeyResolutionStrategy.COMPOSITE);

            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/users/123")
                            .remoteAddress(new InetSocketAddress("192.168.1.100", 8080))
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(resolver.resolve(exchange))
                    .expectNext("composite:192.168.1.100:GET:/api/users/{id}")
                    .verifyComplete();
        }

        @Test
        @DisplayName("should normalize UUID in path")
        void shouldNormalizeUuidInPath() {
            RateLimitKeyResolver resolver =
                    new RateLimitKeyResolver(RateLimitKeyResolver.KeyResolutionStrategy.COMPOSITE);

            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/products/550e8400-e29b-41d4-a716-446655440000")
                            .remoteAddress(new InetSocketAddress("192.168.1.100", 8080))
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(resolver.resolve(exchange))
                    .expectNext("composite:192.168.1.100:GET:/api/products/{id}")
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Path Strategy")
    class PathStrategyTests {

        @Test
        @DisplayName("should resolve by normalized path")
        void shouldResolveByNormalizedPath() {
            RateLimitKeyResolver resolver =
                    new RateLimitKeyResolver(RateLimitKeyResolver.KeyResolutionStrategy.PATH);

            MockServerHttpRequest request = MockServerHttpRequest.get("/api/users/456").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(resolver.resolve(exchange))
                    .expectNext("path:/api/users/{id}")
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Configuration")
    class ConfigurationTests {

        @Test
        @DisplayName("should use default strategy")
        void shouldUseDefaultStrategy() {
            RateLimitKeyResolver resolver = new RateLimitKeyResolver();

            assertThat(resolver.getStrategy())
                    .isEqualTo(RateLimitKeyResolver.KeyResolutionStrategy.IP);
        }
    }
}
