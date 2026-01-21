package com.jnzader.apigen.core.infrastructure.batch;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/**
 * Request body for batch API operations.
 *
 * <p>Allows multiple API operations to be executed in a single HTTP request.
 *
 * <p>Example:
 *
 * <pre>{@code
 * {
 *   "operations": [
 *     { "id": "op1", "method": "GET", "path": "/api/users/1" },
 *     { "id": "op2", "method": "POST", "path": "/api/users", "body": { "name": "John" } },
 *     { "id": "op3", "method": "DELETE", "path": "/api/users/2" }
 *   ],
 *   "stopOnError": false
 * }
 * }</pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BatchRequest(
        @NotEmpty(message = "Operations list cannot be empty")
                @Size(max = 100, message = "Maximum 100 operations per batch request")
                @Valid
                List<BatchOperation> operations,
        boolean stopOnError) {

    /** Default constructor with default values. */
    public BatchRequest {
        if (operations == null) {
            operations = List.of();
        }
    }

    /** Creates a batch request with only operations. */
    public BatchRequest(List<BatchOperation> operations) {
        this(operations, false);
    }

    /**
     * Single operation within a batch request.
     *
     * @param id Optional unique identifier for this operation (for correlation)
     * @param method HTTP method (GET, POST, PUT, PATCH, DELETE)
     * @param path Request path (relative to API base)
     * @param headers Optional headers for this specific operation
     * @param body Optional request body (for POST, PUT, PATCH)
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record BatchOperation(
            String id,
            @NotNull(message = "Method is required")
                    @Pattern(
                            regexp = "GET|POST|PUT|PATCH|DELETE",
                            message = "Method must be GET, POST, PUT, PATCH, or DELETE")
                    String method,
            @NotNull(message = "Path is required")
                    @Pattern(regexp = "^/.*", message = "Path must start with /")
                    String path,
            Map<String, String> headers,
            JsonNode body) {

        /** Creates an operation with only method and path. */
        public BatchOperation(String method, String path) {
            this(null, method, path, null, null);
        }

        /** Creates an operation with method, path, and body. */
        public BatchOperation(String method, String path, JsonNode body) {
            this(null, method, path, null, body);
        }
    }
}
