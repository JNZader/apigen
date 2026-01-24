package com.jnzader.apigen.server.service;

import static com.jnzader.apigen.server.service.generator.util.StringTransformationUtil.toPascalCase;

import com.jnzader.apigen.codegen.generator.CodeGenerator;
import com.jnzader.apigen.codegen.generator.api.Feature;
import com.jnzader.apigen.codegen.generator.api.ProjectConfig;
import com.jnzader.apigen.codegen.generator.api.ProjectGenerator;
import com.jnzader.apigen.codegen.generator.csharp.CSharpAspNetCoreProjectGenerator;
import com.jnzader.apigen.codegen.generator.go.GoGinProjectGenerator;
import com.jnzader.apigen.codegen.generator.java.JavaSpringBootProjectGenerator;
import com.jnzader.apigen.codegen.generator.kotlin.KotlinSpringBootProjectGenerator;
import com.jnzader.apigen.codegen.generator.php.PhpLaravelProjectGenerator;
import com.jnzader.apigen.codegen.generator.python.PythonFastApiProjectGenerator;
import com.jnzader.apigen.codegen.generator.registry.GeneratorRegistry;
import com.jnzader.apigen.codegen.generator.registry.SimpleGeneratorRegistry;
import com.jnzader.apigen.codegen.generator.typescript.TypeScriptNestJsProjectGenerator;
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
        registry.register(new KotlinSpringBootProjectGenerator());
        registry.register(new CSharpAspNetCoreProjectGenerator());
        registry.register(new PythonFastApiProjectGenerator());
        registry.register(new PhpLaravelProjectGenerator());
        registry.register(new TypeScriptNestJsProjectGenerator());
        registry.register(new GoGinProjectGenerator());
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

        // Get target language (default to Java)
        GenerateRequest.TargetConfig target = request.getTarget();
        String language = target != null ? target.getLanguage() : "java";

        // Language-specific build files
        if ("csharp".equals(language)) {
            generateCSharpProjectFiles(projectRoot, config);
        } else if ("kotlin".equals(language)) {
            generateKotlinProjectFiles(projectRoot, config, schema);
        } else if ("python".equals(language)) {
            // Python project files are generated by PythonConfigGenerator
            // No additional files needed here
        } else if ("php".equals(language)) {
            // PHP project files are generated by PhpConfigGenerator
            // No additional files needed here
        } else if ("typescript".equals(language)) {
            // TypeScript project files are generated by TypeScriptConfigGenerator
            // No additional files needed here
        } else if ("go".equals(language)) {
            // Go project files are generated by GoConfigGenerator
            // No additional files needed here
        } else {
            generateJavaProjectFiles(projectRoot, config, schema);
        }

        // .gitignore (language-aware) - Python/PHP/TypeScript/Go generate their own via
        // ConfigGenerator
        if (!"python".equals(language)
                && !"php".equals(language)
                && !"typescript".equals(language)
                && !"go".equals(language)) {
            Files.writeString(projectRoot.resolve(".gitignore"), getGitignoreContent(language));
        }

        // README.md - Python/PHP/TypeScript/Go generate their own via ConfigGenerator
        if (!"python".equals(language)
                && !"php".equals(language)
                && !"typescript".equals(language)
                && !"go".equals(language)) {
            String readme = projectStructureGenerator.generateReadme(config);
            Files.writeString(projectRoot.resolve("README.md"), readme);
        }

        // HTTP test files (for Java and Kotlin only, not C#, Python, PHP, TypeScript, or Go)
        if (!"csharp".equals(language)
                && !"python".equals(language)
                && !"php".equals(language)
                && !"typescript".equals(language)
                && !"go".equals(language)) {
            String httpTests = apiTestingGenerator.generateHttpTestFile(schema);
            Files.writeString(projectRoot.resolve("api-tests.http"), httpTests);

            String postmanCollection =
                    apiTestingGenerator.generatePostmanCollection(schema, config);
            Files.writeString(projectRoot.resolve("postman-collection.json"), postmanCollection);
        }

        // Docker files (if enabled, for Java/Kotlin - C#/Python/PHP/TypeScript have their own)
        if (config.getFeatures() != null && config.getFeatures().isDocker()) {
            if ("csharp".equals(language)) {
                generateCSharpDockerFiles(projectRoot, config);
            } else if ("python".equals(language)) {
                // Python Docker files are generated by PythonConfigGenerator
            } else if ("php".equals(language)) {
                // PHP Docker files are generated by PhpConfigGenerator
            } else if ("typescript".equals(language)) {
                // TypeScript Docker files are generated by TypeScriptConfigGenerator
            } else if ("go".equals(language)) {
                // Go Docker files are generated by GoConfigGenerator
            } else {
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

    /** Generates Java-specific project files (build.gradle, Application.java, etc.). */
    private void generateJavaProjectFiles(
            Path projectRoot, GenerateRequest.ProjectConfig config, SqlSchema schema)
            throws IOException {
        // build.gradle
        String buildGradle = buildConfigGenerator.generateBuildGradle(config);
        Files.writeString(projectRoot.resolve("build.gradle"), buildGradle);

        // settings.gradle
        String settingsGradle = buildConfigGenerator.generateSettingsGradle(config.getArtifactId());
        Files.writeString(projectRoot.resolve("settings.gradle"), settingsGradle);

        // Gradle wrapper
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

        // application-test.yml
        String applicationTestYml = applicationConfigGenerator.generateApplicationTestYml(config);
        Path testResourcesDir = projectRoot.resolve("src/test/resources");
        Files.createDirectories(testResourcesDir);
        Files.writeString(testResourcesDir.resolve("application-test.yml"), applicationTestYml);

        // ApplicationContextTest.java
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
    }

    /** Generates Kotlin-specific project files (build.gradle.kts, Application.kt, etc.). */
    private void generateKotlinProjectFiles(
            Path projectRoot, GenerateRequest.ProjectConfig config, SqlSchema schema)
            throws IOException {
        // build.gradle.kts
        String buildGradleKts = buildConfigGenerator.generateKotlinBuildGradle(config);
        Files.writeString(projectRoot.resolve("build.gradle.kts"), buildGradleKts);

        // settings.gradle.kts
        String settingsGradleKts = "rootProject.name = \"%s\"%n".formatted(config.getArtifactId());
        Files.writeString(projectRoot.resolve("settings.gradle.kts"), settingsGradleKts);

        // Gradle wrapper
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

        // application-test.yml
        String applicationTestYml = applicationConfigGenerator.generateApplicationTestYml(config);
        Path testResourcesDir = projectRoot.resolve("src/test/resources");
        Files.createDirectories(testResourcesDir);
        Files.writeString(testResourcesDir.resolve("application-test.yml"), applicationTestYml);

        // ApplicationContextTest.kt
        String contextTest = generateKotlinContextTest(config);
        Path testPackageDir =
                projectRoot
                        .resolve("src/test/kotlin")
                        .resolve(config.getBasePackage().replace('.', '/'));
        Files.createDirectories(testPackageDir);
        Files.writeString(testPackageDir.resolve("ApplicationContextTest.kt"), contextTest);

        // Main Application class (Kotlin)
        String className = toPascalCase(config.getArtifactId()) + "Application";
        String mainClass = generateKotlinMainClass(config, className);
        Path mainPackageDir =
                projectRoot
                        .resolve("src/main/kotlin")
                        .resolve(config.getBasePackage().replace('.', '/'));
        Files.createDirectories(mainPackageDir);
        Files.writeString(mainPackageDir.resolve(className + ".kt"), mainClass);
    }

    /** Generates C#-specific project files (appsettings.json already generated by codegen). */
    private void generateCSharpProjectFiles(Path projectRoot, GenerateRequest.ProjectConfig config)
            throws IOException {
        // C# projects have their config files generated by CSharpConfigGenerator
        // (Program.cs, appsettings.json, .csproj)
        // We just need to add a global.json for SDK version pinning
        String globalJson = generateGlobalJson();
        Files.writeString(projectRoot.resolve("global.json"), globalJson);
    }

    /** Generates C# Docker files. */
    private void generateCSharpDockerFiles(Path projectRoot, GenerateRequest.ProjectConfig config)
            throws IOException {
        String namespace = config.getBasePackage();
        String dockerfile = generateCSharpDockerfile(namespace);
        Files.writeString(projectRoot.resolve("Dockerfile"), dockerfile);

        String dockerCompose = generateCSharpDockerCompose(config);
        Files.writeString(projectRoot.resolve("docker-compose.yml"), dockerCompose);

        Files.writeString(projectRoot.resolve(".dockerignore"), getCSharpDockerignore());
    }

    /** Generates Kotlin main Application class. */
    private String generateKotlinMainClass(GenerateRequest.ProjectConfig config, String className) {
        String basePackage = config.getBasePackage();
        boolean hasSecurity = config.getModules() != null && config.getModules().isSecurity();

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(basePackage).append("\n\n");
        sb.append("import org.springframework.boot.autoconfigure.SpringBootApplication\n");
        sb.append("import org.springframework.boot.runApplication\n");

        if (hasSecurity) {
            sb.append("import org.springframework.boot.persistence.autoconfigure.EntityScan\n");
            sb.append(
                    "import"
                        + " org.springframework.data.jpa.repository.config.EnableJpaRepositories\n");
            sb.append("\n");
            sb.append("@EnableJpaRepositories(basePackages = [\"")
                    .append(basePackage)
                    .append("\", \"com.jnzader.apigen.security.domain.repository\"])\n");
            sb.append("@EntityScan(basePackages = [\"")
                    .append(basePackage)
                    .append("\", \"com.jnzader.apigen.security.domain.entity\"])\n");
        } else {
            sb.append("import org.springframework.boot.persistence.autoconfigure.EntityScan\n");
            sb.append(
                    "import"
                        + " org.springframework.data.jpa.repository.config.EnableJpaRepositories\n");
            sb.append("\n");
            sb.append("@EnableJpaRepositories(basePackages = [\"")
                    .append(basePackage)
                    .append("\"])\n");
            sb.append("@EntityScan(basePackages = [\"").append(basePackage).append("\"])\n");
        }

        sb.append("@SpringBootApplication\n");
        sb.append("class ").append(className).append("\n\n");
        sb.append("fun main(args: Array<String>) {\n");
        sb.append("    runApplication<").append(className).append(">(*args)\n");
        sb.append("}\n");

        return sb.toString();
    }

    /** Generates Kotlin ApplicationContextTest. */
    private String generateKotlinContextTest(GenerateRequest.ProjectConfig config) {
        String basePackage = config.getBasePackage();
        String className = toPascalCase(config.getArtifactId()) + "Application";

        return """
        package %s

        import org.junit.jupiter.api.Test
        import org.springframework.boot.test.context.SpringBootTest
        import org.springframework.test.context.ActiveProfiles

        @SpringBootTest(classes = [%s::class])
        @ActiveProfiles("test")
        class ApplicationContextTest {

            @Test
            fun contextLoads() {
                // Verifies Spring context loads successfully
            }
        }
        """
                .formatted(basePackage, className);
    }

    /** Generates global.json for C# SDK version pinning. */
    private String generateGlobalJson() {
        return """
        {
          "sdk": {
            "version": "8.0.0",
            "rollForward": "latestMinor"
          }
        }
        """;
    }

    /** Generates C# Dockerfile. */
    private String generateCSharpDockerfile(String namespace) {
        return """
        FROM mcr.microsoft.com/dotnet/aspnet:8.0 AS base
        WORKDIR /app
        EXPOSE 80
        EXPOSE 443

        FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
        WORKDIR /src
        COPY ["%s.csproj", "./"]
        RUN dotnet restore "%s.csproj"
        COPY . .
        RUN dotnet build "%s.csproj" -c Release -o /app/build

        FROM build AS publish
        RUN dotnet publish "%s.csproj" -c Release -o /app/publish

        FROM base AS final
        WORKDIR /app
        COPY --from=publish /app/publish .
        ENTRYPOINT ["dotnet", "%s.dll"]
        """
                .formatted(namespace, namespace, namespace, namespace, namespace);
    }

    /** Generates C# docker-compose.yml. */
    private String generateCSharpDockerCompose(GenerateRequest.ProjectConfig config) {
        String artifactId = config.getArtifactId();
        return """
        version: '3.8'

        services:
          api:
            build: .
            ports:
              - "5000:80"
              - "5001:443"
            environment:
              - ASPNETCORE_ENVIRONMENT=Development
              - ConnectionStrings__DefaultConnection=Host=db;Database=%s;Username=postgres;Password=postgres
            depends_on:
              - db

          db:
            image: postgres:16-alpine
            environment:
              POSTGRES_USER: postgres
              POSTGRES_PASSWORD: postgres
              POSTGRES_DB: %s
            ports:
              - "5432:5432"
            volumes:
              - postgres_data:/var/lib/postgresql/data

        volumes:
          postgres_data:
        """
                .formatted(artifactId, artifactId);
    }

    /** Gets C# .dockerignore content. */
    private String getCSharpDockerignore() {
        return """
        **/.dockerignore
        **/.env
        **/.git
        **/.gitignore
        **/.vs
        **/bin
        **/obj
        **/Dockerfile*
        **/docker-compose*
        **/*.user
        **/*.suo
        **/.idea
        """;
    }

    /** Gets language-aware .gitignore content. */
    private String getGitignoreContent(String language) {
        if ("csharp".equals(language)) {
            return """
            ## Visual Studio / .NET
            bin/
            obj/
            .vs/
            *.user
            *.suo
            *.userosscache
            *.sln.docstates

            ## JetBrains Rider
            .idea/
            *.sln.iml

            ## Build results
            [Dd]ebug/
            [Dd]ebugPublic/
            [Rr]elease/
            [Rr]eleases/
            x64/
            x86/
            bld/
            [Bb]in/
            [Oo]bj/

            ## NuGet
            *.nupkg
            **/packages/*
            project.lock.json
            project.fragment.lock.json

            ## Misc
            .DS_Store
            *.log
            """;
        }
        if ("python".equals(language)) {
            // Python .gitignore is generated by PythonConfigGenerator
            // Return empty to avoid overwriting
            return "";
        }
        return projectStructureGenerator.getGitignoreContent();
    }
}
