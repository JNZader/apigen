package com.jnzader.apigen.server.service;

import com.jnzader.apigen.codegen.generator.CodeGenerator;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.parser.SqlSchemaParser;
import com.jnzader.apigen.server.dto.GenerateRequest;
import com.jnzader.apigen.server.dto.GenerateResponse;
import com.jnzader.apigen.server.service.generator.*;
import com.jnzader.apigen.server.service.generator.util.FileArchiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.jnzader.apigen.server.service.generator.util.StringTransformationUtil.toPascalCase;

/**
 * Facade service for generating Spring Boot projects from SQL schemas.
 * Delegates to specialized generators for each component.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GeneratorService {

    private final SqlSchemaParser schemaParser;
    private final BuildConfigGenerator buildConfigGenerator;
    private final ApplicationConfigGenerator applicationConfigGenerator;
    private final ProjectStructureGenerator projectStructureGenerator;
    private final DockerConfigGenerator dockerConfigGenerator;
    private final ApiTestingGenerator apiTestingGenerator;
    private final FileArchiveService fileArchiveService;

    public GeneratorService() {
        this.schemaParser = new SqlSchemaParser();
        this.buildConfigGenerator = new BuildConfigGenerator();
        this.applicationConfigGenerator = new ApplicationConfigGenerator();
        this.projectStructureGenerator = new ProjectStructureGenerator();
        this.dockerConfigGenerator = new DockerConfigGenerator();
        this.apiTestingGenerator = new ApiTestingGenerator();
        this.fileArchiveService = new FileArchiveService();
    }

    /**
     * Generates a Spring Boot project as a ZIP file.
     *
     * @param request The generation request with SQL and config
     * @return ZIP file bytes
     */
    @SuppressWarnings("java:S5443")
    public byte[] generateProject(GenerateRequest request) throws IOException {
        long startTime = System.currentTimeMillis();

        String sql = request.getSql();
        String basePackage = request.getProject().getBasePackage();
        String artifactId = request.getProject().getArtifactId();

        log.info("Starting project generation for: {}", artifactId);
        log.debug("Base package: {}", basePackage);

        Path tempDir = Files.createTempDirectory("apigen-" + artifactId);

        try {
            log.info("Parsing SQL schema...");
            SqlSchema schema = schemaParser.parseString(sql);

            log.info("Found {} tables, {} junction tables",
                    schema.getTables().size(),
                    schema.getJunctionTables().size());

            if (!schema.getParseErrors().isEmpty()) {
                log.warn("Parse warnings: {}", schema.getParseErrors());
            }

            log.info("Generating code...");
            CodeGenerator generator = new CodeGenerator(basePackage, tempDir);
            CodeGenerator.GenerationResult result = generator.generate(schema);

            log.info("Generated {} files", result.getGeneratedFiles().size());

            generateProjectFiles(tempDir, request, schema);

            log.info("Creating ZIP archive...");
            byte[] zipBytes = fileArchiveService.createZipFromDirectory(tempDir, artifactId);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Project generated successfully in {}ms", duration);

            return zipBytes;

        } finally {
            fileArchiveService.deleteDirectory(tempDir);
        }
    }

    /**
     * Validates the generation request and returns any errors.
     */
    public GenerateResponse validate(GenerateRequest request) {
        List<String> errors = new ArrayList<>();

        try {
            SqlSchema schema = schemaParser.parseString(request.getSql());

            if (schema.getEntityTables().isEmpty()) {
                errors.add("No valid entity tables found in SQL schema");
            }

            List<String> validationErrors = schema.validate();
            errors.addAll(validationErrors);

            if (errors.isEmpty()) {
                return GenerateResponse.builder()
                        .success(true)
                        .message("SQL schema is valid")
                        .stats(GenerateResponse.GenerationStats.builder()
                                .tablesProcessed(schema.getTables().size())
                                .entitiesGenerated(schema.getEntityTables().size())
                                .build())
                        .build();
            }

        } catch (Exception e) {
            errors.add("SQL parse error: " + e.getMessage());
        }

        return GenerateResponse.error("Validation failed", errors);
    }

    /**
     * Generates additional project files using specialized generators.
     */
    private void generateProjectFiles(Path projectRoot, GenerateRequest request, SqlSchema schema) throws IOException {
        GenerateRequest.ProjectConfig config = request.getProject();

        // build.gradle
        String buildGradle = buildConfigGenerator.generateBuildGradle(config);
        Files.writeString(projectRoot.resolve("build.gradle"), buildGradle);

        // settings.gradle
        String settingsGradle = buildConfigGenerator.generateSettingsGradle(config.getArtifactId());
        Files.writeString(projectRoot.resolve("settings.gradle"), settingsGradle);

        // application.yml
        String applicationYml = applicationConfigGenerator.generateApplicationYml(config);
        Path resourcesDir = projectRoot.resolve("src/main/resources");
        Files.createDirectories(resourcesDir);
        Files.writeString(resourcesDir.resolve("application.yml"), applicationYml);

        // application-docker.yml
        String applicationDockerYml = applicationConfigGenerator.generateApplicationDockerYml(config);
        Files.writeString(resourcesDir.resolve("application-docker.yml"), applicationDockerYml);

        // Main Application class
        String className = toPascalCase(config.getArtifactId()) + "Application";
        String mainClass = projectStructureGenerator.generateMainClass(config);
        Path mainPackageDir = projectRoot.resolve("src/main/java")
                .resolve(config.getBasePackage().replace('.', '/'));
        Files.createDirectories(mainPackageDir);
        Files.writeString(mainPackageDir.resolve(className + ".java"), mainClass);

        // .gitignore
        Files.writeString(projectRoot.resolve(".gitignore"), projectStructureGenerator.getGitignoreContent());

        // README.md
        String readme = projectStructureGenerator.generateReadme(config);
        Files.writeString(projectRoot.resolve("README.md"), readme);

        // HTTP test files for REST Client / IntelliJ
        String httpTests = apiTestingGenerator.generateHttpTestFile(schema);
        Files.writeString(projectRoot.resolve("api-tests.http"), httpTests);

        // Postman collection
        String postmanCollection = apiTestingGenerator.generatePostmanCollection(schema, config);
        Files.writeString(projectRoot.resolve("postman-collection.json"), postmanCollection);

        // Docker files (if enabled)
        if (config.getFeatures() != null && config.getFeatures().isDocker()) {
            String dockerfile = dockerConfigGenerator.generateDockerfile(config);
            Files.writeString(projectRoot.resolve("Dockerfile"), dockerfile);

            String dockerCompose = dockerConfigGenerator.generateDockerCompose(config);
            Files.writeString(projectRoot.resolve("docker-compose.yml"), dockerCompose);

            String dockerignore = dockerConfigGenerator.generateDockerignore();
            Files.writeString(projectRoot.resolve(".dockerignore"), dockerignore);

            String dockerReadme = dockerConfigGenerator.generateDockerReadme(config);
            Files.writeString(projectRoot.resolve("DOCKER.md"), dockerReadme);
        }
    }
}
