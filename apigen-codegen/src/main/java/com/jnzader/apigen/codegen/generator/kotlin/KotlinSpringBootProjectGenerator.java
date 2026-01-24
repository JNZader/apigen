package com.jnzader.apigen.codegen.generator.kotlin;

import com.jnzader.apigen.codegen.generator.api.Feature;
import com.jnzader.apigen.codegen.generator.api.LanguageTypeMapper;
import com.jnzader.apigen.codegen.generator.api.ProjectConfig;
import com.jnzader.apigen.codegen.generator.api.ProjectGenerator;
import com.jnzader.apigen.codegen.generator.common.ManyToManyRelation;
import com.jnzader.apigen.codegen.generator.kotlin.controller.KotlinControllerGenerator;
import com.jnzader.apigen.codegen.generator.kotlin.dto.KotlinDTOGenerator;
import com.jnzader.apigen.codegen.generator.kotlin.entity.KotlinEntityGenerator;
import com.jnzader.apigen.codegen.generator.kotlin.mapper.KotlinMapperGenerator;
import com.jnzader.apigen.codegen.generator.kotlin.repository.KotlinRepositoryGenerator;
import com.jnzader.apigen.codegen.generator.kotlin.service.KotlinServiceGenerator;
import com.jnzader.apigen.codegen.generator.kotlin.test.KotlinTestGenerator;
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
    private static final String DEFAULT_KOTLIN_VERSION = "2.1.0";
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
                    Feature.BATCH_OPERATIONS);

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
