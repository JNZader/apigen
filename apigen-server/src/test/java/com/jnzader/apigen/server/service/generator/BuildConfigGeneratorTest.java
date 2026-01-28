package com.jnzader.apigen.server.service.generator;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnzader.apigen.server.config.GeneratedProjectVersions;
import com.jnzader.apigen.server.dto.GenerateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("BuildConfigGenerator Tests")
@SuppressWarnings(
        "java:S5976") // Tests validate different specific features, not the same feature with
// different inputs
class BuildConfigGeneratorTest {

    private BuildConfigGenerator buildConfigGenerator;

    @BeforeEach
    void setUp() {
        buildConfigGenerator = new BuildConfigGenerator();
    }

    @Nested
    @DisplayName("generateBuildGradle()")
    class GenerateBuildGradleTests {

        @Test
        @DisplayName("Should generate build.gradle with correct plugins")
        void shouldGenerateBuildGradleWithCorrectPlugins() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = buildConfigGenerator.generateBuildGradle(config);

            assertThat(result)
                    .contains("plugins {")
                    .contains("id 'java'")
                    .contains("id 'org.springframework.boot'")
                    .contains("id 'io.spring.dependency-management'");
        }

        @Test
        @DisplayName("Should use provided Spring Boot version")
        void shouldUseProvidedSpringBootVersion() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();
            config.setSpringBootVersion("3.5.0");

            String result = buildConfigGenerator.generateBuildGradle(config);

