package com.jnzader.apigen.security.infrastructure.filter;

import static org.mockito.Mockito.*;

import com.jnzader.apigen.security.infrastructure.network.ClientIpResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@DisplayName("AuthRateLimitFilter Tests")
@ExtendWith(MockitoExtension.class)
class AuthRateLimitFilterTest {

    @Mock private HttpServletRequest request;

    @Mock private HttpServletResponse response;

    @Mock private FilterChain filterChain;

    @Mock private ClientIpResolver clientIpResolver;

    private AuthRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new AuthRateLimitFilter(3, 15, clientIpResolver); // 3 attempts per 15 minutes
    }

    @Nested
    @DisplayName("doFilterInternal - Login endpoint")
    class LoginEndpointTests {

        @Test
        @DisplayName("should allow request under limit")
        void shouldAllowRequestUnderLimit() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/auth/login");
            when(request.getMethod()).thenReturn("POST");
            when(clientIpResolver.resolveClientIp(request)).thenReturn("192.168.1.1");
            when(response.getStatus()).thenReturn(HttpStatus.OK.value());

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(response).setHeader("X-Auth-RateLimit-Limit", "3");
            verify(response).setHeader("X-Auth-RateLimit-Remaining", "3");
        }

        @Test
        @DisplayName("should block request over limit")
        void shouldBlockRequestOverLimit() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/auth/login");
            when(request.getMethod()).thenReturn("POST");
            when(clientIpResolver.resolveClientIp(request)).thenReturn("192.168.1.2");
            when(response.getStatus()).thenReturn(HttpStatus.UNAUTHORIZED.value());
            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            when(response.getWriter()).thenReturn(writer);

            // Make 4 requests (limit is 3)
            for (int i = 0; i < 4; i++) {
                filter.doFilterInternal(request, response, filterChain);
            }

            verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            verify(filterChain, times(3)).doFilter(request, response); // Only 3 should pass
        }

        @Test
        @DisplayName("should increment counter on failed login")
        void shouldIncrementCounterOnFailedLogin() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/auth/login");
            when(request.getMethod()).thenReturn("POST");
            when(clientIpResolver.resolveClientIp(request)).thenReturn("192.168.1.3");
            when(response.getStatus()).thenReturn(HttpStatus.UNAUTHORIZED.value());

            filter.doFilterInternal(request, response, filterChain);
            filter.doFilterInternal(request, response, filterChain);

            verify(response, times(2)).setHeader(eq("X-Auth-RateLimit-Remaining"), anyString());
        }

        @Test
        @DisplayName("should reset counter on successful login")
        void shouldResetCounterOnSuccessfulLogin() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/auth/login");
            when(request.getMethod()).thenReturn("POST");
            when(clientIpResolver.resolveClientIp(request)).thenReturn("192.168.1.4");

            // First fail 2 times
            when(response.getStatus()).thenReturn(HttpStatus.UNAUTHORIZED.value());
            filter.doFilterInternal(request, response, filterChain);
            filter.doFilterInternal(request, response, filterChain);

            // Then succeed
            when(response.getStatus()).thenReturn(HttpStatus.OK.value());
            filter.doFilterInternal(request, response, filterChain);

            // Should reset, so another failure should still be allowed
            when(response.getStatus()).thenReturn(HttpStatus.UNAUTHORIZED.value());
            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain, times(4)).doFilter(request, response);
        }

        @Test
        @DisplayName("should apply to register endpoint")
        void shouldApplyToRegisterEndpoint() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/auth/register");
            when(request.getMethod()).thenReturn("POST");
            when(clientIpResolver.resolveClientIp(request)).thenReturn("192.168.1.5");
            when(response.getStatus()).thenReturn(HttpStatus.OK.value());

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(response).setHeader("X-Auth-RateLimit-Limit", "3");
        }

        @Test
        @DisplayName("should increment counter on forbidden response")
        void shouldIncrementCounterOnForbiddenResponse() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/auth/login");
            when(request.getMethod()).thenReturn("POST");
            when(clientIpResolver.resolveClientIp(request)).thenReturn("192.168.1.6");
            when(response.getStatus()).thenReturn(HttpStatus.FORBIDDEN.value());

            filter.doFilterInternal(request, response, filterChain);
            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain, times(2)).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("doFilterInternal - Non-login endpoints")
    class NonLoginEndpointTests {

        @ParameterizedTest(name = "should pass through {0} {1}")
        @CsvSource({"/api/users, POST", "/api/auth/login, GET", "/actuator/health, GET"})
        @DisplayName("should pass through non-login endpoints without rate limiting")
        void shouldPassThroughNonLoginEndpoints(String uri, String method) throws Exception {
            when(request.getRequestURI()).thenReturn(uri);
            when(request.getMethod()).thenReturn(method);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(response, never()).setHeader(eq("X-Auth-RateLimit-Limit"), anyString());
        }
    }

    @Nested
    @DisplayName("Client IP detection")
    class ClientIpDetectionTests {

        @Test
        @DisplayName("should delegate IP resolution to ClientIpResolver")
        void shouldDelegateIpResolutionToClientIpResolver() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/auth/login");
            when(request.getMethod()).thenReturn("POST");
            when(clientIpResolver.resolveClientIp(request)).thenReturn("10.0.0.1");
            when(response.getStatus()).thenReturn(HttpStatus.OK.value());

            filter.doFilterInternal(request, response, filterChain);

            verify(clientIpResolver).resolveClientIp(request);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("should use resolved IP for rate limiting")
        void shouldUseResolvedIpForRateLimiting() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/auth/login");
            when(request.getMethod()).thenReturn("POST");
            when(clientIpResolver.resolveClientIp(request)).thenReturn("10.0.0.1");
            when(response.getStatus()).thenReturn(HttpStatus.UNAUTHORIZED.value());

            // First request
            filter.doFilterInternal(request, response, filterChain);

            // Change resolved IP - should have fresh limit
            when(clientIpResolver.resolveClientIp(request)).thenReturn("10.0.0.2");
            filter.doFilterInternal(request, response, filterChain);

            // Both should pass since different IPs
            verify(filterChain, times(2)).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("Rate limit response")
    class RateLimitResponseTests {

        @Test
        @DisplayName("should set Retry-After header when blocked")
        void shouldSetRetryAfterHeaderWhenBlocked() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/auth/login");
            when(request.getMethod()).thenReturn("POST");
            when(clientIpResolver.resolveClientIp(request)).thenReturn("192.168.1.10");
            when(response.getStatus()).thenReturn(HttpStatus.UNAUTHORIZED.value());
            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            when(response.getWriter()).thenReturn(writer);

            // Exhaust the limit
            for (int i = 0; i < 4; i++) {
                filter.doFilterInternal(request, response, filterChain);
            }

            verify(response).setHeader("Retry-After", "900"); // 15 minutes in seconds
        }

        @Test
        @DisplayName("should return JSON error response when blocked")
        void shouldReturnJsonErrorResponseWhenBlocked() throws Exception {
            when(request.getRequestURI()).thenReturn("/api/auth/login");
            when(request.getMethod()).thenReturn("POST");
            when(clientIpResolver.resolveClientIp(request)).thenReturn("192.168.1.11");
            when(response.getStatus()).thenReturn(HttpStatus.UNAUTHORIZED.value());
            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            when(response.getWriter()).thenReturn(writer);

            // Exhaust the limit
            for (int i = 0; i < 4; i++) {
                filter.doFilterInternal(request, response, filterChain);
            }

            verify(response).setContentType("application/json");
        }
    }
}
