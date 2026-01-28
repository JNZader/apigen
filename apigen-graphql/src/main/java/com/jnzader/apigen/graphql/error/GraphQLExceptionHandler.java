package com.jnzader.apigen.graphql.error;

import com.jnzader.apigen.core.domain.exception.AccountLockedException;
import com.jnzader.apigen.core.domain.exception.AuthenticationException;
import com.jnzader.apigen.core.domain.exception.DuplicateResourceException;
import com.jnzader.apigen.core.domain.exception.ExternalServiceException;
import com.jnzader.apigen.core.domain.exception.IdMismatchException;
import com.jnzader.apigen.core.domain.exception.OperationFailedException;
import com.jnzader.apigen.core.domain.exception.PreconditionFailedException;
import com.jnzader.apigen.core.domain.exception.RateLimitExceededException;
import com.jnzader.apigen.core.domain.exception.ResourceNotFoundException;
import com.jnzader.apigen.core.domain.exception.UnauthorizedActionException;
import com.jnzader.apigen.core.domain.exception.ValidationException;
import graphql.GraphQLError;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

/**
 * Exception handler for GraphQL data fetchers.
 *
 * <p>Converts common exceptions to structured GraphQL errors with RFC 7807-aligned extensions.
 *
 * <p>Handles apigen-core exceptions:
 *
 * <ul>
 *   <li>ResourceNotFoundException → NOT_FOUND (404)
 *   <li>ValidationException → VALIDATION_ERROR (400)
 *   <li>DuplicateResourceException → ALREADY_EXISTS (409)
 *   <li>UnauthorizedActionException → FORBIDDEN (403)
 *   <li>AuthenticationException → UNAUTHORIZED (401)
 *   <li>PreconditionFailedException → CONFLICT (412)
 *   <li>RateLimitExceededException → TOO_MANY_REQUESTS (429)
 *   <li>AccountLockedException → FORBIDDEN (423)
 *   <li>ExternalServiceException → INTERNAL_ERROR (502)
 *   <li>OperationFailedException → INTERNAL_ERROR (500)
 * </ul>
 *
 * <p>Also handles JPA and Spring Security exceptions:
 *
 * <ul>
 *   <li>EntityNotFoundException → NOT_FOUND (404)
 *   <li>ConstraintViolationException → VALIDATION_ERROR (400)
 *   <li>AuthenticationCredentialsNotFoundException → UNAUTHORIZED (401)
 *   <li>AccessDeniedException → FORBIDDEN (403)
 *   <li>OptimisticLockException → CONFLICT (409)
 *   <li>Other exceptions → INTERNAL_ERROR (500)
 * </ul>
 */
