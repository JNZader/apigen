package com.jnzader.apigen.core.infrastructure.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jnzader.apigen.core.infrastructure.batch.BatchRequest.BatchOperation;
import com.jnzader.apigen.core.infrastructure.batch.BatchResponse.OperationResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.DispatcherServlet;

@ExtendWith(MockitoExtension.class)
@DisplayName("BatchService")
class BatchServiceTest {

    @Mock private DispatcherServlet dispatcherServlet;

    private ObjectMapper objectMapper;
    private BatchService batchService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        batchService = new BatchService(dispatcherServlet, objectMapper);
    }

    private MockHttpServletRequest createMockRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("localhost");
        request.setServerPort(8080);
        request.setScheme("http");
        request.setContextPath("");
        return request;
    }

    @Nested
    @DisplayName("processBatch - sequential execution")
    class ProcessBatchTests {

        @Test
        @DisplayName("should process single GET operation successfully")
        void shouldProcessSingleGetOperation() throws Exception {
            // Given
            List<BatchOperation> operations =
                    List.of(new BatchOperation("op1", "GET", "/api/users/1", null, null));
            BatchRequest request = new BatchRequest(operations, false);

            doAnswer(
                            invocation -> {
                                HttpServletResponse response = invocation.getArgument(1);
                                response.setStatus(200);
                                response.setContentType("application/json");
                                PrintWriter writer = response.getWriter();
                                writer.write("{\"id\":1,\"name\":\"John\"}");
                                return null;
                            })
                    .when(dispatcherServlet)
                    .service(any(HttpServletRequest.class), any(HttpServletResponse.class));

            // When
            BatchResponse response = batchService.processBatch(request, createMockRequest());

            // Then
            assertThat(response.results()).hasSize(1);
            assertThat(response.results().getFirst().status()).isEqualTo(200);
            assertThat(response.results().getFirst().isSuccessful()).isTrue();
            assertThat(response.summary().total()).isEqualTo(1);
            assertThat(response.summary().successful()).isEqualTo(1);
            assertThat(response.summary().failed()).isZero();
        }

        @Test
        @DisplayName("should process multiple operations in order")
        void shouldProcessMultipleOperationsInOrder() throws Exception {
            // Given
            List<BatchOperation> operations =
                    List.of(
                            new BatchOperation("op1", "GET", "/api/users/1", null, null),
                            new BatchOperation("op2", "GET", "/api/users/2", null, null),
                            new BatchOperation("op3", "DELETE", "/api/users/3", null, null));
            BatchRequest request = new BatchRequest(operations, false);

            doAnswer(
                            invocation -> {
                                HttpServletResponse response = invocation.getArgument(1);
                                response.setStatus(200);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"status\":\"ok\"}");
                                return null;
                            })
                    .when(dispatcherServlet)
                    .service(any(HttpServletRequest.class), any(HttpServletResponse.class));

            // When
            BatchResponse response = batchService.processBatch(request, createMockRequest());

            // Then
            assertThat(response.results()).hasSize(3);
            assertThat(response.summary().total()).isEqualTo(3);
            assertThat(response.summary().successful()).isEqualTo(3);
            verify(dispatcherServlet, times(3)).service(any(), any());
        }

        @Test
        @DisplayName("should stop on error when stopOnError is true")
        void shouldStopOnErrorWhenFlagIsTrue() throws Exception {
            // Given
            List<BatchOperation> operations =
                    List.of(
                            new BatchOperation("op1", "GET", "/api/users/1", null, null),
                            new BatchOperation("op2", "GET", "/api/users/2", null, null),
                            new BatchOperation("op3", "GET", "/api/users/3", null, null));
            BatchRequest request = new BatchRequest(operations, true);

            doAnswer(
                            invocation -> {
                                HttpServletRequest req = invocation.getArgument(0);
                                HttpServletResponse response = invocation.getArgument(1);

                                // First request succeeds, second fails
                                if (req.getRequestURI().contains("/1")) {
                                    response.setStatus(200);
                                    response.setContentType("application/json");
                                    response.getWriter().write("{\"id\":1}");
                                } else {
                                    response.setStatus(404);
                                    response.setContentType("application/json");
                                    response.getWriter().write("{\"message\":\"Not found\"}");
                                }
                                return null;
                            })
                    .when(dispatcherServlet)
                    .service(any(HttpServletRequest.class), any(HttpServletResponse.class));

            // When
            BatchResponse response = batchService.processBatch(request, createMockRequest());

            // Then
            assertThat(response.results()).hasSize(2);
            assertThat(response.results().get(0).isSuccessful()).isTrue();
            assertThat(response.results().get(1).isSuccessful()).isFalse();
            assertThat(response.summary().total()).isEqualTo(2);
            assertThat(response.summary().failed()).isEqualTo(1);
            verify(dispatcherServlet, times(2)).service(any(), any());
        }

        @Test
        @DisplayName("should continue on error when stopOnError is false")
        void shouldContinueOnErrorWhenFlagIsFalse() throws Exception {
            // Given
            List<BatchOperation> operations =
                    List.of(
                            new BatchOperation("op1", "GET", "/api/users/1", null, null),
                            new BatchOperation("op2", "GET", "/api/error", null, null),
                            new BatchOperation("op3", "GET", "/api/users/3", null, null));
            BatchRequest request = new BatchRequest(operations, false);

            doAnswer(
                            invocation -> {
                                HttpServletRequest req = invocation.getArgument(0);
                                HttpServletResponse response = invocation.getArgument(1);

                                if (req.getRequestURI().contains("/error")) {
                                    response.setStatus(500);
                                    response.setContentType("application/json");
                                    response.getWriter().write("{\"error\":\"Internal error\"}");
                                } else {
                                    response.setStatus(200);
                                    response.setContentType("application/json");
                                    response.getWriter().write("{\"status\":\"ok\"}");
                                }
                                return null;
                            })
                    .when(dispatcherServlet)
                    .service(any(HttpServletRequest.class), any(HttpServletResponse.class));

            // When
            BatchResponse response = batchService.processBatch(request, createMockRequest());

            // Then
            assertThat(response.results()).hasSize(3);
            assertThat(response.results().get(0).isSuccessful()).isTrue();
            assertThat(response.results().get(1).isSuccessful()).isFalse();
            assertThat(response.results().get(2).isSuccessful()).isTrue();
            assertThat(response.summary().successful()).isEqualTo(2);
            assertThat(response.summary().failed()).isEqualTo(1);
        }

        @Test
        @DisplayName("should handle POST operation with body")
        void shouldHandlePostOperationWithBody() throws Exception {
            // Given
            ObjectNode body = objectMapper.createObjectNode();
            body.put("name", "John");
            body.put("email", "john@example.com");

            List<BatchOperation> operations =
                    List.of(new BatchOperation("op1", "POST", "/api/users", null, body));
            BatchRequest request = new BatchRequest(operations, false);

            doAnswer(
                            invocation -> {
                                HttpServletResponse response = invocation.getArgument(1);
                                response.setStatus(201);
                                response.setContentType("application/json");
                                response.setHeader("Location", "/api/users/1");
                                response.getWriter().write("{\"id\":1,\"name\":\"John\"}");
                                return null;
                            })
                    .when(dispatcherServlet)
                    .service(any(HttpServletRequest.class), any(HttpServletResponse.class));

            // When
            BatchResponse response = batchService.processBatch(request, createMockRequest());

            // Then
            assertThat(response.results()).hasSize(1);
            OperationResult result = response.results().getFirst();
            assertThat(result.status()).isEqualTo(201);
            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.headers()).containsEntry("location", "/api/users/1");
        }

        @Test
        @DisplayName("should handle exception during operation execution")
        void shouldHandleExceptionDuringExecution() throws Exception {
            // Given
            List<BatchOperation> operations =
                    List.of(new BatchOperation("op1", "GET", "/api/users/1", null, null));
            BatchRequest request = new BatchRequest(operations, false);

            doAnswer(
                            invocation -> {
                                throw new RuntimeException("Simulated error");
                            })
                    .when(dispatcherServlet)
                    .service(any(HttpServletRequest.class), any(HttpServletResponse.class));

            // When
            BatchResponse response = batchService.processBatch(request, createMockRequest());

            // Then
            assertThat(response.results()).hasSize(1);
            OperationResult result = response.results().getFirst();
            assertThat(result.status()).isEqualTo(500);
            assertThat(result.isSuccessful()).isFalse();
            assertThat(result.error()).isNotNull();
        }

        @Test
        @DisplayName("should track execution time")
        void shouldTrackExecutionTime() throws Exception {
            // Given
            List<BatchOperation> operations =
                    List.of(new BatchOperation("op1", "GET", "/api/users", null, null));
            BatchRequest request = new BatchRequest(operations, false);

            doAnswer(
                            invocation -> {
                                HttpServletResponse response = invocation.getArgument(1);
                                response.setStatus(200);
                                response.setContentType("application/json");
                                response.getWriter().write("{}");
                                return null;
                            })
                    .when(dispatcherServlet)
                    .service(any(HttpServletRequest.class), any(HttpServletResponse.class));

            // When
            BatchResponse response = batchService.processBatch(request, createMockRequest());

            // Then
            assertThat(response.executionTimeMs()).isGreaterThanOrEqualTo(0);
            assertThat(response.timestamp()).isNotNull();
        }

        @Test
        @DisplayName("should handle empty body response")
        void shouldHandleEmptyBodyResponse() throws Exception {
            // Given
            List<BatchOperation> operations =
                    List.of(new BatchOperation("op1", "DELETE", "/api/users/1", null, null));
            BatchRequest request = new BatchRequest(operations, false);

            doAnswer(
                            invocation -> {
                                HttpServletResponse response = invocation.getArgument(1);
                                response.setStatus(204);
                                return null;
                            })
                    .when(dispatcherServlet)
                    .service(any(HttpServletRequest.class), any(HttpServletResponse.class));

            // When
            BatchResponse response = batchService.processBatch(request, createMockRequest());

            // Then
            assertThat(response.results()).hasSize(1);
            OperationResult result = response.results().getFirst();
            assertThat(result.status()).isEqualTo(204);
            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.body()).isNull();
        }
    }

    @Nested
    @DisplayName("processBatchParallel - parallel execution")
    class ProcessBatchParallelTests {

        @Test
        @DisplayName("should process operations in parallel")
        void shouldProcessOperationsInParallel() throws Exception {
            // Given
            List<BatchOperation> operations =
                    List.of(
                            new BatchOperation("op1", "GET", "/api/users/1", null, null),
                            new BatchOperation("op2", "GET", "/api/users/2", null, null),
                            new BatchOperation("op3", "GET", "/api/users/3", null, null));
            BatchRequest request = new BatchRequest(operations, false);

            doAnswer(
                            invocation -> {
                                HttpServletResponse response = invocation.getArgument(1);
                                response.setStatus(200);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"status\":\"ok\"}");
                                return null;
                            })
                    .when(dispatcherServlet)
                    .service(any(HttpServletRequest.class), any(HttpServletResponse.class));

            // When
            BatchResponse response =
                    batchService.processBatchParallel(request, createMockRequest());

            // Then
            assertThat(response.results()).hasSize(3);
            assertThat(response.summary().total()).isEqualTo(3);
            assertThat(response.summary().successful()).isEqualTo(3);
        }

        @Test
        @DisplayName("should ignore stopOnError flag in parallel mode")
        void shouldIgnoreStopOnErrorInParallelMode() throws Exception {
            // Given
            List<BatchOperation> operations =
                    List.of(
                            new BatchOperation("op1", "GET", "/api/users/1", null, null),
                            new BatchOperation("op2", "GET", "/api/error", null, null),
                            new BatchOperation("op3", "GET", "/api/users/3", null, null));
            BatchRequest request = new BatchRequest(operations, true); // stopOnError = true

            doAnswer(
                            invocation -> {
                                HttpServletRequest req = invocation.getArgument(0);
                                HttpServletResponse response = invocation.getArgument(1);

                                if (req.getRequestURI().contains("/error")) {
                                    response.setStatus(500);
                                    response.setContentType("application/json");
                                    response.getWriter().write("{\"error\":\"Error\"}");
                                } else {
                                    response.setStatus(200);
                                    response.setContentType("application/json");
                                    response.getWriter().write("{\"status\":\"ok\"}");
                                }
                                return null;
                            })
                    .when(dispatcherServlet)
                    .service(any(HttpServletRequest.class), any(HttpServletResponse.class));

            // When
            BatchResponse response =
                    batchService.processBatchParallel(request, createMockRequest());

            // Then - all operations should be processed despite stopOnError=true
            assertThat(response.results()).hasSize(3);
            assertThat(response.summary().successful()).isEqualTo(2);
            assertThat(response.summary().failed()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Error message extraction")
    class ErrorMessageExtractionTests {

        @Test
        @DisplayName("should extract message field from error response")
        void shouldExtractMessageField() throws Exception {
            // Given
            List<BatchOperation> operations =
                    List.of(new BatchOperation("op1", "GET", "/api/users/999", null, null));
            BatchRequest request = new BatchRequest(operations, false);

            doAnswer(
                            invocation -> {
                                HttpServletResponse response = invocation.getArgument(1);
                                response.setStatus(404);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"message\":\"User not found\"}");
                                return null;
                            })
                    .when(dispatcherServlet)
                    .service(any(HttpServletRequest.class), any(HttpServletResponse.class));

            // When
            BatchResponse response = batchService.processBatch(request, createMockRequest());

            // Then
            assertThat(response.results().getFirst().error().message()).isEqualTo("User not found");
        }

        @Test
        @DisplayName("should extract detail field from error response")
        void shouldExtractDetailField() throws Exception {
            // Given
            List<BatchOperation> operations =
                    List.of(new BatchOperation("op1", "POST", "/api/users", null, null));
            BatchRequest request = new BatchRequest(operations, false);

            doAnswer(
                            invocation -> {
                                HttpServletResponse response = invocation.getArgument(1);
                                response.setStatus(400);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"detail\":\"Validation failed\"}");
                                return null;
                            })
                    .when(dispatcherServlet)
                    .service(any(HttpServletRequest.class), any(HttpServletResponse.class));

            // When
            BatchResponse response = batchService.processBatch(request, createMockRequest());

            // Then
            assertThat(response.results().getFirst().error().message())
                    .isEqualTo("Validation failed");
        }

        @Test
        @DisplayName("should extract error field from error response")
        void shouldExtractErrorField() throws Exception {
            // Given
            List<BatchOperation> operations =
                    List.of(new BatchOperation("op1", "GET", "/api/users", null, null));
            BatchRequest request = new BatchRequest(operations, false);

            doAnswer(
                            invocation -> {
                                HttpServletResponse response = invocation.getArgument(1);
                                response.setStatus(401);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"error\":\"Unauthorized\"}");
                                return null;
                            })
                    .when(dispatcherServlet)
                    .service(any(HttpServletRequest.class), any(HttpServletResponse.class));

            // When
            BatchResponse response = batchService.processBatch(request, createMockRequest());

            // Then
            assertThat(response.results().getFirst().error().message()).isEqualTo("Unauthorized");
        }

        @Test
        @DisplayName("should extract title field from error response")
        void shouldExtractTitleField() throws Exception {
            // Given
            List<BatchOperation> operations =
                    List.of(new BatchOperation("op1", "GET", "/api/users", null, null));
            BatchRequest request = new BatchRequest(operations, false);

            doAnswer(
                            invocation -> {
                                HttpServletResponse response = invocation.getArgument(1);
                                response.setStatus(403);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"title\":\"Forbidden\"}");
                                return null;
                            })
                    .when(dispatcherServlet)
                    .service(any(HttpServletRequest.class), any(HttpServletResponse.class));

            // When
            BatchResponse response = batchService.processBatch(request, createMockRequest());

            // Then
            assertThat(response.results().getFirst().error().message()).isEqualTo("Forbidden");
        }

        @Test
        @DisplayName("should use default message when no error fields present")
        void shouldUseDefaultMessageWhenNoErrorFields() throws Exception {
            // Given
            List<BatchOperation> operations =
                    List.of(new BatchOperation("op1", "GET", "/api/users", null, null));
            BatchRequest request = new BatchRequest(operations, false);

            doAnswer(
                            invocation -> {
                                HttpServletResponse response = invocation.getArgument(1);
                                response.setStatus(500);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"code\":\"ERR001\"}");
                                return null;
                            })
                    .when(dispatcherServlet)
                    .service(any(HttpServletRequest.class), any(HttpServletResponse.class));

            // When
            BatchResponse response = batchService.processBatch(request, createMockRequest());

            // Then
            assertThat(response.results().getFirst().error().message()).isEqualTo("Request failed");
        }
    }

    @Nested
    @DisplayName("Header handling")
    class HeaderHandlingTests {

        @Test
        @DisplayName("should include Location header in response")
        void shouldIncludeLocationHeader() throws Exception {
            // Given
            List<BatchOperation> operations =
                    List.of(new BatchOperation("op1", "POST", "/api/users", null, null));
            BatchRequest request = new BatchRequest(operations, false);

            doAnswer(
                            invocation -> {
                                HttpServletResponse response = invocation.getArgument(1);
                                response.setStatus(201);
                                response.setHeader("Location", "/api/users/123");
                                response.setContentType("application/json");
                                response.getWriter().write("{\"id\":123}");
                                return null;
                            })
                    .when(dispatcherServlet)
                    .service(any(HttpServletRequest.class), any(HttpServletResponse.class));

            // When
            BatchResponse response = batchService.processBatch(request, createMockRequest());

            // Then
            assertThat(response.results().getFirst().headers())
                    .containsEntry("location", "/api/users/123");
        }

        @Test
        @DisplayName("should include ETag header in response")
        void shouldIncludeETagHeader() throws Exception {
            // Given
            List<BatchOperation> operations =
                    List.of(new BatchOperation("op1", "GET", "/api/users/1", null, null));
            BatchRequest request = new BatchRequest(operations, false);

            doAnswer(
                            invocation -> {
                                HttpServletResponse response = invocation.getArgument(1);
                                response.setStatus(200);
                                response.setHeader("ETag", "\"abc123\"");
                                response.setContentType("application/json");
                                response.getWriter().write("{\"id\":1}");
                                return null;
                            })
                    .when(dispatcherServlet)
                    .service(any(HttpServletRequest.class), any(HttpServletResponse.class));

            // When
            BatchResponse response = batchService.processBatch(request, createMockRequest());

            // Then
            assertThat(response.results().getFirst().headers()).containsEntry("etag", "\"abc123\"");
        }

        @Test
        @DisplayName("should not include non-relevant headers")
        void shouldNotIncludeNonRelevantHeaders() throws Exception {
            // Given
            List<BatchOperation> operations =
                    List.of(new BatchOperation("op1", "GET", "/api/users/1", null, null));
            BatchRequest request = new BatchRequest(operations, false);

            doAnswer(
                            invocation -> {
                                HttpServletResponse response = invocation.getArgument(1);
                                response.setStatus(200);
                                response.setHeader("X-Custom-Header", "custom-value");
                                response.setHeader("Content-Type", "application/json");
                                response.getWriter().write("{\"id\":1}");
                                return null;
                            })
                    .when(dispatcherServlet)
                    .service(any(HttpServletRequest.class), any(HttpServletResponse.class));

            // When
            BatchResponse response = batchService.processBatch(request, createMockRequest());

            // Then - headers should be null since no relevant headers were set
            Map<String, String> headers = response.results().getFirst().headers();
            if (headers != null) {
                assertThat(headers)
                        .doesNotContainKey("X-Custom-Header")
                        .doesNotContainKey("x-custom-header")
                        .doesNotContainKey("Content-Type")
                        .doesNotContainKey("content-type");
            }
        }
    }
}
