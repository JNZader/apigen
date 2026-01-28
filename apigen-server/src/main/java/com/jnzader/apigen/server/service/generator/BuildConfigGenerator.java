package com.jnzader.apigen.server.service.generator;

import com.jnzader.apigen.server.config.GeneratedProjectVersions;
import com.jnzader.apigen.server.dto.GenerateRequest;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Generates build configuration files (build.gradle, settings.gradle). */
@Component
@SuppressWarnings("java:S3457") // Uses \n for Unix line endings in generated build files
public class BuildConfigGenerator {

    private static final String PRODUCTION_DB_COMMENT = " // Production database\n";

    /**
     * Generates the build.gradle file content.
     *
     * @param config the project configuration
     * @return the build.gradle content
     */
    public String generateBuildGradle(GenerateRequest.ProjectConfig config) {
        String springBootVersion =
                config.getSpringBootVersion() != null
                        ? config.getSpringBootVersion()
                        : GeneratedProjectVersions.SPRING_BOOT_VERSION;
        String javaVersion =
                config.getJavaVersion() != null
                        ? config.getJavaVersion()
                        : GeneratedProjectVersions.JAVA_VERSION;
        String groupId = config.getGroupId() != null ? config.getGroupId() : "com.example";

        StringBuilder deps = new StringBuilder();

        // Core dependencies always included (via JitPack)
        deps.append("    // APiGen Core (from JitPack - no auth required)\n");
        deps.append(
                "    implementation '%s:apigen-core:%s'\n"
                        .formatted(
                                GeneratedProjectVersions.JITPACK_GROUP_ID,
                                GeneratedProjectVersions.APIGEN_CORE_VERSION));

        if (config.getModules() != null && config.getModules().isSecurity()) {
            deps.append("\n    // APiGen Security (from JitPack)\n");
            deps.append(
                    "    implementation '%s:apigen-security:%s'\n"
                            .formatted(
                                    GeneratedProjectVersions.JITPACK_GROUP_ID,
                                    GeneratedProjectVersions.APIGEN_SECURITY_VERSION));
        }

        // Feature Pack dependencies
        deps.append(generateFeaturePackDependencies(config));

        String dbDeps = generateDatabaseDependencies(config);

        return
"""
plugins {
    id 'java'
    id 'jacoco'
    id 'info.solidsoft.pitest' version '%s'
    id 'org.springframework.boot' version '%s'
    id 'io.spring.dependency-management' version '%s'
}

group = '%s'
version = '%s'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(%s)
    }
}

repositories {
    mavenCentral()
    // JitPack - no authentication required
    maven { url 'https://jitpack.io' }
    // GitHub Packages - requires authentication (optional, faster builds)
    // maven {
    //     url 'https://maven.pkg.github.com/JNZader/apigen'
    //     credentials {
    //         username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
    //         password = project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
    //     }
    // }
}

dependencies {
%s
    // Spring Boot Starters
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-hateoas'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-cache'

    // OpenAPI / Swagger
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:%s'

    // Database
%s

    // MapStruct
    implementation 'org.mapstruct:mapstruct:%s'
    annotationProcessor 'org.mapstruct:mapstruct-processor:%s'

    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:%s'

    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-jackson-test'
}

test {
    useJUnitPlatform()
    finalizedBy jacocoTestReport
}

// ==========================================================================
// Code Coverage (JaCoCo)
// ==========================================================================
jacoco {
    toolVersion = '%s'
}

jacocoTestReport {
    dependsOn test
    reports {
        xml.required = true
        html.required = true
    }
}

jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = 0.80 // 80%% minimum coverage
            }
        }
    }
}

// ==========================================================================
// Mutation Testing (PIT)
// ==========================================================================
pitest {
    junit5PluginVersion = '%s'
    targetClasses = ['%s.**']
    targetTests = ['%s.**']
    threads = 4
    outputFormats = ['HTML', 'XML']
    timestampedReports = false
    mutationThreshold = 60 // 60%% minimum mutation score
    coverageThreshold = 80 // 80%% minimum coverage
}
"""
                .formatted(
                        GeneratedProjectVersions.PITEST_PLUGIN_VERSION,
                        springBootVersion,
                        GeneratedProjectVersions.SPRING_DEPENDENCY_MANAGEMENT_VERSION,
                        groupId,
                        GeneratedProjectVersions.INITIAL_PROJECT_VERSION,
                        javaVersion,
                        deps,
                        GeneratedProjectVersions.SPRINGDOC_VERSION,
                        dbDeps,
                        GeneratedProjectVersions.MAPSTRUCT_VERSION,
                        GeneratedProjectVersions.MAPSTRUCT_VERSION,
                        GeneratedProjectVersions.LOMBOK_MAPSTRUCT_BINDING_VERSION,
                        GeneratedProjectVersions.JACOCO_VERSION,
                        GeneratedProjectVersions.PITEST_JUNIT5_VERSION,
                        groupId,
                        groupId);
    }