public class GraphQLExceptionHandler implements DataFetcherExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GraphQLExceptionHandler.class);

    @Override
    public CompletableFuture<DataFetcherExceptionHandlerResult> handleException(
            DataFetcherExceptionHandlerParameters params) {
        Throwable exception = params.getException();
        GraphQLError error = toGraphQLError(exception, params);

        log.warn(
                "GraphQL error at path {}: {} - {}",
                params.getPath(),
                error.getErrorType(),
                error.getMessage());

        return CompletableFuture.completedFuture(
                DataFetcherExceptionHandlerResult.newResult().error(error).build());
    }

    @SuppressWarnings("java:S3776") // Complexity is acceptable for exception mapping
    private GraphQLError toGraphQLError(
            Throwable exception, DataFetcherExceptionHandlerParameters params) {
        ApiGenGraphQLError.Builder builder =
                ApiGenGraphQLError.builder()
                        .locations(
                                params.getSourceLocation() != null
                                        ? java.util.List.of(params.getSourceLocation())
                                        : null)
                        .path(params.getPath() != null ? params.getPath().toList() : null);

        // APiGen Core Exceptions
        if (exception instanceof ResourceNotFoundException e) {
            return builder.errorType(GraphQLErrorType.NOT_FOUND)
                    .statusCode(404)
                    .message("Resource not found")
                    .detail(e.getMessage())
                    .build();
        }

        if (exception instanceof ValidationException e) {
            return builder.errorType(GraphQLErrorType.VALIDATION_ERROR)
                    .statusCode(400)
                    .message("Validation failed")
                    .detail(e.getMessage())
                    .build();
        }

        if (exception instanceof DuplicateResourceException e) {
            return builder.errorType(GraphQLErrorType.ALREADY_EXISTS)
                    .statusCode(409)
                    .message("Resource already exists")
                    .detail(e.getMessage())
                    .build();
        }

        if (exception instanceof UnauthorizedActionException e) {
            return builder.errorType(GraphQLErrorType.FORBIDDEN)
                    .statusCode(403)
                    .message("Action not allowed")
                    .detail(e.getMessage())
                    .build();
        }

        if (exception instanceof AuthenticationException e) {
            return builder.errorType(GraphQLErrorType.UNAUTHORIZED)
                    .statusCode(401)
                    .message("Authentication failed")
                    .detail(e.getMessage())
                    .extension("errorCode", e.getErrorCode())
                    .build();
        }

        if (exception instanceof PreconditionFailedException e) {
            return builder.errorType(GraphQLErrorType.CONFLICT)
                    .statusCode(412)
                    .message("Precondition failed")
                    .detail(e.getMessage())
                    .build();
        }

        if (exception instanceof IdMismatchException e) {
            return builder.errorType(GraphQLErrorType.VALIDATION_ERROR)
                    .statusCode(400)
                    .message("ID mismatch")
                    .detail(e.getMessage())
                    .extension("pathId", e.getPathId())
                    .extension("bodyId", e.getBodyId())
                    .build();
        }

        if (exception instanceof RateLimitExceededException e) {
            return builder.errorType(GraphQLErrorType.TOO_MANY_REQUESTS)
                    .statusCode(429)
                    .message("Rate limit exceeded")
                    .detail(e.getMessage())
                    .extension("retryAfterSeconds", e.getRetryAfterSeconds())
                    .build();
        }

        if (exception instanceof AccountLockedException e) {
            return builder.errorType(GraphQLErrorType.FORBIDDEN)
                    .statusCode(423)
                    .message("Account locked")
                    .detail(e.getMessage())
                    .extension("remainingSeconds", e.getRemainingSeconds())
                    .build();
        }

        if (exception instanceof ExternalServiceException e) {
            return builder.errorType(GraphQLErrorType.INTERNAL_ERROR)
                    .statusCode(502)
                    .message("External service error")
                    .detail(e.getMessage())
                    .extension("serviceName", e.getServiceName())
                    .build();
        }

        if (exception instanceof OperationFailedException e) {
            return builder.errorType(GraphQLErrorType.INTERNAL_ERROR)
                    .statusCode(500)
                    .message("Operation failed")
                    .detail(e.getMessage())
                    .build();
        }

        // JPA Exceptions
        if (exception instanceof EntityNotFoundException e) {
            return builder.errorType(GraphQLErrorType.NOT_FOUND)
                    .statusCode(404)
                    .message("Resource not found")
                    .detail(e.getMessage())
                    .build();
        }

        if (exception instanceof ConstraintViolationException e) {
            StringBuilder details = new StringBuilder();
            for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
                if (!details.isEmpty()) {
                    details.append("; ");
                }
                details.append(violation.getPropertyPath())
                        .append(": ")
                        .append(violation.getMessage());
            }
            return builder.errorType(GraphQLErrorType.VALIDATION_ERROR)
                    .statusCode(400)
                    .message("Validation failed")
                    .detail(details.toString())
                    .build();
        }

        if (exception instanceof IllegalArgumentException e) {
            return builder.errorType(GraphQLErrorType.VALIDATION_ERROR)
                    .statusCode(400)
                    .message("Invalid argument")
                    .detail(e.getMessage())
                    .build();
        }

        // Spring Security Exceptions
        if (exception instanceof AuthenticationCredentialsNotFoundException e) {
            return builder.errorType(GraphQLErrorType.UNAUTHORIZED)
                    .statusCode(401)
                    .message("Authentication required")
                    .detail(e.getMessage())
                    .build();
        }

        if (exception instanceof AccessDeniedException e) {
            return builder.errorType(GraphQLErrorType.FORBIDDEN)
                    .statusCode(403)
                    .message("Access denied")
                    .detail(e.getMessage())
                    .build();
        }

        if (exception instanceof OptimisticLockException) {
            return builder.errorType(GraphQLErrorType.CONFLICT)
                    .statusCode(409)
                    .message("Concurrent modification detected")
                    .detail("The resource was modified by another request. Please retry.")
                    .build();
        }

        // Generic internal error for unhandled exceptions
        log.error("Unhandled exception in GraphQL data fetcher", exception);
        return builder.errorType(GraphQLErrorType.INTERNAL_ERROR)
                .statusCode(500)
                .message("Internal server error")
                .detail("An unexpected error occurred")
                .build();
    }
}