            assertThat(result).contains("id 'org.springframework.boot' version '3.5.0'");
        }

        @Test
        @DisplayName("Should use provided Java version")
        void shouldUseProvidedJavaVersion() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();
            config.setJavaVersion("21");

            String result = buildConfigGenerator.generateBuildGradle(config);

            assertThat(result).contains("languageVersion = JavaLanguageVersion.of(21)");
        }

        @Test
        @DisplayName("Should use provided group ID")
        void shouldUseProvidedGroupId() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();
            config.setGroupId("com.mycompany");

            String result = buildConfigGenerator.generateBuildGradle(config);

            assertThat(result).contains("group = 'com.mycompany'");
        }

        @Test
        @DisplayName("Should include GitHub Packages repository")
        void shouldIncludeGitHubPackagesRepository() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = buildConfigGenerator.generateBuildGradle(config);

            assertThat(result).contains(GeneratedProjectVersions.GITHUB_PACKAGES_URL);
        }

        @Test
        @DisplayName("Should include APiGen core dependency")
        void shouldIncludeApiGenCoreDependency() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = buildConfigGenerator.generateBuildGradle(config);

            assertThat(result)
                    .contains(
                            "implementation '"
                                    + GeneratedProjectVersions.APIGEN_GROUP_ID
                                    + ":apigen-core:"
                                    + GeneratedProjectVersions.APIGEN_CORE_VERSION
                                    + "'");
        }

        @Test
        @DisplayName("Should include APiGen security when enabled")
        void shouldIncludeApiGenSecurityWhenEnabled() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();
            config.setModules(GenerateRequest.ModulesConfig.builder().security(true).build());

            String result = buildConfigGenerator.generateBuildGradle(config);

            assertThat(result)
                    .contains(
                            "implementation '"
                                    + GeneratedProjectVersions.APIGEN_GROUP_ID
                                    + ":apigen-security:"
                                    + GeneratedProjectVersions.APIGEN_SECURITY_VERSION
                                    + "'");
        }

        @Test
        @DisplayName("Should not include APiGen security when disabled")
        void shouldNotIncludeApiGenSecurityWhenDisabled() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();
            config.setModules(GenerateRequest.ModulesConfig.builder().security(false).build());

            String result = buildConfigGenerator.generateBuildGradle(config);

            assertThat(result).doesNotContain("apigen-security");
        }

        @Test
        @DisplayName("Should include Spring Boot starters")
        void shouldIncludeSpringBootStarters() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = buildConfigGenerator.generateBuildGradle(config);

            assertThat(result)
                    .contains("spring-boot-starter-web")
                    .contains("spring-boot-starter-data-jpa")
                    .contains("spring-boot-starter-validation")
                    .contains("spring-boot-starter-hateoas")
                    .contains("spring-boot-starter-actuator")
                    .contains("spring-boot-starter-cache");
        }

        @Test
        @DisplayName("Should include OpenAPI/Swagger dependency")
        void shouldIncludeOpenApiSwaggerDependency() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = buildConfigGenerator.generateBuildGradle(config);

            assertThat(result).contains("springdoc-openapi-starter-webmvc-ui");
        }

        @Test
        @DisplayName("Should include MapStruct dependencies")
        void shouldIncludeMapStructDependencies() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = buildConfigGenerator.generateBuildGradle(config);

            assertThat(result)
                    .contains("implementation 'org.mapstruct:mapstruct:")
                    .contains("annotationProcessor 'org.mapstruct:mapstruct-processor:");
        }

        @Test
        @DisplayName("Should include Lombok dependencies")
        void shouldIncludeLombokDependencies() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = buildConfigGenerator.generateBuildGradle(config);

            assertThat(result)
                    .contains("compileOnly 'org.projectlombok:lombok'")
                    .contains("annotationProcessor 'org.projectlombok:lombok'")
                    .contains("lombok-mapstruct-binding");
        }

        @Test
        @DisplayName("Should include H2 for local development")
        void shouldIncludeH2ForLocalDevelopment() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = buildConfigGenerator.generateBuildGradle(config);

            assertThat(result).contains("runtimeOnly 'com.h2database:h2'");
        }

        @ParameterizedTest
        @CsvSource({
            "postgresql, org.postgresql:postgresql",
            "mysql, com.mysql:mysql-connector-j",
            "mariadb, org.mariadb.jdbc:mariadb-java-client",
            "sqlserver, com.microsoft.sqlserver:mssql-jdbc",
            "oracle, com.oracle.database.jdbc:ojdbc11"
        })
        @DisplayName("Should include correct database driver for database type")
        void shouldIncludeCorrectDatabaseDriverForDatabaseType(
                String dbType, String expectedDriver) {
            GenerateRequest.ProjectConfig config = createDefaultConfig();
            config.setDatabase(GenerateRequest.DatabaseConfig.builder().type(dbType).build());

            String result = buildConfigGenerator.generateBuildGradle(config);

            assertThat(result).contains(expectedDriver);
        }

        @Test
        @DisplayName("Should include JUnit platform for testing")
        void shouldIncludeJUnitPlatformForTesting() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = buildConfigGenerator.generateBuildGradle(config);

            assertThat(result).contains("test {").contains("useJUnitPlatform()");
        }

        @Test
        @DisplayName("Should use default values when config values are null")
        void shouldUseDefaultValuesWhenConfigValuesAreNull() {
            GenerateRequest.ProjectConfig config = new GenerateRequest.ProjectConfig();
            config.setName("test");
            config.setArtifactId("test");

            String result = buildConfigGenerator.generateBuildGradle(config);

            assertThat(result)
                    .contains("version '" + GeneratedProjectVersions.SPRING_BOOT_VERSION + "'")
                    .contains(
                            "JavaLanguageVersion.of(" + GeneratedProjectVersions.JAVA_VERSION + ")")
                    .contains("group = 'com.example'");
        }
    }

    @Nested
    @DisplayName("generateSettingsGradle()")
    class GenerateSettingsGradleTests {

        @Test
        @DisplayName("Should generate settings.gradle with project name")
        void shouldGenerateSettingsGradleWithProjectName() {
            String result = buildConfigGenerator.generateSettingsGradle("my-api");

            assertThat(result).contains("rootProject.name = 'my-api'");
        }

        @Test
        @DisplayName("Should handle artifact ID with hyphens")
        void shouldHandleArtifactIdWithHyphens() {
            String result = buildConfigGenerator.generateSettingsGradle("my-awesome-api");

            assertThat(result).contains("rootProject.name = 'my-awesome-api'");
        }
    }

    private GenerateRequest.ProjectConfig createDefaultConfig() {
        return GenerateRequest.ProjectConfig.builder()
                .name("My API")
                .groupId("com.example")
                .artifactId("my-api")
                .javaVersion(GeneratedProjectVersions.JAVA_VERSION)
                .springBootVersion(GeneratedProjectVersions.SPRING_BOOT_VERSION)
                .modules(GenerateRequest.ModulesConfig.builder().build())
                .database(GenerateRequest.DatabaseConfig.builder().build())
                .build();
    }
}
