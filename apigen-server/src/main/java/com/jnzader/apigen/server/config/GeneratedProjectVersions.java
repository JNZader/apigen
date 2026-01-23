package com.jnzader.apigen.server.config;

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
    // APiGen Module Versions
    // ==========================================================================

    /** APiGen Core version for generated projects (from JitPack). */
    public static final String APIGEN_CORE_VERSION = "v2.5.2";

    /** APiGen Security version for generated projects (from JitPack). */
    public static final String APIGEN_SECURITY_VERSION = "v2.5.2";

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
    // Library Versions
    // ==========================================================================

    /** SpringDoc OpenAPI version for Swagger UI. */
    public static final String SPRINGDOC_VERSION = "2.8.9";

    /** MapStruct version for DTO mapping. */
    public static final String MAPSTRUCT_VERSION = "1.6.3";

    /** Lombok-MapStruct binding version. */
    public static final String LOMBOK_MAPSTRUCT_BINDING_VERSION = "0.2.0";

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