    /**
     * Generates the settings.gradle file content.
     *
     * @param artifactId the project artifact ID
     * @return the settings.gradle content
     */
    public String generateSettingsGradle(String artifactId) {
        return
"""
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = '%s'
"""
                .formatted(artifactId);
    }

    /**
     * Generates the settings.gradle.kts file content for Kotlin projects.
     *
     * @param artifactId the project artifact ID
     * @return the settings.gradle.kts content
     */
    public String generateKotlinSettingsGradle(String artifactId) {
        return
"""
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "%s"
"""
                .formatted(artifactId);
    }

    /**
     * Maximum JVM target supported by the Kotlin Gradle plugin's JvmTarget enum (Kotlin 2.3.0+).
     */
    private static final int MAX_KOTLIN_JVM_TARGET = 25;

    /**
     * Generates the build.gradle.kts file content for Kotlin projects.
     *
     * @param config the project configuration
     * @return the build.gradle.kts content
     */
    public String generateKotlinBuildGradle(GenerateRequest.ProjectConfig config) {
        String springBootVersion =
                config.getSpringBootVersion() != null
                        ? config.getSpringBootVersion()
                        : GeneratedProjectVersions.SPRING_BOOT_VERSION;
        String javaVersion =
                config.getJavaVersion() != null
                        ? config.getJavaVersion()
                        : GeneratedProjectVersions.JAVA_VERSION;
        String groupId = config.getGroupId() != null ? config.getGroupId() : "com.example";
        String kotlinVersion = GeneratedProjectVersions.KOTLIN_VERSION;

        // Cap JVM target at max supported by Kotlin Gradle plugin
        String kotlinJvmTarget = getKotlinJvmTarget(javaVersion);

        StringBuilder deps = new StringBuilder();

        // Core dependencies always included (via JitPack)
        deps.append("    // APiGen Core (from JitPack - no auth required)\n");
        deps.append(
                "    implementation(\"%s:apigen-core:%s\")\n"
                        .formatted(
                                GeneratedProjectVersions.JITPACK_GROUP_ID,
                                GeneratedProjectVersions.APIGEN_CORE_VERSION));

        if (config.getModules() != null && config.getModules().isSecurity()) {
            deps.append("\n    // APiGen Security (from JitPack)\n");
            deps.append(
                    "    implementation(\"%s:apigen-security:%s\")\n"
                            .formatted(
                                    GeneratedProjectVersions.JITPACK_GROUP_ID,
                                    GeneratedProjectVersions.APIGEN_SECURITY_VERSION));
        }

        // Feature Pack dependencies
        deps.append(generateKotlinFeaturePackDependencies(config));

        String dbDeps = generateKotlinDatabaseDependencies(config);

        return
"""
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "%s"
    kotlin("plugin.spring") version "%s"
    kotlin("plugin.jpa") version "%s"
    kotlin("kapt") version "%s"
    jacoco
    id("info.solidsoft.pitest") version "%s"
    id("org.springframework.boot") version "%s"
    id("io.spring.dependency-management") version "%s"
}

group = "%s"
version = "%s"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(%s)
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_%s
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

repositories {
    mavenCentral()
    // JitPack - no authentication required
    maven { url = uri("https://jitpack.io") }
}

dependencies {
%s
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-hateoas")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // OpenAPI / Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:%s")

    // Database
%s

    // MapStruct
    implementation("org.mapstruct:mapstruct:%s")
    kapt("org.mapstruct:mapstruct-processor:%s")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-jackson-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

// ==========================================================================
// Code Coverage (JaCoCo)
// ==========================================================================
jacoco {
    toolVersion = "%s"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal() // 80%% minimum coverage
            }
        }
    }
}

// ==========================================================================
// Mutation Testing (PIT)
// ==========================================================================
pitest {
    junit5PluginVersion.set("%s")
    targetClasses.set(listOf("%s.**"))
    targetTests.set(listOf("%s.**"))
    threads.set(4)
    outputFormats.set(listOf("HTML", "XML"))
    timestampedReports.set(false)
    mutationThreshold.set(60) // 60%% minimum mutation score
    coverageThreshold.set(80) // 80%% minimum coverage
}
"""
                .formatted(
                        kotlinVersion,
                        kotlinVersion,
                        kotlinVersion,
                        kotlinVersion,
                        GeneratedProjectVersions.PITEST_PLUGIN_VERSION,
                        springBootVersion,
                        GeneratedProjectVersions.SPRING_DEPENDENCY_MANAGEMENT_VERSION,
                        groupId,
                        GeneratedProjectVersions.INITIAL_PROJECT_VERSION,
                        javaVersion,
                        kotlinJvmTarget,
                        deps,
                        GeneratedProjectVersions.SPRINGDOC_VERSION,
                        dbDeps,
                        GeneratedProjectVersions.MAPSTRUCT_VERSION,
                        GeneratedProjectVersions.MAPSTRUCT_VERSION,
                        GeneratedProjectVersions.JACOCO_VERSION,
                        GeneratedProjectVersions.PITEST_JUNIT5_VERSION,
                        groupId,
                        groupId);
    }

