package com.jnzader.apigen.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationGatewayFilter Tests")
class AuthenticationGatewayFilterTest {

    @Mock private GatewayFilterChain chain;

    private Function<String, AuthenticationGatewayFilter.AuthResult> tokenValidator;
    private AuthenticationGatewayFilter filter;

    @BeforeEach
    void setUp() {
        tokenValidator =
                token -> {
                    if ("valid-token".equals(token)) {
                        return AuthenticationGatewayFilter.AuthResult.success(
                                "user123", List.of("ROLE_USER"));
                    }
                    return AuthenticationGatewayFilter.AuthResult.failure("Invalid token");
                };

        filter =
                new AuthenticationGatewayFilter(
                        tokenValidator, List.of("/public/**", "/health", "/actuator/**"));
    }

    @Nested
    @DisplayName("Excluded Paths")
    class ExcludedPathsTests {

        @Test
        @DisplayName("should allow excluded paths without authentication")
        void shouldAllowExcludedPathsWithoutAuthentication() {
            MockServerHttpRequest request = MockServerHttpRequest.get("/public/resource").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

            verify(chain).filter(exchange);
        }

        @Test
        @DisplayName("should allow health endpoint without authentication")
        void shouldAllowHealthEndpointWithoutAuthentication() {
            MockServerHttpRequest request = MockServerHttpRequest.get("/health").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

            verify(chain).filter(exchange);
        }

        @Test
        @DisplayName("should allow actuator endpoints without authentication")
        void shouldAllowActuatorEndpointsWithoutAuthentication() {
            MockServerHttpRequest request = MockServerHttpRequest.get("/actuator/health").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

            verify(chain).filter(exchange);
        }
    }

    @Nested
    @DisplayName("Authentication Required")
    class AuthenticationRequiredTests {

        @Test
        @DisplayName("should return 401 when no authorization header")
        void shouldReturn401WhenNoAuthorizationHeader() {
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/protected").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(chain, never()).filter(any());
        }

        @Test
        @DisplayName("should return 401 when invalid token prefix")
        void shouldReturn401WhenInvalidTokenPrefix() {
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/protected")
                            .header(HttpHeaders.AUTHORIZATION, "Basic invalid-token")
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(chain, never()).filter(any());
        }

        @Test
        @DisplayName("should return 401 when token validation fails")
        void shouldReturn401WhenTokenValidationFails() {
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/protected")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(chain, never()).filter(any());
        }
    }

    @Nested
    @DisplayName("Successful Authentication")
    class SuccessfulAuthenticationTests {

        @Test
        @DisplayName("should proceed with valid token")
        void shouldProceedWithValidToken() {
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/protected")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

            verify(chain)
                    .filter(
                            argThat(
                                    ex -> {
                                        String userId =
                                                ex.getRequest()
                                                        .getHeaders()
                                                        .getFirst(
                                                                AuthenticationGatewayFilter
                                                                        .USER_ID_HEADER);
                                        String roles =
                                                ex.getRequest()
                                                        .getHeaders()
                                                        .getFirst(
                                                                AuthenticationGatewayFilter
                                                                        .USER_ROLES_HEADER);
                                        return "user123".equals(userId)
                                                && "ROLE_USER".equals(roles);
                                    }));
        }
    }

    @Nested
    @DisplayName("AuthResult")
    class AuthResultTests {

        @Test
        @DisplayName("should create success result")
        void shouldCreateSuccessResult() {
            AuthenticationGatewayFilter.AuthResult result =
                    AuthenticationGatewayFilter.AuthResult.success("user1", List.of("ADMIN"));

            assertThat(result.isAuthenticated()).isTrue();
            assertThat(result.getUserId()).contains("user1");
            assertThat(result.roles()).containsExactly("ADMIN");
            assertThat(result.getErrorMessage()).isEmpty();
        }

        @Test
        @DisplayName("should create failure result")
        void shouldCreateFailureResult() {
            AuthenticationGatewayFilter.AuthResult result =
                    AuthenticationGatewayFilter.AuthResult.failure("Token expired");

            assertThat(result.isAuthenticated()).isFalse();
            assertThat(result.getUserId()).isEmpty();
            assertThat(result.roles()).isEmpty();
            assertThat(result.getErrorMessage()).contains("Token expired");
        }
    }

    @Nested
    @DisplayName("Filter Order")
    class FilterOrderTests {

        @Test
        @DisplayName("should have order after logging filter")
        void shouldHaveOrderAfterLoggingFilter() {
            assertThat(filter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 100);
        }
    }
}
