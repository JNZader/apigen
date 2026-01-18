package com.jnzader.apigen.server.service.generator;

import com.jnzader.apigen.server.config.GeneratedProjectVersions;
import com.jnzader.apigen.server.dto.GenerateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProjectStructureGenerator Tests")
@SuppressWarnings("java:S5976") // Tests validate different specific features, not the same feature with different inputs
class ProjectStructureGeneratorTest {

    private ProjectStructureGenerator projectStructureGenerator;

    @BeforeEach
    void setUp() {
        projectStructureGenerator = new ProjectStructureGenerator();
    }

    @Nested
    @DisplayName("generateMainClass()")
    class GenerateMainClassTests {

        @Test
        @DisplayName("Should generate main class with correct package")
        void shouldGenerateMainClassWithCorrectPackage() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = projectStructureGenerator.generateMainClass(config);

            assertThat(result).contains("package com.example.myapi;");
        }

        @Test
        @DisplayName("Should generate class name from artifact ID in PascalCase")
        void shouldGenerateClassNameFromArtifactIdInPascalCase() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();
            config.setArtifactId("my-api");

            String result = projectStructureGenerator.generateMainClass(config);

            assertThat(result).contains("public class MyApiApplication");
        }

        @Test
        @DisplayName("Should include Spring Boot imports")
        void shouldIncludeSpringBootImports() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = projectStructureGenerator.generateMainClass(config);

            assertThat(result)
                    .contains("import org.springframework.boot.SpringApplication;")
                    .contains("import org.springframework.boot.autoconfigure.SpringBootApplication;");
        }

        @Test
        @DisplayName("Should include @SpringBootApplication annotation")
        void shouldIncludeSpringBootApplicationAnnotation() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = projectStructureGenerator.generateMainClass(config);

            assertThat(result).contains("@SpringBootApplication");
        }

        @Test
        @DisplayName("Should include @ComponentScan with base packages")
        void shouldIncludeComponentScanWithBasePackages() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = projectStructureGenerator.generateMainClass(config);

            assertThat(result).contains("@ComponentScan(basePackages = {\"com.example.myapi\", \"com.jnzader.apigen.core\"})");
        }

        @Test
        @DisplayName("Should include @EnableCaching annotation")
        void shouldIncludeEnableCachingAnnotation() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = projectStructureGenerator.generateMainClass(config);

            assertThat(result).contains("@EnableCaching");
        }

        @Test
        @DisplayName("Should include main method")
        void shouldIncludeMainMethod() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = projectStructureGenerator.generateMainClass(config);

            assertThat(result)
                    .contains("public static void main(String[] args)")
                    .contains("SpringApplication.run(MyApiApplication.class, args);");
        }

        @Test
        @DisplayName("Should handle artifact ID with multiple hyphens")
        void shouldHandleArtifactIdWithMultipleHyphens() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();
            config.setArtifactId("my-awesome-api-service");

            String result = projectStructureGenerator.generateMainClass(config);

            assertThat(result).contains("public class MyAwesomeApiServiceApplication");
        }
    }

    @Nested
    @DisplayName("getMainClassName()")
    class GetMainClassNameTests {

        @Test
        @DisplayName("Should return PascalCase class name with Application suffix")
        void shouldReturnPascalCaseClassNameWithApplicationSuffix() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();
            config.setArtifactId("my-api");

            String result = projectStructureGenerator.getMainClassName(config);

            assertThat(result).isEqualTo("MyApiApplication");
        }

        @Test
        @DisplayName("Should handle simple artifact ID")
        void shouldHandleSimpleArtifactId() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();
            config.setArtifactId("demo");

            String result = projectStructureGenerator.getMainClassName(config);

            assertThat(result).isEqualTo("DemoApplication");
        }
    }

    @Nested
    @DisplayName("generateReadme()")
    class GenerateReadmeTests {

        @Test
        @DisplayName("Should include project name in title")
        void shouldIncludeProjectNameInTitle() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();
            config.setName("My Awesome API");

            String result = projectStructureGenerator.generateReadme(config);

            assertThat(result).contains("# My Awesome API");
        }

        @Test
        @DisplayName("Should include APiGen attribution")
        void shouldIncludeApiGenAttribution() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = projectStructureGenerator.generateReadme(config);

            assertThat(result)
                    .contains("APiGen Studio")
                    .contains("https://github.com/jnzader/apigen");
        }

        @Test
        @DisplayName("Should include quick start commands")
        void shouldIncludeQuickStartCommands() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = projectStructureGenerator.generateReadme(config);

            assertThat(result)
                    .contains("./gradlew build")
                    .contains("./gradlew bootRun");
        }

        @Test
        @DisplayName("Should include API documentation URLs")
        void shouldIncludeApiDocumentationUrls() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = projectStructureGenerator.generateReadme(config);

            assertThat(result)
                    .contains("http://localhost:8080/swagger-ui.html")
                    .contains("http://localhost:8080/api-docs")
                    .contains("http://localhost:8080/h2-console");
        }

        @Test
        @DisplayName("Should include project structure")
        void shouldIncludeProjectStructure() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = projectStructureGenerator.generateReadme(config);

            assertThat(result)
                    .contains("Project Structure")
                    .contains("domain/")
                    .contains("application/")
                    .contains("infrastructure/")
                    .contains("entity/")
                    .contains("repository/")
                    .contains("dto/")
                    .contains("mapper/")
                    .contains("service/")
                    .contains("controller/");
        }

        @Test
        @DisplayName("Should include features list")
        void shouldIncludeFeaturesList() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = projectStructureGenerator.generateReadme(config);

            assertThat(result)
                    .contains("Features")
                    .contains("CRUD operations")
                    .contains("HATEOAS links")
                    .contains("Soft delete")
                    .contains("Audit trails")
                    .contains("OpenAPI documentation")
                    .contains("Caching");
        }

        @Test
        @DisplayName("Should include correct package path in structure")
        void shouldIncludeCorrectPackagePathInStructure() {
            GenerateRequest.ProjectConfig config = createDefaultConfig();

            String result = projectStructureGenerator.generateReadme(config);

            assertThat(result).contains("com/example/myapi/");
        }
    }

    @Nested
    @DisplayName("getGitignoreContent()")
    class GetGitignoreContentTests {

        @Test
        @DisplayName("Should include Gradle entries")
        void shouldIncludeGradleEntries() {
            String result = projectStructureGenerator.getGitignoreContent();

            assertThat(result)
                    .contains(".gradle/")
                    .contains("build/")
                    .contains("!gradle/wrapper/gradle-wrapper.jar");
        }

        @Test
        @DisplayName("Should include IDE entries")
        void shouldIncludeIdeEntries() {
            String result = projectStructureGenerator.getGitignoreContent();

            assertThat(result)
                    .contains(".idea/")
                    .contains("*.iml")
                    .contains(".project")
                    .contains(".classpath")
                    .contains(".vscode/");
        }

        @Test
        @DisplayName("Should include log entries")
        void shouldIncludeLogEntries() {
            String result = projectStructureGenerator.getGitignoreContent();

            assertThat(result)
                    .contains("*.log")
                    .contains("logs/");
        }

        @Test
        @DisplayName("Should include OS entries")
        void shouldIncludeOsEntries() {
            String result = projectStructureGenerator.getGitignoreContent();

            assertThat(result)
                    .contains(".DS_Store")
                    .contains("Thumbs.db");
        }

        @Test
        @DisplayName("Should include environment file entries")
        void shouldIncludeEnvironmentFileEntries() {
            String result = projectStructureGenerator.getGitignoreContent();

            assertThat(result)
                    .contains(".env")
                    .contains("*.local");
        }
    }

    private GenerateRequest.ProjectConfig createDefaultConfig() {
        return GenerateRequest.ProjectConfig.builder()
                .name("My API")
                .groupId("com.example")
                .artifactId("my-api")
                .javaVersion(GeneratedProjectVersions.JAVA_VERSION)
                .springBootVersion(GeneratedProjectVersions.SPRING_BOOT_VERSION)
                .build();
    }
}
