package com.jnzader.apigen.codegen.generator.typescript;

import static com.jnzader.apigen.codegen.generator.util.RelationshipUtils.*;

import com.jnzader.apigen.codegen.generator.api.Feature;
import com.jnzader.apigen.codegen.generator.api.LanguageTypeMapper;
import com.jnzader.apigen.codegen.generator.api.ProjectConfig;
import com.jnzader.apigen.codegen.generator.api.ProjectGenerator;
import com.jnzader.apigen.codegen.generator.typescript.auth.TypeScriptJwtAuthGenerator;
import com.jnzader.apigen.codegen.generator.typescript.auth.TypeScriptRateLimitGenerator;
import com.jnzader.apigen.codegen.generator.typescript.config.TypeScriptConfigGenerator;
import com.jnzader.apigen.codegen.generator.typescript.controller.TypeScriptControllerGenerator;
import com.jnzader.apigen.codegen.generator.typescript.dto.TypeScriptDTOGenerator;
import com.jnzader.apigen.codegen.generator.typescript.entity.TypeScriptEntityGenerator;
import com.jnzader.apigen.codegen.generator.typescript.mail.TypeScriptMailServiceGenerator;
import com.jnzader.apigen.codegen.generator.typescript.module.TypeScriptModuleGenerator;
import com.jnzader.apigen.codegen.generator.typescript.repository.TypeScriptRepositoryGenerator;
import com.jnzader.apigen.codegen.generator.typescript.security.reset.TypeScriptPasswordResetGenerator;
import com.jnzader.apigen.codegen.generator.typescript.security.social.TypeScriptSocialLoginGenerator;
import com.jnzader.apigen.codegen.generator.typescript.service.TypeScriptServiceGenerator;
import com.jnzader.apigen.codegen.generator.typescript.storage.TypeScriptFileStorageGenerator;
import com.jnzader.apigen.codegen.generator.typescript.test.TypeScriptTestGenerator;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Project generator for TypeScript/NestJS applications.
 *
 * <p>This generator creates complete NestJS API projects from SQL schemas, including:
 *
 * <ul>
 *   <li>TypeORM entities with relationships
 *   <li>DTOs with class-validator decorators
 *   <li>Repositories for data access
 *   <li>Services with business logic
 *   <li>Controllers with OpenAPI decorators
 *   <li>NestJS modules
 *   <li>Project configuration files (package.json, tsconfig.json, etc.)
 * </ul>
 */
@SuppressWarnings("java:S3400") // Template methods return constants for code generation
public class TypeScriptNestJsProjectGenerator implements ProjectGenerator {

    private static final String LANGUAGE = "typescript";
    private static final String FRAMEWORK = "nestjs";
    private static final String DEFAULT_TS_VERSION = "5.9";
    private static final String DEFAULT_FRAMEWORK_VERSION = "11.1";

    private static final Set<Feature> SUPPORTED_FEATURES =
            Set.of(
                    Feature.CRUD,
                    Feature.AUDITING,
                    Feature.SOFT_DELETE,
                    Feature.FILTERING,
                    Feature.PAGINATION,
                    Feature.OPENAPI,
                    Feature.DOCKER,
                    Feature.MANY_TO_ONE,
                    Feature.ONE_TO_MANY,
                    // Security features
                    Feature.JWT_AUTH,
                    Feature.RATE_LIMITING,
                    // Feature Pack 2025
                    Feature.MAIL_SERVICE,
                    Feature.PASSWORD_RESET,
                    Feature.SOCIAL_LOGIN,
                    Feature.FILE_UPLOAD,
                    Feature.S3_STORAGE,
                    Feature.AZURE_STORAGE,
                    // Testing
                    Feature.UNIT_TESTS,
                    Feature.INTEGRATION_TESTS);

    private final TypeScriptTypeMapper typeMapper = new TypeScriptTypeMapper();

    @Override
    public String getLanguage() {
        return LANGUAGE;
    }

    @Override
    public String getFramework() {
        return FRAMEWORK;
    }

    @Override
    public String getDisplayName() {
        return "TypeScript / NestJS 11.x";
    }

    @Override
    public Set<Feature> getSupportedFeatures() {
        return SUPPORTED_FEATURES;
    }

    @Override
    public LanguageTypeMapper getTypeMapper() {
        return typeMapper;
    }

    @Override
    public String getDefaultLanguageVersion() {
        return DEFAULT_TS_VERSION;
    }

