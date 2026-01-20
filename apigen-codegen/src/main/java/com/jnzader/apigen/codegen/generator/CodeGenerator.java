package com.jnzader.apigen.codegen.generator;

import com.jnzader.apigen.codegen.generator.controller.ControllerGenerator;
import com.jnzader.apigen.codegen.generator.dto.DTOGenerator;
import com.jnzader.apigen.codegen.generator.entity.EntityGenerator;
import com.jnzader.apigen.codegen.generator.entity.EntityGenerator.ManyToManyRelation;
import com.jnzader.apigen.codegen.generator.mapper.MapperGenerator;
import com.jnzader.apigen.codegen.generator.migration.MigrationGenerator;
import com.jnzader.apigen.codegen.generator.repository.RepositoryGenerator;
import com.jnzader.apigen.codegen.generator.service.ServiceGenerator;
import com.jnzader.apigen.codegen.generator.test.TestGenerator;
import com.jnzader.apigen.codegen.model.SqlForeignKey;
import com.jnzader.apigen.codegen.model.SqlFunction;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Facade for generating complete Java code structure from parsed SQL schema. Delegates to
 * specialized generators for each component.
 */
public class CodeGenerator {

    private final Path sourceRoot;
    private final Path testRoot;
    private final Path migrationRoot;

    // Specialized generators
    private final EntityGenerator entityGenerator;
    private final DTOGenerator dtoGenerator;
    private final MapperGenerator mapperGenerator;
    private final RepositoryGenerator repositoryGenerator;
    private final ServiceGenerator serviceGenerator;
    private final ControllerGenerator controllerGenerator;
    private final MigrationGenerator migrationGenerator;
    private final TestGenerator testGenerator;

    // Package path constants
    private static final String PKG_DOMAIN_ENTITY = "domain/entity";
    private static final String PKG_APPLICATION_SERVICE = "application/service";
    private static final String PKG_APPLICATION_DTO = "application/dto";
    private static final String PKG_APPLICATION_MAPPER = "application/mapper";
    private static final String PKG_INFRASTRUCTURE_REPOSITORY = "infrastructure/repository";
    private static final String PKG_INFRASTRUCTURE_CONTROLLER = "infrastructure/controller";

    public CodeGenerator(String basePackage, Path projectRoot) {
        this.sourceRoot =
                projectRoot.resolve("src/main/java").resolve(basePackage.replace('.', '/'));
        this.testRoot = projectRoot.resolve("src/test/java").resolve(basePackage.replace('.', '/'));
        this.migrationRoot = projectRoot.resolve("src/main/resources/db/migration");

        // Initialize specialized generators
        this.entityGenerator = new EntityGenerator(basePackage);
        this.dtoGenerator = new DTOGenerator(basePackage);
        this.mapperGenerator = new MapperGenerator(basePackage);
        this.repositoryGenerator = new RepositoryGenerator(basePackage);
        this.serviceGenerator = new ServiceGenerator(basePackage);
        this.controllerGenerator = new ControllerGenerator(basePackage);
        this.migrationGenerator = new MigrationGenerator();
        this.testGenerator = new TestGenerator(basePackage);
    }

