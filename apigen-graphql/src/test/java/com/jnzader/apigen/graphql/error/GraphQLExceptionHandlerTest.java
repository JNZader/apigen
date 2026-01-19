package com.jnzader.apigen.graphql.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import graphql.GraphQLError;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ResultPath;
import graphql.language.SourceLocation;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

@DisplayName("GraphQLExceptionHandler Tests")
class GraphQLExceptionHandlerTest {

    private GraphQLExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GraphQLExceptionHandler();
    }

    private DataFetcherExceptionHandlerParameters createParams(Throwable exception) {
        DataFetcherExceptionHandlerParameters params =
                mock(DataFetcherExceptionHandlerParameters.class);
        when(params.getException()).thenReturn(exception);
        when(params.getPath()).thenReturn(ResultPath.parse("/field"));
        when(params.getSourceLocation()).thenReturn(new SourceLocation(1, 1));
        return params;
    }

    @Nested
    @DisplayName("Exception Handling")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("should handle EntityNotFoundException")
        void shouldHandleEntityNotFoundException() throws Exception {
            EntityNotFoundException exception = new EntityNotFoundException("Product not found");
            DataFetcherExceptionHandlerParameters params = createParams(exception);

            CompletableFuture<DataFetcherExceptionHandlerResult> future =
                    handler.handleException(params);
            DataFetcherExceptionHandlerResult result = future.join();

            assertThat(result.getErrors()).hasSize(1);
            GraphQLError error = result.getErrors().get(0);
            assertThat(error.getErrorType()).isEqualTo(GraphQLErrorType.NOT_FOUND);
            assertThat(error.getExtensions().get("status")).isEqualTo(404);
        }

        @Test
        @DisplayName("should handle ConstraintViolationException")
        void shouldHandleConstraintViolationException() throws Exception {
            ConstraintViolation<?> violation = mock(ConstraintViolation.class);
            Path path = mock(Path.class);
            when(path.toString()).thenReturn("email");
            when(violation.getPropertyPath()).thenReturn(path);
            when(violation.getMessage()).thenReturn("must be a valid email");

            ConstraintViolationException exception =
                    new ConstraintViolationException("Validation failed", Set.of(violation));
            DataFetcherExceptionHandlerParameters params = createParams(exception);

            CompletableFuture<DataFetcherExceptionHandlerResult> future =
                    handler.handleException(params);
            DataFetcherExceptionHandlerResult result = future.join();

            assertThat(result.getErrors()).hasSize(1);
            GraphQLError error = result.getErrors().get(0);
            assertThat(error.getErrorType()).isEqualTo(GraphQLErrorType.VALIDATION_ERROR);
            assertThat(error.getExtensions().get("status")).isEqualTo(400);
        }

        @Test
        @DisplayName("should handle IllegalArgumentException")
        void shouldHandleIllegalArgumentException() throws Exception {
            IllegalArgumentException exception = new IllegalArgumentException("Invalid value");
            DataFetcherExceptionHandlerParameters params = createParams(exception);

            CompletableFuture<DataFetcherExceptionHandlerResult> future =
                    handler.handleException(params);
            DataFetcherExceptionHandlerResult result = future.join();

            assertThat(result.getErrors()).hasSize(1);
            GraphQLError error = result.getErrors().get(0);
            assertThat(error.getErrorType()).isEqualTo(GraphQLErrorType.VALIDATION_ERROR);
            assertThat(error.getExtensions().get("status")).isEqualTo(400);
        }

        @Test
        @DisplayName("should handle AuthenticationCredentialsNotFoundException")
        void shouldHandleAuthenticationCredentialsNotFoundException() throws Exception {
            AuthenticationCredentialsNotFoundException exception =
                    new AuthenticationCredentialsNotFoundException("Not authenticated");
            DataFetcherExceptionHandlerParameters params = createParams(exception);

            CompletableFuture<DataFetcherExceptionHandlerResult> future =
                    handler.handleException(params);
            DataFetcherExceptionHandlerResult result = future.join();

            assertThat(result.getErrors()).hasSize(1);
            GraphQLError error = result.getErrors().get(0);
            assertThat(error.getErrorType()).isEqualTo(GraphQLErrorType.UNAUTHORIZED);
            assertThat(error.getExtensions().get("status")).isEqualTo(401);
        }

        @Test
        @DisplayName("should handle AccessDeniedException")
        void shouldHandleAccessDeniedException() throws Exception {
            AccessDeniedException exception = new AccessDeniedException("Access denied");
            DataFetcherExceptionHandlerParameters params = createParams(exception);

            CompletableFuture<DataFetcherExceptionHandlerResult> future =
                    handler.handleException(params);
            DataFetcherExceptionHandlerResult result = future.join();

            assertThat(result.getErrors()).hasSize(1);
            GraphQLError error = result.getErrors().get(0);
            assertThat(error.getErrorType()).isEqualTo(GraphQLErrorType.FORBIDDEN);
            assertThat(error.getExtensions().get("status")).isEqualTo(403);
        }

        @Test
        @DisplayName("should handle OptimisticLockException")
        void shouldHandleOptimisticLockException() throws Exception {
            OptimisticLockException exception =
                    new OptimisticLockException("Concurrent modification");
            DataFetcherExceptionHandlerParameters params = createParams(exception);

            CompletableFuture<DataFetcherExceptionHandlerResult> future =
                    handler.handleException(params);
            DataFetcherExceptionHandlerResult result = future.join();

            assertThat(result.getErrors()).hasSize(1);
            GraphQLError error = result.getErrors().get(0);
            assertThat(error.getErrorType()).isEqualTo(GraphQLErrorType.CONFLICT);
            assertThat(error.getExtensions().get("status")).isEqualTo(409);
        }

        @Test
        @DisplayName("should handle generic exception as internal error")
        void shouldHandleGenericExceptionAsInternalError() throws Exception {
            RuntimeException exception = new RuntimeException("Unexpected error");
            DataFetcherExceptionHandlerParameters params = createParams(exception);

            CompletableFuture<DataFetcherExceptionHandlerResult> future =
                    handler.handleException(params);
            DataFetcherExceptionHandlerResult result = future.join();

            assertThat(result.getErrors()).hasSize(1);
            GraphQLError error = result.getErrors().get(0);
            assertThat(error.getErrorType()).isEqualTo(GraphQLErrorType.INTERNAL_ERROR);
            assertThat(error.getExtensions().get("status")).isEqualTo(500);
        }
    }
}
