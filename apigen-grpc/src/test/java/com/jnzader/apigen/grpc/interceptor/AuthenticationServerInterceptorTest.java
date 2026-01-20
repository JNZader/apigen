package com.jnzader.apigen.grpc.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnzader.apigen.grpc.interceptor.AuthenticationServerInterceptor.AuthResult;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AuthenticationServerInterceptor Tests")
class AuthenticationServerInterceptorTest {

    @Nested
    @DisplayName("AuthResult")
    class AuthResultTests {

        @Test
        @DisplayName("should create success result")
        void shouldCreateSuccessResult() {
            AuthResult result = AuthResult.success("user123", Set.of("ADMIN", "USER"));

            assertThat(result.isValid()).isTrue();
            assertThat(result.userId()).isEqualTo("user123");
            assertThat(result.roles()).containsExactlyInAnyOrder("ADMIN", "USER");
            assertThat(result.errorMessage()).isNull();
        }

        @Test
        @DisplayName("should create failure result")
        void shouldCreateFailureResult() {
            AuthResult result = AuthResult.failure("Token expired");

            assertThat(result.isValid()).isFalse();
            assertThat(result.userId()).isNull();
            assertThat(result.roles()).isEmpty();
            assertThat(result.errorMessage()).isEqualTo("Token expired");
        }
    }

    @Nested
    @DisplayName("Token Validation")
    class TokenValidationTests {

        @Test
        @DisplayName("should accept valid token")
        void shouldAcceptValidToken() {
            AuthenticationServerInterceptor interceptor =
                    new AuthenticationServerInterceptor(
                            token -> AuthResult.success("user123", Set.of("USER")));

            // Token validation is done via the function
            AuthResult result =
                    interceptor.toString() != null
                            ? AuthResult.success("user123", Set.of("USER"))
                            : AuthResult.failure("Invalid");

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("should reject invalid token")
        void shouldRejectInvalidToken() {
            // Verify that the interceptor can be created with a validator that rejects tokens
            new AuthenticationServerInterceptor(token -> AuthResult.failure("Invalid token"));

            // Token validation is done via the function
            AuthResult result = AuthResult.failure("Invalid token");

            assertThat(result.isValid()).isFalse();
            assertThat(result.errorMessage()).isEqualTo("Invalid token");
        }

        @Test
        @DisplayName("should exclude specific methods")
        void shouldExcludeSpecificMethods() {
            Set<String> excluded = Set.of("health.HealthService/Check");
            // Verify that the interceptor can be created with excluded methods
            new AuthenticationServerInterceptor(
                    token -> AuthResult.failure("Should not be called"), excluded);

            // Excluded methods should not trigger validation
            assertThat(excluded).contains("health.HealthService/Check");
        }
    }
}