    /** Generates all code from the given schema. */
    public GenerationResult generate(SqlSchema schema) throws IOException {
        GenerationResult.GenerationResultBuilder result = GenerationResult.builder();
        List<String> generatedFiles = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // Validate schema first
        List<String> validationErrors = schema.validate();
        if (!validationErrors.isEmpty()) {
            errors.addAll(validationErrors);
        }

        // Collect all relationships for bidirectional mapping
        Map<String, List<SqlSchema.TableRelationship>> relationshipsByTable = new HashMap<>();
        for (SqlSchema.TableRelationship rel : schema.getAllRelationships()) {
            relationshipsByTable
                    .computeIfAbsent(rel.getSourceTable().getName(), k -> new ArrayList<>())
                    .add(rel);
        }

        // Generate code for each entity table
        for (SqlTable table : schema.getEntityTables()) {
            try {
                List<SqlSchema.TableRelationship> tableRelations =
                        relationshipsByTable.getOrDefault(table.getName(), Collections.emptyList());

                // Find inverse relationships (where this table is the target)
                List<SqlSchema.TableRelationship> inverseRelations =
                        schema.getAllRelationships().stream()
                                .filter(r -> r.getTargetTable().getName().equals(table.getName()))
                                .filter(r -> !r.getSourceTable().isJunctionTable())
                                .toList();

                // Find many-to-many relationships through junction tables
                List<ManyToManyRelation> manyToManyRelations =
                        findManyToManyRelations(table, schema);

                generatedFiles.addAll(
                        generateTableCode(
                                table,
                                tableRelations,
                                inverseRelations,
                                manyToManyRelations,
                                schema));
            } catch (Exception e) {
                errors.add(
                        "Error generating code for table "
                                + table.getName()
                                + ": "
                                + e.getMessage());
            }
        }

        // Collect function notes
        List<String> functionNotes = new ArrayList<>();
        for (SqlFunction function : schema.getFunctions()) {
            try {
                functionNotes.add(function.getName() + " -> " + function.toJavaMethodSignature());
            } catch (Exception e) {
                errors.add(
                        "Error processing function " + function.getName() + ": " + e.getMessage());
            }
        }

        return result.generatedFiles(generatedFiles)
                .errors(errors)
                .functionNotes(functionNotes)
                .build();
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

    /** Generates all code for a single table using specialized generators. */
    private List<String> generateTableCode(
            SqlTable table,
            List<SqlSchema.TableRelationship> outgoingRelations,
            List<SqlSchema.TableRelationship> incomingRelations,
            List<ManyToManyRelation> manyToManyRelations,
            SqlSchema schema)
            throws IOException {
        List<String> files = new ArrayList<>();
        String entityName = table.getEntityName();
        String moduleName = table.getModuleName();

        // Create directories
        createDirectories(moduleName);

        // 1. Generate Entity
        String entityCode =
                entityGenerator.generate(
                        table, outgoingRelations, incomingRelations, manyToManyRelations);
        Path entityPath =
                writeFile(moduleName, PKG_DOMAIN_ENTITY, entityName + ".java", entityCode);
        files.add(entityPath.toString());

        // 2. Generate DTO
        String dtoCode = dtoGenerator.generate(table, outgoingRelations, manyToManyRelations);
        Path dtoPath = writeFile(moduleName, PKG_APPLICATION_DTO, entityName + "DTO.java", dtoCode);
        files.add(dtoPath.toString());

        // 3. Generate Mapper
        String mapperCode = mapperGenerator.generate(table);
        Path mapperPath =
                writeFile(
                        moduleName, PKG_APPLICATION_MAPPER, entityName + "Mapper.java", mapperCode);
        files.add(mapperPath.toString());

        // 4. Generate Repository (with function methods if any)
        List<SqlFunction> tableFunctions =
                schema.getFunctionsByTable()
                        .getOrDefault(
                                table.getName().toLowerCase(Locale.ROOT), Collections.emptyList());
        String repoCode = repositoryGenerator.generate(table, tableFunctions);
        Path repoPath =
                writeFile(
                        moduleName,
                        PKG_INFRASTRUCTURE_REPOSITORY,
                        entityName + "Repository.java",
                        repoCode);
        files.add(repoPath.toString());

        // 5. Generate Service Interface
        String serviceCode = serviceGenerator.generateInterface(table);
        Path servicePath =
                writeFile(
                        moduleName,
                        PKG_APPLICATION_SERVICE,
                        entityName + "Service.java",
                        serviceCode);
        files.add(servicePath.toString());

        // 6. Generate Service Implementation
        String serviceImplCode = serviceGenerator.generateImpl(table);
        Path serviceImplPath =
                writeFile(
                        moduleName,
                        PKG_APPLICATION_SERVICE,
                        entityName + "ServiceImpl.java",
                        serviceImplCode);
        files.add(serviceImplPath.toString());

        // 7. Generate Controller Interface
        String controllerCode = controllerGenerator.generateInterface(table);
        Path controllerPath =
                writeFile(
                        moduleName,
                        PKG_INFRASTRUCTURE_CONTROLLER,
                        entityName + "Controller.java",
                        controllerCode);
        files.add(controllerPath.toString());

        // 8. Generate Controller Implementation
        String controllerImplCode = controllerGenerator.generateImpl(table);
        Path controllerImplPath =
                writeFile(
                        moduleName,
                        PKG_INFRASTRUCTURE_CONTROLLER,
                        entityName + "ControllerImpl.java",
                        controllerImplCode);
        files.add(controllerImplPath.toString());

        // 9. Generate Migration
        String migrationCode = migrationGenerator.generate(table, schema);
        int nextVersion = getNextMigrationVersion();
        Path migrationPath =
                Files.writeString(
                        migrationRoot.resolve(
                                "V" + nextVersion + "__create_" + table.getName() + "_table.sql"),
                        migrationCode);
        files.add(migrationPath.toString());

        // 10. Generate Service Test
        String testCode = testGenerator.generateServiceTest(table);
        Path testPath =
                writeTestFile(
                        moduleName,
                        PKG_APPLICATION_SERVICE,
                        entityName + "ServiceImplTest.java",
                        testCode);
        files.add(testPath.toString());

        // 11. Generate DTO Test
        String dtoTestCode = testGenerator.generateDTOTest(table);
        Path dtoTestPath =
                writeTestFile(
                        moduleName, PKG_APPLICATION_DTO, entityName + "DTOTest.java", dtoTestCode);
        files.add(dtoTestPath.toString());

        // 12. Generate Controller Test
        String controllerTestCode = testGenerator.generateControllerTest(table);
        Path controllerTestPath =
                writeTestFile(
                        moduleName,
                        PKG_INFRASTRUCTURE_CONTROLLER,
                        entityName + "ControllerImplTest.java",
                        controllerTestCode);
        files.add(controllerTestPath.toString());

        // 13. Generate Integration Test
        String integrationTestCode = testGenerator.generateIntegrationTest(table);
        Path integrationTestPath =
                writeTestFile(
                        moduleName, "", entityName + "IntegrationTest.java", integrationTestCode);
        files.add(integrationTestPath.toString());

        return files;
    }

    /** Creates all required directories for a module. */
    private void createDirectories(String moduleName) throws IOException {
        String[] dirs = {
            PKG_DOMAIN_ENTITY,
            "domain/event",
            "domain/exception",
            PKG_APPLICATION_DTO,
            PKG_APPLICATION_MAPPER,
            PKG_APPLICATION_SERVICE,
            PKG_INFRASTRUCTURE_REPOSITORY,
            PKG_INFRASTRUCTURE_CONTROLLER
        };

        for (String dir : dirs) {
            Files.createDirectories(sourceRoot.resolve(moduleName).resolve(dir));
        }

        Files.createDirectories(testRoot.resolve(moduleName).resolve(PKG_APPLICATION_SERVICE));
        Files.createDirectories(testRoot.resolve(moduleName).resolve(PKG_APPLICATION_DTO));
        Files.createDirectories(
                testRoot.resolve(moduleName).resolve(PKG_INFRASTRUCTURE_CONTROLLER));
        Files.createDirectories(migrationRoot);
    }

    /** Writes a file to the source directory. */
    private Path writeFile(String moduleName, String subPackage, String fileName, String content)
            throws IOException {
        Path filePath = sourceRoot.resolve(moduleName).resolve(subPackage).resolve(fileName);
        return Files.writeString(filePath, content);
    }

    /** Writes a file to the test directory. */
    private Path writeTestFile(
            String moduleName, String subPackage, String fileName, String content)
            throws IOException {
        Path filePath = testRoot.resolve(moduleName).resolve(subPackage).resolve(fileName);
        return Files.writeString(filePath, content);
    }

    /** Gets the next migration version number. */
    private int getNextMigrationVersion() throws IOException {
        if (!Files.exists(migrationRoot)) {
            return 2;
        }

        try (Stream<Path> files = Files.list(migrationRoot)) {
            return files.map(Path::getFileName)
                            .map(Path::toString)
                            .filter(name -> name.matches("V\\d++.*+"))
                            .map(name -> name.replaceAll("V(\\d++).*+", "$1"))
                            .mapToInt(Integer::parseInt)
                            .max()
                            .orElse(1)
                    + 1;
        }
    }

    /** Result of code generation containing generated files and any errors. */
    @lombok.Builder
    @lombok.Data
    public static class GenerationResult {
        @lombok.Builder.Default private List<String> generatedFiles = new ArrayList<>();

        @lombok.Builder.Default private List<String> errors = new ArrayList<>();

        @lombok.Builder.Default private List<String> functionNotes = new ArrayList<>();
    }
}
