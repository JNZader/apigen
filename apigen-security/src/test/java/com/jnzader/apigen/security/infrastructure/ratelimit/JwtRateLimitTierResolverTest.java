package com.jnzader.apigen.security.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@DisplayName("JwtRateLimitTierResolver Tests")
class JwtRateLimitTierResolverTest {

    private JwtRateLimitTierResolver resolver;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        resolver = new JwtRateLimitTierResolver();
        request = mock(HttpServletRequest.class);
    }

    @Nested
    @DisplayName("resolve")
    class ResolveTests {

        @Test
        @DisplayName("should return ANONYMOUS for null authentication")
        void shouldReturnAnonymousForNullAuthentication() {
            RateLimitTier tier = resolver.resolve(null, request);
            assertThat(tier).isEqualTo(RateLimitTier.ANONYMOUS);
        }

        @Test
        @DisplayName("should return ANONYMOUS for unauthenticated user")
        void shouldReturnAnonymousForUnauthenticatedUser() {
            Authentication auth = mock(Authentication.class);
            when(auth.isAuthenticated()).thenReturn(false);

            RateLimitTier tier = resolver.resolve(auth, request);
            assertThat(tier).isEqualTo(RateLimitTier.ANONYMOUS);
        }

        @Test
        @DisplayName("should resolve tier from 'tier' JWT claim")
        void shouldResolveTierFromTierClaim() {
            JwtAuthenticationToken jwtAuth = createJwtAuth(Map.of("tier", "pro"));

            RateLimitTier tier = resolver.resolve(jwtAuth, request);
            assertThat(tier).isEqualTo(RateLimitTier.PRO);
        }

        @Test
        @DisplayName("should resolve tier from 'subscription' JWT claim")
        void shouldResolveTierFromSubscriptionClaim() {
            JwtAuthenticationToken jwtAuth = createJwtAuth(Map.of("subscription", "basic"));

            RateLimitTier tier = resolver.resolve(jwtAuth, request);
            assertThat(tier).isEqualTo(RateLimitTier.BASIC);
        }

        @Test
        @DisplayName("should resolve tier from 'plan' JWT claim")
        void shouldResolveTierFromPlanClaim() {
            JwtAuthenticationToken jwtAuth = createJwtAuth(Map.of("plan", "free"));

            RateLimitTier tier = resolver.resolve(jwtAuth, request);
            assertThat(tier).isEqualTo(RateLimitTier.FREE);
        }

        @Test
        @DisplayName("should prioritize 'tier' claim over other claims")
        void shouldPrioritizeTierClaimOverOtherClaims() {
            JwtAuthenticationToken jwtAuth =
                    createJwtAuth(
                            Map.of(
                                    "tier", "pro",
                                    "subscription", "basic",
                                    "plan", "free"));

            RateLimitTier tier = resolver.resolve(jwtAuth, request);
            assertThat(tier).isEqualTo(RateLimitTier.PRO);
        }

        @Test
        @DisplayName("should resolve tier from ROLE_TIER_ authority")
        void shouldResolveTierFromAuthority() {
            Jwt jwt =
                    Jwt.withTokenValue("token")
                            .header("alg", "HS256")
                            .subject("user123")
                            .issuedAt(Instant.now())
                            .expiresAt(Instant.now().plusSeconds(3600))
                            .build();

            Collection<GrantedAuthority> authorities =
                    List.of(
                            new SimpleGrantedAuthority("ROLE_USER"),
                            new SimpleGrantedAuthority("ROLE_TIER_BASIC"));

            JwtAuthenticationToken jwtAuth = new JwtAuthenticationToken(jwt, authorities);

            RateLimitTier tier = resolver.resolve(jwtAuth, request);
            assertThat(tier).isEqualTo(RateLimitTier.BASIC);
        }

        @Test
        @DisplayName("should default to FREE for authenticated user without tier info")
        void shouldDefaultToFreeForAuthenticatedUserWithoutTierInfo() {
            JwtAuthenticationToken jwtAuth = createJwtAuth(Map.of());

            RateLimitTier tier = resolver.resolve(jwtAuth, request);
            assertThat(tier).isEqualTo(RateLimitTier.FREE);
        }
    }

    @Nested
    @DisplayName("getUserIdentifier")
    class GetUserIdentifierTests {

        @Test
        @DisplayName("should return ip:address for null authentication")
        void shouldReturnIpForNullAuthentication() {
            String identifier = resolver.getUserIdentifier(null, "192.168.1.1");
            assertThat(identifier).isEqualTo("ip:192.168.1.1");
        }

        @Test
        @DisplayName("should return ip:address for unauthenticated user")
        void shouldReturnIpForUnauthenticatedUser() {
            Authentication auth = mock(Authentication.class);
            when(auth.isAuthenticated()).thenReturn(false);

            String identifier = resolver.getUserIdentifier(auth, "10.0.0.1");
            assertThat(identifier).isEqualTo("ip:10.0.0.1");
        }

        @Test
        @DisplayName("should return user:subject for JWT authentication")
        void shouldReturnUserSubjectForJwtAuthentication() {
            JwtAuthenticationToken jwtAuth = createJwtAuth(Map.of());

            String identifier = resolver.getUserIdentifier(jwtAuth, "192.168.1.1");
            assertThat(identifier).isEqualTo("user:user123");
        }

        @Test
        @DisplayName("should return user:name for non-JWT authentication")
        void shouldReturnUserNameForNonJwtAuthentication() {
            Authentication auth = mock(Authentication.class);
            when(auth.isAuthenticated()).thenReturn(true);
            when(auth.getName()).thenReturn("testuser");

            String identifier = resolver.getUserIdentifier(auth, "192.168.1.1");
            assertThat(identifier).isEqualTo("user:testuser");
        }
    }

    private JwtAuthenticationToken createJwtAuth(Map<String, Object> claims) {
        Jwt.Builder jwtBuilder =
                Jwt.withTokenValue("token")
                        .header("alg", "HS256")
                        .subject("user123")
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(3600));

        claims.forEach(jwtBuilder::claim);
        Jwt jwt = jwtBuilder.build();

        // Need to provide authorities for the token to be considered authenticated
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        return new JwtAuthenticationToken(jwt, authorities);
    }
}