    /**
     * Gets the Kotlin JVM target, capped at the maximum supported by the Kotlin Gradle plugin.
     *
     * @param javaVersion the requested Java version
     * @return the Kotlin JVM target (max 21)
     */
    private String getKotlinJvmTarget(String javaVersion) {
        try {
            int version = Integer.parseInt(javaVersion);
            return String.valueOf(Math.min(version, MAX_KOTLIN_JVM_TARGET));
        } catch (NumberFormatException _) {
            return String.valueOf(MAX_KOTLIN_JVM_TARGET);
        }
    }

    /**
     * Generates database driver dependencies for Kotlin DSL.
     *
     * @param config the project configuration
     * @return the database dependencies
     */
    private String generateKotlinDatabaseDependencies(GenerateRequest.ProjectConfig config) {
        GenerateRequest.DatabaseConfig db =
                config.getDatabase() != null
                        ? config.getDatabase()
                        : new GenerateRequest.DatabaseConfig();

        String dbType = db.getType().toLowerCase(Locale.ROOT);

        StringBuilder deps = new StringBuilder();
        deps.append("    runtimeOnly(\"com.h2database:h2\") // For local development/testing\n");

        switch (dbType) {
            case "mysql" ->
                    deps.append("    runtimeOnly(\"com.mysql:mysql-connector-j\")")
                            .append(PRODUCTION_DB_COMMENT);
            case "mariadb" ->
                    deps.append("    runtimeOnly(\"org.mariadb.jdbc:mariadb-java-client\")")
                            .append(PRODUCTION_DB_COMMENT);
            case "sqlserver" ->
                    deps.append("    runtimeOnly(\"com.microsoft.sqlserver:mssql-jdbc\")")
                            .append(PRODUCTION_DB_COMMENT);
            case "oracle" ->
                    deps.append("    runtimeOnly(\"com.oracle.database.jdbc:ojdbc11\")")
                            .append(PRODUCTION_DB_COMMENT);
            default ->
                    deps.append("    runtimeOnly(\"org.postgresql:postgresql\")")
                            .append(PRODUCTION_DB_COMMENT);
        }

        return deps.toString();
    }

    /**
     * Generates database driver dependencies based on the configured database type.
     *
     * @param config the project configuration
     * @return the database dependencies
     */
    private String generateDatabaseDependencies(GenerateRequest.ProjectConfig config) {
        GenerateRequest.DatabaseConfig db =
                config.getDatabase() != null
                        ? config.getDatabase()
                        : new GenerateRequest.DatabaseConfig();

        String dbType = db.getType().toLowerCase(Locale.ROOT);

        StringBuilder deps = new StringBuilder();
        deps.append("    runtimeOnly 'com.h2database:h2' // For local development/testing\n");

        switch (dbType) {
            case "mysql" ->
                    deps.append("    runtimeOnly 'com.mysql:mysql-connector-j'")
                            .append(PRODUCTION_DB_COMMENT);
            case "mariadb" ->
                    deps.append("    runtimeOnly 'org.mariadb.jdbc:mariadb-java-client'")
                            .append(PRODUCTION_DB_COMMENT);
            case "sqlserver" ->
                    deps.append("    runtimeOnly 'com.microsoft.sqlserver:mssql-jdbc'")
                            .append(PRODUCTION_DB_COMMENT);
            case "oracle" ->
                    deps.append("    runtimeOnly 'com.oracle.database.jdbc:ojdbc11'")
                            .append(PRODUCTION_DB_COMMENT);
            default ->
                    deps.append("    runtimeOnly 'org.postgresql:postgresql'")
                            .append(PRODUCTION_DB_COMMENT);
        }

        return deps.toString();
    }

