package com.jnzader.apigen.security.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.jnzader.apigen.core.infrastructure.config.properties.AppProperties;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Unit tests for OAuth2SecurityConfig. Tests JWT authentication converter and CORS configuration
 * for OAuth2 mode.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OAuth2SecurityConfig Tests")
class OAuth2SecurityConfigTest {

    @Mock private SecurityProperties securityProperties;

    @Mock private SecurityProperties.OAuth2Properties oauth2Properties;

    @Mock private AppProperties appProperties;

    @Mock private AppProperties.CorsProperties corsProperties;

    private OAuth2SecurityConfig config;

    @BeforeEach
    void setUp() {
        when(securityProperties.getOauth2()).thenReturn(oauth2Properties);
        when(appProperties.cors()).thenReturn(corsProperties);

        // Default CORS settings
        when(corsProperties.allowedOrigins()).thenReturn(List.of("http://localhost:3000"));
        when(corsProperties.allowedMethods()).thenReturn(List.of("GET", "POST", "PUT", "DELETE"));
        when(corsProperties.allowedHeaders()).thenReturn(List.of("Authorization", "Content-Type"));
        when(corsProperties.exposedHeaders()).thenReturn(new ArrayList<>(List.of("Authorization")));
        when(corsProperties.allowCredentials()).thenReturn(true);
        when(corsProperties.maxAge()).thenReturn(3600L);

        config = new OAuth2SecurityConfig(securityProperties, appProperties);
    }

    @Nested
    @DisplayName("JWT Authentication Converter")
    class JwtAuthenticationConverterTests {

        @BeforeEach
        void setUpOAuth2Properties() {
            when(oauth2Properties.getRolesClaim()).thenReturn("permissions");
            when(oauth2Properties.getRolePrefix()).thenReturn("ROLE_");
        }

        @Test
        @DisplayName("should create JwtAuthenticationConverter bean")
        void shouldCreateJwtAuthenticationConverterBean() {
            JwtAuthenticationConverter converter = config.jwtAuthenticationConverter();

            assertThat(converter).isNotNull();
        }

        @Test
        @DisplayName("should extract roles from simple array claim (Auth0 style)")
        void shouldExtractRolesFromSimpleArrayClaim() {
            JwtAuthenticationConverter converter = config.jwtAuthenticationConverter();

            Jwt jwt = createJwt(Map.of("permissions", List.of("admin", "user", "read")));

            var authentication = converter.convert(jwt);

            assertThat(authentication).isNotNull();
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            assertThat(authorities)
                    .extracting(GrantedAuthority::getAuthority)
                    .contains("ROLE_ADMIN", "ROLE_USER", "ROLE_READ");
        }

        @Test
        @DisplayName("should preserve role that already has prefix")
        void shouldPreserveRoleThatAlreadyHasPrefix() {
            JwtAuthenticationConverter converter = config.jwtAuthenticationConverter();

            Jwt jwt = createJwt(Map.of("permissions", List.of("ROLE_ADMIN", "user")));

            var authentication = converter.convert(jwt);

            assertThat(authentication).isNotNull();
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            assertThat(authorities)
                    .extracting(GrantedAuthority::getAuthority)
                    .contains("ROLE_ADMIN", "ROLE_USER");
        }

        @Test
        @DisplayName("should add default USER role when no roles found")
        void shouldAddDefaultUserRoleWhenNoRolesFound() {
            JwtAuthenticationConverter converter = config.jwtAuthenticationConverter();

            Jwt jwt = createJwt(Map.of("sub", "user123"));

            var authentication = converter.convert(jwt);

            assertThat(authentication).isNotNull();
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            assertThat(authorities)
                    .extracting(GrantedAuthority::getAuthority)
                    .contains("ROLE_USER");
        }

        @Test
        @DisplayName("should skip empty roles")
        void shouldSkipEmptyRoles() {
            JwtAuthenticationConverter converter = config.jwtAuthenticationConverter();

            Jwt jwt = createJwt(Map.of("permissions", List.of("admin", "", "  ", "user")));

            var authentication = converter.convert(jwt);

            assertThat(authentication).isNotNull();
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            // Should contain ROLE_ADMIN and ROLE_USER (empty strings are skipped)
            // Note: May include additional default authorities from Spring Security
            assertThat(authorities)
                    .extracting(GrantedAuthority::getAuthority)
                    .contains("ROLE_ADMIN", "ROLE_USER")
                    .doesNotContain("ROLE_", "ROLE_  ");
        }

        @Test
        @DisplayName("should trim whitespace from roles")
        void shouldTrimWhitespaceFromRoles() {
            JwtAuthenticationConverter converter = config.jwtAuthenticationConverter();

            Jwt jwt = createJwt(Map.of("permissions", List.of("  admin  ", " user ")));

            var authentication = converter.convert(jwt);

            assertThat(authentication).isNotNull();
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            assertThat(authorities)
                    .extracting(GrantedAuthority::getAuthority)
                    .contains("ROLE_ADMIN", "ROLE_USER");
        }
    }

