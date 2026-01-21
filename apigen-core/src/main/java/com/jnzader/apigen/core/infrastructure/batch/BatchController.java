package com.jnzader.apigen.core.infrastructure.batch;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for batch API operations.
 *
 * <p>Allows executing multiple API operations in a single HTTP request, reducing round-trip latency
 * and improving performance for bulk operations.
 *
 * <p>Example request:
 *
 * <pre>{@code
 * POST /api/batch
 * Content-Type: application/json
 *
 * {
 *   "operations": [
 *     { "id": "get-user", "method": "GET", "path": "/api/users/1" },
 *     { "id": "create-user", "method": "POST", "path": "/api/users", "body": { "name": "John" } },
 *     { "id": "delete-user", "method": "DELETE", "path": "/api/users/2" }
 *   ],
 *   "stopOnError": false
 * }
 * }</pre>
 *
 * <p>Example response:
 *
 * <pre>{@code
 * {
 *   "results": [
 *     { "id": "get-user", "status": 200, "body": { "id": 1, "name": "Jane" } },
 *     { "id": "create-user", "status": 201, "body": { "id": 3, "name": "John" },
 *       "headers": { "Location": "/api/users/3" } },
 *     { "id": "delete-user", "status": 204, "body": null }
 *   ],
 *   "summary": { "total": 3, "successful": 3, "failed": 0 },
 *   "executionTimeMs": 150,
 *   "timestamp": "2024-01-15T10:30:00Z"
 * }
 * }</pre>
 */
@RestController
@RequestMapping("${apigen.batch.path:/api/batch}")
@Tag(name = "Batch Operations", description = "Execute multiple API operations in a single request")
public class BatchController {

    private static final Logger log = LoggerFactory.getLogger(BatchController.class);

    private final BatchService batchService;

    public BatchController(BatchService batchService) {
        this.batchService = batchService;
    }

    /**
     * Executes multiple API operations in a single request.
     *
     * @param request the batch request containing operations to execute
     * @param parallel if true, operations are executed in parallel (default: false)
     * @param httpRequest the original HTTP request for context
     * @return the batch response with results for each operation
     */
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Execute batch operations",
            description =
                    "Executes multiple API operations in a single HTTP request. Operations are"
                            + " executed sequentially by default, or in parallel if the 'parallel'"
                            + " parameter is set to true. When stopOnError is true, execution stops"
                            + " after the first failed operation (only applies to sequential"
                            + " execution).")
    @ApiResponse(
            responseCode = "200",
            description = "Batch executed successfully",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = BatchResponse.class)))
    @ApiResponse(
            responseCode = "400",
            description = "Invalid batch request",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @ApiResponse(
            responseCode = "413",
            description = "Too many operations in batch (max: 100)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    public ResponseEntity<BatchResponse> executeBatch(
            @Valid @RequestBody BatchRequest request,
            @RequestParam(name = "parallel", defaultValue = "false") boolean parallel,
            HttpServletRequest httpRequest) {

        log.info(
                "Received batch request with {} operations (parallel={}, stopOnError={})",
                request.operations().size(),
                parallel,
                request.stopOnError());

        BatchResponse response;
        if (parallel) {
            response = batchService.processBatchParallel(request, httpRequest);
        } else {
            response = batchService.processBatch(request, httpRequest);
        }

        return ResponseEntity.ok(response);
    }
}
