package com.jnzader.apigen.core.infrastructure.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnzader.apigen.core.domain.exception.*;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@DisplayName("ApiError Tests")
class ApiErrorTest {

    @Nested
    @DisplayName("NotFound")
    class NotFoundTests {

        @Test
        @DisplayName("should create with resource type and ID")
        void shouldCreateWithResourceTypeAndId() {
            ApiError.NotFound error = new ApiError.NotFound("User", 123L);

            assertThat(error.resourceType()).isEqualTo("User");
            assertThat(error.resourceId()).isEqualTo(123L);
            assertThat(error.message()).contains("User", "123");
            assertThat(error.httpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(error.statusCode()).isEqualTo(404);
        }

        @Test
        @DisplayName("should create with message only")
        void shouldCreateWithMessageOnly() {
            ApiError.NotFound error = new ApiError.NotFound("Custom message");

            assertThat(error.message()).isEqualTo("Custom message");
        }
    }

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        @DisplayName("should create with message and field errors")
        void shouldCreateWithMessageAndFieldErrors() {
            Map<String, String> fieldErrors = Map.of("email", "Invalid format");

            ApiError.Validation error = new ApiError.Validation("Validation failed", fieldErrors);

            assertThat(error.message()).isEqualTo("Validation failed");
            assertThat(error.fieldErrors()).containsEntry("email", "Invalid format");
            assertThat(error.httpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("should create with message only")
        void shouldCreateWithMessageOnly() {
            ApiError.Validation error = new ApiError.Validation("Simple error");

            assertThat(error.fieldErrors()).isEmpty();
        }

        @Test
        @DisplayName("should create with field errors only")
        void shouldCreateWithFieldErrorsOnly() {
            ApiError.Validation error = new ApiError.Validation(Map.of("name", "Required"));

            assertThat(error.message()).isEqualTo("Error de validación");
            assertThat(error.fieldErrors()).containsEntry("name", "Required");
        }
    }

    @Nested
    @DisplayName("Conflict")
    class ConflictTests {

        @Test
        @DisplayName("should create with message and conflict type")
        void shouldCreateWithMessageAndConflictType() {
            ApiError.Conflict error = new ApiError.Conflict("Already exists", "DUPLICATE_KEY");

            assertThat(error.message()).isEqualTo("Already exists");
            assertThat(error.conflictType()).isEqualTo("DUPLICATE_KEY");
            assertThat(error.httpStatus()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("should create with message only")
        void shouldCreateWithMessageOnly() {
            ApiError.Conflict error = new ApiError.Conflict("Conflict");

            assertThat(error.conflictType()).isEqualTo("CONFLICT");
        }
    }

    @Nested
    @DisplayName("Forbidden")
    class ForbiddenTests {

        @Test
        @DisplayName("should create with all fields")
        void shouldCreateWithAllFields() {
            ApiError.Forbidden error = new ApiError.Forbidden("Not allowed", "ADMIN", "user1");

            assertThat(error.message()).isEqualTo("Not allowed");
            assertThat(error.requiredRole()).isEqualTo("ADMIN");
            assertThat(error.currentUser()).isEqualTo("user1");
            assertThat(error.httpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("should create with message only")
        void shouldCreateWithMessageOnly() {
            ApiError.Forbidden error = new ApiError.Forbidden("Access denied");

            assertThat(error.requiredRole()).isNull();
            assertThat(error.currentUser()).isNull();
        }
    }

    @Nested
    @DisplayName("PreconditionFailed")
    class PreconditionFailedTests {

        @Test
        @DisplayName("should create with ETags")
        void shouldCreateWithETags() {
            ApiError.PreconditionFailed error =
                    new ApiError.PreconditionFailed("ETag mismatch", "etag1", "etag2");

            assertThat(error.expectedEtag()).isEqualTo("etag1");
            assertThat(error.providedEtag()).isEqualTo("etag2");
            assertThat(error.httpStatus()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
        }

        @Test
        @DisplayName("should create with message only")
        void shouldCreateWithMessageOnly() {
            ApiError.PreconditionFailed error = new ApiError.PreconditionFailed("Failed");

            assertThat(error.expectedEtag()).isNull();
        }
    }

    @Nested
    @DisplayName("IdMismatch")
    class IdMismatchTests {

        @Test
        @DisplayName("should create with IDs")
        void shouldCreateWithIds() {
            ApiError.IdMismatch error = new ApiError.IdMismatch(1L, 2L);

            assertThat(error.pathId()).isEqualTo(1L);
            assertThat(error.bodyId()).isEqualTo(2L);
            assertThat(error.message()).contains("1", "2");
            assertThat(error.httpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("Internal")
    class InternalTests {

        @Test
        @DisplayName("should create with message and cause")
        void shouldCreateWithMessageAndCause() {
            Exception cause = new RuntimeException("Root cause");
            ApiError.Internal error = new ApiError.Internal("Error occurred", cause);

            assertThat(error.message()).isEqualTo("Error occurred");
            assertThat(error.cause()).isEqualTo(cause);
            assertThat(error.httpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("should create with message only")
        void shouldCreateWithMessageOnly() {
            ApiError.Internal error = new ApiError.Internal("Error");

            assertThat(error.cause()).isNull();
        }

        @Test
        @DisplayName("should create with cause only")
        void shouldCreateWithCauseOnly() {
            Exception cause = new RuntimeException("Root");
            ApiError.Internal error = new ApiError.Internal(cause);

            assertThat(error.message()).isEqualTo("Error interno del servidor");
        }
    }

    @Nested
    @DisplayName("from() factory method")
    class FromFactoryMethodTests {

        @Test
        @DisplayName("should convert ResourceNotFoundException")
        void shouldConvertResourceNotFoundException() {
            var exception = new ResourceNotFoundException("Not found");

            ApiError error = ApiError.from(exception);

            assertThat(error).isInstanceOf(ApiError.NotFound.class);
        }

        @Test
        @DisplayName("should convert ValidationException")
        void shouldConvertValidationException() {
            var exception = new ValidationException("Invalid");

            ApiError error = ApiError.from(exception);

            assertThat(error).isInstanceOf(ApiError.Validation.class);
        }

        @Test
        @DisplayName("should convert DuplicateResourceException")
        void shouldConvertDuplicateResourceException() {
            var exception = new DuplicateResourceException("Duplicate");

            ApiError error = ApiError.from(exception);

            assertThat(error).isInstanceOf(ApiError.Conflict.class);
        }

        @Test
        @DisplayName("should convert UnauthorizedActionException")
        void shouldConvertUnauthorizedActionException() {
            var exception = new UnauthorizedActionException("Forbidden");

            ApiError error = ApiError.from(exception);

            assertThat(error).isInstanceOf(ApiError.Forbidden.class);
        }

        @Test
        @DisplayName("should convert PreconditionFailedException")
        void shouldConvertPreconditionFailedException() {
            var exception = new PreconditionFailedException("Precondition failed");

            ApiError error = ApiError.from(exception);

            assertThat(error).isInstanceOf(ApiError.PreconditionFailed.class);
        }

        @Test
        @DisplayName("should convert IdMismatchException")
        void shouldConvertIdMismatchException() {
            var exception = new IdMismatchException(1L, 2L);

            ApiError error = ApiError.from(exception);

            assertThat(error).isInstanceOf(ApiError.IdMismatch.class);
        }

        @Test
        @DisplayName("should convert OperationFailedException")
        void shouldConvertOperationFailedException() {
            var exception = new OperationFailedException("Failed");

            ApiError error = ApiError.from(exception);

            assertThat(error).isInstanceOf(ApiError.Internal.class);
        }

        @Test
        @DisplayName("should convert IllegalArgumentException")
        void shouldConvertIllegalArgumentException() {
            var exception = new IllegalArgumentException("Invalid arg");

            ApiError error = ApiError.from(exception);

            assertThat(error).isInstanceOf(ApiError.Validation.class);
        }

        @Test
        @DisplayName("should convert unknown exception to Internal")
        void shouldConvertUnknownExceptionToInternal() {
            var exception = new RuntimeException("Unknown");

            ApiError error = ApiError.from(exception);

            assertThat(error).isInstanceOf(ApiError.Internal.class);
        }

        @Test
        @DisplayName("should handle null exception")
        void shouldHandleNullException() {
            ApiError error = ApiError.from(null);

            assertThat(error).isInstanceOf(ApiError.Internal.class);
            assertThat(error.message()).isEqualTo("Error desconocido");
        }
    }

    @Nested
    @DisplayName("toProblemDetail()")
    class ToProblemDetailTests {

        @Test
        @DisplayName("should convert to ProblemDetail")
        void shouldConvertToProblemDetail() {
            ApiError.NotFound error = new ApiError.NotFound("User", 1L);

            ProblemDetail detail = error.toProblemDetail("/api/users/1");

            assertThat(detail.status()).isEqualTo(404);
            assertThat(detail.title()).isEqualTo("Recurso no encontrado");
            assertThat(detail.instance()).hasToString("/api/users/1");
        }

        @Test
        @DisplayName("should include field errors for Validation")
        void shouldIncludeFieldErrorsForValidation() {
            ApiError.Validation error = new ApiError.Validation(Map.of("name", "Required"));

            ProblemDetail detail = error.toProblemDetail("/api/test");

            assertThat(detail.extensions()).containsKey("fieldErrors");
        }

        @Test
        @DisplayName("should include ETags for PreconditionFailed")
        void shouldIncludeETagsForPreconditionFailed() {
            ApiError.PreconditionFailed error =
                    new ApiError.PreconditionFailed("Mismatch", "etag1", "etag2");

            ProblemDetail detail = error.toProblemDetail("/api/test");

            assertThat(detail.extensions())
                    .containsEntry("expectedEtag", "etag1")
                    .containsEntry("providedEtag", "etag2");
        }

        @Test
        @DisplayName("should include IDs for IdMismatch")
        void shouldIncludeIdsForIdMismatch() {
            ApiError.IdMismatch error = new ApiError.IdMismatch(1L, 2L);

            ProblemDetail detail = error.toProblemDetail("/api/test");

            assertThat(detail.extensions()).containsEntry("pathId", 1L).containsEntry("bodyId", 2L);
        }
    }

    @Nested
    @DisplayName("title() and typeUri()")
    class TitleAndTypeUriTests {

        @Test
        @DisplayName("should return correct title for each type")
        void shouldReturnCorrectTitleForEachType() {
            assertThat(new ApiError.NotFound("msg").title()).isEqualTo("Recurso no encontrado");
            assertThat(new ApiError.Validation("msg").title()).isEqualTo("Error de validación");
            assertThat(new ApiError.Conflict("msg").title()).isEqualTo("Conflicto de recursos");
            assertThat(new ApiError.Forbidden("msg").title()).isEqualTo("Acción no permitida");
            assertThat(new ApiError.PreconditionFailed("msg").title())
                    .isEqualTo("Precondición fallida");
            assertThat(new ApiError.IdMismatch(1L, 2L).title()).isEqualTo("IDs no coinciden");
            assertThat(new ApiError.Internal("msg").title())
                    .isEqualTo("Error interno del servidor");
        }

        @Test
        @DisplayName("should return correct typeUri for each type")
        void shouldReturnCorrectTypeUriForEachType() {
            assertThat(new ApiError.NotFound("msg").typeUri()).contains("not-found");
            assertThat(new ApiError.Validation("msg").typeUri()).contains("validation");
            assertThat(new ApiError.Conflict("msg").typeUri()).contains("conflict");
            assertThat(new ApiError.Forbidden("msg").typeUri()).contains("forbidden");
            assertThat(new ApiError.PreconditionFailed("msg").typeUri()).contains("precondition");
            assertThat(new ApiError.IdMismatch(1L, 2L).typeUri()).contains("id-mismatch");
            assertThat(new ApiError.Internal("msg").typeUri()).contains("internal");
        }
    }
}
