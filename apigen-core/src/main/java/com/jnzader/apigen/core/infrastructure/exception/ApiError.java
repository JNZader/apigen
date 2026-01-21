package com.jnzader.apigen.core.infrastructure.exception;

import com.jnzader.apigen.core.domain.exception.DuplicateResourceException;
import com.jnzader.apigen.core.domain.exception.IdMismatchException;
import com.jnzader.apigen.core.domain.exception.OperationFailedException;
import com.jnzader.apigen.core.domain.exception.PreconditionFailedException;
import com.jnzader.apigen.core.domain.exception.ResourceNotFoundException;
import com.jnzader.apigen.core.domain.exception.UnauthorizedActionException;
import com.jnzader.apigen.core.domain.exception.ValidationException;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Sealed interface representing typed API errors.
 *
 * <p>Provides type-safe error handling with exhaustive pattern matching. Each variant corresponds
 * to a specific HTTP error type.
 *
 * <p>Usage with Result pattern:
 *
 * <pre>
 * result.fold(
 *     success -> ResponseEntity.ok(success),
 *     error -> ApiError.from(error).toResponse()
 * );
 * </pre>
 *
 * <p>Usage with pattern matching:
 *
 * <pre>
 * switch (apiError) {
 *     case ApiError.NotFound nf -> handleNotFound(nf);
 *     case ApiError.Validation v -> handleValidation(v);
 *     // ... exhaustive thanks to sealed
 * }
 * </pre>
 */
public sealed interface ApiError {

    /** Gets the error message. All implementations must provide a message. */
    String message();

    /**
     * Resource not found (HTTP 404).
     *
     * @param resourceType Type of resource (e.g., "User", "Product")
     * @param resourceId Identifier of the searched resource
     * @param message Descriptive error message
     */
    record NotFound(String resourceType, Object resourceId, String message) implements ApiError {
        public NotFound(String resourceType, Object resourceId) {
            this(
                    resourceType,
                    resourceId,
                    String.format("%s with ID '%s' not found", resourceType, resourceId));
        }

        public NotFound(String message) {
            this("Resource", null, message);
        }
    }

    /**
     * Validation error (HTTP 400).
     *
     * @param message General validation message
     * @param fieldErrors Map of field -> specific error message
     */
    record Validation(String message, Map<String, String> fieldErrors) implements ApiError {
        public Validation(String message) {
            this(message, Map.of());
        }

        public Validation(Map<String, String> fieldErrors) {
            this("Validation error", fieldErrors);
        }
    }

    /**
     * Resource conflict (HTTP 409). Used for duplicates or unique constraint violations.
     *
     * @param message Conflict description
     * @param conflictType Conflict type (e.g., "DUPLICATE_KEY", "UNIQUE_CONSTRAINT")
     */
    record Conflict(String message, String conflictType) implements ApiError {
        public Conflict(String message) {
            this(message, "CONFLICT");
        }
    }

    /**
     * Unauthorized action (HTTP 403).
     *
     * @param message Restriction description
     * @param requiredRole Required role for the action (optional)
     * @param currentUser Current user (optional)
     */
    record Forbidden(String message, String requiredRole, String currentUser) implements ApiError {
        public Forbidden(String message) {
            this(message, null, null);
        }
    }

    /**
     * Precondition failed (HTTP 412). Used for ETag conflicts in optimistic concurrency.
     *
     * @param message Failure description
     * @param expectedEtag Expected/current ETag of the resource
     * @param providedEtag ETag provided in the request
     */
    record PreconditionFailed(String message, String expectedEtag, String providedEtag)
            implements ApiError {
        public PreconditionFailed(String message) {
            this(message, null, null);
        }
    }

    /**
     * IDs do not match (HTTP 400). When the ID in the path differs from the ID in the body.
     *
     * @param pathId ID in the URL
     * @param bodyId ID in the request body
     * @param message Descriptive message
     */
    record IdMismatch(Object pathId, Object bodyId, String message) implements ApiError {
        public IdMismatch(Object pathId, Object bodyId) {
            this(
                    pathId,
                    bodyId,
                    String.format(
                            "The path ID (%s) does not match the body ID (%s)", pathId, bodyId));
        }
    }

    /**
     * Internal server error (HTTP 500).
     *
     * @param message Error message
     * @param cause Original cause (for logging, not exposed to client)
     */
    record Internal(String message, Throwable cause) implements ApiError {
        public Internal(String message) {
            this(message, null);
        }

        public Internal(Throwable cause) {
            this("Internal server error", cause);
        }
    }

