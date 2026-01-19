package com.jnzader.apigen.server.config;

import com.jnzader.apigen.server.controller.GeneratorController;
import com.jnzader.apigen.server.dto.GenerateResponse;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Global exception handler for the APiGen Server. */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** Handle validation errors from @Valid annotations. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenerateResponse> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        List<String> errors = new ArrayList<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.add(error.getField() + ": " + error.getDefaultMessage());
        }

        log.warn("Validation failed: {}", errors);

        return ResponseEntity.badRequest()
                .body(GenerateResponse.error("Validation failed", errors));
    }

    /** Handle generation errors. */
    @ExceptionHandler(GeneratorController.GenerationException.class)
    public ResponseEntity<GenerateResponse> handleGenerationError(
            GeneratorController.GenerationException ex) {
        log.error("Generation error", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(GenerateResponse.error("Generation failed", List.of(ex.getMessage())));
    }

    /** Handle all other unexpected errors. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenerateResponse> handleGenericError(Exception ex) {
        log.error("Unexpected error", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        GenerateResponse.error(
                                "An unexpected error occurred",
                                List.of(ex.getClass().getSimpleName() + ": " + ex.getMessage())));
    }
}
