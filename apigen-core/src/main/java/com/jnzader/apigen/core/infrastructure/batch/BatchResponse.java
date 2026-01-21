package com.jnzader.apigen.core.infrastructure.batch;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Response for batch API operations.
 *
 * <p>Contains the results of each operation in the batch request.
 *
 * <p>Example:
 *
 * <pre>{@code
 * {
 *   "results": [
 *     { "id": "op1", "status": 200, "body": { "id": 1, "name": "Jane" } },
 *     { "id": "op2", "status": 201, "body": { "id": 3, "name": "John" } },
 *     { "id": "op3", "status": 204, "body": null }
 *   ],
 *   "summary": {
 *     "total": 3,
 *     "successful": 3,
 *     "failed": 0
 *   },
 *   "executionTimeMs": 150
 * }
 * }</pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BatchResponse(
        List<OperationResult> results,
        BatchSummary summary,
        long executionTimeMs,
        Instant timestamp) {

    /** Creates a batch response with current timestamp. */
    public BatchResponse(
            List<OperationResult> results, BatchSummary summary, long executionTimeMs) {
        this(results, summary, executionTimeMs, Instant.now());
    }

    /**
     * Result of a single operation within the batch.
     *
     * @param id The operation ID (if provided in request)
     * @param status HTTP status code of the operation
     * @param headers Response headers (if any)
     * @param body Response body (may be null for DELETE operations)
     * @param error Error details if the operation failed
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record OperationResult(
            String id, int status, Map<String, String> headers, JsonNode body, ErrorDetail error) {

        /** Creates a successful result. */
        public static OperationResult success(String id, int status, JsonNode body) {
            return new OperationResult(id, status, null, body, null);
        }

        /** Creates a successful result with headers. */
        public static OperationResult success(
                String id, int status, Map<String, String> headers, JsonNode body) {
            return new OperationResult(id, status, headers, body, null);
        }

        /** Creates a failed result. */
        public static OperationResult failure(String id, int status, String error) {
            return new OperationResult(id, status, null, null, new ErrorDetail(error, null));
        }

        /** Creates a failed result with detailed error. */
        public static OperationResult failure(String id, int status, String error, String detail) {
            return new OperationResult(id, status, null, null, new ErrorDetail(error, detail));
        }

        /** Checks if this operation was successful (2xx status). */
        public boolean isSuccessful() {
            return status >= 200 && status < 300;
        }
    }

    /**
     * Error details for a failed operation.
     *
     * @param message Error message
     * @param detail Additional error details
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorDetail(String message, String detail) {}

    /**
     * Summary of batch execution.
     *
     * @param total Total number of operations
     * @param successful Number of successful operations
     * @param failed Number of failed operations
     */
    public record BatchSummary(int total, int successful, int failed) {

        /** Creates a summary from a list of results. */
        public static BatchSummary fromResults(List<OperationResult> results) {
            int successful = (int) results.stream().filter(OperationResult::isSuccessful).count();
            return new BatchSummary(results.size(), successful, results.size() - successful);
        }
    }

    /** Builder for creating BatchResponse instances. */
    public static class Builder {
        private final List<OperationResult> results = new java.util.ArrayList<>();
        private long startTime = System.currentTimeMillis();

        public Builder addResult(OperationResult result) {
            results.add(result);
            return this;
        }

        public BatchResponse build() {
            long executionTime = System.currentTimeMillis() - startTime;
            BatchSummary summary = BatchSummary.fromResults(results);
            return new BatchResponse(results, summary, executionTime);
        }
    }

    /** Creates a new builder. */
    public static Builder builder() {
        return new Builder();
    }
}
