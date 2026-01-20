package com.jnzader.apigen.security.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Unit tests for SecurityDisabledConfig. Tests CORS configuration and PasswordEncoder when security
 * is disabled.
 */
@DisplayName("SecurityDisabledConfig Tests")
class SecurityDisabledConfigTest {

    private SecurityDisabledConfig config;

    @BeforeEach
    void setUp() {
        config = new SecurityDisabledConfig();
    }

    @Nested
    @DisplayName("CORS Configuration")
    class CorsConfigurationTests {

        private CorsConfiguration getCorsConfig() {
            CorsConfigurationSource source = config.corsConfigurationSource();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/test");
            return source.getCorsConfiguration(request);
        }

        @Test
        @DisplayName("should create CorsConfigurationSource bean")
        void shouldCreateCorsConfigurationSourceBean() {
            CorsConfigurationSource source = config.corsConfigurationSource();

            assertThat(source).isNotNull();
        }

        @ParameterizedTest(name = "should allow {0} origin")
        @ValueSource(
                strings = {
                    "http://localhost:3000",
                    "http://localhost:4200",
                    "http://localhost:8080"
                })
        @DisplayName("should allow localhost origins")
        void shouldAllowLocalhostOrigins(String origin) {
            CorsConfiguration corsConfig = getCorsConfig();

            assertThat(corsConfig)
                    .isNotNull()
                    .extracting(CorsConfiguration::getAllowedOrigins)
                    .asInstanceOf(InstanceOfAssertFactories.LIST)
                    .contains(origin);
        }

        @Test
        @DisplayName("should allow standard HTTP methods")
        void shouldAllowStandardHttpMethods() {
            CorsConfiguration corsConfig = getCorsConfig();

            assertThat(corsConfig)
                    .isNotNull()
                    .extracting(CorsConfiguration::getAllowedMethods)
                    .asInstanceOf(InstanceOfAssertFactories.LIST)
                    .containsExactlyInAnyOrder("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        }

        @Test
        @DisplayName("should allow required headers")
        void shouldAllowRequiredHeaders() {
            CorsConfiguration corsConfig = getCorsConfig();

            assertThat(corsConfig)
                    .isNotNull()
                    .extracting(CorsConfiguration::getAllowedHeaders)
                    .asInstanceOf(InstanceOfAssertFactories.LIST)
                    .contains(
                            "Authorization",
                            "Content-Type",
                            "X-Requested-With",
                            "Accept",
                            "Origin");
        }

        @Test
        @DisplayName("should expose Authorization header")
        void shouldExposeAuthorizationHeader() {
            CorsConfiguration corsConfig = getCorsConfig();

            assertThat(corsConfig)
                    .isNotNull()
                    .extracting(CorsConfiguration::getExposedHeaders)
                    .asInstanceOf(InstanceOfAssertFactories.LIST)
                    .contains("Authorization");
        }

        @Test
        @DisplayName("should expose pagination headers")
        void shouldExposePaginationHeaders() {
            CorsConfiguration corsConfig = getCorsConfig();

            assertThat(corsConfig)
                    .isNotNull()
                    .extracting(CorsConfiguration::getExposedHeaders)
                    .asInstanceOf(InstanceOfAssertFactories.LIST)
                    .contains("X-Total-Count", "X-Page-Number", "X-Page-Size");
        }

        @Test
        @DisplayName("should expose caching headers")
        void shouldExposeCachingHeaders() {
            CorsConfiguration corsConfig = getCorsConfig();

            assertThat(corsConfig)
                    .isNotNull()
                    .extracting(CorsConfiguration::getExposedHeaders)
                    .asInstanceOf(InstanceOfAssertFactories.LIST)
                    .contains("ETag", "Last-Modified");
        }

        @Test
        @DisplayName("should allow credentials")
        void shouldAllowCredentials() {
            CorsConfiguration corsConfig = getCorsConfig();

            assertThat(corsConfig)
                    .isNotNull()
                    .extracting(CorsConfiguration::getAllowCredentials)
                    .isEqualTo(Boolean.TRUE);
        }

        @Test
        @DisplayName("should set max age to 1 hour")
        void shouldSetMaxAgeToOneHour() {
            CorsConfiguration corsConfig = getCorsConfig();

            assertThat(corsConfig)
                    .isNotNull()
                    .extracting(CorsConfiguration::getMaxAge)
                    .isEqualTo(3600L);
        }
    }

    @Nested
    @DisplayName("Password Encoder")
    class PasswordEncoderTests {

        @Test
        @DisplayName("should create BCryptPasswordEncoder bean")
        void shouldCreateBCryptPasswordEncoderBean() {
            PasswordEncoder encoder = config.passwordEncoder();

            assertThat(encoder).isNotNull();
        }

        @Test
        @DisplayName("should encode passwords correctly")
        void shouldEncodePasswordsCorrectly() {
            PasswordEncoder encoder = config.passwordEncoder();
            String rawPassword = "testPassword123";

            String encoded = encoder.encode(rawPassword);

            assertThat(encoded)
                    .isNotEqualTo(rawPassword)
                    .satisfies(enc -> assertThat(encoder.matches(rawPassword, enc)).isTrue());
        }

        @Test
        @DisplayName("should not match wrong password")
        void shouldNotMatchWrongPassword() {
            PasswordEncoder encoder = config.passwordEncoder();
            String rawPassword = "testPassword123";
            String wrongPassword = "wrongPassword";

            String encoded = encoder.encode(rawPassword);

            assertThat(encoder.matches(wrongPassword, encoded)).isFalse();
        }

        @Test
        @DisplayName("should generate different hashes for same password")
        void shouldGenerateDifferentHashesForSamePassword() {
            PasswordEncoder encoder = config.passwordEncoder();
            String password = "samePassword";

            String encoded1 = encoder.encode(password);
            String encoded2 = encoder.encode(password);

            // BCrypt generates different hashes due to random salt
            assertThat(encoded1).isNotEqualTo(encoded2);
            // But both should match the original password
            assertThat(encoder.matches(password, encoded1))
                    .as("First encoded should match original password")
                    .isTrue();
            assertThat(encoder.matches(password, encoded2))
                    .as("Second encoded should match original password")
                    .isTrue();
        }
    }
}
