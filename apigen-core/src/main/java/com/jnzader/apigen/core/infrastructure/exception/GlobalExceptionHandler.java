package com.jnzader.apigen.core.infrastructure.exception;

import com.jnzader.apigen.core.domain.exception.DuplicateResourceException;
import com.jnzader.apigen.core.domain.exception.IdMismatchException;
import com.jnzader.apigen.core.domain.exception.OperationFailedException;
import com.jnzader.apigen.core.domain.exception.PreconditionFailedException;
import com.jnzader.apigen.core.domain.exception.ResourceNotFoundException;
import com.jnzader.apigen.core.domain.exception.UnauthorizedActionException;
import com.jnzader.apigen.core.domain.exception.ValidationException;
import com.jnzader.apigen.core.infrastructure.i18n.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Global exception handler for the REST API.
 *
 * <p>Centralizes error handling to provide consistent responses conforming to RFC 7807 (Problem
 * Details for HTTP APIs).
 *
 * <p>All error messages are internationalized based on the Accept-Language header.
 *
 * <p>Content-Type: application/problem+json
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Media type for RFC 7807 Problem Details. */
    public static final MediaType APPLICATION_PROBLEM_JSON =
            MediaType.valueOf("application/problem+json");

    private final MessageService messageService;

    public GlobalExceptionHandler(MessageService messageService) {
        this.messageService = messageService;
    }

    /** Handles {@link ResourceNotFoundException}. Returns HTTP 404 Not Found. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {

        log.debug("Resource not found: {}", ex.getMessage());

        ProblemDetail problem =
                ProblemDetail.builder()
                        .type(URI.create("urn:apigen:problem:not-found"))
                        .title(messageService.getNotFoundTitle())
                        .status(HttpStatus.NOT_FOUND.value())
                        .detail(ex.getMessage())
                        .instance(request.getRequestURI())
                        .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /** Handles {@link DuplicateResourceException}. Returns HTTP 409 Conflict. */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateResourceException(
            DuplicateResourceException ex, HttpServletRequest request) {

        log.debug("Duplicate resource: {}", ex.getMessage());

        ProblemDetail problem =
                ProblemDetail.builder()
                        .type(URI.create("urn:apigen:problem:conflict"))
                        .title(messageService.getConflictTitle())
                        .status(HttpStatus.CONFLICT.value())
                        .detail(ex.getMessage())
                        .instance(request.getRequestURI())
                        .build();

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /** Handles {@link ValidationException}. Returns HTTP 400 Bad Request. */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(
            ValidationException ex, HttpServletRequest request) {

        log.debug("Validation error: {}", ex.getMessage());

        ProblemDetail problem =
                ProblemDetail.builder()
                        .type(URI.create("urn:apigen:problem:validation-error"))
                        .title(messageService.getValidationTitle())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .detail(ex.getMessage())
                        .instance(request.getRequestURI())
                        .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /** Handles {@link IdMismatchException}. Returns HTTP 400 Bad Request. */
    @ExceptionHandler(IdMismatchException.class)
    public ResponseEntity<ProblemDetail> handleIdMismatchException(
            IdMismatchException ex, HttpServletRequest request) {

        log.debug("ID mismatch: {}", ex.getMessage());

        ProblemDetail problem =
                ProblemDetail.builder()
                        .type(URI.create("urn:apigen:problem:id-mismatch"))
                        .title(messageService.getIdMismatchTitle())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .detail(messageService.getIdMismatchDetail(ex.getPathId(), ex.getBodyId()))
                        .instance(request.getRequestURI())
                        .extension("pathId", ex.getPathId())
                        .extension("bodyId", ex.getBodyId())
                        .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /**
     * Handles {@link PreconditionFailedException}. Returns HTTP 412 Precondition Failed (for ETag
     * mismatch).
     */
    @ExceptionHandler(PreconditionFailedException.class)
    public ResponseEntity<ProblemDetail> handlePreconditionFailedException(
            PreconditionFailedException ex, HttpServletRequest request) {

        log.debug("Precondition failed: {}", ex.getMessage());

        ProblemDetail problem =
                ProblemDetail.builder()
                        .type(URI.create("urn:apigen:problem:precondition-failed"))
                        .title(messageService.getPreconditionFailedTitle())
                        .status(HttpStatus.PRECONDITION_FAILED.value())
                        .detail(ex.getMessage())
                        .instance(request.getRequestURI())
                        .build();

        return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /** Handles {@link OperationFailedException}. Returns HTTP 500 Internal Server Error. */
    @ExceptionHandler(OperationFailedException.class)
    public ResponseEntity<ProblemDetail> handleOperationFailedException(
            OperationFailedException ex, HttpServletRequest request) {

        log.error("Operation failed: {}", ex.getMessage(), ex);

        ProblemDetail problem =
                ProblemDetail.builder()
                        .type(URI.create("urn:apigen:problem:operation-failed"))
                        .title(messageService.getOperationFailedTitle())
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .detail(ex.getMessage())
                        .instance(request.getRequestURI())
                        .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /** Handles {@link UnauthorizedActionException}. Returns HTTP 403 Forbidden. */
    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<ProblemDetail> handleUnauthorizedActionException(
            UnauthorizedActionException ex, HttpServletRequest request) {

        log.warn("Unauthorized action: {}", ex.getMessage());

        ProblemDetail problem =
                ProblemDetail.builder()
                        .type(URI.create("urn:apigen:problem:forbidden"))
                        .title(messageService.getForbiddenTitle())
                        .status(HttpStatus.FORBIDDEN.value())
                        .detail(ex.getMessage())
                        .instance(request.getRequestURI())
                        .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /**
     * Handles validation exceptions for method arguments (@Valid on DTOs). Returns HTTP 400 Bad
     * Request with list of validation errors.
     */
    @SuppressWarnings("java:S2259") // False positive
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        log.debug("Validation failed for request to {}", request.getRequestURI());

        // Collect field errors
        Map<String, List<String>> fieldErrors =
                ex.getBindingResult().getFieldErrors().stream()
                        .collect(
                                Collectors.groupingBy(
                                        org.springframework.validation.FieldError::getField,
                                        Collectors.mapping(
                                                org.springframework.validation.FieldError
                                                        ::getDefaultMessage,
                                                Collectors.toList())));

        // Collect global errors
        List<String> globalErrors =
                ex.getBindingResult().getGlobalErrors().stream()
                        .map(org.springframework.validation.ObjectError::getDefaultMessage)
                        .toList();

        Map<String, Object> extensions = new HashMap<>();
        if (!fieldErrors.isEmpty()) {
            extensions.put("fieldErrors", fieldErrors);
        }
        if (!globalErrors.isEmpty()) {
            extensions.put("globalErrors", globalErrors);
        }
        extensions.put("errorCount", ex.getBindingResult().getErrorCount());

        ProblemDetail problem =
                ProblemDetail.builder()
                        .type(URI.create("urn:apigen:problem:validation-error"))
                        .title(messageService.getMessage("error.title.validation-input"))
                        .status(HttpStatus.BAD_REQUEST.value())
                        .detail(
                                messageService.getValidationDetail(
                                        ex.getBindingResult().getErrorCount()))
                        .instance(request.getRequestURI())
                        .extensions(extensions)
                        .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /** Handles {@link IllegalArgumentException}. Returns HTTP 400 Bad Request. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {

        log.debug("Illegal argument: {}", ex.getMessage());

        ProblemDetail problem =
                ProblemDetail.builder()
                        .type(URI.create("urn:apigen:problem:bad-request"))
                        .title(messageService.getBadRequestTitle())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .detail(ex.getMessage())
                        .instance(request.getRequestURI())
                        .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /**
     * Handles {@link HttpMessageNotReadableException}. Thrown when request body JSON cannot be
     * parsed. Returns HTTP 400 Bad Request.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.debug("Malformed JSON in request: {}", ex.getMessage());

        ProblemDetail problem =
                ProblemDetail.builder()
                        .type(URI.create("urn:apigen:problem:malformed-json"))
                        .title(messageService.getMalformedJsonTitle())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .detail(messageService.getMalformedJsonDetail())
                        .instance(request.getRequestURI())
                        .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /**
     * Handles {@link ConstraintViolationException}. Thrown when query or path parameters violate
     * validation constraints. Returns HTTP 400 Bad Request.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {

        log.debug("Constraint violation: {}", ex.getMessage());

        Map<String, Object> extensions = new HashMap<>();
        List<String> violations =
                ex.getConstraintViolations().stream()
                        .map(
                                violation ->
                                        violation.getPropertyPath() + ": " + violation.getMessage())
                        .toList();
        extensions.put("violations", violations);

        ProblemDetail problem =
                ProblemDetail.builder()
                        .type(URI.create("urn:apigen:problem:constraint-violation"))
                        .title(messageService.getConstraintViolationTitle())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .detail(messageService.getConstraintViolationDetail())
                        .instance(request.getRequestURI())
                        .extensions(extensions)
                        .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /**
     * Handles {@link MethodArgumentTypeMismatchException}. Thrown when a parameter cannot be
     * converted to the expected type. Returns HTTP 400 Bad Request.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        log.debug("Type mismatch for parameter '{}': {}", ex.getName(), ex.getMessage());

        ProblemDetail problem =
                ProblemDetail.builder()
                        .type(URI.create("urn:apigen:problem:type-mismatch"))
                        .title(messageService.getTypeMismatchTitle())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .detail(messageService.getTypeMismatchDetail(ex.getName(), ex.getValue()))
                        .instance(request.getRequestURI())
                        .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /** Handles any other uncaught exception. Returns HTTP 500 Internal Server Error. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGlobalException(
            Exception ex, HttpServletRequest request) {

        log.error(
                "Unexpected error processing request to {}: {}",
                request.getRequestURI(),
                ex.getMessage(),
                ex);

        ProblemDetail problem =
                ProblemDetail.builder()
                        .type(URI.create("urn:apigen:problem:internal-error"))
                        .title(messageService.getInternalErrorTitle())
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .detail(messageService.getInternalErrorDetail())
                        .instance(request.getRequestURI())
                        .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
