package com.jnzader.apigen.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.jnzader.apigen.server.config.GeneratedProjectVersions;
import com.jnzader.apigen.server.dto.GenerateRequest;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
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
 * Integration test that generates a full project, compiles it, and runs a context smoke test.
 *
 * <p>This test catches Spring Boot compatibility issues that unit tests miss, such as:
 *
 * <ul>
 *   <li>Missing @EnableJpaRepositories causing "No qualifying bean" errors
 *   <li>Incorrect package scanning configurations
 *   <li>Missing imports or annotation changes in new Spring Boot versions
 *   <li>BeanDefinitionOverrideException from duplicate bean registrations
 *   <li>Runtime context loading failures
 * </ul>
 *
 * <p>The context test is controlled by the CI_COMPILE_GENERATED_PROJECT environment variable. When
 * enabled, it extracts the generated ZIP and runs ./gradlew test --tests "ApplicationContextTest"
 * to verify the Spring context loads successfully.
 */
@DisplayName("Generated Project Compilation Integration Tests")
@Tag("integration")
class GeneratedProjectCompilationIT {

    /** Pattern to detect JitPack dependency resolution failures. */
    private static final Pattern JITPACK_DEPENDENCY_ERROR =
            Pattern.compile("Could not (find|resolve).*apigen-(core|security)");

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
                .as("Application.java should have JPA annotations with security packages")
                .contains(
                        "import"
                            + " org.springframework.data.jpa.repository.config.EnableJpaRepositories;")
                .contains("import org.springframework.boot.persistence.autoconfigure.EntityScan;")
                .contains("@EnableJpaRepositories(basePackages = {\"com.example.myapi\"")
                .contains("com.jnzader.apigen.security.domain.repository")
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
                .as("Application.java should have JPA annotations with only app package")
                .contains("@EnableJpaRepositories(basePackages = \"com.example.myapi\")")
                .contains("@EntityScan(basePackages = \"com.example.myapi\")")
                .doesNotContain("com.jnzader.apigen.security");
    }

    @Test
    @DisplayName("Generated project should include ApplicationContextTest")
    void generatedProjectShouldIncludeContextTest() throws IOException {
        // Load fixture
        GenerateRequest request = loadFixture("fixtures/blog-api-with-security.json");

        // Generate project
        byte[] zipBytes = generatorService.generateProject(request);

        // Extract and find ApplicationContextTest.java
        String contextTest = extractFileFromZip(zipBytes, "ApplicationContextTest.java");

        assertThat(contextTest)
                .as("ApplicationContextTest should be a @SpringBootTest")
                .contains("@SpringBootTest(classes = MyApiApplication.class)")
                .contains("@ActiveProfiles(\"test\")")
                .contains("void contextLoads()");
    }

    @Test
    @DisplayName("Generated project with security should pass context test")
    @EnabledIfEnvironmentVariable(named = "CI_COMPILE_GENERATED_PROJECT", matches = "true")
    void generatedProjectWithSecurityShouldPassContextTest()
            throws IOException, InterruptedException {
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

        // Try with fallback versions if JitPack dependency is not available
        String lastOutput = "";
        for (String version : GeneratedProjectVersions.FALLBACK_VERSIONS) {
            // Update build.gradle to use this version
            updateApigenVersionInBuildGradle(projectDir, version);

            // Run context test
            ProcessBuilder pb =
                    new ProcessBuilder(
                                    "./gradlew",
                                    "test",
                                    "--tests",
                                    "ApplicationContextTest",
                                    "--no-daemon",
                                    "--refresh-dependencies")
                            .directory(projectDir.toFile())
                            .redirectErrorStream(true);

            Process process = pb.start();

            // Capture output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(5, TimeUnit.MINUTES);
            int exitCode = finished ? process.exitValue() : -1;
            lastOutput = output.toString();

            if (exitCode == 0) {
                return; // Success!
            }

            // Check if it's a JitPack dependency error - if so, try next version
            if (isJitPackDependencyError(lastOutput)) {
                System.out.println(
                        "JitPack dependency not available for "
                                + version
                                + ", trying next fallback...");
                continue;
            }

            // It's a different error, fail immediately
            fail("Generated project context test failed.\n\nBuild output:\n%s", lastOutput);
        }

        // All versions failed
        fail(
                """
                Generated project context test failed with all fallback versions.

                Last output:
                %s\
                """,
                lastOutput);
    }

    @Test
    @DisplayName("Generated Kotlin project should compile successfully")
    @EnabledIfEnvironmentVariable(named = "CI_COMPILE_GENERATED_PROJECT", matches = "true")
    void generatedKotlinProjectShouldCompile() throws IOException, InterruptedException {
        // Load Kotlin fixture
        GenerateRequest request = loadFixture("fixtures/blog-api-kotlin.json");
        String artifactId = request.getProject().getArtifactId();

        // Generate Kotlin project
        byte[] zipBytes = generatorService.generateProject(request);

        // Extract to temp directory
        extractZipToDirectory(zipBytes, tempDir);

        Path projectDir = tempDir.resolve(artifactId);

        // Make gradlew executable
        Path gradlew = projectDir.resolve("gradlew");
        if (Files.exists(gradlew)) {
            gradlew.toFile().setExecutable(true);
        }

        // Try with fallback versions if JitPack dependency is not available
        String lastOutput = "";
        for (String version : GeneratedProjectVersions.FALLBACK_VERSIONS) {
            // Update build.gradle.kts to use this version
            updateApigenVersionInBuildGradle(projectDir, version);

            // Run Kotlin compilation
            ProcessBuilder pb =
                    new ProcessBuilder(
                                    "./gradlew",
                                    "compileKotlin",
                                    "--no-daemon",
                                    "--refresh-dependencies")
                            .directory(projectDir.toFile())
                            .redirectErrorStream(true);

            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(5, TimeUnit.MINUTES);
            int exitCode = finished ? process.exitValue() : -1;
            lastOutput = output.toString();

            if (exitCode == 0) {
                return; // Success!
            }

            // Check if it's a JitPack dependency error - if so, try next version
            if (isJitPackDependencyError(lastOutput)) {
                System.out.println(
                        "JitPack dependency not available for "
                                + version
                                + ", trying next fallback...");
                continue;
            }

            // It's a different error, fail immediately
            fail("Generated Kotlin project compilation failed.\n\nBuild output:\n%s", lastOutput);
        }

        // All versions failed
        fail(
                """
                Generated Kotlin project compilation failed with all fallback versions.

                Last output:
                %s\
                """,
                lastOutput);
    }

    @Test
    @DisplayName("Generated C# project should compile successfully")
    @EnabledIfEnvironmentVariable(named = "CI_COMPILE_GENERATED_PROJECT", matches = "true")
    void generatedCSharpProjectShouldCompile() throws IOException, InterruptedException {
        // Load C# fixture
        GenerateRequest request = loadFixture("fixtures/blog-api-csharp.json");
        String artifactId = request.getProject().getArtifactId();

        // Generate C# project
        byte[] zipBytes = generatorService.generateProject(request);

        // Extract to temp directory
        extractZipToDirectory(zipBytes, tempDir);

        Path projectDir = tempDir.resolve(artifactId);

        // Run dotnet build - verifies all .cs files compile correctly
        ProcessBuilder pb =
                new ProcessBuilder("dotnet", "build", "--no-restore")
                        .directory(projectDir.toFile())
                        .redirectErrorStream(true);

        // First restore packages
        ProcessBuilder restorePb =
                new ProcessBuilder("dotnet", "restore")
                        .directory(projectDir.toFile())
                        .redirectErrorStream(true);

        Process restoreProcess = restorePb.start();
        StringBuilder restoreOutput = new StringBuilder();
        try (BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                restoreProcess.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                restoreOutput.append(line).append("\n");
            }
        }
        restoreProcess.waitFor(3, TimeUnit.MINUTES);

        // Now build
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        output.append("=== dotnet restore output ===\n");
        output.append(restoreOutput);
        output.append("\n=== dotnet build output ===\n");

        try (BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        boolean finished = process.waitFor(5, TimeUnit.MINUTES);
        int exitCode = finished ? process.exitValue() : -1;

        assertThat(exitCode)
                .as("Generated C# project should compile.\n\nBuild output:\n%s", output.toString())
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

    @Test
    @DisplayName("Generated Python/FastAPI project should pass syntax check")
    @EnabledIfEnvironmentVariable(named = "CI_COMPILE_GENERATED_PROJECT", matches = "true")
    void generatedPythonProjectShouldCompile() throws IOException, InterruptedException {
        // Load Python fixture
        GenerateRequest request = loadFixture("fixtures/blog-api-python.json");
        String artifactId = request.getProject().getArtifactId();

        // Generate Python project
        byte[] zipBytes = generatorService.generateProject(request);

        // Extract to temp directory
        extractZipToDirectory(zipBytes, tempDir);

        Path projectDir = tempDir.resolve(artifactId);

        // Find all Python files and check syntax
        StringBuilder allOutput = new StringBuilder();
        boolean allPassed = true;

        try (var paths = Files.walk(projectDir)) {
            for (Path pyFile : paths.filter(p -> p.toString().endsWith(".py")).toList()) {
                ProcessBuilder syntaxCheck =
                        new ProcessBuilder("python", "-m", "py_compile", pyFile.toString())
                                .directory(projectDir.toFile())
                                .redirectErrorStream(true);

                Process process = syntaxCheck.start();
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }

                boolean finished = process.waitFor(1, TimeUnit.MINUTES);
                int exitCode = finished ? process.exitValue() : -1;

                if (exitCode != 0) {
                    allPassed = false;
                    allOutput
                            .append("Failed: ")
                            .append(pyFile.getFileName())
                            .append("\n")
                            .append(output)
                            .append("\n");
                }
            }
        }

        assertThat(allPassed)
                .as(
                        "All Python files should have valid syntax.\n\nErrors:\n%s",
                        allOutput.toString())
                .isTrue();
    }

    @Test
    @DisplayName("Generated PHP/Laravel project should pass syntax check")
    @EnabledIfEnvironmentVariable(named = "CI_COMPILE_GENERATED_PROJECT", matches = "true")
    void generatedPhpProjectShouldCompile() throws IOException, InterruptedException {
        // Load PHP fixture
        GenerateRequest request = loadFixture("fixtures/blog-api-php.json");
        String artifactId = request.getProject().getArtifactId();

        // Generate PHP project
        byte[] zipBytes = generatorService.generateProject(request);

        // Extract to temp directory
        extractZipToDirectory(zipBytes, tempDir);

        Path projectDir = tempDir.resolve(artifactId);

        // Run PHP syntax check on all .php files
        StringBuilder allOutput = new StringBuilder();
        boolean allPassed = true;

        try (var paths = Files.walk(projectDir)) {
            for (Path phpFile : paths.filter(p -> p.toString().endsWith(".php")).toList()) {
                ProcessBuilder syntaxCheck =
                        new ProcessBuilder("php", "-l", phpFile.toString())
                                .directory(projectDir.toFile())
                                .redirectErrorStream(true);

                Process process = syntaxCheck.start();
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }

                boolean finished = process.waitFor(1, TimeUnit.MINUTES);
                int exitCode = finished ? process.exitValue() : -1;

                if (exitCode != 0) {
                    allPassed = false;
                    allOutput
                            .append("Failed: ")
                            .append(phpFile.getFileName())
                            .append("\n")
                            .append(output)
                            .append("\n");
                }
            }
        }

        assertThat(allPassed)
                .as("All PHP files should have valid syntax.\n\nErrors:\n%s", allOutput.toString())
                .isTrue();
    }

    @Test
    @DisplayName("Generated TypeScript/NestJS project should compile")
    @EnabledIfEnvironmentVariable(named = "CI_COMPILE_GENERATED_PROJECT", matches = "true")
    void generatedTypeScriptProjectShouldCompile() throws IOException, InterruptedException {
        // Load TypeScript fixture
        GenerateRequest request = loadFixture("fixtures/blog-api-typescript.json");
        String artifactId = request.getProject().getArtifactId();

        // Generate TypeScript project
        byte[] zipBytes = generatorService.generateProject(request);

        // Extract to temp directory
        extractZipToDirectory(zipBytes, tempDir);

        Path projectDir = tempDir.resolve(artifactId);

        // Run npm install
        ProcessBuilder installPb =
                new ProcessBuilder("npm", "install")
                        .directory(projectDir.toFile())
                        .redirectErrorStream(true);

        Process installProcess = installPb.start();
        StringBuilder installOutput = new StringBuilder();
        try (BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                installProcess.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                installOutput.append(line).append("\n");
            }
        }
        installProcess.waitFor(5, TimeUnit.MINUTES);

        // Run TypeScript compilation
        ProcessBuilder buildPb =
                new ProcessBuilder("npm", "run", "build")
                        .directory(projectDir.toFile())
                        .redirectErrorStream(true);

        Process buildProcess = buildPb.start();
        StringBuilder buildOutput = new StringBuilder();
        buildOutput.append("=== npm install output ===\n");
        buildOutput.append(installOutput);
        buildOutput.append("\n=== npm run build output ===\n");

        try (BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                buildProcess.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                buildOutput.append(line).append("\n");
            }
        }

        boolean finished = buildProcess.waitFor(5, TimeUnit.MINUTES);
        int exitCode = finished ? buildProcess.exitValue() : -1;

        assertThat(exitCode)
                .as(
                        "Generated TypeScript project should compile.\n\nBuild output:\n%s",
                        buildOutput.toString())
                .isZero();
    }

    @Test
    @DisplayName("Generated Go/Gin project should compile")
    @EnabledIfEnvironmentVariable(named = "CI_COMPILE_GENERATED_PROJECT", matches = "true")
    void generatedGoProjectShouldCompile() throws IOException, InterruptedException {
        // Load Go fixture
        GenerateRequest request = loadFixture("fixtures/blog-api-go.json");
        String artifactId = request.getProject().getArtifactId();

        // Generate Go project
        byte[] zipBytes = generatorService.generateProject(request);

        // Extract to temp directory
        extractZipToDirectory(zipBytes, tempDir);

        Path projectDir = tempDir.resolve(artifactId);

        // Initialize go module and download dependencies
        ProcessBuilder modPb =
                new ProcessBuilder("go", "mod", "tidy")
                        .directory(projectDir.toFile())
                        .redirectErrorStream(true);

        Process modProcess = modPb.start();
        StringBuilder modOutput = new StringBuilder();
        try (BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                modProcess.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                modOutput.append(line).append("\n");
            }
        }
        modProcess.waitFor(3, TimeUnit.MINUTES);

        // Run Go build
        ProcessBuilder buildPb =
                new ProcessBuilder("go", "build", "./...")
                        .directory(projectDir.toFile())
                        .redirectErrorStream(true);

        Process buildProcess = buildPb.start();
        StringBuilder buildOutput = new StringBuilder();
        buildOutput.append("=== go mod tidy output ===\n");
        buildOutput.append(modOutput);
        buildOutput.append("\n=== go build output ===\n");

        try (BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                buildProcess.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                buildOutput.append(line).append("\n");
            }
        }

        boolean finished = buildProcess.waitFor(5, TimeUnit.MINUTES);
        int exitCode = finished ? buildProcess.exitValue() : -1;

        assertThat(exitCode)
                .as(
                        "Generated Go project should compile.\n\nBuild output:\n%s",
                        buildOutput.toString())
                .isZero();
    }

    @Test
    @DisplayName("Generated Go/Chi project should compile")
    @EnabledIfEnvironmentVariable(named = "CI_COMPILE_GENERATED_PROJECT", matches = "true")
    void generatedGoChiProjectShouldCompile() throws IOException, InterruptedException {
        // Load Go/Chi fixture
        GenerateRequest request = loadFixture("fixtures/blog-api-go-chi.json");
        String artifactId = request.getProject().getArtifactId();

        // Generate Go/Chi project
        byte[] zipBytes = generatorService.generateProject(request);

        // Extract to temp directory
        extractZipToDirectory(zipBytes, tempDir);

        Path projectDir = tempDir.resolve(artifactId);

        // Initialize go module and download dependencies
        ProcessBuilder modPb =
                new ProcessBuilder("go", "mod", "tidy")
                        .directory(projectDir.toFile())
                        .redirectErrorStream(true);

        Process modProcess = modPb.start();
        StringBuilder modOutput = new StringBuilder();
        try (BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                modProcess.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                modOutput.append(line).append("\n");
            }
        }
        modProcess.waitFor(3, TimeUnit.MINUTES);

        // Run Go build
        ProcessBuilder buildPb =
                new ProcessBuilder("go", "build", "./...")
                        .directory(projectDir.toFile())
                        .redirectErrorStream(true);

        Process buildProcess = buildPb.start();
        StringBuilder buildOutput = new StringBuilder();
        buildOutput.append("=== go mod tidy output ===\n");
        buildOutput.append(modOutput);
        buildOutput.append("\n=== go build output ===\n");

        try (BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                buildProcess.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                buildOutput.append(line).append("\n");
            }
        }

        boolean finished = buildProcess.waitFor(5, TimeUnit.MINUTES);
        int exitCode = finished ? buildProcess.exitValue() : -1;

        assertThat(exitCode)
                .as(
                        "Generated Go/Chi project should compile.\n\nBuild output:\n%s",
                        buildOutput.toString())
                .isZero();
    }

    @Test
    @DisplayName("Generated Rust/Axum project should compile")
    @EnabledIfEnvironmentVariable(named = "CI_COMPILE_GENERATED_PROJECT", matches = "true")
    void generatedRustAxumProjectShouldCompile() throws IOException, InterruptedException {
        // Load Rust/Axum fixture
        GenerateRequest request = loadFixture("fixtures/blog-api-rust-axum.json");
        String artifactId = request.getProject().getArtifactId();

        // Generate Rust/Axum project
        byte[] zipBytes = generatorService.generateProject(request);

        // Extract to temp directory
        extractZipToDirectory(zipBytes, tempDir);

        Path projectDir = tempDir.resolve(artifactId);

        // Run cargo check (faster than full build, validates compilation)
        ProcessBuilder checkPb =
                new ProcessBuilder("cargo", "check")
                        .directory(projectDir.toFile())
                        .redirectErrorStream(true);

        Process checkProcess = checkPb.start();
        StringBuilder checkOutput = new StringBuilder();
        checkOutput.append("=== cargo check output ===\n");

        try (BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                checkProcess.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                checkOutput.append(line).append("\n");
            }
        }

        boolean finished = checkProcess.waitFor(10, TimeUnit.MINUTES);
        int exitCode = finished ? checkProcess.exitValue() : -1;

        assertThat(exitCode)
                .as(
                        "Generated Rust/Axum project should compile.\n\nBuild output:\n%s",
                        checkOutput.toString())
                .isZero();
    }

    /**
     * Checks if the build output indicates a JitPack dependency resolution failure. This happens
     * when a new version is tagged but JitPack hasn't built it yet or the build failed.
     */
    private boolean isJitPackDependencyError(String output) {
        return JITPACK_DEPENDENCY_ERROR.matcher(output).find();
    }

    /**
     * Updates the APiGen version in the project's build.gradle or build.gradle.kts file. This is
     * used by the fallback mechanism when a JitPack version is not available.
     */
    private void updateApigenVersionInBuildGradle(Path projectDir, String version)
            throws IOException {
        // Try build.gradle first (Java/Groovy)
        Path buildGradle = projectDir.resolve("build.gradle");
        if (Files.exists(buildGradle)) {
            String content = Files.readString(buildGradle, StandardCharsets.UTF_8);
            content =
                    content.replaceAll(
                            "com\\.github\\.jnzader\\.apigen:apigen-(core|security):v[\\d.]+",
                            "com.github.jnzader.apigen:apigen-$1:" + version);
            Files.writeString(buildGradle, content, StandardCharsets.UTF_8);
            return;
        }

        // Try build.gradle.kts (Kotlin DSL)
        Path buildGradleKts = projectDir.resolve("build.gradle.kts");
        if (Files.exists(buildGradleKts)) {
            String content = Files.readString(buildGradleKts, StandardCharsets.UTF_8);
            content =
                    content.replaceAll(
                            "com\\.github\\.jnzader\\.apigen:apigen-(core|security):v[\\d.]+",
                            "com.github.jnzader.apigen:apigen-$1:" + version);
            Files.writeString(buildGradleKts, content, StandardCharsets.UTF_8);
        }
    }
}
