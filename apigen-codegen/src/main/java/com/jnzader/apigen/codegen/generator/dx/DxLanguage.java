package com.jnzader.apigen.codegen.generator.dx;

/**
 * Enum representing languages/frameworks for Developer Experience file generation.
 *
 * <p>Each language has specific tool configurations for mise, pre-commit, and setup scripts.
 */
public enum DxLanguage {
    JAVA_SPRING("java", "spring-boot", "25", "./gradlew", "spotlessCheck", "spotlessApply"),
    KOTLIN_SPRING("kotlin", "spring-boot", "25", "./gradlew", "spotlessCheck", "spotlessApply"),
    CSHARP_ASPNET("csharp", "aspnet-core", "8.0", "dotnet", "format --verify-no-changes", "format"),
    PYTHON_FASTAPI(
            "python", "fastapi", "3.12", "python", "ruff check . && mypy app", "ruff format ."),
    TYPESCRIPT_NESTJS("typescript", "nestjs", "20", "npm", "npx biome check .", "npx biome check --write ."),
    PHP_LARAVEL("php", "laravel", "8.4", "php", "vendor/bin/pint --test", "vendor/bin/pint"),
    GO_GIN("go", "gin", "1.23", "go", "golangci-lint run", "gofmt -w ."),
    GO_CHI("go", "chi", "1.23", "go", "golangci-lint run", "gofmt -w ."),
    RUST_AXUM(
            "rust",
            "axum",
            "1.85",
            "cargo",
            "cargo fmt --check && cargo clippy -- -D warnings",
            "cargo fmt");

    private final String language;
    private final String framework;
    private final String languageVersion;
    private final String buildTool;
    private final String lintCommand;
    private final String formatCommand;

    DxLanguage(
            String language,
            String framework,
            String languageVersion,
            String buildTool,
            String lintCommand,
            String formatCommand) {
        this.language = language;
        this.framework = framework;
        this.languageVersion = languageVersion;
        this.buildTool = buildTool;
        this.lintCommand = lintCommand;
        this.formatCommand = formatCommand;
    }

    public String getLanguage() {
        return language;
    }

    public String getFramework() {
        return framework;
    }

    public String getLanguageVersion() {
        return languageVersion;
    }

    public String getBuildTool() {
        return buildTool;
    }

    public String getLintCommand() {
        return lintCommand;
    }

    public String getFormatCommand() {
        return formatCommand;
    }

    /**
     * Finds a DxLanguage by language and framework identifiers.
     *
     * @param language the language identifier (e.g., "java", "python")
     * @param framework the framework identifier (e.g., "spring-boot", "fastapi")
     * @return the matching DxLanguage or JAVA_SPRING as default
     */
    public static DxLanguage fromLanguageAndFramework(String language, String framework) {
        for (DxLanguage dx : values()) {
            if (dx.language.equalsIgnoreCase(language)
                    && dx.framework.equalsIgnoreCase(framework)) {
                return dx;
            }
        }
        // Default to Java/Spring Boot if not found
        return JAVA_SPRING;
    }
}
