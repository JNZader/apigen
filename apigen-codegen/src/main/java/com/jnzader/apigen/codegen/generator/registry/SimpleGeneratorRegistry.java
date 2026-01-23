package com.jnzader.apigen.codegen.generator.registry;

import com.jnzader.apigen.codegen.generator.api.Feature;
import com.jnzader.apigen.codegen.generator.api.ProjectGenerator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Simple in-memory implementation of {@link GeneratorRegistry}.
 *
 * <p>This implementation is suitable for CLI tools and standalone applications where generators are
 * registered manually. For Spring-based applications, consider using {@code
 * SpringGeneratorRegistry} which provides auto-discovery.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * SimpleGeneratorRegistry registry = new SimpleGeneratorRegistry();
 * registry.register(new JavaSpringBootProjectGenerator());
 * registry.register(new PythonFastApiProjectGenerator());
 *
 * // Get specific generator
 * ProjectGenerator javaGen = registry.getGenerator("java", "spring-boot").orElseThrow();
 *
 * // Get all generators
 * List<ProjectGenerator> all = registry.getAllGenerators();
 * }</pre>
 *
 * <p>Thread-safety: This implementation is NOT thread-safe. For concurrent access, use external
 * synchronization or a thread-safe variant.
 */
public class SimpleGeneratorRegistry implements GeneratorRegistry {

    /** Map of language:framework -> generator. LinkedHashMap preserves insertion order. */
    private final Map<String, ProjectGenerator> generators = new LinkedHashMap<>();

    /** Map of language -> first registered generator (default). */
    private final Map<String, ProjectGenerator> defaultGenerators = new LinkedHashMap<>();

    @Override
    public Optional<ProjectGenerator> getGenerator(String language, String framework) {
        if (language == null || framework == null) {
            return Optional.empty();
        }
        String key = buildKey(language, framework);
        return Optional.ofNullable(generators.get(key));
    }

    @Override
    public Optional<ProjectGenerator> getDefaultGenerator(String language) {
        if (language == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(defaultGenerators.get(normalizeKey(language)));
    }

    @Override
    public List<ProjectGenerator> getAllGenerators() {
        return new ArrayList<>(generators.values());
    }

    @Override
    public List<ProjectGenerator> getGeneratorsByFeature(Feature feature) {
        return generators.values().stream()
                .filter(gen -> gen.supports(feature))
                .collect(Collectors.toList());
    }

    @Override
    public List<ProjectGenerator> getGeneratorsByLanguage(String language) {
        if (language == null) {
            return List.of();
        }
        String normalizedLang = normalizeKey(language);
        return generators.values().stream()
                .filter(gen -> normalizeKey(gen.getLanguage()).equals(normalizedLang))
                .collect(Collectors.toList());
    }

    @Override
    public void register(ProjectGenerator generator) {
        if (generator == null) {
            throw new IllegalArgumentException("Generator cannot be null");
        }

        String language = generator.getLanguage();
        String framework = generator.getFramework();

        if (language == null || language.isBlank()) {
            throw new IllegalArgumentException("Generator language cannot be null or blank");
        }
        if (framework == null || framework.isBlank()) {
            throw new IllegalArgumentException("Generator framework cannot be null or blank");
        }

        String key = buildKey(language, framework);
        generators.put(key, generator);

        // Set as default for language if first registered
        String langKey = normalizeKey(language);
        defaultGenerators.putIfAbsent(langKey, generator);
    }

    @Override
    public Set<String> getSupportedLanguages() {
        return generators.values().stream()
                .map(ProjectGenerator::getLanguage)
                .map(this::normalizeKey)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getSupportedFrameworks(String language) {
        if (language == null) {
            return Set.of();
        }
        String normalizedLang = normalizeKey(language);
        return generators.values().stream()
                .filter(gen -> normalizeKey(gen.getLanguage()).equals(normalizedLang))
                .map(ProjectGenerator::getFramework)
                .collect(Collectors.toSet());
    }

    /**
     * Builds the lookup key for a language/framework combination.
     *
     * @param language the language
     * @param framework the framework
     * @return the normalized key
     */
    private String buildKey(String language, String framework) {
        return normalizeKey(language) + ":" + normalizeKey(framework);
    }

    /**
     * Normalizes a key component to lowercase.
     *
     * @param key the key to normalize
     * @return the normalized key
     */
    private String normalizeKey(String key) {
        return key.toLowerCase(Locale.ROOT);
    }

    /**
     * Clears all registered generators.
     *
     * <p>Useful for testing.
     */
    public void clear() {
        generators.clear();
        defaultGenerators.clear();
    }

    /**
     * Gets the number of registered generators.
     *
     * @return the count
     */
    public int size() {
        return generators.size();
    }
}
