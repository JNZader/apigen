package com.jnzader.apigen.core.infrastructure.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jnzader.apigen.core.infrastructure.batch.BatchResponse.BatchSummary;
import com.jnzader.apigen.core.infrastructure.batch.BatchResponse.OperationResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BatchResponse")
class BatchResponseTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("OperationResult")
    class OperationResultTests {

        @Test
        @DisplayName("should create successful result")
        void createSuccessResult() {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("id", 1);
            body.put("name", "John");

            OperationResult result = OperationResult.success("op1", 200, body);

            assertThat(result.id()).isEqualTo("op1");
            assertThat(result.status()).isEqualTo(200);
            assertThat(result.body()).isEqualTo(body);
            assertThat(result.headers()).isNull();
            assertThat(result.error()).isNull();
            assertThat(result.isSuccessful()).isTrue();
        }

        @Test
        @DisplayName("should create successful result with headers")
        void createSuccessResultWithHeaders() {
            ObjectNode body = objectMapper.createObjectNode();
            Map<String, String> headers = Map.of("Location", "/api/users/1");

            OperationResult result = OperationResult.success("op1", 201, headers, body);

            assertThat(result.id()).isEqualTo("op1");
            assertThat(result.status()).isEqualTo(201);
            assertThat(result.headers()).isEqualTo(headers);
            assertThat(result.body()).isEqualTo(body);
            assertThat(result.isSuccessful()).isTrue();
        }

        @Test
        @DisplayName("should create failure result")
        void createFailureResult() {
            OperationResult result = OperationResult.failure("op1", 404, "Resource not found");

            assertThat(result.id()).isEqualTo("op1");
            assertThat(result.status()).isEqualTo(404);
            assertThat(result.body()).isNull();
            assertThat(result.error()).isNotNull();
            assertThat(result.error().message()).isEqualTo("Resource not found");
            assertThat(result.error().detail()).isNull();
            assertThat(result.isSuccessful()).isFalse();
        }

        @Test
        @DisplayName("should create failure result with detail")
        void createFailureResultWithDetail() {
            OperationResult result =
                    OperationResult.failure("op1", 400, "Validation failed", "Name is required");

            assertThat(result.status()).isEqualTo(400);
            assertThat(result.error().message()).isEqualTo("Validation failed");
            assertThat(result.error().detail()).isEqualTo("Name is required");
        }

        @Test
        @DisplayName("should identify successful status codes")
        void identifySuccessfulStatusCodes() {
            assertThat(OperationResult.success("op", 200, null).isSuccessful()).isTrue();
            assertThat(OperationResult.success("op", 201, null).isSuccessful()).isTrue();
            assertThat(OperationResult.success("op", 204, null).isSuccessful()).isTrue();
            assertThat(OperationResult.success("op", 299, null).isSuccessful()).isTrue();
        }

        @Test
        @DisplayName("should identify failed status codes")
        void identifyFailedStatusCodes() {
            assertThat(OperationResult.failure("op", 400, "error").isSuccessful()).isFalse();
            assertThat(OperationResult.failure("op", 404, "error").isSuccessful()).isFalse();
            assertThat(OperationResult.failure("op", 500, "error").isSuccessful()).isFalse();
        }

        @Test
        @DisplayName("should identify 199 as unsuccessful (boundary before 2xx)")
        void shouldIdentify199AsUnsuccessful() {
            // Given - status code 199 is just below the 2xx range
            OperationResult result = new OperationResult("op", 199, null, null, null);

            // Then
            assertThat(result.isSuccessful()).isFalse();
        }

        @Test
        @DisplayName("should identify 300 as unsuccessful (boundary after 2xx)")
        void shouldIdentify300AsUnsuccessful() {
            // Given - status code 300 is just above the 2xx range
            OperationResult result = new OperationResult("op", 300, null, null, null);

            // Then
            assertThat(result.isSuccessful()).isFalse();
        }

        @Test
        @DisplayName("should identify exact boundaries 200 and 299 as successful")
        void shouldIdentifyExactBoundariesAsSuccessful() {
            // Given - exact boundary values
            OperationResult at200 = new OperationResult("op1", 200, null, null, null);
            OperationResult at299 = new OperationResult("op2", 299, null, null, null);

            // Then
            assertThat(at200.isSuccessful()).isTrue();
            assertThat(at299.isSuccessful()).isTrue();
        }
    }

    @Nested
    @DisplayName("BatchSummary")
    class BatchSummaryTests {

        @Test
        @DisplayName("should create summary from results")
        void createSummaryFromResults() {
            List<OperationResult> results =
                    List.of(
                            OperationResult.success("op1", 200, null),
                            OperationResult.success("op2", 201, null),
                            OperationResult.failure("op3", 404, "Not found"),
                            OperationResult.success("op4", 204, null));

            BatchSummary summary = BatchSummary.fromResults(results);

            assertThat(summary.total()).isEqualTo(4);
            assertThat(summary.successful()).isEqualTo(3);
            assertThat(summary.failed()).isEqualTo(1);
        }

        @Test
        @DisplayName("should handle all successful results")
        void handleAllSuccessfulResults() {
            List<OperationResult> results =
                    List.of(
                            OperationResult.success("op1", 200, null),
                            OperationResult.success("op2", 201, null));

            BatchSummary summary = BatchSummary.fromResults(results);

            assertThat(summary.total()).isEqualTo(2);
            assertThat(summary.successful()).isEqualTo(2);
            assertThat(summary.failed()).isZero();
        }

        @Test
        @DisplayName("should handle all failed results")
        void handleAllFailedResults() {
            List<OperationResult> results =
                    List.of(
                            OperationResult.failure("op1", 400, "error"),
                            OperationResult.failure("op2", 500, "error"));

            BatchSummary summary = BatchSummary.fromResults(results);

            assertThat(summary.total()).isEqualTo(2);
            assertThat(summary.successful()).isZero();
            assertThat(summary.failed()).isEqualTo(2);
        }

        @Test
        @DisplayName("should handle empty results")
        void handleEmptyResults() {
            BatchSummary summary = BatchSummary.fromResults(List.of());

            assertThat(summary.total()).isZero();
            assertThat(summary.successful()).isZero();
            assertThat(summary.failed()).isZero();
        }
    }

    @Nested
    @DisplayName("BatchResponse")
    class BatchResponseTests {

        @Test
        @DisplayName("should create batch response with timestamp")
        void createBatchResponseWithTimestamp() {
            List<OperationResult> results = List.of(OperationResult.success("op1", 200, null));
            BatchSummary summary = BatchSummary.fromResults(results);

            BatchResponse response = new BatchResponse(results, summary, 100);

            assertThat(response.results()).hasSize(1);
            assertThat(response.summary()).isEqualTo(summary);
            assertThat(response.executionTimeMs()).isEqualTo(100);
            assertThat(response.timestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should build batch response with builder")
        void buildBatchResponseWithBuilder() {
            BatchResponse.Builder builder = BatchResponse.builder();
            builder.addResult(OperationResult.success("op1", 200, null));
            builder.addResult(OperationResult.success("op2", 201, null));
            builder.addResult(OperationResult.failure("op3", 400, "error"));

            BatchResponse response = builder.build();

            assertThat(response.results()).hasSize(3);
            assertThat(response.summary().total()).isEqualTo(3);
            assertThat(response.summary().successful()).isEqualTo(2);
            assertThat(response.summary().failed()).isEqualTo(1);
            assertThat(response.executionTimeMs()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("should build empty batch response")
        void buildEmptyBatchResponse() {
            BatchResponse response = BatchResponse.builder().build();

            assertThat(response.results()).isEmpty();
            assertThat(response.summary().total()).isZero();
        }
    }
}
