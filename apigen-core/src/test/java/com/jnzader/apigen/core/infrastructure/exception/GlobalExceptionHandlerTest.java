package com.jnzader.apigen.core.infrastructure.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jnzader.apigen.core.domain.exception.*;
import com.jnzader.apigen.core.infrastructure.i18n.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Unit tests for GlobalExceptionHandler.
 *
 * <p>Verifies that each exception type is correctly mapped to: - The appropriate HTTP code - RFC
 * 7807 (Problem Details) format - Correct fields in the response
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @Mock private HttpServletRequest request;
    @Mock private MessageService messageService;

    private static final String TEST_URI = "/api/test-entities";
    private static final String TEST_MESSAGE = "Test error message";

    // English message constants for testing
    private static final String MSG_NOT_FOUND_TITLE = "Resource not found";
    private static final String MSG_CONFLICT_TITLE = "Resource conflict";
    private static final String MSG_VALIDATION_TITLE = "Validation error";
    private static final String MSG_VALIDATION_INPUT_TITLE = "Validation input error";
    private static final String MSG_ID_MISMATCH_TITLE = "ID mismatch";
    private static final String MSG_PRECONDITION_FAILED_TITLE = "Precondition failed";
    private static final String MSG_OPERATION_FAILED_TITLE = "Operation failed";
    private static final String MSG_FORBIDDEN_TITLE = "Forbidden action";
    private static final String MSG_BAD_REQUEST_TITLE = "Invalid argument";
    private static final String MSG_INTERNAL_ERROR_TITLE = "Internal server error";
    private static final String MSG_INTERNAL_ERROR_DETAIL =
            "An unexpected error occurred. Please try again later.";
    private static final String MSG_MALFORMED_JSON_TITLE = "Malformed JSON";
    private static final String MSG_MALFORMED_JSON_DETAIL =
            "The request body contains invalid JSON";
    private static final String MSG_CONSTRAINT_VIOLATION_TITLE = "Constraint violation";
    private static final String MSG_CONSTRAINT_VIOLATION_DETAIL =
            "Request parameters violate validation constraints";
    private static final String MSG_TYPE_MISMATCH_TITLE = "Type mismatch";

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(messageService);
        when(request.getRequestURI()).thenReturn(TEST_URI);

        // Setup common message service returns
        when(messageService.getNotFoundTitle()).thenReturn(MSG_NOT_FOUND_TITLE);
        when(messageService.getConflictTitle()).thenReturn(MSG_CONFLICT_TITLE);
        when(messageService.getValidationTitle()).thenReturn(MSG_VALIDATION_TITLE);
        when(messageService.getIdMismatchTitle()).thenReturn(MSG_ID_MISMATCH_TITLE);
        when(messageService.getPreconditionFailedTitle()).thenReturn(MSG_PRECONDITION_FAILED_TITLE);
        when(messageService.getOperationFailedTitle()).thenReturn(MSG_OPERATION_FAILED_TITLE);
        when(messageService.getForbiddenTitle()).thenReturn(MSG_FORBIDDEN_TITLE);
        when(messageService.getBadRequestTitle()).thenReturn(MSG_BAD_REQUEST_TITLE);
        when(messageService.getInternalErrorTitle()).thenReturn(MSG_INTERNAL_ERROR_TITLE);
        when(messageService.getInternalErrorDetail()).thenReturn(MSG_INTERNAL_ERROR_DETAIL);
        when(messageService.getMalformedJsonTitle()).thenReturn(MSG_MALFORMED_JSON_TITLE);
        when(messageService.getMalformedJsonDetail()).thenReturn(MSG_MALFORMED_JSON_DETAIL);
        when(messageService.getConstraintViolationTitle())
                .thenReturn(MSG_CONSTRAINT_VIOLATION_TITLE);
        when(messageService.getConstraintViolationDetail())
                .thenReturn(MSG_CONSTRAINT_VIOLATION_DETAIL);
        when(messageService.getTypeMismatchTitle()).thenReturn(MSG_TYPE_MISMATCH_TITLE);
    }

    // ==================== ResourceNotFoundException Tests ====================

    @Nested
    @DisplayName("ResourceNotFoundException Handler")
    class ResourceNotFoundExceptionTests {

        @Test
        @DisplayName("should return 404 NOT_FOUND with problem details")
        void shouldReturn404WithProblemDetails() {
            // Given
            ResourceNotFoundException ex = new ResourceNotFoundException(TEST_MESSAGE);

            // When
            ResponseEntity<ProblemDetail> response =
                    handler.handleResourceNotFoundException(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(404);
            assertThat(response.getBody().title()).isEqualTo(MSG_NOT_FOUND_TITLE);
            assertThat(response.getBody().detail()).isEqualTo(TEST_MESSAGE);
            assertThat(response.getBody().instance()).hasToString(TEST_URI);
        }

        @Test
        @DisplayName("should return correct content type")
        void shouldReturnCorrectContentType() {
            // Given
            ResourceNotFoundException ex = new ResourceNotFoundException(TEST_MESSAGE);

            // When
            ResponseEntity<ProblemDetail> response =
                    handler.handleResourceNotFoundException(ex, request);

            // Then
            assertThat(response.getHeaders().getContentType())
                    .isEqualTo(GlobalExceptionHandler.APPLICATION_PROBLEM_JSON);
        }
    }

    // ==================== DuplicateResourceException Tests ====================

    @Nested
    @DisplayName("DuplicateResourceException Handler")
    class DuplicateResourceExceptionTests {

        @Test
        @DisplayName("should return 409 CONFLICT with problem details")
        void shouldReturn409WithProblemDetails() {
            // Given
            DuplicateResourceException ex = new DuplicateResourceException(TEST_MESSAGE);

            // When
            ResponseEntity<ProblemDetail> response =
                    handler.handleDuplicateResourceException(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(409);
            assertThat(response.getBody().title()).isEqualTo(MSG_CONFLICT_TITLE);
            assertThat(response.getBody().detail()).isEqualTo(TEST_MESSAGE);
        }
    }

    // ==================== ValidationException Tests ====================

    @Nested
    @DisplayName("ValidationException Handler")
    class ValidationExceptionTests {

        @Test
        @DisplayName("should return 400 BAD_REQUEST with problem details")
        void shouldReturn400WithProblemDetails() {
            // Given
            ValidationException ex = new ValidationException(TEST_MESSAGE);

            // When
            ResponseEntity<ProblemDetail> response = handler.handleValidationException(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(400);
            assertThat(response.getBody().title()).isEqualTo(MSG_VALIDATION_TITLE);
            assertThat(response.getBody().detail()).isEqualTo(TEST_MESSAGE);
        }
    }

    // ==================== IdMismatchException Tests ====================

    @Nested
    @DisplayName("IdMismatchException Handler")
    class IdMismatchExceptionTests {

        @Test
        @DisplayName("should return 400 BAD_REQUEST with IDs in extensions")
        void shouldReturn400WithIdsInExtensions() {
            // Given
            Long pathId = 1L;
            Long bodyId = 2L;
            IdMismatchException ex = new IdMismatchException(pathId, bodyId);
            when(messageService.getIdMismatchDetail(any(), any()))
                    .thenReturn("Path ID (1) does not match body ID (2)");

            // When
            ResponseEntity<ProblemDetail> response = handler.handleIdMismatchException(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(400);
            assertThat(response.getBody().title()).isEqualTo(MSG_ID_MISMATCH_TITLE);
            assertThat(response.getBody().extensions()).isNotNull();
            assertThat(response.getBody().extensions()).containsEntry("pathId", pathId);
            assertThat(response.getBody().extensions()).containsEntry("bodyId", bodyId);
        }
    }

    // ==================== PreconditionFailedException Tests ====================

    @Nested
    @DisplayName("PreconditionFailedException Handler")
    class PreconditionFailedExceptionTests {

        @Test
        @DisplayName("should return 412 PRECONDITION_FAILED with problem details")
        void shouldReturn412WithProblemDetails() {
            // Given
            PreconditionFailedException ex = new PreconditionFailedException(TEST_MESSAGE);

            // When
            ResponseEntity<ProblemDetail> response =
                    handler.handlePreconditionFailedException(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(412);
            assertThat(response.getBody().title()).isEqualTo(MSG_PRECONDITION_FAILED_TITLE);
            assertThat(response.getBody().detail()).isEqualTo(TEST_MESSAGE);
        }
    }

    // ==================== OperationFailedException Tests ====================

    @Nested
    @DisplayName("OperationFailedException Handler")
    class OperationFailedExceptionTests {

        @Test
        @DisplayName("should return 500 INTERNAL_SERVER_ERROR with problem details")
        void shouldReturn500WithProblemDetails() {
            // Given
            OperationFailedException ex = new OperationFailedException(TEST_MESSAGE);

            // When
            ResponseEntity<ProblemDetail> response =
                    handler.handleOperationFailedException(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(500);
            assertThat(response.getBody().title()).isEqualTo(MSG_OPERATION_FAILED_TITLE);
            assertThat(response.getBody().detail()).isEqualTo(TEST_MESSAGE);
        }
    }

    // ==================== UnauthorizedActionException Tests ====================

    @Nested
    @DisplayName("UnauthorizedActionException Handler")
    class UnauthorizedActionExceptionTests {

        @Test
        @DisplayName("should return 403 FORBIDDEN with problem details")
        void shouldReturn403WithProblemDetails() {
            // Given
            UnauthorizedActionException ex = new UnauthorizedActionException(TEST_MESSAGE);

            // When
            ResponseEntity<ProblemDetail> response =
                    handler.handleUnauthorizedActionException(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(403);
            assertThat(response.getBody().title()).isEqualTo(MSG_FORBIDDEN_TITLE);
            assertThat(response.getBody().detail()).isEqualTo(TEST_MESSAGE);
        }
    }

    // ==================== MethodArgumentNotValidException Tests ====================

    @Nested
    @DisplayName("MethodArgumentNotValidException Handler")
    class MethodArgumentNotValidExceptionTests {

        @Test
        @DisplayName("should return 400 BAD_REQUEST with field errors")
        void shouldReturn400WithFieldErrors() {
            // Given
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError fieldError = new FieldError("testEntity", "name", "Name is required");
            when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
            when(bindingResult.getGlobalErrors()).thenReturn(List.of());
            when(bindingResult.getErrorCount()).thenReturn(1);
            when(messageService.getMessage("error.title.validation-input"))
                    .thenReturn(MSG_VALIDATION_INPUT_TITLE);
            when(messageService.getValidationDetail(1))
                    .thenReturn("The request contains 1 validation error(s)");

            MethodArgumentNotValidException ex =
                    new MethodArgumentNotValidException(null, bindingResult);

            // When
            ResponseEntity<ProblemDetail> response =
                    handler.handleMethodArgumentNotValid(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(400);
            assertThat(response.getBody().title()).isEqualTo(MSG_VALIDATION_INPUT_TITLE);
            assertThat(response.getBody().extensions()).containsKey("fieldErrors");
            assertThat(response.getBody().extensions()).containsEntry("errorCount", 1);
        }

        @Test
        @DisplayName("should return 400 BAD_REQUEST with global errors")
        void shouldReturn400WithGlobalErrors() {
            // Given
            BindingResult bindingResult = mock(BindingResult.class);
            ObjectError globalError = new ObjectError("testEntity", "Global validation failed");
            when(bindingResult.getFieldErrors()).thenReturn(List.of());
            when(bindingResult.getGlobalErrors()).thenReturn(List.of(globalError));
            when(bindingResult.getErrorCount()).thenReturn(1);
            when(messageService.getMessage("error.title.validation-input"))
                    .thenReturn(MSG_VALIDATION_INPUT_TITLE);
            when(messageService.getValidationDetail(1))
                    .thenReturn("The request contains 1 validation error(s)");

            MethodArgumentNotValidException ex =
                    new MethodArgumentNotValidException(null, bindingResult);

            // When
            ResponseEntity<ProblemDetail> response =
                    handler.handleMethodArgumentNotValid(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().extensions()).containsKey("globalErrors");
        }

        @Test
        @DisplayName("should include multiple field errors grouped by field name")
        void shouldIncludeMultipleFieldErrorsGroupedByFieldName() {
            // Given
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError error1 = new FieldError("testEntity", "name", "Name is required");
            FieldError error2 =
                    new FieldError("testEntity", "name", "Name must be at least 2 characters");
            FieldError error3 = new FieldError("testEntity", "email", "Invalid email format");
            when(bindingResult.getFieldErrors()).thenReturn(List.of(error1, error2, error3));
            when(bindingResult.getGlobalErrors()).thenReturn(List.of());
            when(bindingResult.getErrorCount()).thenReturn(3);
            when(messageService.getMessage("error.title.validation-input"))
                    .thenReturn(MSG_VALIDATION_INPUT_TITLE);
            when(messageService.getValidationDetail(3))
                    .thenReturn("The request contains 3 validation error(s)");

            MethodArgumentNotValidException ex =
                    new MethodArgumentNotValidException(null, bindingResult);

            // When
            ResponseEntity<ProblemDetail> response =
                    handler.handleMethodArgumentNotValid(ex, request);

            // Then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().extensions()).containsEntry("errorCount", 3);
            assertThat(response.getBody().detail()).contains("3 validation error(s)");
        }
    }

    // ==================== IllegalArgumentException Tests ====================

    @Nested
    @DisplayName("IllegalArgumentException Handler")
    class IllegalArgumentExceptionTests {

        @Test
        @DisplayName("should return 400 BAD_REQUEST with problem details")
        void shouldReturn400WithProblemDetails() {
            // Given
            IllegalArgumentException ex = new IllegalArgumentException(TEST_MESSAGE);

            // When
            ResponseEntity<ProblemDetail> response =
                    handler.handleIllegalArgumentException(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(400);
            assertThat(response.getBody().title()).isEqualTo(MSG_BAD_REQUEST_TITLE);
            assertThat(response.getBody().detail()).isEqualTo(TEST_MESSAGE);
        }
    }

    // ==================== HttpMessageNotReadableException Tests ====================

    @Nested
    @DisplayName("HttpMessageNotReadableException Handler")
    class HttpMessageNotReadableExceptionTests {

        @Test
        @DisplayName("should return 400 BAD_REQUEST with malformed JSON details")
        void shouldReturn400WithMalformedJsonDetails() {
            // Given
            HttpMessageNotReadableException ex =
                    new HttpMessageNotReadableException(
                            "Could not read JSON", (Throwable) null, null);

            // When
            ResponseEntity<ProblemDetail> response =
                    handler.handleHttpMessageNotReadableException(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(400);
            assertThat(response.getBody().title()).isEqualTo(MSG_MALFORMED_JSON_TITLE);
            assertThat(response.getBody().detail()).isEqualTo(MSG_MALFORMED_JSON_DETAIL);
        }

        @Test
        @DisplayName("should return correct content type")
        void shouldReturnCorrectContentType() {
            // Given
            HttpMessageNotReadableException ex =
                    new HttpMessageNotReadableException("Invalid JSON", (Throwable) null, null);

            // When
            ResponseEntity<ProblemDetail> response =
                    handler.handleHttpMessageNotReadableException(ex, request);

            // Then
            assertThat(response.getHeaders().getContentType())
                    .isEqualTo(GlobalExceptionHandler.APPLICATION_PROBLEM_JSON);
        }

        @Test
        @DisplayName("should include correct type URI")
        void shouldIncludeCorrectTypeUri() {
            // Given
            HttpMessageNotReadableException ex =
                    new HttpMessageNotReadableException("Malformed", (Throwable) null, null);

            // When
            ResponseEntity<ProblemDetail> response =
                    handler.handleHttpMessageNotReadableException(ex, request);

            // Then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().type()).hasToString("urn:apigen:problem:malformed-json");
        }
    }

    // ==================== ConstraintViolationException Tests ====================

    @Nested
    @DisplayName("ConstraintViolationException Handler")
    class ConstraintViolationExceptionTests {

        @Test
        @DisplayName("should return 400 BAD_REQUEST with constraint violations")
        void shouldReturn400WithConstraintViolations() {
            // Given
            jakarta.validation.ConstraintViolation<?> violation =
                    mock(jakarta.validation.ConstraintViolation.class);
            jakarta.validation.Path path = mock(jakarta.validation.Path.class);
            when(path.toString()).thenReturn("paramName");
            when(violation.getPropertyPath()).thenReturn(path);
            when(violation.getMessage()).thenReturn("must not be null");

            jakarta.validation.ConstraintViolationException ex =
                    new jakarta.validation.ConstraintViolationException(
                            java.util.Set.of(violation));

            // When
            ResponseEntity<ProblemDetail> response =
                    handler.handleConstraintViolationException(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(400);
            assertThat(response.getBody().title()).isEqualTo(MSG_CONSTRAINT_VIOLATION_TITLE);
            assertThat(response.getBody().detail()).isEqualTo(MSG_CONSTRAINT_VIOLATION_DETAIL);
        }

        @Test
        @DisplayName("should include violations in extensions")
        void shouldIncludeViolationsInExtensions() {
            // Given
            jakarta.validation.ConstraintViolation<?> violation1 =
                    mock(jakarta.validation.ConstraintViolation.class);
            jakarta.validation.Path path1 = mock(jakarta.validation.Path.class);
            when(path1.toString()).thenReturn("id");
            when(violation1.getPropertyPath()).thenReturn(path1);
            when(violation1.getMessage()).thenReturn("must be positive");

            jakarta.validation.ConstraintViolation<?> violation2 =
                    mock(jakarta.validation.ConstraintViolation.class);
            jakarta.validation.Path path2 = mock(jakarta.validation.Path.class);
            when(path2.toString()).thenReturn("name");
            when(violation2.getPropertyPath()).thenReturn(path2);
            when(violation2.getMessage()).thenReturn("must not be blank");

            jakarta.validation.ConstraintViolationException ex =
                    new jakarta.validation.ConstraintViolationException(
                            java.util.Set.of(violation1, violation2));

            // When
            ResponseEntity<ProblemDetail> response =
                    handler.handleConstraintViolationException(ex, request);

            // Then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().extensions()).containsKey("violations");
            @SuppressWarnings("unchecked")
            java.util.List<String> violations =
                    (java.util.List<String>) response.getBody().extensions().get("violations");
            assertThat(violations).hasSize(2);
        }

        @Test
        @DisplayName("should include correct type URI")
        void shouldIncludeCorrectTypeUri() {
            // Given
            jakarta.validation.ConstraintViolation<?> violation =
                    mock(jakarta.validation.ConstraintViolation.class);
            jakarta.validation.Path path = mock(jakarta.validation.Path.class);
            when(path.toString()).thenReturn("field");
            when(violation.getPropertyPath()).thenReturn(path);
            when(violation.getMessage()).thenReturn("invalid");

            jakarta.validation.ConstraintViolationException ex =
                    new jakarta.validation.ConstraintViolationException(
                            java.util.Set.of(violation));

            // When
            ResponseEntity<ProblemDetail> response =
                    handler.handleConstraintViolationException(ex, request);

            // Then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().type())
                    .hasToString("urn:apigen:problem:constraint-violation");
        }
    }

    // ==================== MethodArgumentTypeMismatchException Tests ====================

    @Nested
    @DisplayName("MethodArgumentTypeMismatchException Handler")
    class MethodArgumentTypeMismatchExceptionTests {

        @Test
        @DisplayName("should return 400 BAD_REQUEST with type mismatch details")
        void shouldReturn400WithTypeMismatchDetails() {
            // Given
            when(messageService.getTypeMismatchDetail("id", "abc"))
                    .thenReturn("Parameter 'id' with value 'abc' could not be converted");

            MethodArgumentTypeMismatchException ex =
                    new MethodArgumentTypeMismatchException(
                            "abc", Long.class, "id", null, new NumberFormatException());

            // When
            ResponseEntity<ProblemDetail> response =
                    handler.handleMethodArgumentTypeMismatchException(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(400);
            assertThat(response.getBody().title()).isEqualTo(MSG_TYPE_MISMATCH_TITLE);
            assertThat(response.getBody().detail()).contains("id");
        }

        @Test
        @DisplayName("should include correct type URI")
        void shouldIncludeCorrectTypeUri() {
            // Given
            when(messageService.getTypeMismatchDetail("page", "invalid"))
                    .thenReturn("Parameter 'page' with value 'invalid' could not be converted");

            MethodArgumentTypeMismatchException ex =
                    new MethodArgumentTypeMismatchException(
                            "invalid", Integer.class, "page", null, new NumberFormatException());

            // When
            ResponseEntity<ProblemDetail> response =
                    handler.handleMethodArgumentTypeMismatchException(ex, request);

            // Then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().type()).hasToString("urn:apigen:problem:type-mismatch");
        }

        @Test
        @DisplayName("should handle null value gracefully")
        void shouldHandleNullValueGracefully() {
            // Given
            when(messageService.getTypeMismatchDetail("id", null))
                    .thenReturn("Parameter 'id' with value 'null' could not be converted");

            MethodArgumentTypeMismatchException ex =
                    new MethodArgumentTypeMismatchException(
                            null, Long.class, "id", null, new IllegalArgumentException());

            // When
            ResponseEntity<ProblemDetail> response =
                    handler.handleMethodArgumentTypeMismatchException(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
        }
    }

    // ==================== Generic Exception Tests ====================

    @Nested
    @DisplayName("Generic Exception Handler")
    class GenericExceptionTests {

        @Test
        @DisplayName("should return 500 INTERNAL_SERVER_ERROR for unexpected exceptions")
        void shouldReturn500ForUnexpectedExceptions() {
            // Given
            Exception ex = new RuntimeException("Unexpected error");

            // When
            ResponseEntity<ProblemDetail> response = handler.handleGlobalException(ex, request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(500);
            assertThat(response.getBody().title()).isEqualTo(MSG_INTERNAL_ERROR_TITLE);
            assertThat(response.getBody().detail())
                    .doesNotContain("Unexpected error"); // Should not expose internal details
        }

        @Test
        @DisplayName("should hide internal error details from client")
        void shouldHideInternalErrorDetailsFromClient() {
            // Given
            Exception ex = new NullPointerException("Sensitive internal error information");

            // When
            ResponseEntity<ProblemDetail> response = handler.handleGlobalException(ex, request);

            // Then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().detail()).isEqualTo(MSG_INTERNAL_ERROR_DETAIL);
        }
    }

    // ==================== RFC 7807 Compliance Tests ====================

    @Nested
    @DisplayName("RFC 7807 Compliance")
    class RFC7807ComplianceTests {

        @Test
        @DisplayName("should include type URI in all responses")
        void shouldIncludeTypeUriInAllResponses() {
            // Given
            ResourceNotFoundException ex = new ResourceNotFoundException(TEST_MESSAGE);

            // When
            ResponseEntity<ProblemDetail> response =
                    handler.handleResourceNotFoundException(ex, request);

            // Then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().type()).isNotNull();
            assertThat(response.getBody().type().toString()).startsWith("urn:apigen:problem:");
        }

        @Test
        @DisplayName("should include timestamp in all responses")
        void shouldIncludeTimestampInAllResponses() {
            // Given
            ResourceNotFoundException ex = new ResourceNotFoundException(TEST_MESSAGE);

            // When
            ResponseEntity<ProblemDetail> response =
                    handler.handleResourceNotFoundException(ex, request);

            // Then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().timestamp()).isNotNull();
        }

        @Test
        @DisplayName("should include instance (request URI) in all responses")
        void shouldIncludeInstanceInAllResponses() {
            // Given
            DuplicateResourceException ex = new DuplicateResourceException(TEST_MESSAGE);

            // When
            ResponseEntity<ProblemDetail> response =
                    handler.handleDuplicateResourceException(ex, request);

            // Then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().instance()).isNotNull();
            assertThat(response.getBody().instance()).hasToString(TEST_URI);
        }
    }
}
