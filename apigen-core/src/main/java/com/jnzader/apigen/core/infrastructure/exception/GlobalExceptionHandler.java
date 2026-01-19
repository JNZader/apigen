package com.jnzader.apigen.core.infrastructure.exception;

import com.jnzader.apigen.core.domain.exception.*;
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
 * Manejador global de excepciones para la API REST.
 *
 * <p>Centraliza la gestión de errores para proporcionar respuestas consistentes conforme al
 * estándar RFC 7807 (Problem Details for HTTP APIs).
 *
 * <p>Content-Type: application/problem+json
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Media type para RFC 7807 Problem Details. */
    public static final MediaType APPLICATION_PROBLEM_JSON =
            MediaType.valueOf("application/problem+json");

    /**
     * Maneja las excepciones de tipo {@link ResourceNotFoundException}. Retorna un estado HTTP 404
     * Not Found.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {

        log.debug("Resource not found: {}", ex.getMessage());

        ProblemDetail problem =
                ProblemDetail.builder()
                        .type(URI.create("https://api.example.com/problems/not-found"))
                        .title("Recurso no encontrado")
                        .status(HttpStatus.NOT_FOUND.value())
                        .detail(ex.getMessage())
                        .instance(request.getRequestURI())
                        .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /**
     * Maneja las excepciones de tipo {@link DuplicateResourceException}. Retorna un estado HTTP 409
     * Conflict.
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateResourceException(
            DuplicateResourceException ex, HttpServletRequest request) {

        log.debug("Duplicate resource: {}", ex.getMessage());

        ProblemDetail problem =
                ProblemDetail.builder()
                        .type(URI.create("https://api.example.com/problems/conflict"))
                        .title("Conflicto de recurso")
                        .status(HttpStatus.CONFLICT.value())
                        .detail(ex.getMessage())
                        .instance(request.getRequestURI())
                        .build();

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /**
     * Maneja las excepciones de tipo {@link ValidationException}. Retorna un estado HTTP 400 Bad
     * Request.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(
            ValidationException ex, HttpServletRequest request) {

        log.debug("Validation error: {}", ex.getMessage());

        ProblemDetail problem =
                ProblemDetail.builder()
                        .type(URI.create("https://api.example.com/problems/validation-error"))
                        .title("Error de validación")
                        .status(HttpStatus.BAD_REQUEST.value())
                        .detail(ex.getMessage())
                        .instance(request.getRequestURI())
                        .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /**
     * Maneja las excepciones de tipo {@link IdMismatchException}. Retorna un estado HTTP 400 Bad
     * Request.
     */
    @ExceptionHandler(IdMismatchException.class)
    public ResponseEntity<ProblemDetail> handleIdMismatchException(
            IdMismatchException ex, HttpServletRequest request) {

        log.debug("ID mismatch: {}", ex.getMessage());

        ProblemDetail problem =
                ProblemDetail.builder()
                        .type(URI.create("https://api.example.com/problems/id-mismatch"))
                        .title("ID no coincide")
                        .status(HttpStatus.BAD_REQUEST.value())
                        .detail(ex.getMessage())
                        .instance(request.getRequestURI())
                        .extension("pathId", ex.getPathId())
                        .extension("bodyId", ex.getBodyId())
                        .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /**
     * Maneja las excepciones de tipo {@link PreconditionFailedException}. Retorna un estado HTTP
     * 412 Precondition Failed (para ETag mismatch).
     */
    @ExceptionHandler(PreconditionFailedException.class)
    public ResponseEntity<ProblemDetail> handlePreconditionFailedException(
            PreconditionFailedException ex, HttpServletRequest request) {

        log.debug("Precondition failed: {}", ex.getMessage());

        ProblemDetail problem =
                ProblemDetail.builder()
                        .type(URI.create("https://api.example.com/problems/precondition-failed"))
                        .title("Precondición fallida")
                        .status(HttpStatus.PRECONDITION_FAILED.value())
                        .detail(ex.getMessage())
                        .instance(request.getRequestURI())
                        .build();

        return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /**
     * Maneja las excepciones de tipo {@link OperationFailedException}. Retorna un estado HTTP 500
     * Internal Server Error.
     */
    @ExceptionHandler(OperationFailedException.class)
    public ResponseEntity<ProblemDetail> handleOperationFailedException(
            OperationFailedException ex, HttpServletRequest request) {

        log.error("Operation failed: {}", ex.getMessage(), ex);

        ProblemDetail problem =
                ProblemDetail.builder()
                        .type(URI.create("https://api.example.com/problems/operation-failed"))
                        .title("Operación fallida")
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .detail(ex.getMessage())
                        .instance(request.getRequestURI())
                        .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /**
     * Maneja las excepciones de tipo {@link UnauthorizedActionException}. Retorna un estado HTTP
     * 403 Forbidden.
     */
    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<ProblemDetail> handleUnauthorizedActionException(
            UnauthorizedActionException ex, HttpServletRequest request) {

        log.warn("Unauthorized action: {}", ex.getMessage());

        ProblemDetail problem =
                ProblemDetail.builder()
                        .type(URI.create("https://api.example.com/problems/forbidden"))
                        .title("Acción no autorizada")
                        .status(HttpStatus.FORBIDDEN.value())
                        .detail(ex.getMessage())
                        .instance(request.getRequestURI())
                        .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /**
     * Maneja las excepciones de validación de argumentos de método (@Valid en DTOs). Retorna un
     * estado HTTP 400 Bad Request con lista de errores de validación.
     */
    @SuppressWarnings("java:S2259") // False positive: Objects.requireNonNullElse garantiza non-null
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        log.debug("Validation failed for request to {}", request.getRequestURI());

        // Recopilar errores de campo
        Map<String, List<String>> fieldErrors =
                ex.getBindingResult().getFieldErrors().stream()
                        .collect(
                                Collectors.groupingBy(
                                        org.springframework.validation.FieldError::getField,
                                        Collectors.mapping(
                                                org.springframework.validation.FieldError
                                                        ::getDefaultMessage,
                                                Collectors.toList())));

        // Recopilar errores globales
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
                        .type(URI.create("https://api.example.com/problems/validation-error"))
                        .title("Error de validación de entrada")
                        .status(HttpStatus.BAD_REQUEST.value())
                        .detail(
                                "La solicitud contiene "
                                        + ex.getBindingResult().getErrorCount()
                                        + " error(es) de validación")
                        .instance(request.getRequestURI())
                        .extensions(extensions)
                        .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /**
     * Maneja las excepciones de tipo {@link IllegalArgumentException}. Retorna un estado HTTP 400
     * Bad Request.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {

        log.debug("Illegal argument: {}", ex.getMessage());

        ProblemDetail problem =
                ProblemDetail.builder()
                        .type(URI.create("https://api.example.com/problems/bad-request"))
                        .title("Argumento inválido")
                        .status(HttpStatus.BAD_REQUEST.value())
                        .detail(ex.getMessage())
                        .instance(request.getRequestURI())
                        .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /**
     * Maneja las excepciones de tipo {@link HttpMessageNotReadableException}. Se lanza cuando el
     * JSON del body no puede ser parseado (malformado). Retorna un estado HTTP 400 Bad Request.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.debug("Malformed JSON in request: {}", ex.getMessage());

        ProblemDetail problem =
                ProblemDetail.builder()
                        .type(URI.create("https://api.example.com/problems/malformed-json"))
                        .title("JSON malformado")
                        .status(HttpStatus.BAD_REQUEST.value())
                        .detail("El cuerpo de la solicitud contiene JSON inválido o malformado")
                        .instance(request.getRequestURI())
                        .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /**
     * Maneja las excepciones de tipo {@link ConstraintViolationException}. Se lanza cuando los
     * parámetros de query o path violan restricciones de validación. Retorna un estado HTTP 400 Bad
     * Request.
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
                        .type(URI.create("https://api.example.com/problems/constraint-violation"))
                        .title("Parámetros inválidos")
                        .status(HttpStatus.BAD_REQUEST.value())
                        .detail(
                                "Los parámetros de la solicitud no cumplen con las restricciones de"
                                        + " validación")
                        .instance(request.getRequestURI())
                        .extensions(extensions)
                        .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /**
     * Maneja las excepciones de tipo {@link MethodArgumentTypeMismatchException}. Se lanza cuando
     * un parámetro no puede ser convertido al tipo esperado. Retorna un estado HTTP 400 Bad
     * Request.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        log.debug("Type mismatch for parameter '{}': {}", ex.getName(), ex.getMessage());

        ProblemDetail problem =
                ProblemDetail.builder()
                        .type(URI.create("https://api.example.com/problems/type-mismatch"))
                        .title("Tipo de parámetro inválido")
                        .status(HttpStatus.BAD_REQUEST.value())
                        .detail(
                                String.format(
                                        "El parámetro '%s' tiene un valor inválido: '%s'",
                                        ex.getName(), ex.getValue()))
                        .instance(request.getRequestURI())
                        .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /**
     * Maneja cualquier otra excepción no capturada específicamente. Retorna un estado HTTP 500
     * Internal Server Error.
     */
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
                        .type(URI.create("https://api.example.com/problems/internal-error"))
                        .title("Error interno del servidor")
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .detail(
                                "Ha ocurrido un error inesperado. Por favor, inténtelo de nuevo más"
                                        + " tarde.")
                        .instance(request.getRequestURI())
                        .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
