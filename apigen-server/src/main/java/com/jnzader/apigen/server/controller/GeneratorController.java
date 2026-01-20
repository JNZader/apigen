package com.jnzader.apigen.server.controller;

import com.jnzader.apigen.server.dto.GenerateRequest;
import com.jnzader.apigen.server.dto.GenerateResponse;
import com.jnzader.apigen.server.service.GeneratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST Controller for code generation endpoints. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class GeneratorController {

    private final GeneratorService generatorService;

    /**
     * Generates a Spring Boot project from SQL schema. Returns a ZIP file containing the complete
     * project structure.
     *
     * @param request The generation request with SQL and project config
     * @return ZIP file as byte array
     */
    @PostMapping("/generate")
    public ResponseEntity<byte[]> generate(@Valid @RequestBody GenerateRequest request) {
        log.info(
                "Received generation request for project: {}",
                request.getProject().getArtifactId());

        try {
            byte[] zipBytes = generatorService.generateProject(request);

            String filename = request.getProject().getArtifactId() + ".zip";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(zipBytes.length);

            log.info("Returning ZIP file: {} ({} bytes)", filename, zipBytes.length);

            return ResponseEntity.ok().headers(headers).body(zipBytes);

        } catch (Exception e) {
            log.error("Generation failed", e);
            throw new GenerationException("Failed to generate project: " + e.getMessage(), e);
        }
    }

    /**
     * Validates a generation request without generating code. Useful for checking SQL syntax before
     * full generation.
     *
     * @param request The generation request to validate
     * @return Validation result with any errors
     */
    @PostMapping("/validate")
    public ResponseEntity<GenerateResponse> validate(@Valid @RequestBody GenerateRequest request) {
        log.info("Validating SQL schema for project: {}", request.getProject().getArtifactId());

        GenerateResponse response = generatorService.validate(request);

        return ResponseEntity.ok(response);
    }

    /** Health check endpoint. */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse("ok", "APiGen Server is running"));
    }

    /** Simple health response record. */
    public record HealthResponse(String status, String message) {}

    /** Error response record. */
    public record ErrorResponse(String error, String message) {}

    /** Handles GenerationException and returns proper HTTP 500 response. */
    @ExceptionHandler(GenerationException.class)
    public ResponseEntity<ErrorResponse> handleGenerationException(GenerationException e) {
        log.error("Generation error: {}", e.getMessage());
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse("GENERATION_ERROR", e.getMessage()));
    }

    /** Exception for generation errors. */
    public static class GenerationException extends RuntimeException {
        public GenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