    // ==================== Factory Methods ====================

    /**
     * Converts an Exception to its corresponding ApiError. Useful for transforming Result pattern
     * errors to typed errors.
     *
     * @param exception The exception to convert
     * @return The corresponding ApiError
     */
    static ApiError from(Exception exception) {
        return switch (exception) {
            case ResourceNotFoundException ex -> new NotFound(ex.getMessage());

            case ValidationException ex -> new Validation(ex.getMessage());

            case DuplicateResourceException ex ->
                    new Conflict(ex.getMessage(), "DUPLICATE_RESOURCE");

            case UnauthorizedActionException ex -> new Forbidden(ex.getMessage());

            case PreconditionFailedException ex ->
                    new PreconditionFailed(
                            ex.getMessage(), ex.getCurrentEtag(), ex.getProvidedEtag());

            case IdMismatchException ex ->
                    new IdMismatch(ex.getPathId(), ex.getBodyId(), ex.getMessage());

            case OperationFailedException ex -> new Internal(ex.getMessage(), ex.getCause());

            case IllegalArgumentException ex -> new Validation(ex.getMessage());

            case null, default ->
                    new Internal(
                            exception != null ? exception.getMessage() : "Unknown error",
                            exception);
        };
    }

    /**
     * Gets the HTTP status code corresponding to this error.
     *
     * @return The appropriate HttpStatus
     */
    default HttpStatus httpStatus() {
        return switch (this) {
            case NotFound _ -> HttpStatus.NOT_FOUND;
            case Validation _ -> HttpStatus.BAD_REQUEST;
            case Conflict _ -> HttpStatus.CONFLICT;
            case Forbidden _ -> HttpStatus.FORBIDDEN;
            case PreconditionFailed _ -> HttpStatus.PRECONDITION_FAILED;
            case IdMismatch _ -> HttpStatus.BAD_REQUEST;
            case Internal _ -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    /**
     * Gets the numeric HTTP code.
     *
     * @return The status code (e.g., 404, 400, 500)
     */
    default int statusCode() {
        return httpStatus().value();
    }

    /**
     * Gets the standard error title for RFC 7807.
     *
     * @return The error title
     */
    default String title() {
        return switch (this) {
            case NotFound _ -> "Resource not found";
            case Validation _ -> "Validation error";
            case Conflict _ -> "Resource conflict";
            case Forbidden _ -> "Action not allowed";
            case PreconditionFailed _ -> "Precondition failed";
            case IdMismatch _ -> "IDs do not match";
            case Internal _ -> "Internal server error";
        };
    }

    /**
     * Gets the detailed error message.
     *
     * @return The detail message
     */
    default String detail() {
        return message();
    }

    /**
     * Gets the type URI for RFC 7807.
     *
     * @return URI that identifies the problem type
     */
    default String typeUri() {
        return switch (this) {
            case NotFound _ -> "urn:problem-type:resource-not-found";
            case Validation _ -> "urn:problem-type:validation-error";
            case Conflict _ -> "urn:problem-type:resource-conflict";
            case Forbidden _ -> "urn:problem-type:forbidden";
            case PreconditionFailed _ -> "urn:problem-type:precondition-failed";
            case IdMismatch _ -> "urn:problem-type:id-mismatch";
            case Internal _ -> "urn:problem-type:internal-error";
        };
    }

    /**
     * Converts to ProblemDetail for RFC 7807 responses.
     *
     * @param instance Instance URI (typically the request URI)
     * @return ProblemDetail ready to serialize
     */
    default ProblemDetail toProblemDetail(String instance) {
        ProblemDetail.Builder builder =
                ProblemDetail.builder()
                        .type(java.net.URI.create(typeUri()))
                        .title(title())
                        .status(statusCode())
                        .detail(detail())
                        .instance(instance);

        // Add type-specific extensions
        switch (this) {
            case Validation v when !v.fieldErrors().isEmpty() ->
                    builder.extension("fieldErrors", v.fieldErrors());

            case PreconditionFailed pf when pf.expectedEtag() != null -> {
                builder.extension("expectedEtag", pf.expectedEtag());
                builder.extension("providedEtag", pf.providedEtag());
            }

            case IdMismatch im -> {
                builder.extension("pathId", im.pathId());
                builder.extension("bodyId", im.bodyId());
            }

            case Conflict c -> builder.extension("conflictType", c.conflictType());

            default -> {
                /* No extensions needed */
            }
        }

        return builder.build();
    }
}