    @Override
    public String getDefaultFrameworkVersion() {
        return DEFAULT_FRAMEWORK_VERSION;
    }

    @Override
    public Map<String, String> generate(SqlSchema schema, ProjectConfig config) {
        Map<String, String> files = new LinkedHashMap<>();
        String projectName = config.getProjectName();

        // Initialize specialized generators
        TypeScriptEntityGenerator entityGenerator = new TypeScriptEntityGenerator();
        TypeScriptDTOGenerator dtoGenerator = new TypeScriptDTOGenerator();
        TypeScriptRepositoryGenerator repositoryGenerator = new TypeScriptRepositoryGenerator();
        TypeScriptServiceGenerator serviceGenerator = new TypeScriptServiceGenerator();
        TypeScriptControllerGenerator controllerGenerator = new TypeScriptControllerGenerator();
        TypeScriptModuleGenerator moduleGenerator = new TypeScriptModuleGenerator();
        TypeScriptConfigGenerator configGenerator = new TypeScriptConfigGenerator(projectName);

        // Collect all relationships for bidirectional mapping
        Map<String, List<SqlSchema.TableRelationship>> relationshipsByTable =
                buildRelationshipsByTable(schema);

        // Generate shared files
        files.put("src/entities/base.entity.ts", entityGenerator.generateBaseEntity());
        files.put("src/dto/paginated-response.dto.ts", dtoGenerator.generatePaginatedResponseDto());
        files.put("src/dto/base-response.dto.ts", dtoGenerator.generateBaseResponseDto());

        // Generate main.ts
        files.put("src/main.ts", moduleGenerator.generateMain(projectName));

        // Generate app.module.ts
        files.put("src/app.module.ts", moduleGenerator.generateAppModule(schema, projectName));

        // Generate code for each entity table
        for (SqlTable table : schema.getEntityTables()) {
            String className = table.getEntityName();
            String kebabName = typeMapper.toKebabCase(className);
            String modulePath = "src/modules/" + kebabName;

            List<SqlSchema.TableRelationship> tableRelations =
                    getRelationshipsForTable(table.getName(), relationshipsByTable);

            // Find inverse relationships (where this table is the target)
            List<SqlSchema.TableRelationship> inverseRelations =
                    findInverseRelationships(table, schema);

            // 1. Generate Entity
            String entityCode = entityGenerator.generate(table, tableRelations, inverseRelations);
            files.put(modulePath + "/entities/" + kebabName + ".entity.ts", entityCode);

            // 2. Generate DTOs
            String createDtoCode = dtoGenerator.generateCreateDto(table, tableRelations);
            files.put(modulePath + "/dto/create-" + kebabName + ".dto.ts", createDtoCode);

            String updateDtoCode = dtoGenerator.generateUpdateDto(table, tableRelations);
            files.put(modulePath + "/dto/update-" + kebabName + ".dto.ts", updateDtoCode);

            String responseDtoCode = dtoGenerator.generateResponseDto(table, tableRelations);
            files.put(modulePath + "/dto/" + kebabName + "-response.dto.ts", responseDtoCode);

            // 3. Generate Repository
            String repositoryCode = repositoryGenerator.generate(table);
            files.put(modulePath + "/repositories/" + kebabName + ".repository.ts", repositoryCode);

            // 4. Generate Service
            String serviceCode = serviceGenerator.generate(table);
            files.put(modulePath + "/services/" + kebabName + ".service.ts", serviceCode);

            // 5. Generate Controller
            String controllerCode = controllerGenerator.generate(table);
            files.put(modulePath + "/controllers/" + kebabName + ".controller.ts", controllerCode);

            // 6. Generate Module
            String moduleCode = moduleGenerator.generate(table);
            files.put(modulePath + "/" + kebabName + ".module.ts", moduleCode);

            // 7. Generate index.ts for module barrel export
            files.put(modulePath + "/index.ts", generateModuleIndex(kebabName));
        }

        // Generate configuration files
        boolean hasJwtAuth = config.isFeatureEnabled(Feature.JWT_AUTH);
        boolean hasRateLimit = config.isFeatureEnabled(Feature.RATE_LIMITING);
        files.putAll(configGenerator.generate(schema, config, hasJwtAuth, hasRateLimit));

        // Generate security files
        generateSecurityFiles(files, config);

        // Generate test directory structure
        files.put("test/.gitkeep", "");
        files.put("test/jest-e2e.json", generateJestE2eConfig());

        // Generate tests for each entity
        if (config.isFeatureEnabled(Feature.UNIT_TESTS)
                || config.isFeatureEnabled(Feature.INTEGRATION_TESTS)) {
            TypeScriptTestGenerator testGenerator = new TypeScriptTestGenerator();

            for (SqlTable table : schema.getEntityTables()) {
                if (config.isFeatureEnabled(Feature.UNIT_TESTS)) {
                    files.putAll(testGenerator.generateTests(table));
                }
                if (config.isFeatureEnabled(Feature.INTEGRATION_TESTS)) {
                    String kebabName = typeMapper.toKebabCase(table.getEntityName());
                    files.put(
                            "test/" + kebabName + ".e2e-spec.ts",
                            testGenerator.generateE2eTest(table));
                }
            }
        }

        return files;
    }

