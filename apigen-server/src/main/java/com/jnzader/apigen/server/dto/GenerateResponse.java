package com.jnzader.apigen.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for project generation.
 * Contains information about the generation result.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateResponse {

    /**
     * Whether the generation was successful
     */
    private boolean success;

    /**
     * Human-readable message
     */
    private String message;

    /**
     * List of generated files
     */
    @Builder.Default
    private List<String> generatedFiles = new ArrayList<>();

    /**
     * List of warnings during generation
     */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    /**
     * List of errors if generation failed
     */
    @Builder.Default
    private List<String> errors = new ArrayList<>();

    /**
     * Statistics about the generation
     */
    private GenerationStats stats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerationStats {
        private int tablesProcessed;
        private int entitiesGenerated;
        private int filesGenerated;
        private long generationTimeMs;
    }

    /**
     * Create a success response
     */
    public static GenerateResponse success(String message, List<String> files, GenerationStats stats) {
        return GenerateResponse.builder()
                .success(true)
                .message(message)
                .generatedFiles(files)
                .stats(stats)
                .build();
    }

    /**
     * Create an error response
     */
    public static GenerateResponse error(String message, List<String> errors) {
        return GenerateResponse.builder()
                .success(false)
                .message(message)
                .errors(errors)
                .build();
    }
}
