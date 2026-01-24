package com.jnzader.apigen.codegen.generator.java;

import com.jnzader.apigen.codegen.generator.api.Feature;
import com.jnzader.apigen.codegen.generator.api.LanguageTypeMapper;
import com.jnzader.apigen.codegen.generator.api.ProjectConfig;
import com.jnzader.apigen.codegen.generator.api.ProjectGenerator;
import com.jnzader.apigen.codegen.generator.common.ManyToManyRelation;
import com.jnzader.apigen.codegen.generator.java.controller.JavaControllerGenerator;
import com.jnzader.apigen.codegen.generator.java.dto.JavaDTOGenerator;
import com.jnzader.apigen.codegen.generator.java.entity.JavaEntityGenerator;
import com.jnzader.apigen.codegen.generator.java.jte.JteGenerator;
import com.jnzader.apigen.codegen.generator.java.mail.MailServiceGenerator;
import com.jnzader.apigen.codegen.generator.java.mapper.JavaMapperGenerator;
import com.jnzader.apigen.codegen.generator.java.repository.JavaRepositoryGenerator;
import com.jnzader.apigen.codegen.generator.java.security.reset.PasswordResetGenerator;
import com.jnzader.apigen.codegen.generator.java.security.social.SocialLoginGenerator;
import com.jnzader.apigen.codegen.generator.java.service.JavaServiceGenerator;
import com.jnzader.apigen.codegen.generator.java.storage.FileStorageGenerator;
import com.jnzader.apigen.codegen.generator.java.test.JavaTestGenerator;
import com.jnzader.apigen.codegen.generator.migration.MigrationGenerator;
import com.jnzader.apigen.codegen.model.SqlForeignKey;
import com.jnzader.apigen.codegen.model.SqlFunction;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Project generator for Java/Spring Boot applications.
 *
 * <p>This generator creates complete Spring Boot projects from SQL schemas, including:
 *
 * <ul>
 *   <li>JPA Entities with relationships
 *   <li>DTOs with validation annotations
 *   <li>MapStruct Mappers
 *   <li>Spring Data JPA Repositories
 *   <li>Service interfaces and implementations
 *   <li>REST Controllers
 *   <li>Flyway migrations
 *   <li>Unit and integration tests
 * </ul>
 */
public class JavaSpringBootProjectGenerator implements ProjectGenerator {

