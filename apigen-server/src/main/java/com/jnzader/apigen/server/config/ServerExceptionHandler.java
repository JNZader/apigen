package com.jnzader.apigen.server.config;

import com.jnzader.apigen.exceptions.domain.ExternalServiceException;
import com.jnzader.apigen.exceptions.domain.OperationFailedException;
import com.jnzader.apigen.exceptions.domain.ValidationException;
import com.jnzader.apigen.exceptions.infrastructure.ProblemDetail;
import com.jnzader.apigen.server.exception.GitHubApiException;
import com.jnzader.apigen.server.exception.ProjectGenerationException;
import com.jnzader.apigen.server.exception.SchemaValidationException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Server-specific exception handler for the APiGen Server.
 *
 * <p>Handles all exceptions and returns RFC 7807 compliant Problem Details responses.
 *
 * <p>Named differently from apigen-core's GlobalExceptionHandler to avoid bean naming conflicts.
 */
@RestControllerAdvice
@Slf4j
public class ServerExceptionHandler {

    private static final MediaType APPLICATION_PROBLEM_JSON =
            MediaType.valueOf("application/problem+json");

    /** Handle GitHub API errors. */
    @ExceptionHandler(GitHubApiException.class)
    public ResponseEntity<ProblemDetail> handleGitHubApiException(
            GitHubApiException ex, HttpServletRequest request) {
        log.error("GitHub API error: {}", ex.getMessage(), ex);

        ProblemDetail.Builder builder =
                ProblemDetail.builder()
                        .type(URI.create("urn:apigen:problem:external-service-error"))
                        .title("GitHub API Error")
                        .status(HttpStatus.BAD_GATEWAY.value())
                        .detail(ex.getMessage())
                        .instance(request.getRequestURI())
                        .extension("service", "github");

        if (ex.hasRateLimitInfo()) {
            builder.extension("rateLimitRemaining", ex.getRateLimitRemaining());
            builder.extension("rateLimitReset", ex.getRateLimitReset().toString());
        }

        if (ex.getOriginalError() != null) {
            builder.extension("originalError", ex.getOriginalError());
        }

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(builder.build());
    }

    /** Handle schema validation errors (SQL/OpenAPI). */
    @ExceptionHandler(SchemaValidationException.class)
    public ResponseEntity<ProblemDetail> handleSchemaValidationException(
            SchemaValidationException ex, HttpServletRequest request) {
        log.debug("Schema validation failed: {}", ex.getMessage());

        ProblemDetail problem =
                ProblemDetail.builder()
                        .type(URI.create("urn:apigen:problem:schema-validation-error"))
                        .title("Schema Validation Failed")
                        .status(HttpStatus.BAD_REQUEST.value())
                        .detail(ex.getMessage())
                        .instance(request.getRequestURI())
                        .extension("schemaType", ex.getSchemaType().name())
                        .extension("errors", ex.getValidationErrors())
                        .build();

        return ResponseEntity.badRequest().contentType(APPLICATION_PROBLEM_JSON).body(problem);
    }

    /** Handle project generation errors. */
    @ExceptionHandler(ProjectGenerationException.class)
    public ResponseEntity<ProblemDetail> handleProjectGenerationException(
            ProjectGenerationException ex, HttpServletRequest request) {
        log.error("Project generation failed: {}", ex.getMessage(), ex);

        ProblemDetail.Builder builder =
                ProblemDetail.builder()
                        .type(URI.create("urn:apigen:problem:generation-error"))
                        .title("Project Generation Failed")
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .detail(ex.getMessage())
                        .instance(request.getRequestURI());

        if (ex.getProjectName() != null) {
            builder.extension("projectName", ex.getProjectName());
        }
        if (ex.getGeneratorType() != null) {
            builder.extension("generatorType", ex.getGeneratorType());
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(builder.build());
    }

    /** Handle external service errors. */
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ProblemDetail> handleExternalServiceException(
            ExternalServiceException ex, HttpServletRequest request) {
        log.error("External service error: {}", ex.getMessage(), ex);

        ProblemDetail.Builder builder =
                ProblemDetail.builder()
                        .type(URI.create("urn:apigen:problem:external-service-error"))
                        .title("External Service Error")
                        .status(HttpStatus.BAD_GATEWAY.value())
                        .detail(ex.getMessage())
                        .instance(request.getRequestURI())
                        .extension("service", ex.getServiceName());

        if (ex.getOriginalError() != null) {
            builder.extension("originalError", ex.getOriginalError());
        }

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(builder.build());
    }

    /** Handle validation errors from @Valid annotations. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        log.warn("Validation failed: {}", fieldErrors);

        ProblemDetail problem =
                ProblemDetail.builder()
                        .type(URI.create("urn:apigen:problem:validation-error"))
                        .title("Validation Failed")
                        .status(HttpStatus.BAD_REQUEST.value())
                        .detail(
                                String.format(
                                        "Request contains %d validation error(s)",
                                        fieldErrors.size()))
                        .instance(request.getRequestURI())
                        .extension("fieldErrors", fieldErrors)
                        .build();

        return ResponseEntity.badRequest().contentType(APPLICATION_PROBLEM_JSON).body(problem);
    }

    /** Handle core ValidationException. */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(
            ValidationException ex, HttpServletRequest request) {
        log.debug("Validation error: {}", ex.getMessage());

        ProblemDetail problem =
                ProblemDetail.builder()
                        .type(URI.create("urn:apigen:problem:validation-error"))
                        .title("Validation Error")
                        .status(HttpStatus.BAD_REQUEST.value())
                        .detail(ex.getMessage())
                        .instance(request.getRequestURI())
                        .build();

        return ResponseEntity.badRequest().contentType(APPLICATION_PROBLEM_JSON).body(problem);
    }

    /** Handle operation failed errors. */
    @ExceptionHandler(OperationFailedException.class)
    public ResponseEntity<ProblemDetail> handleOperationFailedException(
            OperationFailedException ex, HttpServletRequest request) {
        log.error("Operation failed: {}", ex.getMessage(), ex);

        ProblemDetail problem =
                ProblemDetail.builder()
                        .type(URI.create("urn:apigen:problem:operation-failed"))
                        .title("Operation Failed")
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .detail(ex.getMessage())
                        .instance(request.getRequestURI())
                        .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /** Handle IllegalArgumentException. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.debug("Invalid argument: {}", ex.getMessage());

        ProblemDetail problem =
                ProblemDetail.builder()
                        .type(URI.create("urn:apigen:problem:invalid-argument"))
                        .title("Invalid Argument")
                        .status(HttpStatus.BAD_REQUEST.value())
                        .detail(ex.getMessage())
                        .instance(request.getRequestURI())
                        .build();

        return ResponseEntity.badRequest().contentType(APPLICATION_PROBLEM_JSON).body(problem);
    }

    /** Handle all other unexpected errors. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericError(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        ProblemDetail problem =
                ProblemDetail.builder()
                        .type(URI.create("urn:apigen:problem:internal-error"))
                        .title("Internal Server Error")
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .detail("An unexpected error occurred")
                        .instance(request.getRequestURI())
                        .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
