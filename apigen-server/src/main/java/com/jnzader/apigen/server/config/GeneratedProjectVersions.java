package com.jnzader.apigen.server.config;

import java.util.List;

/**
 * Centralized configuration for versions used in generated projects. Change values here to update
 * all generated project dependencies.
 *
 * <p>These versions are used when generating new Spring Boot projects and their build.gradle files.
 */
public final class GeneratedProjectVersions {

    private GeneratedProjectVersions() {
        // Utility class
    }

    // ==========================================================================
    // APiGen Module Versions (auto-updated by semantic-release)
    // ==========================================================================

    /** APiGen Core version for generated projects (JitPack uses git tags with 'v' prefix). */
    public static final String APIGEN_CORE_VERSION = "v3.0.7";

    /** APiGen Security version for generated projects (JitPack uses git tags with 'v' prefix). */
    public static final String APIGEN_SECURITY_VERSION = "v3.0.7";

    /**
     * Fallback versions to try if the primary version is not available. Used by integration tests
     * to handle JitPack build delays.
     */
    public static final List<String> FALLBACK_VERSIONS = List.of("v3.0.2", "v3.0.1", "v3.0.0");

    // ==========================================================================
    // Repository Configuration
    // ==========================================================================

    /** JitPack group ID for Maven coordinates (no auth required). */
    public static final String JITPACK_GROUP_ID = "com.github.jnzader.apigen";

    /** GitHub Packages Maven repository URL (requires authentication). */
    public static final String GITHUB_PACKAGES_URL = "https://maven.pkg.github.com/JNZader/apigen";

    /** APiGen group ID for GitHub Packages. */
    public static final String APIGEN_GROUP_ID = "com.jnzader";

    // ==========================================================================
    // Core Platform Versions
    // ==========================================================================

    /** Default initial version for generated projects. */
    public static final String INITIAL_PROJECT_VERSION = "0.0.1-SNAPSHOT";

    /** Default Java version for generated projects. */
    public static final String JAVA_VERSION = "25";

    /** Default Gradle version for generated projects. */
    public static final String GRADLE_VERSION = "9.3.0";

    /** Default Spring Boot version for generated projects. */
    public static final String SPRING_BOOT_VERSION = "4.0.0";

    /** Spring Dependency Management plugin version. */
    public static final String SPRING_DEPENDENCY_MANAGEMENT_VERSION = "1.1.7";

    // ==========================================================================
    // Language Versions
    // ==========================================================================

    /** Kotlin version for generated Kotlin projects. */
    public static final String KOTLIN_VERSION = "2.3.0";

    // ==========================================================================
    // Library Versions
    // ==========================================================================

    /** SpringDoc OpenAPI version for Swagger UI (3.x for Spring Boot 4.x). */
    public static final String SPRINGDOC_VERSION = "3.0.1";

    /** MapStruct version for DTO mapping. */
    public static final String MAPSTRUCT_VERSION = "1.6.3";

    /** Lombok-MapStruct binding version. */
    public static final String LOMBOK_MAPSTRUCT_BINDING_VERSION = "0.2.0";

    // ==========================================================================
    // Feature Pack Library Versions
    // ==========================================================================

    /** AWS SDK v2 version for S3 file storage. */
    public static final String AWS_SDK_VERSION = "2.31.47";

    /** Azure Storage Blob version for Azure file storage. */
    public static final String AZURE_STORAGE_BLOB_VERSION = "12.31.0";

    /** jte template engine version. */
    public static final String JTE_VERSION = "3.1.17";

    /** Swagger Parser version for OpenAPI import. */
    public static final String SWAGGER_PARSER_VERSION = "2.1.30";

    // ==========================================================================
    // Testing Library Versions
    // ==========================================================================

    /** PIT Mutation Testing plugin version. */
    public static final String PITEST_PLUGIN_VERSION = "1.17.1";

    /** PIT JUnit 5 plugin version (must match pitest-junit5-plugin). */
    public static final String PITEST_JUNIT5_VERSION = "1.2.1";

    /** JaCoCo plugin version for code coverage. */
    public static final String JACOCO_VERSION = "0.8.13";

    // ==========================================================================
    // Database Driver Versions (optional, for reference)
    // ==========================================================================

    /** PostgreSQL Docker image tag. */
    public static final String POSTGRES_DOCKER_IMAGE = "postgres:16-alpine";

    /** MySQL Docker image tag. */
    public static final String MYSQL_DOCKER_IMAGE = "mysql:8.0";

    /** MariaDB Docker image tag. */
    public static final String MARIADB_DOCKER_IMAGE = "mariadb:11";

    /** SQL Server Docker image tag. */
    public static final String SQLSERVER_DOCKER_IMAGE =
            "mcr.microsoft.com/mssql/server:2022-latest";

    /** Oracle Docker image tag. */
    public static final String ORACLE_DOCKER_IMAGE = "gvenzl/oracle-xe:21-slim";

    // ==========================================================================
    // Docker Configuration
    // ==========================================================================

    /**
     * Eclipse Temurin JRE base image pattern. Use with String.format(TEMURIN_JRE_IMAGE_PATTERN,
     * javaVersion)
     */
    public static final String TEMURIN_JRE_IMAGE_PATTERN = "eclipse-temurin:%s-jre-alpine";

    /** Gets the Temurin JRE image for the specified Java version. */
    public static String getTemurinJreImage(String javaVersion) {
        return String.format(TEMURIN_JRE_IMAGE_PATTERN, javaVersion);
    }
}
