package com.jnzader.apigen.server.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnzader.apigen.server.dto.GenerateRequest;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Integration test that generates a full project and compiles it.
 *
 * <p>This test catches Spring Boot compatibility issues that unit tests miss, such as:
 *
 * <ul>
 *   <li>Missing @EnableJpaRepositories causing "No qualifying bean" errors
 *   <li>Incorrect package scanning configurations
 *   <li>Missing imports or annotation changes in new Spring Boot versions
 * </ul>
 *
 * <p>The compilation test is controlled by the CI_COMPILE_GENERATED_PROJECT environment variable.
 * When enabled, it extracts the generated ZIP and runs ./gradlew compileJava to verify the project
 * compiles successfully.
 */
@DisplayName("Generated Project Compilation Integration Tests")
@Tag("integration")
class GeneratedProjectCompilationIT {

    private GeneratorService generatorService;
    private JsonMapper jsonMapper;

    @TempDir Path tempDir;

    @BeforeEach
    void setUp() {
        generatorService = new GeneratorService();
        jsonMapper =
                JsonMapper.builder()
                        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                        .build();
    }

    @Test
    @DisplayName(
            "Generated Application class should have correct JPA annotations when security enabled")
    void generatedApplicationShouldHaveJpaAnnotationsWithSecurity() throws IOException {
        // Load fixture
        GenerateRequest request = loadFixture("fixtures/blog-api-with-security.json");

        // Generate project
        byte[] zipBytes = generatorService.generateProject(request);

        // Extract and find Application.java
        String applicationJava = extractFileFromZip(zipBytes, "MyApiApplication.java");

        assertThat(applicationJava)
                .as("Application.java should contain @EnableJpaRepositories import")
                .contains(
                        "import"
                            + " org.springframework.data.jpa.repository.config.EnableJpaRepositories;");

        assertThat(applicationJava)
                .as("Application.java should contain @EntityScan import (Spring Boot 4.0 location)")
                .contains("import org.springframework.boot.persistence.autoconfigure.EntityScan;");

        assertThat(applicationJava)
                .as("Application.java should have @EnableJpaRepositories with security package")
                .contains("@EnableJpaRepositories(basePackages = {\"com.example.myapi\"")
                .contains("com.jnzader.apigen.security.domain.repository");

        assertThat(applicationJava)
                .as("Application.java should have @EntityScan with security package")
                .contains("@EntityScan(basePackages = {\"com.example.myapi\"")
                .contains("com.jnzader.apigen.security.domain.entity");
    }

    @Test
    @DisplayName("Generated Application class should have simple JPA annotations without security")
    void generatedApplicationShouldHaveSimpleJpaAnnotationsWithoutSecurity() throws IOException {
        // Load fixture and disable security
        GenerateRequest request = loadFixture("fixtures/blog-api-with-security.json");
        request.getProject().getModules().setSecurity(false);

        // Generate project
        byte[] zipBytes = generatorService.generateProject(request);

        // Extract and find Application.java
        String applicationJava = extractFileFromZip(zipBytes, "MyApiApplication.java");

        assertThat(applicationJava)
                .as("Application.java should have @EnableJpaRepositories with only app package")
                .contains("@EnableJpaRepositories(basePackages = \"com.example.myapi\")")
                .doesNotContain("com.jnzader.apigen.security");

        assertThat(applicationJava)
                .as("Application.java should have @EntityScan with only app package")
                .contains("@EntityScan(basePackages = \"com.example.myapi\")")
                .doesNotContain("com.jnzader.apigen.security");
    }

    @Test
    @DisplayName("Generated project with security should compile successfully")
    @EnabledIfEnvironmentVariable(named = "CI_COMPILE_GENERATED_PROJECT", matches = "true")
    void generatedProjectWithSecurityShouldCompile() throws IOException, InterruptedException {
        // Load fixture
        GenerateRequest request = loadFixture("fixtures/blog-api-with-security.json");
        String artifactId = request.getProject().getArtifactId();

        // Generate project
        byte[] zipBytes = generatorService.generateProject(request);

        // Extract to temp directory (ZIP already contains artifactId/ prefix)
        extractZipToDirectory(zipBytes, tempDir);

        // Project is now at tempDir/my-api/
        Path projectDir = tempDir.resolve(artifactId);

        // Make gradlew executable
        Path gradlew = projectDir.resolve("gradlew");
        if (Files.exists(gradlew)) {
            gradlew.toFile().setExecutable(true);
        }

        // Run compilation
        ProcessBuilder pb =
                new ProcessBuilder("./gradlew", "compileJava", "--no-daemon", "-x", "test")
                        .directory(projectDir.toFile())
                        .redirectErrorStream(true);

        Process process = pb.start();

        // Capture output
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        boolean finished = process.waitFor(5, TimeUnit.MINUTES);
        int exitCode = finished ? process.exitValue() : -1;

        assertThat(exitCode)
                .as(
                        "Generated project should compile successfully.\n\nBuild output:\n%s",
                        output.toString())
                .isZero();
    }

    private GenerateRequest loadFixture(String resourcePath) throws IOException {
        try (var is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertThat(is).as("Fixture resource should exist: " + resourcePath).isNotNull();
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return jsonMapper.readValue(json, GenerateRequest.class);
        }
    }

    private String extractFileFromZip(byte[] zipBytes, String fileName) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(fileName)) {
                    return new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                }
                zis.closeEntry();
            }
        }
        throw new AssertionError("File not found in ZIP: " + fileName);
    }

    private void extractZipToDirectory(byte[] zipBytes, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path targetPath = targetDir.resolve(entry.getName());

                // Prevent zip slip vulnerability
                if (!targetPath.normalize().startsWith(targetDir.normalize())) {
                    throw new IOException("Bad zip entry: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    Files.write(targetPath, zis.readAllBytes());
                }
                zis.closeEntry();
            }
        }
    }
}
