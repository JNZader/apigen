package com.jnzader.apigen.codegen.generator.api;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

/**
 * Configuration for project generation.
 *
 * <p>This class encapsulates all settings needed to generate a project, independent of the target
 * language or framework.
 */
@Data
@Builder
public class ProjectConfig {

    /** The base package/namespace for generated code (e.g., "com.example.myapp") */
    private String basePackage;

    /** The project/module name */
    private String projectName;

    /** The artifact identifier (e.g., "my-api") */
    private String artifactId;

    /** The group identifier (e.g., "com.example") */
    private String groupId;

    /** The output directory for generated files */
    private Path outputDirectory;

    /** The target language version (e.g., "21" for Java 21) */
    private String languageVersion;

    /** The target framework version (e.g., "3.2.0" for Spring Boot) */
    private String frameworkVersion;

    /** Features to enable in the generated project */
    @Builder.Default private Set<Feature> enabledFeatures = Set.of();

    /** Additional configuration options specific to the generator */
    @Builder.Default private Map<String, Object> options = Map.of();

    /**
     * Database configuration for migrations and repository generation.
     *
     * <p>This is optional; if not set, generators may use defaults.
     */
    private DatabaseConfig database;

    /** Checks if a specific feature is enabled. */
    public boolean isFeatureEnabled(Feature feature) {
        return enabledFeatures != null && enabledFeatures.contains(feature);
    }

    /** Gets an option value with a default. */
    @SuppressWarnings("unchecked")
    public <T> T getOption(String key, T defaultValue) {
        if (options == null) {
            return defaultValue;
        }
        Object value = options.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /** Database configuration. */
    @Data
    @Builder
    public static class DatabaseConfig {
        private String type;
        private String name;
        private String username;
        private String password;
        private Integer port;
    }
}
