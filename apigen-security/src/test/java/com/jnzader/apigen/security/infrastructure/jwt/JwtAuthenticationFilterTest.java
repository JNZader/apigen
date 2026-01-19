package com.jnzader.apigen.security.infrastructure.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@DisplayName("JwtAuthenticationFilter Tests")
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;

    @Mock private UserDetailsService userDetailsService;

    @Mock private HttpServletRequest request;

    @Mock private HttpServletResponse response;

    @Mock private FilterChain filterChain;

    @Mock private UserDetails userDetails;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("doFilterInternal")
    class DoFilterInternalTests {

        @Test
        @DisplayName("should continue without authentication when no Authorization header")
        void shouldContinueWithoutAuthWhenNoHeader() throws Exception {
            when(request.getHeader("Authorization")).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("should continue without authentication when not Bearer token")
        void shouldContinueWhenNotBearerToken() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Basic abc123");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("should authenticate with valid token")
        void shouldAuthenticateWithValidToken() throws Exception {
            String token = "valid.jwt.token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtService.extractUsername(token)).thenReturn("testuser");
            when(jwtService.isAccessToken(token)).thenReturn(true);
            when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);
            when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
            when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        }

        @Test
        @DisplayName("should not authenticate with invalid token")
        void shouldNotAuthenticateWithInvalidToken() throws Exception {
            String token = "invalid.jwt.token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtService.extractUsername(token)).thenReturn("testuser");
            when(jwtService.isAccessToken(token)).thenReturn(true);
            when(jwtService.isTokenValid(token, userDetails)).thenReturn(false);
            when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("should not authenticate with refresh token")
        void shouldNotAuthenticateWithRefreshToken() throws Exception {
            String token = "refresh.jwt.token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtService.extractUsername(token)).thenReturn("testuser");
            when(jwtService.isAccessToken(token)).thenReturn(false);
            when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("should handle exception gracefully")
        void shouldHandleExceptionGracefully() throws Exception {
            String token = "error.jwt.token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtService.extractUsername(token)).thenThrow(new RuntimeException("Token error"));

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    @DisplayName("shouldNotFilter")
    class ShouldNotFilterTests {

        @Test
        @DisplayName("should not filter auth endpoints")
        void shouldNotFilterAuthEndpoints() {
            when(request.getServletPath()).thenReturn("/api/auth/login");
            assertThat(filter.shouldNotFilter(request)).isTrue();
        }

        @Test
        @DisplayName("should filter other endpoints")
        void shouldFilterOtherEndpoints() {
            when(request.getServletPath()).thenReturn("/api/users");
            assertThat(filter.shouldNotFilter(request)).isFalse();
        }
    }
}
