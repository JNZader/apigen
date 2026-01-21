package com.jnzader.apigen.security.infrastructure.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnzader.apigen.security.application.service.TokenBlacklistService;
import com.jnzader.apigen.security.domain.entity.Role;
import com.jnzader.apigen.security.domain.entity.User;
import com.jnzader.apigen.security.infrastructure.config.SecurityProperties;
import java.time.Instant;
import java.util.HashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

/** Tests para JwtService. */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService Tests")
class JwtServiceTest {

    private static final String TEST_SECRET =
            "test-secret-key-that-is-at-least-32-bytes-long-for-HS256";
    private static final int ACCESS_TOKEN_EXPIRATION = 15;
    private static final int REFRESH_TOKEN_EXPIRATION = 10080;

    @Mock private TokenBlacklistService blacklistService;

    @Mock private UserDetails userDetails;

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        SecurityProperties properties = createSecurityProperties();
        jwtService = new JwtService(properties, blacklistService, new ObjectMapper());
        testUser = createTestUser();
    }

    private SecurityProperties createSecurityProperties() {
        SecurityProperties properties = new SecurityProperties();
        properties.setEnabled(true);

        SecurityProperties.JwtProperties jwt = new SecurityProperties.JwtProperties();
        jwt.setSecret(TEST_SECRET);
        jwt.setExpirationMinutes(ACCESS_TOKEN_EXPIRATION);
        jwt.setRefreshExpirationMinutes(REFRESH_TOKEN_EXPIRATION);
        jwt.setIssuer("apigen-test");
        properties.setJwt(jwt);

        return properties;
    }

    private User createTestUser() {
        Role role = new Role();
        role.setId(1L);
        role.setName("USER");
        role.setPermissions(new HashSet<>());

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("encoded-password");
        user.setRole(role);
        user.setEnabled(true);

        return user;
    }

    @Nested
    @DisplayName("Token Generation")
    class TokenGenerationTests {

        @Test
        @DisplayName("should generate access token")
        void shouldGenerateAccessToken() {
            String token = jwtService.generateAccessToken(testUser);

            assertThat(token).isNotNull().isNotBlank();
            assertThat(jwtService.isAccessToken(token)).isTrue();
            assertThat(jwtService.isRefreshToken(token)).isFalse();
        }

        @Test
        @DisplayName("should generate refresh token")
        void shouldGenerateRefreshToken() {
            String token = jwtService.generateRefreshToken(testUser);

            assertThat(token).isNotNull().isNotBlank();
            assertThat(jwtService.isRefreshToken(token)).isTrue();
            assertThat(jwtService.isAccessToken(token)).isFalse();
        }

        @Test
        @DisplayName("should include user claims in access token")
        void shouldIncludeUserClaimsInAccessToken() {
            String token = jwtService.generateAccessToken(testUser);

            assertThat(jwtService.extractUsername(token)).isEqualTo("testuser");
            assertThat(jwtService.extractUserId(token)).isEqualTo(1L);
            assertThat(jwtService.extractRole(token)).isEqualTo("USER");
        }

        @Test
        @DisplayName("should generate unique token IDs")
        void shouldGenerateUniqueTokenIds() {
            String token1 = jwtService.generateAccessToken(testUser);
            String token2 = jwtService.generateAccessToken(testUser);

            assertThat(jwtService.extractTokenId(token1))
                    .isNotEqualTo(jwtService.extractTokenId(token2));
        }
    }

    @Nested
    @DisplayName("Token Validation")
    class TokenValidationTests {

        @Test
        @DisplayName("should validate valid token")
        void shouldValidateValidToken() {
            String token = jwtService.generateAccessToken(testUser);
            when(userDetails.getUsername()).thenReturn("testuser");
            when(blacklistService.isBlacklisted(anyString())).thenReturn(false);

            assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
        }

        @Test
        @DisplayName("should reject token with wrong username")
        void shouldRejectTokenWithWrongUsername() {
            String token = jwtService.generateAccessToken(testUser);
            when(userDetails.getUsername()).thenReturn("otheruser");
            when(blacklistService.isBlacklisted(anyString())).thenReturn(false);

            assertThat(jwtService.isTokenValid(token, userDetails)).isFalse();
        }

        @Test
        @DisplayName("should reject blacklisted token")
        void shouldRejectBlacklistedToken() {
            String token = jwtService.generateAccessToken(testUser);
            when(blacklistService.isBlacklisted(anyString())).thenReturn(true);

            assertThat(jwtService.isTokenValid(token, userDetails)).isFalse();
        }

        @Test
        @DisplayName("should validate token structure")
        void shouldValidateTokenStructure() {
            String validToken = jwtService.generateAccessToken(testUser);

            assertThat(jwtService.isTokenStructureValid(validToken)).isTrue();
            assertThat(jwtService.isTokenStructureValid("invalid.token.here")).isFalse();
            assertThat(jwtService.isTokenStructureValid("not-a-jwt")).isFalse();
        }
    }

    @Nested
    @DisplayName("Token Expiration")
    class TokenExpirationTests {

        @Test
        @DisplayName("should extract expiration from token")
        void shouldExtractExpirationFromToken() {
            String token = jwtService.generateAccessToken(testUser);
            Instant expiration = jwtService.extractExpiration(token);

            assertThat(expiration)
                    .isAfter(Instant.now())
                    .isBefore(Instant.now().plusSeconds(ACCESS_TOKEN_EXPIRATION * 60 + 60));
        }

        @Test
        @DisplayName("should detect non-expired token")
        void shouldDetectNonExpiredToken() {
            String token = jwtService.generateAccessToken(testUser);

            assertThat(jwtService.isTokenExpired(token)).isFalse();
        }
    }

    @Nested
    @DisplayName("Claim Extraction")
    class ClaimExtractionTests {

        @Test
        @DisplayName("should extract all claims from token")
        void shouldExtractAllClaimsFromToken() {
            String token = jwtService.generateAccessToken(testUser);

            assertThat(jwtService.extractUsername(token)).isEqualTo("testuser");
            assertThat(jwtService.extractUserId(token)).isEqualTo(1L);
            assertThat(jwtService.extractRole(token)).isEqualTo("USER");
            assertThat(jwtService.extractTokenType(token)).isEqualTo("access");
            assertThat(jwtService.extractTokenId(token)).isNotNull();
        }

        @Test
        @DisplayName("should extract claims ignoring expiration")
        void shouldExtractClaimsIgnoringExpiration() {
            String token = jwtService.generateAccessToken(testUser);
            var claims = jwtService.extractClaimsIgnoringExpiration(token);

            assertThat(claims).isNotNull();
            assertThat(claims.getSubject()).isEqualTo("testuser");
        }
    }
}
