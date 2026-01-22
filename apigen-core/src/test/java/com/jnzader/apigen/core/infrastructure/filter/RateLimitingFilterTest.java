package com.jnzader.apigen.core.infrastructure.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@DisplayName("RateLimitingFilter Tests")
@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock private HttpServletRequest request;

    @Mock private HttpServletResponse response;

    @Mock private FilterChain filterChain;

    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter(true, 5, 60); // enabled, 5 requests per 60 seconds
    }

    @Nested
    @DisplayName("doFilterInternal")
    class DoFilterInternalTests {

        @Test
        @DisplayName("should allow request under limit")
        void shouldAllowRequestUnderLimit() throws Exception {
            when(request.getRemoteAddr()).thenReturn("192.168.1.1");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(response).setHeader("X-RateLimit-Limit", "5");
            verify(response).setHeader("X-RateLimit-Remaining", "4");
        }

        @Test
        @DisplayName("should block request over limit")
        void shouldBlockRequestOverLimit() throws Exception {
            when(request.getRemoteAddr()).thenReturn("192.168.1.2");
            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            when(response.getWriter()).thenReturn(writer);

            // Make 6 requests (limit is 5)
            for (int i = 0; i < 6; i++) {
                filter.doFilterInternal(request, response, filterChain);
            }

            verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            verify(filterChain, times(5)).doFilter(request, response); // Only 5 should pass
        }

        @Test
        @DisplayName("should use X-Forwarded-For header")
        void shouldUseXForwardedForHeader() throws Exception {
            when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("should use X-Real-IP header when X-Forwarded-For is missing")
        void shouldUseXRealIpHeader() throws Exception {
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn("10.0.0.2");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("should extract first IP from comma-separated list")
        void shouldExtractFirstIpFromList() throws Exception {
            when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.3, 192.168.1.1");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("should handle unknown IP header")
        void shouldHandleUnknownIpHeader() throws Exception {
            when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
            when(request.getHeader("X-Real-IP")).thenReturn("unknown");
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("shouldNotFilter")
    class ShouldNotFilterTests {

        @Test
        @DisplayName("should not filter actuator endpoints")
        void shouldNotFilterActuatorEndpoints() {
            when(request.getRequestURI()).thenReturn("/actuator/health");
            assertThat(filter.shouldNotFilter(request)).isTrue();
        }

        @Test
        @DisplayName("should filter API endpoints")
        void shouldFilterApiEndpoints() {
            when(request.getRequestURI()).thenReturn("/api/users");
            assertThat(filter.shouldNotFilter(request)).isFalse();
        }

        @Test
        @DisplayName("should not filter when rate limiting is disabled")
        void shouldNotFilterWhenDisabled() {
            RateLimitingFilter disabledFilter = new RateLimitingFilter(false, 5, 60);
            // No need to mock URI - disabled filter returns true before checking path
            assertThat(disabledFilter.shouldNotFilter(request)).isTrue();
        }
    }
}
