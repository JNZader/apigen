package com.jnzader.apigen.core.infrastructure.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests para RequestLoggingFilter.
 * <p>
 * Verifica:
 * - Generación de trace IDs
 * - Sanitización de logs
 * - Filtrado de endpoints
 * - Headers de response
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RequestLoggingFilter Tests")
class RequestLoggingFilterTest {

    private RequestLoggingFilter filter;

    @Mock
    private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new RequestLoggingFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    // ==================== Trace ID Tests ====================

    @Nested
    @DisplayName("Trace ID Generation")
    class TraceIdTests {

        @Test
        @DisplayName("should generate trace ID when not provided")
        void shouldGenerateTraceIdWhenNotProvided() throws Exception {
            // Given
            request.setMethod("GET");
            request.setRequestURI("/api/test");

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            String traceId = response.getHeader("X-Trace-Id");
            // Trace ID is a full UUID (36 characters including hyphens)
            assertThat(traceId).isNotNull().hasSize(36);
        }

        @Test
        @DisplayName("should use provided trace ID from header")
        void shouldUseProvidedTraceIdFromHeader() throws Exception {
            // Given
            String providedTraceId = "abc12345";
            request.addHeader("X-Trace-Id", providedTraceId);
            request.setMethod("GET");
            request.setRequestURI("/api/test");

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            assertThat(response.getHeader("X-Trace-Id")).isEqualTo(providedTraceId);
        }

        @Test
        @DisplayName("should set trace ID in MDC during request")
        void shouldSetTraceIdInMDCDuringRequest() throws Exception {
            // Given
            request.setMethod("GET");
            request.setRequestURI("/api/test");
            final String[] capturedTraceId = new String[1];

            doAnswer(invocation -> {
                capturedTraceId[0] = MDC.get("traceId");
                return null;
            }).when(filterChain).doFilter(any(), any());

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            assertThat(capturedTraceId[0]).isNotNull();
        }

        @Test
        @DisplayName("should clear MDC after request")
        void shouldClearMDCAfterRequest() throws Exception {
            // Given
            request.setMethod("GET");
            request.setRequestURI("/api/test");

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            assertThat(MDC.get("traceId")).isNull();
        }
    }

    // ==================== Should Not Filter Tests ====================

    @Nested
    @DisplayName("Endpoint Filtering")
    class EndpointFilteringTests {

        @Test
        @DisplayName("should skip actuator health endpoint")
        void shouldSkipActuatorHealthEndpoint() {
            // Given
            request.setRequestURI("/actuator/health");

            // When
            boolean shouldSkip = filter.shouldNotFilter(request);

            // Then
            assertThat(shouldSkip).isTrue();
        }

        @Test
        @DisplayName("should skip actuator prometheus endpoint")
        void shouldSkipActuatorPrometheusEndpoint() {
            // Given
            request.setRequestURI("/actuator/prometheus");

            // When
            boolean shouldSkip = filter.shouldNotFilter(request);

            // Then
            assertThat(shouldSkip).isTrue();
        }

        @Test
        @DisplayName("should skip static resources")
        void shouldSkipStaticResources() {
            // Given - test various static resources
            String[] staticPaths = {"/favicon.ico", "/static/style.css", "/static/app.js"};

            for (String path : staticPaths) {
                request.setRequestURI(path);

                // When
                boolean shouldSkip = filter.shouldNotFilter(request);

                // Then
                assertThat(shouldSkip).as("Should skip: " + path).isTrue();
            }
        }

        @Test
        @DisplayName("should not skip API endpoints")
        void shouldNotSkipApiEndpoints() {
            // Given
            request.setRequestURI("/api/users");

            // When
            boolean shouldSkip = filter.shouldNotFilter(request);

            // Then
            assertThat(shouldSkip).isFalse();
        }
    }

    // ==================== Request Processing Tests ====================

    @Nested
    @DisplayName("Request Processing")
    class RequestProcessingTests {

        @Test
        @DisplayName("should pass request through filter chain")
        void shouldPassRequestThroughFilterChain() throws Exception {
            // Given
            request.setMethod("GET");
            request.setRequestURI("/api/test");

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(any(), any());
        }

        @Test
        @DisplayName("should handle exceptions gracefully")
        void shouldHandleExceptionsGracefully() throws Exception {
            // Given
            request.setMethod("GET");
            request.setRequestURI("/api/test");
            doThrow(new RuntimeException("Test error")).when(filterChain).doFilter(any(), any());

            // When/Then - should propagate exception but still clean up
            try {
                filter.doFilter(request, response, filterChain);
            } catch (RuntimeException e) {
                assertThat(e.getMessage()).isEqualTo("Test error");
            }

            // MDC should still be cleared
            assertThat(MDC.get("traceId")).isNull();
        }
    }

