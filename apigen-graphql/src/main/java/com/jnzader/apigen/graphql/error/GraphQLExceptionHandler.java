package com.jnzader.apigen.graphql.error;

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
 * <p>Handles:
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

    private GraphQLError toGraphQLError(
            Throwable exception, DataFetcherExceptionHandlerParameters params) {
        ApiGenGraphQLError.Builder builder =
                ApiGenGraphQLError.builder()
                        .locations(
                                params.getSourceLocation() != null
                                        ? java.util.List.of(params.getSourceLocation())
                                        : null)
                        .path(params.getPath() != null ? params.getPath().toList() : null);

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

        if (exception instanceof OptimisticLockException _) {
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
