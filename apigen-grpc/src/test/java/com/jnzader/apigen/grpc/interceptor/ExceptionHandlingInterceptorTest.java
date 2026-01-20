package com.jnzader.apigen.grpc.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.Status;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ExceptionHandlingInterceptor Tests")
class ExceptionHandlingInterceptorTest {

    private ExceptionHandlingInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new ExceptionHandlingInterceptor();
    }

    @Nested
    @DisplayName("Status Mapping")
    class StatusMappingTests {

        @Test
        @DisplayName("should map EntityNotFoundException to NOT_FOUND")
        void shouldMapEntityNotFoundException() {
            EntityNotFoundException exception = new EntityNotFoundException("Entity not found");

            Status status = interceptor.mapExceptionToStatus(exception);

            assertThat(status.getCode()).isEqualTo(Status.Code.NOT_FOUND);
            assertThat(status.getDescription()).contains("Entity not found");
        }

        @Test
        @DisplayName("should map ConstraintViolationException to INVALID_ARGUMENT")
        void shouldMapConstraintViolationException() {
            ConstraintViolationException exception =
                    new ConstraintViolationException("Validation failed", Set.of());

            Status status = interceptor.mapExceptionToStatus(exception);

            assertThat(status.getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
            assertThat(status.getDescription()).contains("Validation failed");
        }

        @Test
        @DisplayName("should map IllegalArgumentException to INVALID_ARGUMENT")
        void shouldMapIllegalArgumentException() {
            IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");

            Status status = interceptor.mapExceptionToStatus(exception);

            assertThat(status.getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
            assertThat(status.getDescription()).contains("Invalid argument");
        }

        @Test
        @DisplayName("should map IllegalStateException to FAILED_PRECONDITION")
        void shouldMapIllegalStateException() {
            IllegalStateException exception = new IllegalStateException("Invalid state");

            Status status = interceptor.mapExceptionToStatus(exception);

            assertThat(status.getCode()).isEqualTo(Status.Code.FAILED_PRECONDITION);
            assertThat(status.getDescription()).contains("Invalid state");
        }

        @Test
        @DisplayName("should map OptimisticLockException to ABORTED")
        void shouldMapOptimisticLockException() {
            OptimisticLockException exception = new OptimisticLockException("Concurrent update");

            Status status = interceptor.mapExceptionToStatus(exception);

            assertThat(status.getCode()).isEqualTo(Status.Code.ABORTED);
            assertThat(status.getDescription()).contains("Concurrent modification");
        }

        @Test
        @DisplayName("should map SecurityException to PERMISSION_DENIED")
        void shouldMapSecurityException() {
            SecurityException exception = new SecurityException("Access denied");

            Status status = interceptor.mapExceptionToStatus(exception);

            assertThat(status.getCode()).isEqualTo(Status.Code.PERMISSION_DENIED);
            assertThat(status.getDescription()).contains("Access denied");
        }

        @Test
        @DisplayName("should map UnsupportedOperationException to UNIMPLEMENTED")
        void shouldMapUnsupportedOperationException() {
            UnsupportedOperationException exception =
                    new UnsupportedOperationException("Not implemented");

            Status status = interceptor.mapExceptionToStatus(exception);

            assertThat(status.getCode()).isEqualTo(Status.Code.UNIMPLEMENTED);
            assertThat(status.getDescription()).contains("Not implemented");
        }

        @Test
        @DisplayName("should map generic exception to INTERNAL")
        void shouldMapGenericException() {
            RuntimeException exception = new RuntimeException("Unexpected error");

            Status status = interceptor.mapExceptionToStatus(exception);

            assertThat(status.getCode()).isEqualTo(Status.Code.INTERNAL);
            assertThat(status.getDescription()).contains("Internal server error");
        }
    }
}
