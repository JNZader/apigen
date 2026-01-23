package com.jnzader.apigen.codegen.generator.registry;

import com.jnzader.apigen.codegen.generator.api.Feature;
import com.jnzader.apigen.codegen.generator.api.ProjectGenerator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Registry for project generators.
 *
 * <p>Provides lookup and registration of {@link ProjectGenerator} implementations. Implementations
 * may use different discovery mechanisms:
 *
 * <ul>
 *   <li>{@link SimpleGeneratorRegistry} - Manual registration, suitable for CLI tools
 *   <li>{@code SpringGeneratorRegistry} - Auto-discovery via Spring context, for web servers
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * GeneratorRegistry registry = new SimpleGeneratorRegistry();
 * registry.register(new JavaSpringBootProjectGenerator());
 *
 * ProjectGenerator generator = registry.getGenerator("java", "spring-boot").orElseThrow();
 * }</pre>
 */
public interface GeneratorRegistry {

    /**
     * Gets a generator for the specified language and framework.
     *
     * @param language the target language (e.g., "java", "python")
     * @param framework the target framework (e.g., "spring-boot", "fastapi")
     * @return the generator, or empty if not found
     */
    Optional<ProjectGenerator> getGenerator(String language, String framework);

    /**
     * Gets the default generator for a language.
     *
     * <p>If multiple generators exist for a language, returns the first one registered.
     *
     * @param language the target language
     * @return the default generator for the language, or empty if none registered
     */
    Optional<ProjectGenerator> getDefaultGenerator(String language);

    /**
     * Gets all registered generators.
     *
     * @return list of all generators
     */
    List<ProjectGenerator> getAllGenerators();

    /**
     * Gets all generators that support a specific feature.
     *
     * @param feature the required feature
     * @return list of generators supporting the feature
     */
    List<ProjectGenerator> getGeneratorsByFeature(Feature feature);

    /**
     * Gets all generators for a specific language.
     *
     * @param language the target language
     * @return list of generators for the language
     */
    List<ProjectGenerator> getGeneratorsByLanguage(String language);

    /**
     * Registers a generator.
     *
     * <p>If a generator for the same language/framework combination already exists, it will be
     * replaced.
     *
     * @param generator the generator to register
     */
    void register(ProjectGenerator generator);

    /**
     * Checks if a generator exists for the specified language and framework.
     *
     * @param language the target language
     * @param framework the target framework
     * @return true if a generator is registered
     */
    default boolean hasGenerator(String language, String framework) {
        return getGenerator(language, framework).isPresent();
    }

    /**
     * Gets all supported languages.
     *
     * @return set of supported language identifiers
     */
    Set<String> getSupportedLanguages();

    /**
     * Gets all supported frameworks for a language.
     *
     * @param language the target language
     * @return set of supported framework identifiers
     */
    Set<String> getSupportedFrameworks(String language);
}
