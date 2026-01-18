package com.jnzader.apigen.core.infrastructure.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@DisplayName("RequestIdInterceptor Tests")
@ExtendWith(MockitoExtension.class)
class RequestIdInterceptorTest {

    private RequestIdInterceptor interceptor;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new RequestIdInterceptor();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Nested
    @DisplayName("preHandle")
    class PreHandleTests {

        @Test
        @DisplayName("should use request ID from header")
        void shouldUseRequestIdFromHeader() {
            when(request.getHeader("X-Request-ID")).thenReturn("test-request-id-123");

            boolean result = interceptor.preHandle(request, response, null);

            assertThat(result).isTrue();
            assertThat(MDC.get("requestId")).isEqualTo("test-request-id-123");
            verify(response).setHeader("X-Request-ID", "test-request-id-123");
        }

        @Test
        @DisplayName("should generate request ID when header is null")
        void shouldGenerateRequestIdWhenHeaderIsNull() {
            when(request.getHeader("X-Request-ID")).thenReturn(null);

            boolean result = interceptor.preHandle(request, response, null);

            assertThat(result).isTrue();
            assertThat(MDC.get("requestId")).isNotNull().hasSize(16);
            verify(response).setHeader(eq("X-Request-ID"), anyString());
        }

        @Test
        @DisplayName("should generate request ID when header is blank")
        void shouldGenerateRequestIdWhenHeaderIsBlank() {
            when(request.getHeader("X-Request-ID")).thenReturn("   ");

            boolean result = interceptor.preHandle(request, response, null);

            assertThat(result).isTrue();
            assertThat(MDC.get("requestId")).isNotNull().hasSize(16);
        }

        @Test
        @DisplayName("should generate unique request IDs")
        void shouldGenerateUniqueRequestIds() {
            when(request.getHeader("X-Request-ID")).thenReturn(null);

            interceptor.preHandle(request, response, null);
            String id1 = MDC.get("requestId");

            MDC.clear();
            interceptor.preHandle(request, response, null);
            String id2 = MDC.get("requestId");

            assertThat(id1).isNotEqualTo(id2);
        }
    }

    @Nested
    @DisplayName("afterCompletion")
    class AfterCompletionTests {

        @Test
        @DisplayName("should clear MDC after completion")
        void shouldClearMdcAfterCompletion() {
            MDC.put("requestId", "test-id");

            interceptor.afterCompletion(request, response, null, null);

            assertThat(MDC.get("requestId")).isNull();
        }

        @Test
        @DisplayName("should handle exception in afterCompletion")
        void shouldHandleExceptionInAfterCompletion() {
            MDC.put("requestId", "test-id");
            Exception ex = new RuntimeException("Test error");

            interceptor.afterCompletion(request, response, null, ex);

            assertThat(MDC.get("requestId")).isNull();
        }
    }

    @Nested
    @DisplayName("postHandle")
    class PostHandleTests {

        @Test
        @DisplayName("should not throw exception")
        void shouldNotThrowException() {
            // postHandle should do nothing - verify it completes without throwing
            assertThatCode(() -> interceptor.postHandle(request, response, null, null))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Constants")
    class ConstantsTests {

        @Test
        @DisplayName("should have correct header name")
        void shouldHaveCorrectHeaderName() {
            assertThat(RequestIdInterceptor.REQUEST_ID_HEADER).isEqualTo("X-Request-ID");
        }

        @Test
        @DisplayName("should have correct MDC key")
        void shouldHaveCorrectMdcKey() {
            assertThat(RequestIdInterceptor.MDC_REQUEST_ID_KEY).isEqualTo("requestId");
        }
    }
}