    /**
     * Generates feature pack dependencies for Kotlin DSL based on enabled features.
     *
     * @param config the project configuration
     * @return the feature pack dependencies
     */
    private String generateKotlinFeaturePackDependencies(GenerateRequest.ProjectConfig config) {
        StringBuilder deps = new StringBuilder();
        GenerateRequest.FeaturesConfig features = config.getFeatures();
        if (features == null) {
            return "";
        }

        // Mail Service
        if (features.isMailService()) {
            deps.append("\n    // Mail Service\n");
            deps.append(
                    "    implementation(\"org.springframework.boot:spring-boot-starter-mail\")\n");
            deps.append(
                    "    implementation(\"org.springframework.boot:spring-boot-starter-thymeleaf\")\n");
        }

        // File Upload / Storage
        if (features.isFileUpload()) {
            GenerateRequest.StorageConfig storageConfig = config.getStorageConfig();
            String storageType =
                    storageConfig != null
                            ? storageConfig.getType().toLowerCase(Locale.ROOT)
                            : "local";

            if ("s3".equals(storageType)) {
                deps.append("\n    // AWS S3 Storage\n");
                deps.append(
                        "    implementation(\"software.amazon.awssdk:s3:%s\")\n"
                                .formatted(GeneratedProjectVersions.AWS_SDK_VERSION));
                deps.append(
                        "    implementation(\"software.amazon.awssdk:sts:%s\")\n"
                                .formatted(GeneratedProjectVersions.AWS_SDK_VERSION));
            } else if ("azure".equals(storageType)) {
                deps.append("\n    // Azure Blob Storage\n");
                deps.append(
                        "    implementation(\"com.azure:azure-storage-blob:%s\")\n"
                                .formatted(GeneratedProjectVersions.AZURE_STORAGE_BLOB_VERSION));
            }
        }

        // jte Templates
        if (features.isJteTemplates()) {
            deps.append("\n    // jte Templates\n");
            deps.append(
                    "    implementation(\"gg.jte:jte-spring-boot-starter-3:%s\")\n"
                            .formatted(GeneratedProjectVersions.JTE_VERSION));
        }

        // Social Login (uses Spring Security OAuth2 Client)
        if (features.isSocialLogin()) {
            deps.append("\n    // Social Login (OAuth2 Client)\n");
            deps.append(
                    "    implementation(\"org.springframework.boot:spring-boot-starter-oauth2-client\")\n");
        }

        return deps.toString();
    }

    /**
     * Generates feature pack dependencies based on enabled features.
     *
     * @param config the project configuration
     * @return the feature pack dependencies
     */
    private String generateFeaturePackDependencies(GenerateRequest.ProjectConfig config) {
        StringBuilder deps = new StringBuilder();
        GenerateRequest.FeaturesConfig features = config.getFeatures();
        if (features == null) {
            return "";
        }

        // Mail Service
        if (features.isMailService()) {
            deps.append("\n    // Mail Service\n");
            deps.append("    implementation 'org.springframework.boot:spring-boot-starter-mail'\n");
            deps.append(
                    "    implementation"
                            + " 'org.springframework.boot:spring-boot-starter-thymeleaf'\n");
        }

        // File Upload / Storage
        if (features.isFileUpload()) {
            GenerateRequest.StorageConfig storageConfig = config.getStorageConfig();
            String storageType =
                    storageConfig != null
                            ? storageConfig.getType().toLowerCase(Locale.ROOT)
                            : "local";

            if ("s3".equals(storageType)) {
                deps.append("\n    // AWS S3 Storage\n");
                deps.append(
                        "    implementation 'software.amazon.awssdk:s3:%s'\n"
                                .formatted(GeneratedProjectVersions.AWS_SDK_VERSION));
                deps.append(
                        "    implementation 'software.amazon.awssdk:sts:%s'\n"
                                .formatted(GeneratedProjectVersions.AWS_SDK_VERSION));
            } else if ("azure".equals(storageType)) {
                deps.append("\n    // Azure Blob Storage\n");
                deps.append(
                        "    implementation 'com.azure:azure-storage-blob:%s'\n"
                                .formatted(GeneratedProjectVersions.AZURE_STORAGE_BLOB_VERSION));
            }
        }

        // jte Templates
        if (features.isJteTemplates()) {
            deps.append("\n    // jte Templates\n");
            deps.append(
                    "    implementation 'gg.jte:jte-spring-boot-starter-3:%s'\n"
                            .formatted(GeneratedProjectVersions.JTE_VERSION));
        }

        // Social Login (uses Spring Security OAuth2 Client)
        if (features.isSocialLogin()) {
            deps.append("\n    // Social Login (OAuth2 Client)\n");
            deps.append(
                    "    implementation"
                            + " 'org.springframework.boot:spring-boot-starter-oauth2-client'\n");
        }

        return deps.toString();
    }
}