    private static final String LANGUAGE = "java";
    private static final String FRAMEWORK = "spring-boot";
    private static final String DEFAULT_JAVA_VERSION = "25";
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
                    Feature.JTE_TEMPLATES);

    // Package path constants
    private static final String PKG_DOMAIN_ENTITY = "domain/entity";
    private static final String PKG_APPLICATION_SERVICE = "application/service";
    private static final String PKG_APPLICATION_DTO = "application/dto";
    private static final String PKG_APPLICATION_MAPPER = "application/mapper";
    private static final String PKG_INFRASTRUCTURE_REPOSITORY = "infrastructure/repository";
    private static final String PKG_INFRASTRUCTURE_CONTROLLER = "infrastructure/controller";

    private final JavaTypeMapper typeMapper = new JavaTypeMapper();

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
        return "Java / Spring Boot 4.x";
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
        return DEFAULT_JAVA_VERSION;
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
        JavaEntityGenerator entityGenerator = new JavaEntityGenerator(basePackage);
        JavaDTOGenerator dtoGenerator = new JavaDTOGenerator(basePackage);
        JavaMapperGenerator mapperGenerator = new JavaMapperGenerator(basePackage);
        JavaRepositoryGenerator repositoryGenerator = new JavaRepositoryGenerator(basePackage);
        JavaServiceGenerator serviceGenerator = new JavaServiceGenerator(basePackage);
        JavaControllerGenerator controllerGenerator = new JavaControllerGenerator(basePackage);
        MigrationGenerator migrationGenerator = new MigrationGenerator();
        JavaTestGenerator testGenerator = new JavaTestGenerator(basePackage);

        // Collect all relationships for bidirectional mapping
        Map<String, List<SqlSchema.TableRelationship>> relationshipsByTable = new HashMap<>();
        for (SqlSchema.TableRelationship rel : schema.getAllRelationships()) {
            relationshipsByTable
                    .computeIfAbsent(rel.getSourceTable().getName(), k -> new ArrayList<>())
                    .add(rel);
        }

        int migrationVersion = 2;

        // Generate code for each entity table
        for (SqlTable table : schema.getEntityTables()) {
            List<SqlSchema.TableRelationship> tableRelations =
                    relationshipsByTable.getOrDefault(table.getName(), Collections.emptyList());

            // Find inverse relationships (where this table is the target)
            List<SqlSchema.TableRelationship> inverseRelations =
                    schema.getAllRelationships().stream()
                            .filter(r -> r.getTargetTable().getName().equals(table.getName()))
                            .filter(r -> !r.getSourceTable().isJunctionTable())
                            .toList();

            // Find many-to-many relationships through junction tables
            List<ManyToManyRelation> manyToManyRelations = findManyToManyRelations(table, schema);

            String entityName = table.getEntityName();
            String moduleName = table.getModuleName();
            String basePath = "src/main/java/" + basePackage.replace('.', '/');
            String testPath = "src/test/java/" + basePackage.replace('.', '/');

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
                            + ".java",
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
                            + "DTO.java",
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
                            + "Mapper.java",
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
                            + "Repository.java",
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
                            + "Service.java",
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
                            + "ServiceImpl.java",
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
                            + "Controller.java",
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
                            + "ControllerImpl.java",
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
                                + "ServiceImplTest.java",
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
                                + "DTOTest.java",
                        dtoTestCode);

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
                                + "ControllerImplTest.java",
                        controllerTestCode);
            }

            // 11. Generate Integration Test
            if (config.isFeatureEnabled(Feature.INTEGRATION_TESTS)) {
                String integrationTestCode = testGenerator.generateIntegrationTest(table);
                files.put(
                        testPath + "/" + moduleName + "/" + entityName + "IntegrationTest.java",
                        integrationTestCode);
            }
        }

        // Generate Feature Pack files (cross-cutting concerns)
        generateFeaturePackFiles(files, schema, config);

        return files;
    }

    /** Generates Feature Pack files (mail, storage, social login, password reset, jte). */
    private void generateFeaturePackFiles(
            Map<String, String> files, SqlSchema schema, ProjectConfig config) {
        String basePackage = config.getBasePackage();

        // Mail Service
        if (config.isFeatureEnabled(Feature.MAIL_SERVICE)) {
            MailServiceGenerator mailGenerator = new MailServiceGenerator(basePackage);
            // Generate all templates: welcome, password reset, notification
            files.putAll(mailGenerator.generate(true, true, true));
        }

        // Password Reset (requires Mail Service)
        if (config.isFeatureEnabled(Feature.PASSWORD_RESET)) {
            PasswordResetGenerator passwordResetGenerator = new PasswordResetGenerator(basePackage);
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

            FileStorageGenerator storageGenerator = new FileStorageGenerator(basePackage);
            // Max file size: 10MB, allowed extensions: jpg,jpeg,png,gif,pdf,doc,docx, generate
            // metadata entity
            files.putAll(
                    storageGenerator.generate(
                            storageType, 10, "jpg,jpeg,png,gif,pdf,doc,docx", true));
        }

        // Social Login
        if (config.isFeatureEnabled(Feature.SOCIAL_LOGIN)) {
            SocialLoginGenerator socialLoginGenerator = new SocialLoginGenerator(basePackage);
            // Enable Google and GitHub by default, disable LinkedIn, auto-create user, link by
            // email
            files.putAll(socialLoginGenerator.generate(true, true, false, true, true));
        }

        // jte Templates
        if (config.isFeatureEnabled(Feature.JTE_TEMPLATES)) {
            JteGenerator jteGenerator = new JteGenerator(basePackage);
            // Generate admin and CRUD views with Tailwind + Alpine, path /admin
            files.putAll(
                    jteGenerator.generate(
                            schema.getEntityTables(), true, true, true, true, "/admin"));
        }
    }

    @Override
    public List<String> validateConfig(ProjectConfig config) {
        List<String> errors = new ArrayList<>();

        if (config.getBasePackage() == null || config.getBasePackage().isBlank()) {
            errors.add("Base package is required for Java/Spring Boot projects");
        }

        return errors;
    }

    /** Finds many-to-many relationships for a table through junction tables. */
    private List<ManyToManyRelation> findManyToManyRelations(SqlTable table, SqlSchema schema) {
        List<ManyToManyRelation> relations = new ArrayList<>();

        for (SqlTable junctionTable : schema.getJunctionTables()) {
            List<SqlForeignKey> fks = junctionTable.getForeignKeys();
            if (fks.size() != 2) continue;

            SqlForeignKey fk1 = fks.get(0);
            SqlForeignKey fk2 = fks.get(1);

            SqlForeignKey thisFk = null;
            SqlForeignKey otherFk = null;

            if (fk1.getReferencedTable().equalsIgnoreCase(table.getName())) {
                thisFk = fk1;
                otherFk = fk2;
            } else if (fk2.getReferencedTable().equalsIgnoreCase(table.getName())) {
                thisFk = fk2;
                otherFk = fk1;
            }

            if (thisFk != null && otherFk != null) {
                SqlTable otherTable = schema.getTableByName(otherFk.getReferencedTable());
                if (otherTable != null) {
                    relations.add(
                            new ManyToManyRelation(
                                    junctionTable.getName(),
                                    thisFk.getColumnName(),
                                    otherFk.getColumnName(),
                                    otherTable));
                }
            }
        }

        return relations;
    }
}
