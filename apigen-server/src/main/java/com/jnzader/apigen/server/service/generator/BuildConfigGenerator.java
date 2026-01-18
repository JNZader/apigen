package com.jnzader.apigen.server.service.generator;

import com.jnzader.apigen.server.config.GeneratedProjectVersions;
import com.jnzader.apigen.server.dto.GenerateRequest;
import org.springframework.stereotype.Component;

/**
 * Generates build configuration files (build.gradle, settings.gradle).
 */
@Component
public class BuildConfigGenerator {

    /**
     * Generates the build.gradle file content.
     *
     * @param config the project configuration
     * @return the build.gradle content
     */
    public String generateBuildGradle(GenerateRequest.ProjectConfig config) {
        String springBootVersion = config.getSpringBootVersion() != null
                ? config.getSpringBootVersion() : GeneratedProjectVersions.SPRING_BOOT_VERSION;
        String javaVersion = config.getJavaVersion() != null
                ? config.getJavaVersion() : GeneratedProjectVersions.JAVA_VERSION;
        String groupId = config.getGroupId() != null
                ? config.getGroupId() : "com.example";

        StringBuilder deps = new StringBuilder();

        // Core dependencies always included (via JitPack)
        deps.append("    // APiGen Core (from GitHub via JitPack)\n");
        deps.append("    implementation 'com.github.jnzader.apigen:apigen-core:main-SNAPSHOT'\n");

        if (config.getModules() != null && config.getModules().isSecurity()) {
            deps.append("\n    // APiGen Security (from GitHub via JitPack)\n");
            deps.append("    implementation 'com.github.jnzader.apigen:apigen-security:main-SNAPSHOT'\n");
        }

        String dbDeps = generateDatabaseDependencies(config);

        return """
plugins {
    id 'java'
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
    maven { url 'https://jitpack.io' }
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
}

test {
    useJUnitPlatform()
}
""".formatted(
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
                GeneratedProjectVersions.LOMBOK_MAPSTRUCT_BINDING_VERSION
        );
    }

    /**
     * Generates the settings.gradle file content.
     *
     * @param artifactId the project artifact ID
     * @return the settings.gradle content
     */
    public String generateSettingsGradle(String artifactId) {
        return "rootProject.name = '%s'%n".formatted(artifactId);
    }

    /**
     * Generates database driver dependencies based on the configured database type.
     *
     * @param config the project configuration
     * @return the database dependencies
     */
    private String generateDatabaseDependencies(GenerateRequest.ProjectConfig config) {
        GenerateRequest.DatabaseConfig db = config.getDatabase() != null
                ? config.getDatabase()
                : new GenerateRequest.DatabaseConfig();

        String dbType = db.getType().toLowerCase();

        StringBuilder deps = new StringBuilder();
        deps.append("    runtimeOnly 'com.h2database:h2' // For local development/testing\n");

        switch (dbType) {
            case "mysql" -> deps.append("    runtimeOnly 'com.mysql:mysql-connector-j' // Production database\n");
            case "mariadb" -> deps.append("    runtimeOnly 'org.mariadb.jdbc:mariadb-java-client' // Production database\n");
            case "sqlserver" -> deps.append("    runtimeOnly 'com.microsoft.sqlserver:mssql-jdbc' // Production database\n");
            case "oracle" -> deps.append("    runtimeOnly 'com.oracle.database.jdbc:ojdbc11' // Production database\n");
            default -> deps.append("    runtimeOnly 'org.postgresql:postgresql' // Production database\n");
        }

        return deps.toString();
    }
}