    /** Generates security-related files based on enabled features. */
    private void generateSecurityFiles(Map<String, String> files, ProjectConfig config) {
        // JWT Authentication
        if (config.isFeatureEnabled(Feature.JWT_AUTH)) {
            TypeScriptJwtAuthGenerator jwtGenerator = new TypeScriptJwtAuthGenerator();
            // 30 minute access token, 7 day refresh token
            files.putAll(jwtGenerator.generate(30, 7 * 24 * 60));
        }

        // Rate Limiting
        if (config.isFeatureEnabled(Feature.RATE_LIMITING)) {
            TypeScriptRateLimitGenerator rateLimitGenerator = new TypeScriptRateLimitGenerator();
            // 60 seconds TTL, 100 requests limit, no Redis by default
            files.putAll(rateLimitGenerator.generate(60, 100, false));
        }

        // Feature Pack 2025

        // Mail Service
        if (config.isFeatureEnabled(Feature.MAIL_SERVICE)) {
            TypeScriptMailServiceGenerator mailGenerator = new TypeScriptMailServiceGenerator();
            boolean hasPasswordReset = config.isFeatureEnabled(Feature.PASSWORD_RESET);
            files.putAll(mailGenerator.generate(true, hasPasswordReset, true));
        }

        // Password Reset
        if (config.isFeatureEnabled(Feature.PASSWORD_RESET)) {
            TypeScriptPasswordResetGenerator resetGenerator =
                    new TypeScriptPasswordResetGenerator();
            // 30 minute token expiration
            files.putAll(resetGenerator.generate(30));
        }

        // Social Login (OAuth2)
        if (config.isFeatureEnabled(Feature.SOCIAL_LOGIN)) {
            TypeScriptSocialLoginGenerator socialGenerator = new TypeScriptSocialLoginGenerator();
            files.putAll(socialGenerator.generate(List.of("google", "github")));
        }

        // File Storage
        if (config.isFeatureEnabled(Feature.FILE_UPLOAD)) {
            TypeScriptFileStorageGenerator storageGenerator = new TypeScriptFileStorageGenerator();
            boolean useS3 = config.isFeatureEnabled(Feature.S3_STORAGE);
            boolean useAzure = config.isFeatureEnabled(Feature.AZURE_STORAGE);
            files.putAll(storageGenerator.generate(useS3, useAzure));
        }
    }

    private String generateModuleIndex(String kebabName) {
        return """
        export * from './%s.module';
        export * from './entities/%s.entity';
        export * from './dto/create-%s.dto';
        export * from './dto/update-%s.dto';
        export * from './dto/%s-response.dto';
        export * from './services/%s.service';
        export * from './controllers/%s.controller';
        """
                .formatted(
                        kebabName, kebabName, kebabName, kebabName, kebabName, kebabName,
                        kebabName);
    }

    private String generateJestE2eConfig() {
        return """
        {
          "moduleFileExtensions": ["js", "json", "ts"],
          "rootDir": ".",
          "testEnvironment": "node",
          "testRegex": ".e2e-spec.ts$",
          "transform": {
            "^.+\\\\.(t|j)s$": "ts-jest"
          }
        }
        """;
    }

    @Override
    public List<String> validateConfig(ProjectConfig config) {
        List<String> errors = new ArrayList<>();

        if (config.getProjectName() == null || config.getProjectName().isBlank()) {
            errors.add("Project name is required for TypeScript/NestJS projects");
        }

        return errors;
    }
}
