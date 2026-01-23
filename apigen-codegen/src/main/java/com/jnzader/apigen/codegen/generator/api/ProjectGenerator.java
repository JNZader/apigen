package com.jnzader.apigen.codegen.generator.api;

import com.jnzader.apigen.codegen.model.SqlSchema;
import java.util.Map;
import java.util.Set;

/**
 * Main interface for project generators.
 *
 * <p>Implementations of this interface generate complete project code from SQL schemas for specific
 * language/framework combinations (e.g., Java/Spring Boot, Python/FastAPI, TypeScript/NestJS).
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ProjectGenerator generator = registry.getGenerator("java", "spring-boot").orElseThrow();
 * Map<String, String> files = generator.generate(schema, config);
 * files.forEach((path, content) -> writeFile(outputDir.resolve(path), content));
 * }</pre>
 */
public interface ProjectGenerator {

    /**
     * Returns the target language identifier.
     *
     * @return language identifier (e.g., "java", "kotlin", "python", "typescript")
     */
    String getLanguage();

    /**
     * Returns the target framework identifier.
     *
     * @return framework identifier (e.g., "spring-boot", "fastapi", "nestjs")
     */
    String getFramework();

    /**
     * Returns a human-readable display name for this generator.
     *
     * @return display name (e.g., "Java / Spring Boot 3.x")
     */
    String getDisplayName();

    /**
     * Returns the set of features supported by this generator.
     *
     * @return set of supported features
     */
    Set<Feature> getSupportedFeatures();

    /**
     * Returns the type mapper for this language.
     *
     * @return the language type mapper
     */
    LanguageTypeMapper getTypeMapper();

    /**
     * Generates project code from the given SQL schema.
     *
     * @param schema the parsed SQL schema
     * @param config the project configuration
     * @return map of relative file paths to file contents
     */
    Map<String, String> generate(SqlSchema schema, ProjectConfig config);

    /**
     * Validates that the configuration is compatible with this generator.
     *
     * @param config the project configuration
     * @return list of validation errors (empty if valid)
     */
    default java.util.List<String> validateConfig(ProjectConfig config) {
        return java.util.List.of();
    }

    /**
     * Returns the default language version for this generator.
     *
     * @return default language version (e.g., "21" for Java)
     */
    default String getDefaultLanguageVersion() {
        return null;
    }

    /**
     * Returns the default framework version for this generator.
     *
     * @return default framework version (e.g., "3.2.0" for Spring Boot)
     */
    default String getDefaultFrameworkVersion() {
        return null;
    }

    /**
     * Checks if this generator supports a specific feature.
     *
     * @param feature the feature to check
     * @return true if the feature is supported
     */
    default boolean supports(Feature feature) {
        return getSupportedFeatures().contains(feature);
    }
}