    @Nested
    @DisplayName("Keycloak Role Extraction")
    class KeycloakRoleExtractionTests {

        @Test
        @DisplayName("should extract roles from realm_access.roles (Keycloak style)")
        void shouldExtractRolesFromRealmAccessRoles() {
            when(oauth2Properties.getRolesClaim()).thenReturn("realm_access.roles");
            when(oauth2Properties.getRolePrefix()).thenReturn("ROLE_");

            JwtAuthenticationConverter converter = config.jwtAuthenticationConverter();

            Jwt jwt =
                    createJwt(
                            Map.of(
                                    "realm_access",
                                    Map.of("roles", List.of("admin", "offline_access"))));

            var authentication = converter.convert(jwt);

            assertThat(authentication).isNotNull();
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            assertThat(authorities)
                    .extracting(GrantedAuthority::getAuthority)
                    .contains("ROLE_ADMIN", "ROLE_OFFLINE_ACCESS");
        }

        @Test
        @DisplayName("should extract roles from resource_access (Keycloak client roles)")
        void shouldExtractRolesFromResourceAccess() {
            when(oauth2Properties.getRolesClaim()).thenReturn("permissions");
            when(oauth2Properties.getRolePrefix()).thenReturn("ROLE_");

            JwtAuthenticationConverter converter = config.jwtAuthenticationConverter();

            Jwt jwt =
                    createJwt(
                            Map.of(
                                    "resource_access",
                                    Map.of(
                                            "my-client",
                                                    Map.of(
                                                            "roles",
                                                            List.of("client_admin", "client_user")),
                                            "other-client",
                                                    Map.of("roles", List.of("other_role")))));

            var authentication = converter.convert(jwt);

            assertThat(authentication).isNotNull();
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            assertThat(authorities)
                    .extracting(GrantedAuthority::getAuthority)
                    .contains("ROLE_CLIENT_ADMIN", "ROLE_CLIENT_USER", "ROLE_OTHER_ROLE");
        }

