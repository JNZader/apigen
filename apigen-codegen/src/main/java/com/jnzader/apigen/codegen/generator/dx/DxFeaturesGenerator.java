package com.jnzader.apigen.codegen.generator.dx;

import com.jnzader.apigen.codegen.generator.api.Feature;
import com.jnzader.apigen.codegen.generator.api.ProjectConfig;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aggregator for all Developer Experience (DX) feature generators.
 *
 * <p>This class provides a single entry point for generating all DX-related files:
 *
 * <ul>
 *   <li>{@link Feature#MISE_TASKS} - mise.toml task runner
 *   <li>{@link Feature#PRE_COMMIT} - .pre-commit-config.yaml hooks
 *   <li>{@link Feature#SETUP_SCRIPT} - scripts/setup.sh and scripts/setup.ps1
 *   <li>{@link Feature#GITHUB_TEMPLATES} - .github/ PR and Issue templates
 *   <li>{@link Feature#DEV_COMPOSE} - Enhanced docker-compose.yml
 * </ul>
 *
 * @since 2.19.0
 */
public class DxFeaturesGenerator {

    private static final String DEFAULT_DB_USER = "app";
    private static final String DEFAULT_DB_PASSWORD = "secret";
    private static final String DEFAULT_DB_NAME = "appdb";
    private static final String APIGEN_VERSION = "2.19.0";

    private final String projectName;
    private final String dbUser;
    private final String dbPassword;
    private final String dbName;

    /**
     * Creates a new DX features generator.
     *
     * @param projectName the name of the project (used in Docker images, containers, etc.)
     */
    public DxFeaturesGenerator(String projectName) {
        this(projectName, DEFAULT_DB_USER, DEFAULT_DB_PASSWORD, DEFAULT_DB_NAME);
    }

    /**
     * Creates a new DX features generator with custom database credentials.
     *
     * @param projectName the name of the project
     * @param dbUser database username
     * @param dbPassword database password
     * @param dbName database name
     */
    public DxFeaturesGenerator(
            String projectName, String dbUser, String dbPassword, String dbName) {
        this.projectName = projectName;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.dbName = dbName;
    }

    /**
     * Generates all enabled DX feature files based on project configuration.
     *
     * @param config the project configuration with enabled features
     * @param language the target language/framework
     * @return map of file paths to file contents
     */
    public Map<String, String> generate(ProjectConfig config, DxLanguage language) {
        Map<String, String> files = new LinkedHashMap<>();

        // MISE_TASKS - mise.toml task runner
        if (config.isFeatureEnabled(Feature.MISE_TASKS)) {
            MiseGenerator miseGenerator = new MiseGenerator(projectName, dbUser, dbName);
            files.putAll(miseGenerator.generate(language));
        }

        // PRE_COMMIT - .pre-commit-config.yaml
        if (config.isFeatureEnabled(Feature.PRE_COMMIT)) {
            PreCommitGenerator preCommitGenerator = new PreCommitGenerator();
            files.putAll(preCommitGenerator.generate(language));
        }

        // SETUP_SCRIPT - scripts/setup.sh and scripts/setup.ps1
        if (config.isFeatureEnabled(Feature.SETUP_SCRIPT)) {
            SetupScriptGenerator setupGenerator =
                    new SetupScriptGenerator(projectName, dbUser, dbName, APIGEN_VERSION);
            files.putAll(setupGenerator.generate(language));
        }

        // GITHUB_TEMPLATES - .github/ templates
        if (config.isFeatureEnabled(Feature.GITHUB_TEMPLATES)) {
            GitHubTemplatesGenerator githubGenerator = new GitHubTemplatesGenerator();
            files.putAll(githubGenerator.generate());
        }

        // DEV_COMPOSE - Enhanced docker-compose.yml
        if (config.isFeatureEnabled(Feature.DEV_COMPOSE)) {
            DevComposeGenerator composeGenerator =
                    new DevComposeGenerator(projectName, dbUser, dbPassword, dbName);
            files.putAll(composeGenerator.generate(language));
        }

        return files;
    }

    /**
     * Generates all DX feature files (ignoring config, enables all features).
     *
     * @param language the target language/framework
     * @return map of file paths to file contents
     */
    public Map<String, String> generateAll(DxLanguage language) {
        Map<String, String> files = new LinkedHashMap<>();

        // mise.toml
        MiseGenerator miseGenerator = new MiseGenerator(projectName, dbUser, dbName);
        files.putAll(miseGenerator.generate(language));

        // .pre-commit-config.yaml
        PreCommitGenerator preCommitGenerator = new PreCommitGenerator();
        files.putAll(preCommitGenerator.generate(language));

        // scripts/setup.sh and scripts/setup.ps1
        SetupScriptGenerator setupGenerator =
                new SetupScriptGenerator(projectName, dbUser, dbName, APIGEN_VERSION);
        files.putAll(setupGenerator.generate(language));

        // .github/ templates
        GitHubTemplatesGenerator githubGenerator = new GitHubTemplatesGenerator();
        files.putAll(githubGenerator.generate());

        // docker-compose.yml
        DevComposeGenerator composeGenerator =
                new DevComposeGenerator(projectName, dbUser, dbPassword, dbName);
        files.putAll(composeGenerator.generate(language));

        return files;
    }

    /**
     * Checks if any DX feature is enabled in the configuration.
     *
     * @param config the project configuration
     * @return true if at least one DX feature is enabled
     */
    public static boolean hasAnyDxFeature(ProjectConfig config) {
        return config.isFeatureEnabled(Feature.MISE_TASKS)
                || config.isFeatureEnabled(Feature.PRE_COMMIT)
                || config.isFeatureEnabled(Feature.SETUP_SCRIPT)
                || config.isFeatureEnabled(Feature.GITHUB_TEMPLATES)
                || config.isFeatureEnabled(Feature.DEV_COMPOSE);
    }

    /**
     * Resolves DxLanguage from project generator language and framework identifiers.
     *
     * @param language the language identifier (e.g., "java", "python")
     * @param framework the framework identifier (e.g., "spring-boot", "fastapi")
     * @return the corresponding DxLanguage
     */
    public static DxLanguage resolveDxLanguage(String language, String framework) {
        return DxLanguage.fromLanguageAndFramework(language, framework);
    }
}
