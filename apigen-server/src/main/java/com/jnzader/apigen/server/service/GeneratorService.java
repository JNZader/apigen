package com.jnzader.apigen.server.service;

import static com.jnzader.apigen.server.service.generator.util.StringTransformationUtil.toPascalCase;

import com.jnzader.apigen.codegen.generator.CodeGenerator;
import com.jnzader.apigen.codegen.generator.api.Feature;
import com.jnzader.apigen.codegen.generator.api.ProjectConfig;
import com.jnzader.apigen.codegen.generator.api.ProjectGenerator;
import com.jnzader.apigen.codegen.generator.java.JavaSpringBootProjectGenerator;
import com.jnzader.apigen.codegen.generator.registry.GeneratorRegistry;
import com.jnzader.apigen.codegen.generator.registry.SimpleGeneratorRegistry;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.parser.SqlSchemaParser;
import com.jnzader.apigen.server.dto.GenerateRequest;
import com.jnzader.apigen.server.dto.GenerateResponse;
import com.jnzader.apigen.server.service.generator.ApiTestingGenerator;
import com.jnzader.apigen.server.service.generator.ApplicationConfigGenerator;
import com.jnzader.apigen.server.service.generator.BuildConfigGenerator;
import com.jnzader.apigen.server.service.generator.DockerConfigGenerator;
import com.jnzader.apigen.server.service.generator.GradleWrapperGenerator;
import com.jnzader.apigen.server.service.generator.ProjectStructureGenerator;
import com.jnzader.apigen.server.service.generator.util.FileArchiveService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Facade service for generating Spring Boot projects from SQL schemas. Delegates to specialized
 * generators for each component. Supports multi-language generation via GeneratorRegistry.
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
    private final GradleWrapperGenerator gradleWrapperGenerator;
    private final FileArchiveService fileArchiveService;
    private final GeneratorRegistry generatorRegistry;

    public GeneratorService() {
        this.schemaParser = new SqlSchemaParser();
        this.buildConfigGenerator = new BuildConfigGenerator();
        this.applicationConfigGenerator = new ApplicationConfigGenerator();
        this.projectStructureGenerator = new ProjectStructureGenerator();
        this.dockerConfigGenerator = new DockerConfigGenerator();
        this.apiTestingGenerator = new ApiTestingGenerator();
        this.gradleWrapperGenerator = new GradleWrapperGenerator();
        this.fileArchiveService = new FileArchiveService();
        this.generatorRegistry = createDefaultRegistry();
    }

    /** Creates the default generator registry with all available generators. */
    private static GeneratorRegistry createDefaultRegistry() {
        SimpleGeneratorRegistry registry = new SimpleGeneratorRegistry();
        registry.register(new JavaSpringBootProjectGenerator());
        // Future: registry.register(new KotlinSpringBootProjectGenerator());
        // Future: registry.register(new PythonFastApiProjectGenerator());
        return registry;
    }

    /** Returns the generator registry for listing available generators. */
    public GeneratorRegistry getGeneratorRegistry() {
        return generatorRegistry;
    }

    /**
     * Generates a project as a ZIP file using the appropriate generator based on target config.
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

        // Get target configuration (defaults to java/spring-boot)
        GenerateRequest.TargetConfig target = request.getTarget();
        if (target == null) {
            target = new GenerateRequest.TargetConfig();
        }
        String language = target.getLanguage();
        String framework = target.getFramework();

        log.info(
                "Starting project generation for: {} (target: {}/{})",
                artifactId,
                language,
                framework);
        log.debug("Base package: {}", basePackage);

        Path tempDir = Files.createTempDirectory("apigen-" + artifactId);

        try {
            log.info("Parsing SQL schema...");
            SqlSchema schema = schemaParser.parseString(sql);

            log.info(
                    "Found {} tables, {} junction tables",
                    schema.getTables().size(),
                    schema.getJunctionTables().size());

            if (!schema.getParseErrors().isEmpty()) {
                log.warn("Parse warnings: {}", schema.getParseErrors());
            }

            // Try to use new registry-based generator
            ProjectGenerator projectGenerator =
                    generatorRegistry.getGenerator(language, framework).orElse(null);

            int generatedFileCount;

            if (projectGenerator != null) {
                log.info(
                        "Using generator: {} ({})",
                        projectGenerator.getDisplayName(),
                        projectGenerator.getClass().getSimpleName());

                // Convert request config to new API ProjectConfig
                ProjectConfig projectConfig = toProjectConfig(request, tempDir);

                // Generate using new API
                Map<String, String> generatedFiles =
                        projectGenerator.generate(schema, projectConfig);
                generatedFileCount = generatedFiles.size();

                // Write generated files to temp directory
                writeGeneratedFiles(tempDir, generatedFiles);

                log.info("Generated {} files using new generator", generatedFileCount);
            } else {
                // Fallback to legacy CodeGenerator for unsupported targets
                log.warn(
                        "No generator found for {}/{}, falling back to legacy CodeGenerator",
                        language,
                        framework);
                CodeGenerator generator = new CodeGenerator(basePackage, tempDir);
                CodeGenerator.GenerationResult result = generator.generate(schema);
                generatedFileCount = result.getGeneratedFiles().size();

                log.info("Generated {} files using legacy generator", generatedFileCount);
            }

            // Generate additional project files (build.gradle, configs, Docker, etc.)
            generateProjectFiles(tempDir, request, schema);

            log.info("Creating ZIP archive...");
            byte[] zipBytes = fileArchiveService.createZipFromDirectory(tempDir, artifactId);

            long duration = System.currentTimeMillis() - startTime;
            log.info(
                    "Project generated successfully in {}ms ({} code files)",
                    duration,
                    generatedFileCount);

            return zipBytes;

        } finally {
            fileArchiveService.deleteDirectory(tempDir);
        }
    }

    /** Converts the request configuration to the new API ProjectConfig. */
    private ProjectConfig toProjectConfig(GenerateRequest request, Path outputDirectory) {
        GenerateRequest.ProjectConfig reqConfig = request.getProject();
        GenerateRequest.FeaturesConfig features = reqConfig.getFeatures();

        // Map request features to Feature enum
        Set<Feature> enabledFeatures = new HashSet<>();
        enabledFeatures.add(Feature.CRUD); // Always enabled

        if (features != null) {
            if (features.isHateoas()) enabledFeatures.add(Feature.HATEOAS);
            if (features.isAuditing()) enabledFeatures.add(Feature.AUDITING);
            if (features.isSoftDelete()) enabledFeatures.add(Feature.SOFT_DELETE);
            if (features.isCaching()) enabledFeatures.add(Feature.CACHING);
            if (features.isSwagger()) enabledFeatures.add(Feature.OPENAPI);
            if (features.isDocker()) enabledFeatures.add(Feature.DOCKER);
        }

        // Map database config
        ProjectConfig.DatabaseConfig dbConfig = null;
        if (reqConfig.getDatabase() != null) {
            GenerateRequest.DatabaseConfig reqDb = reqConfig.getDatabase();
            dbConfig =
                    ProjectConfig.DatabaseConfig.builder()
                            .type(reqDb.getType())
                            .name(reqDb.getName())
                            .port(reqDb.getPort())
                            .username(reqDb.getUsername())
                            .password(reqDb.getPassword())
                            .build();
        }

        return ProjectConfig.builder()
                .basePackage(reqConfig.getBasePackage())
                .projectName(reqConfig.getName())
                .artifactId(reqConfig.getArtifactId())
                .groupId(reqConfig.getGroupId())
                .outputDirectory(outputDirectory)
                .languageVersion(reqConfig.getJavaVersion())
                .frameworkVersion(reqConfig.getSpringBootVersion())
                .enabledFeatures(enabledFeatures)
                .database(dbConfig)
                .build();
    }

    /** Writes generated files from the new API to the filesystem. */
    private void writeGeneratedFiles(Path projectRoot, Map<String, String> files)
            throws IOException {
        for (Map.Entry<String, String> entry : files.entrySet()) {
            Path filePath = projectRoot.resolve(entry.getKey());
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, entry.getValue());
        }
    }

    /** Validates the generation request and returns any errors. */
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
                        .stats(
                                GenerateResponse.GenerationStats.builder()
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

    /** Generates additional project files using specialized generators. */
    private void generateProjectFiles(Path projectRoot, GenerateRequest request, SqlSchema schema)
            throws IOException {
        GenerateRequest.ProjectConfig config = request.getProject();

        // build.gradle
        String buildGradle = buildConfigGenerator.generateBuildGradle(config);
        Files.writeString(projectRoot.resolve("build.gradle"), buildGradle);

        // settings.gradle
        String settingsGradle = buildConfigGenerator.generateSettingsGradle(config.getArtifactId());
        Files.writeString(projectRoot.resolve("settings.gradle"), settingsGradle);

        // Gradle wrapper (gradlew, gradlew.bat, gradle-wrapper.jar, gradle-wrapper.properties)
        gradleWrapperGenerator.generateWrapperFiles(projectRoot);

        // application.yml
        String applicationYml = applicationConfigGenerator.generateApplicationYml(config);
        Path resourcesDir = projectRoot.resolve("src/main/resources");
        Files.createDirectories(resourcesDir);
        Files.writeString(resourcesDir.resolve("application.yml"), applicationYml);

        // application-docker.yml
        String applicationDockerYml =
                applicationConfigGenerator.generateApplicationDockerYml(config);
        Files.writeString(resourcesDir.resolve("application-docker.yml"), applicationDockerYml);

        // application-test.yml (in test resources)
        String applicationTestYml = applicationConfigGenerator.generateApplicationTestYml(config);
        Path testResourcesDir = projectRoot.resolve("src/test/resources");
        Files.createDirectories(testResourcesDir);
        Files.writeString(testResourcesDir.resolve("application-test.yml"), applicationTestYml);

        // ApplicationContextTest.java (smoke test to verify context loads)
        String contextTest = projectStructureGenerator.generateApplicationContextTest(config);
        Path testPackageDir =
                projectRoot
                        .resolve("src/test/java")
                        .resolve(config.getBasePackage().replace('.', '/'));
        Files.createDirectories(testPackageDir);
        Files.writeString(testPackageDir.resolve("ApplicationContextTest.java"), contextTest);

        // Main Application class
        String className = toPascalCase(config.getArtifactId()) + "Application";
        String mainClass = projectStructureGenerator.generateMainClass(config);
        Path mainPackageDir =
                projectRoot
                        .resolve("src/main/java")
                        .resolve(config.getBasePackage().replace('.', '/'));
        Files.createDirectories(mainPackageDir);
        Files.writeString(mainPackageDir.resolve(className + ".java"), mainClass);

        // .gitignore
        Files.writeString(
                projectRoot.resolve(".gitignore"), projectStructureGenerator.getGitignoreContent());

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