        @Test
        @DisplayName("should not add duplicate roles from resource_access")
        void shouldNotAddDuplicateRolesFromResourceAccess() {
            when(oauth2Properties.getRolesClaim()).thenReturn("permissions");
            when(oauth2Properties.getRolePrefix()).thenReturn("ROLE_");

            JwtAuthenticationConverter converter = config.jwtAuthenticationConverter();

            Jwt jwt =
                    createJwt(
                            Map.of(
                                    "resource_access",
                                    Map.of(
                                            "client1", Map.of("roles", List.of("admin")),
                                            "client2", Map.of("roles", List.of("admin", "user")))));

            var authentication = converter.convert(jwt);

            assertThat(authentication).isNotNull();
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            // Should have admin only once, plus user
            long adminCount =
                    authorities.stream().filter(a -> a.getAuthority().equals("ROLE_ADMIN")).count();
            assertThat(adminCount).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("AWS Cognito Role Extraction")
    class CognitoRoleExtractionTests {

        @Test
        @DisplayName("should extract roles from cognito:groups")
        void shouldExtractRolesFromCognitoGroups() {
            when(oauth2Properties.getRolesClaim()).thenReturn("permissions");
            when(oauth2Properties.getRolePrefix()).thenReturn("ROLE_");

            JwtAuthenticationConverter converter = config.jwtAuthenticationConverter();

            Jwt jwt = createJwt(Map.of("cognito:groups", List.of("Admins", "Developers")));

            var authentication = converter.convert(jwt);

            assertThat(authentication).isNotNull();
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            assertThat(authorities)
                    .extracting(GrantedAuthority::getAuthority)
                    .contains("ROLE_ADMINS", "ROLE_DEVELOPERS");
        }
    }

    @Nested
    @DisplayName("Custom Role Prefix")
    class CustomRolePrefixTests {

        @Test
        @DisplayName("should use custom role prefix (SCOPE_)")
        void shouldUseCustomRolePrefixScope() {
            when(oauth2Properties.getRolesClaim()).thenReturn("permissions");
            when(oauth2Properties.getRolePrefix()).thenReturn("SCOPE_");

            JwtAuthenticationConverter converter = config.jwtAuthenticationConverter();

            Jwt jwt = createJwt(Map.of("permissions", List.of("read", "write")));

            var authentication = converter.convert(jwt);

            assertThat(authentication).isNotNull();
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            assertThat(authorities)
                    .extracting(GrantedAuthority::getAuthority)
                    .contains("SCOPE_READ", "SCOPE_WRITE");
        }

        @Test
        @DisplayName("should add default role with custom prefix when no roles found")
        void shouldAddDefaultRoleWithCustomPrefix() {
            when(oauth2Properties.getRolesClaim()).thenReturn("permissions");
            when(oauth2Properties.getRolePrefix()).thenReturn("PERM_");

            JwtAuthenticationConverter converter = config.jwtAuthenticationConverter();

            Jwt jwt = createJwt(Map.of("sub", "user123"));

            var authentication = converter.convert(jwt);

            assertThat(authentication).isNotNull();
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            assertThat(authorities)
                    .extracting(GrantedAuthority::getAuthority)
                    .contains("PERM_USER");
        }
    }

    @Nested
    @DisplayName("CORS Configuration")
    class CorsConfigurationTests {

        private CorsConfiguration getCorsConfig(CorsConfigurationSource source) {
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

        @Test
        @DisplayName("should use allowed origins from app properties")
        void shouldUseAllowedOriginsFromAppProperties() {
            when(corsProperties.allowedOrigins())
                    .thenReturn(List.of("https://myapp.com", "https://api.myapp.com"));

            CorsConfigurationSource source = config.corsConfigurationSource();
            CorsConfiguration corsConfig = getCorsConfig(source);

            assertThat(corsConfig).isNotNull();
            assertThat(corsConfig.getAllowedOrigins())
                    .containsExactlyInAnyOrder("https://myapp.com", "https://api.myapp.com");
        }

        @Test
        @DisplayName("should add ETag header if not present")
        void shouldAddETagHeaderIfNotPresent() {
            when(corsProperties.exposedHeaders())
                    .thenReturn(new ArrayList<>(List.of("Authorization")));

            CorsConfigurationSource source = config.corsConfigurationSource();
            CorsConfiguration corsConfig = getCorsConfig(source);

            assertThat(corsConfig).isNotNull();
            assertThat(corsConfig.getExposedHeaders()).contains("ETag");
        }

        @Test
        @DisplayName("should add Last-Modified header if not present")
        void shouldAddLastModifiedHeaderIfNotPresent() {
            when(corsProperties.exposedHeaders())
                    .thenReturn(new ArrayList<>(List.of("Authorization")));

            CorsConfigurationSource source = config.corsConfigurationSource();
            CorsConfiguration corsConfig = getCorsConfig(source);

            assertThat(corsConfig).isNotNull();
            assertThat(corsConfig.getExposedHeaders()).contains("Last-Modified");
        }

        @Test
        @DisplayName("should not duplicate ETag header if already present")
        void shouldNotDuplicateETagHeaderIfAlreadyPresent() {
            when(corsProperties.exposedHeaders())
                    .thenReturn(new ArrayList<>(List.of("Authorization", "ETag")));

            CorsConfigurationSource source = config.corsConfigurationSource();
            CorsConfiguration corsConfig = getCorsConfig(source);

            assertThat(corsConfig).isNotNull();
            long etagCount =
                    corsConfig.getExposedHeaders().stream().filter(h -> h.equals("ETag")).count();
            assertThat(etagCount).isEqualTo(1);
        }

        @Test
        @DisplayName("should not duplicate Last-Modified header if already present")
        void shouldNotDuplicateLastModifiedHeaderIfAlreadyPresent() {
            when(corsProperties.exposedHeaders())
                    .thenReturn(new ArrayList<>(List.of("Authorization", "Last-Modified")));

            CorsConfigurationSource source = config.corsConfigurationSource();
            CorsConfiguration corsConfig = getCorsConfig(source);

            assertThat(corsConfig).isNotNull();
            long lastModifiedCount =
                    corsConfig.getExposedHeaders().stream()
                            .filter(h -> h.equals("Last-Modified"))
                            .count();
            assertThat(lastModifiedCount).isEqualTo(1);
        }

        @Test
        @DisplayName("should use credentials setting from properties")
        void shouldUseCredentialsSettingFromProperties() {
            when(corsProperties.allowCredentials()).thenReturn(false);

            CorsConfigurationSource source = config.corsConfigurationSource();
            CorsConfiguration corsConfig = getCorsConfig(source);

            assertThat(corsConfig).isNotNull();
            assertThat(corsConfig.getAllowCredentials()).isFalse();
        }

        @Test
        @DisplayName("should use max age from properties")
        void shouldUseMaxAgeFromProperties() {
            when(corsProperties.maxAge()).thenReturn(7200L);

            CorsConfigurationSource source = config.corsConfigurationSource();
            CorsConfiguration corsConfig = getCorsConfig(source);

            assertThat(corsConfig).isNotNull();
            assertThat(corsConfig.getMaxAge()).isEqualTo(7200L);
        }
    }

    /** Helper method to create a mock JWT for testing. */
    private Jwt createJwt(Map<String, Object> claims) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");
        headers.put("typ", "JWT");

        Map<String, Object> allClaims = new HashMap<>();
        allClaims.put("sub", "user123");
        allClaims.put("iss", "https://auth.example.com/");
        allClaims.put("aud", "my-api");
        allClaims.putAll(claims);

        return new Jwt(
                "mock-token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                headers,
                allClaims);
    }
}
