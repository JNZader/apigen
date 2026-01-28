package com.jnzader.apigen.codegen.generator.kotlin;

import com.jnzader.apigen.codegen.generator.api.Feature;
import com.jnzader.apigen.codegen.generator.api.LanguageTypeMapper;
import com.jnzader.apigen.codegen.generator.api.ProjectConfig;
import com.jnzader.apigen.codegen.generator.api.ProjectGenerator;
import com.jnzader.apigen.codegen.generator.common.ManyToManyRelation;
import com.jnzader.apigen.codegen.generator.dx.DxFeaturesGenerator;
import com.jnzader.apigen.codegen.generator.dx.DxLanguage;
import com.jnzader.apigen.codegen.generator.java.jte.JteGenerator;
import com.jnzader.apigen.codegen.generator.kotlin.controller.KotlinControllerGenerator;
import com.jnzader.apigen.codegen.generator.kotlin.dto.KotlinDTOGenerator;
import com.jnzader.apigen.codegen.generator.kotlin.entity.KotlinEntityGenerator;
import com.jnzader.apigen.codegen.generator.kotlin.mail.KotlinMailServiceGenerator;
import com.jnzader.apigen.codegen.generator.kotlin.mapper.KotlinMapperGenerator;
import com.jnzader.apigen.codegen.generator.kotlin.repository.KotlinRepositoryGenerator;
import com.jnzader.apigen.codegen.generator.kotlin.security.reset.KotlinPasswordResetGenerator;
import com.jnzader.apigen.codegen.generator.kotlin.security.social.KotlinSocialLoginGenerator;
import com.jnzader.apigen.codegen.generator.kotlin.service.KotlinServiceGenerator;
import com.jnzader.apigen.codegen.generator.kotlin.storage.KotlinFileStorageGenerator;
import com.jnzader.apigen.codegen.generator.kotlin.test.KotlinTestGenerator;
import com.jnzader.apigen.codegen.generator.migration.MigrationGenerator;
import com.jnzader.apigen.codegen.generator.util.RelationshipUtils;
import com.jnzader.apigen.codegen.model.SqlFunction;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlSchema.TableRelationship;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Project generator for Kotlin/Spring Boot applications.
 *
 * <p>This generator creates complete Spring Boot projects in Kotlin from SQL schemas, including:
 *
 * <ul>
 *   <li>JPA Entity classes with relationships
 *   <li>Data class DTOs with validation annotations
 *   <li>MapStruct Mappers
 *   <li>Spring Data JPA Repositories
 *   <li>Service interfaces and implementations
 *   <li>REST Controllers
 *   <li>Flyway migrations
 *   <li>Unit and integration tests
 * </ul>
 */
public class KotlinSpringBootProjectGenerator implements ProjectGenerator {

    private static final String LANGUAGE = "kotlin";
    private static final String FRAMEWORK = "spring-boot";
    private static final String DEFAULT_KOTLIN_VERSION = "2.3.0";
    private static final String DEFAULT_SPRING_BOOT_VERSION = "4.0.0";

    private static final Set<Feature> SUPPORTED_FEATURES =
            Set.of(
                    Feature.CRUD,
                    Feature.HATEOAS,
                    Feature.AUDITING,
                    Feature.SOFT_DELETE,
                    Feature.ETAG_CACHING,
                    Feature.CACHING,
                    Feature.FILTERING,
                    Feature.PAGINATION,
                    Feature.OPENAPI,
                    Feature.JWT_AUTH,
                    Feature.OAUTH2,
                    Feature.RATE_LIMITING,
                    Feature.MIGRATIONS,
                    Feature.UNIT_TESTS,
                    Feature.INTEGRATION_TESTS,
                    Feature.DOCKER,
                    Feature.MANY_TO_MANY,
                    Feature.ONE_TO_MANY,
                    Feature.MANY_TO_ONE,
                    Feature.BATCH_OPERATIONS,
                    // Feature Pack 2025
                    Feature.SOCIAL_LOGIN,
                    Feature.PASSWORD_RESET,
                    Feature.MAIL_SERVICE,
                    Feature.FILE_UPLOAD,
                    Feature.S3_STORAGE,
                    Feature.AZURE_STORAGE,
                    Feature.JTE_TEMPLATES,
                    // Developer Experience Features
                    Feature.MISE_TASKS,
                    Feature.PRE_COMMIT,
                    Feature.SETUP_SCRIPT,
                    Feature.GITHUB_TEMPLATES,
                    Feature.DEV_COMPOSE);