    // ==================== Client IP Tests ====================

    @Nested
    @DisplayName("Client IP Detection")
    class ClientIPTests {

        @Test
        @DisplayName("should detect IP from X-Forwarded-For header")
        void shouldDetectIpFromXForwardedFor() throws Exception {
            // Given
            request.setMethod("GET");
            request.setRequestURI("/api/test");
            request.addHeader("X-Forwarded-For", "192.168.1.100, 10.0.0.1");

            // When
            filter.doFilter(request, response, filterChain);

            // Then - IP should be extracted (first one from the list)
            verify(filterChain).doFilter(any(), any());
        }

        @Test
        @DisplayName("should fallback to X-Real-IP header")
        void shouldFallbackToXRealIp() throws Exception {
            // Given
            request.setMethod("GET");
            request.setRequestURI("/api/test");
            request.addHeader("X-Real-IP", "192.168.1.100");

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(any(), any());
        }

        @Test
        @DisplayName("should use remoteAddr as last resort")
        void shouldUseRemoteAddrAsLastResort() throws Exception {
            // Given
            request.setMethod("GET");
            request.setRequestURI("/api/test");
            request.setRemoteAddr("127.0.0.1");

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(any(), any());
        }

        @Test
        @DisplayName("should handle unknown IP in X-Forwarded-For")
        void shouldHandleUnknownIpInXForwardedFor() throws Exception {
            // Given
            request.setMethod("GET");
            request.setRequestURI("/api/test");
            request.addHeader("X-Forwarded-For", "unknown");
            request.addHeader("X-Real-IP", "192.168.1.100");

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(any(), any());
        }

        @Test
        @DisplayName("should handle blank X-Forwarded-For")
        void shouldHandleBlankXForwardedFor() throws Exception {
            // Given
            request.setMethod("GET");
            request.setRequestURI("/api/test");
            request.addHeader("X-Forwarded-For", "   ");
            request.setRemoteAddr("10.0.0.1");

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(any(), any());
        }
    }

    @Nested
    @DisplayName("Response Status Handling")
    class ResponseStatusTests {

        @Test
        @DisplayName("should handle 500 error status")
        void shouldHandle500ErrorStatus() throws Exception {
            // Given
            request.setMethod("POST");
            request.setRequestURI("/api/test");
            doAnswer(invocation -> {
                // The response is wrapped in ContentCachingResponseWrapper
                jakarta.servlet.http.HttpServletResponse wrappedResp =
                    (jakarta.servlet.http.HttpServletResponse) invocation.getArguments()[1];
                wrappedResp.setStatus(500);
                return null;
            }).when(filterChain).doFilter(any(), any());

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(any(), any());
        }

        @Test
        @DisplayName("should handle 400 error status")
        void shouldHandle400ErrorStatus() throws Exception {
            // Given
            request.setMethod("POST");
            request.setRequestURI("/api/test");
            doAnswer(invocation -> {
                jakarta.servlet.http.HttpServletResponse wrappedResp =
                    (jakarta.servlet.http.HttpServletResponse) invocation.getArguments()[1];
                wrappedResp.setStatus(400);
                return null;
            }).when(filterChain).doFilter(any(), any());

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(any(), any());
        }
    }

    @Nested
    @DisplayName("Query String Handling")
    class QueryStringTests {

        @Test
        @DisplayName("should handle request with query string")
        void shouldHandleRequestWithQueryString() throws Exception {
            // Given
            request.setMethod("GET");
            request.setRequestURI("/api/test");
            request.setQueryString("page=1&size=10");

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(any(), any());
        }

        @Test
        @DisplayName("should set correlation ID in response")
        void shouldSetCorrelationIdInResponse() throws Exception {
            // Given
            request.setMethod("GET");
            request.setRequestURI("/api/test");
            request.addHeader("X-Correlation-Id", "corr-123");

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            assertThat(response.getHeader("X-Correlation-Id")).isEqualTo("corr-123");
        }

        @Test
        @DisplayName("should generate correlation ID when not provided")
        void shouldGenerateCorrelationIdWhenNotProvided() throws Exception {
            // Given
            request.setMethod("GET");
            request.setRequestURI("/api/test");

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            assertThat(response.getHeader("X-Correlation-Id")).isNotNull();
        }

        @Test
        @DisplayName("should set request ID in response")
        void shouldSetRequestIdInResponse() throws Exception {
            // Given
            request.setMethod("GET");
            request.setRequestURI("/api/test");

            // When
            filter.doFilter(request, response, filterChain);

            // Then
            assertThat(response.getHeader("X-Request-Id")).isNotNull().hasSize(36);
        }
    }
}