    // Package path constants
    private static final String PKG_DOMAIN_ENTITY = "domain/entity";
    private static final String PKG_APPLICATION_SERVICE = "application/service";
    private static final String PKG_APPLICATION_DTO = "application/dto";
    private static final String PKG_APPLICATION_MAPPER = "application/mapper";
    private static final String PKG_INFRASTRUCTURE_REPOSITORY = "infrastructure/repository";
    private static final String PKG_INFRASTRUCTURE_CONTROLLER = "infrastructure/controller";

    private final KotlinTypeMapper typeMapper = new KotlinTypeMapper();

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
        return "Kotlin / Spring Boot 4.x";
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
        return DEFAULT_KOTLIN_VERSION;
    }

    @Override
    public String getDefaultFrameworkVersion() {
        return DEFAULT_SPRING_BOOT_VERSION;
    }

    @Override
    public Map<String, String> generate(SqlSchema schema, ProjectConfig config) {
        Map<String, String> files = new LinkedHashMap<>();
        String basePackage = config.getBasePackage();

        // Initialize specialized generators
        KotlinEntityGenerator entityGenerator = new KotlinEntityGenerator(basePackage);
        KotlinDTOGenerator dtoGenerator = new KotlinDTOGenerator(basePackage);
        KotlinMapperGenerator mapperGenerator = new KotlinMapperGenerator(basePackage);
        KotlinRepositoryGenerator repositoryGenerator = new KotlinRepositoryGenerator(basePackage);
        KotlinServiceGenerator serviceGenerator = new KotlinServiceGenerator(basePackage);
        KotlinControllerGenerator controllerGenerator = new KotlinControllerGenerator(basePackage);
        MigrationGenerator migrationGenerator = new MigrationGenerator();
        KotlinTestGenerator testGenerator = new KotlinTestGenerator(basePackage);

        // Build relationships map using utility
        Map<String, List<TableRelationship>> relationshipsByTable =
                RelationshipUtils.buildRelationshipsByTable(schema);

        int migrationVersion = 2;

        // Initialize Feature Pack generators
        KotlinMailServiceGenerator mailGenerator = new KotlinMailServiceGenerator(basePackage);
        KotlinPasswordResetGenerator passwordResetGenerator =
                new KotlinPasswordResetGenerator(basePackage);
        KotlinSocialLoginGenerator socialLoginGenerator =
                new KotlinSocialLoginGenerator(basePackage);
        KotlinFileStorageGenerator fileStorageGenerator =
                new KotlinFileStorageGenerator(basePackage);

        // Generate code for each entity table
        for (SqlTable table : schema.getEntityTables()) {
            List<TableRelationship> tableRelations =
                    RelationshipUtils.getRelationshipsForTable(
                            table.getName(), relationshipsByTable);

            // Find inverse relationships using utility
            List<TableRelationship> inverseRelations =
                    RelationshipUtils.findInverseRelationships(table, schema);

            // Find many-to-many relationships using utility
            List<ManyToManyRelation> manyToManyRelations =
                    RelationshipUtils.findManyToManyRelations(table, schema);

            String entityName = table.getEntityName();
            String moduleName = table.getModuleName();
            String basePath = "src/main/kotlin/" + basePackage.replace('.', '/');
            String testPath = "src/test/kotlin/" + basePackage.replace('.', '/');

            // 1. Generate Entity
            String entityCode =
                    entityGenerator.generate(
                            table, tableRelations, inverseRelations, manyToManyRelations);
            files.put(
                    basePath
                            + "/"
                            + moduleName
                            + "/"
                            + PKG_DOMAIN_ENTITY
                            + "/"
                            + entityName
                            + ".kt",
                    entityCode);

            // 2. Generate DTO
            String dtoCode = dtoGenerator.generate(table, tableRelations, manyToManyRelations);
            files.put(
                    basePath
                            + "/"
                            + moduleName
                            + "/"
                            + PKG_APPLICATION_DTO
                            + "/"
                            + entityName
                            + "DTO.kt",
                    dtoCode);

            // 3. Generate Mapper
            String mapperCode =
                    mapperGenerator.generate(
                            table, tableRelations, inverseRelations, manyToManyRelations);
            files.put(
                    basePath
                            + "/"
                            + moduleName
                            + "/"
                            + PKG_APPLICATION_MAPPER
                            + "/"
                            + entityName
                            + "Mapper.kt",
                    mapperCode);

            // 4. Generate Repository
            List<SqlFunction> tableFunctions =
                    schema.getFunctionsByTable()
                            .getOrDefault(
                                    table.getName().toLowerCase(Locale.ROOT),
                                    Collections.emptyList());
            String repoCode = repositoryGenerator.generate(table, tableFunctions);
            files.put(
                    basePath
                            + "/"
                            + moduleName
                            + "/"
                            + PKG_INFRASTRUCTURE_REPOSITORY
                            + "/"
                            + entityName
                            + "Repository.kt",
                    repoCode);

            // 5. Generate Service Interface
            String serviceCode = serviceGenerator.generateInterface(table);
            files.put(
                    basePath
                            + "/"
                            + moduleName
                            + "/"
                            + PKG_APPLICATION_SERVICE
                            + "/"
                            + entityName
                            + "Service.kt",
                    serviceCode);

            // 6. Generate Service Implementation
            String serviceImplCode = serviceGenerator.generateImpl(table);
            files.put(
                    basePath
                            + "/"
                            + moduleName
                            + "/"
                            + PKG_APPLICATION_SERVICE
                            + "/"
                            + entityName
                            + "ServiceImpl.kt",
                    serviceImplCode);

            // 7. Generate Controller Interface
            String controllerCode = controllerGenerator.generateInterface(table);
            files.put(
                    basePath
                            + "/"
                            + moduleName
                            + "/"
                            + PKG_INFRASTRUCTURE_CONTROLLER
                            + "/"
                            + entityName
                            + "Controller.kt",
                    controllerCode);

            // 8. Generate Controller Implementation
            String controllerImplCode = controllerGenerator.generateImpl(table);
            files.put(
                    basePath
                            + "/"
                            + moduleName
                            + "/"
                            + PKG_INFRASTRUCTURE_CONTROLLER
                            + "/"
                            + entityName
                            + "ControllerImpl.kt",
                    controllerImplCode);

            // 9. Generate Migration
            if (config.isFeatureEnabled(Feature.MIGRATIONS)) {
                String migrationCode = migrationGenerator.generate(table, schema);
                files.put(
                        "src/main/resources/db/migration/V"
                                + migrationVersion
                                + "__create_"
                                + table.getName()
                                + "_table.sql",
                        migrationCode);
                migrationVersion++;
            }

            // 10. Generate Tests
            if (config.isFeatureEnabled(Feature.UNIT_TESTS)) {
                // Entity Test
                String entityTestCode = testGenerator.generateEntityTest(table);
                files.put(
                        testPath
                                + "/"
                                + moduleName
                                + "/"
                                + PKG_DOMAIN_ENTITY
                                + "/"
                                + entityName
                                + "Test.kt",
                        entityTestCode);

                // Repository Test
                String repositoryTestCode = testGenerator.generateRepositoryTest(table);
                files.put(
                        testPath
                                + "/"
                                + moduleName
                                + "/"
                                + PKG_INFRASTRUCTURE_REPOSITORY
                                + "/"
                                + entityName
                                + "RepositoryTest.kt",
                        repositoryTestCode);

                // Service Test
                String serviceTestCode = testGenerator.generateServiceTest(table);
                files.put(
                        testPath
                                + "/"
                                + moduleName
                                + "/"
                                + PKG_APPLICATION_SERVICE
                                + "/"
                                + entityName
                                + "ServiceImplTest.kt",
                        serviceTestCode);

                // DTO Test
                String dtoTestCode = testGenerator.generateDTOTest(table);
                files.put(
                        testPath
                                + "/"
                                + moduleName
                                + "/"
                                + PKG_APPLICATION_DTO
                                + "/"
                                + entityName
                                + "DTOTest.kt",
                        dtoTestCode);

                // Mapper Test
                String mapperTestCode = testGenerator.generateMapperTest(table);
                files.put(
                        testPath
                                + "/"
                                + moduleName
                                + "/"
                                + PKG_APPLICATION_MAPPER
                                + "/"
                                + entityName
                                + "MapperTest.kt",
                        mapperTestCode);

                // Controller Test
                String controllerTestCode = testGenerator.generateControllerTest(table);
                files.put(
                        testPath
                                + "/"
                                + moduleName
                                + "/"
                                + PKG_INFRASTRUCTURE_CONTROLLER
                                + "/"
                                + entityName
                                + "ControllerImplTest.kt",
                        controllerTestCode);
            }

            // 11. Generate Integration Test
            if (config.isFeatureEnabled(Feature.INTEGRATION_TESTS)) {
                String integrationTestCode = testGenerator.generateIntegrationTest(table);
                files.put(
                        testPath + "/" + moduleName + "/" + entityName + "IntegrationTest.kt",
                        integrationTestCode);
            }
        }

        // Generate Feature Pack files (cross-cutting concerns)
        generateFeaturePackFiles(
                files,
                schema,
                config,
                mailGenerator,
                passwordResetGenerator,
                socialLoginGenerator,
                fileStorageGenerator);

        return files;
    }

    @Override
    public List<String> validateConfig(ProjectConfig config) {
        List<String> errors = new ArrayList<>();

        if (config.getBasePackage() == null || config.getBasePackage().isBlank()) {
            errors.add("Base package is required for Kotlin/Spring Boot projects");
        }

        return errors;
    }

    /** Generates Feature Pack files (mail, storage, social login, password reset, jte). */
    private void generateFeaturePackFiles(
            Map<String, String> files,
            SqlSchema schema,
            ProjectConfig config,
            KotlinMailServiceGenerator mailGenerator,
            KotlinPasswordResetGenerator passwordResetGenerator,
            KotlinSocialLoginGenerator socialLoginGenerator,
            KotlinFileStorageGenerator fileStorageGenerator) {

        // Mail Service
        if (config.isFeatureEnabled(Feature.MAIL_SERVICE)) {
            // Generate all templates: welcome, password reset, notification
            files.putAll(mailGenerator.generate(true, true, true));
        }

        // Password Reset (requires Mail Service)
        if (config.isFeatureEnabled(Feature.PASSWORD_RESET)) {
            // Token expiration: 60 minutes by default
            files.putAll(passwordResetGenerator.generate(60));
        }

        // File Storage
        if (config.isFeatureEnabled(Feature.FILE_UPLOAD)
                || config.isFeatureEnabled(Feature.S3_STORAGE)
                || config.isFeatureEnabled(Feature.AZURE_STORAGE)) {

            String storageType = "local";
            if (config.isFeatureEnabled(Feature.S3_STORAGE)) {
                storageType = "s3";
            } else if (config.isFeatureEnabled(Feature.AZURE_STORAGE)) {
                storageType = "azure";
            }

            // Max file size: 10MB, allowed extensions: jpg,jpeg,png,gif,pdf,doc,docx, generate
            // metadata entity
            files.putAll(
                    fileStorageGenerator.generate(
                            storageType, 10, "jpg,jpeg,png,gif,pdf,doc,docx", true));
        }

        // Social Login
        if (config.isFeatureEnabled(Feature.SOCIAL_LOGIN)) {
            // Enable Google and GitHub by default, disable LinkedIn, auto-create user, link by
            // email
            files.putAll(socialLoginGenerator.generate(true, true, false, true, true));
        }

        // jte Templates (language-agnostic, uses Java's JteGenerator)
        if (config.isFeatureEnabled(Feature.JTE_TEMPLATES)) {
            JteGenerator jteGenerator = new JteGenerator(config.getBasePackage());
            // Generate admin and CRUD views with Tailwind + Alpine, path /admin
            files.putAll(
                    jteGenerator.generate(
                            schema.getEntityTables(), true, true, true, true, "/admin"));
        }

        // Developer Experience Features
        if (DxFeaturesGenerator.hasAnyDxFeature(config)) {
            String projectName = extractProjectName(config.getBasePackage());
            DxFeaturesGenerator dxGenerator = new DxFeaturesGenerator(projectName);
            files.putAll(dxGenerator.generate(config, DxLanguage.KOTLIN_SPRING));
        }
    }

    /** Extracts project name from base package (last segment). */
    private String extractProjectName(String basePackage) {
        if (basePackage == null || basePackage.isBlank()) {
            return "app";
        }
        String[] parts = basePackage.split("\\.");
        return parts[parts.length - 1];
    }
}
