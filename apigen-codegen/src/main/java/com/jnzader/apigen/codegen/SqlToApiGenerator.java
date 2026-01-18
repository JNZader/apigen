package com.jnzader.apigen.codegen;

import com.jnzader.apigen.codegen.generator.CodeGenerator;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import com.jnzader.apigen.codegen.parser.SqlSchemaParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Main entry point for generating API code from SQL schema files.
 * <p>
 * Usage:
 * <pre>
 *   java SqlToApiGenerator &lt;sql-file&gt; [project-root] [base-package]
 * </pre>
 * <p>
 * Examples:
 * <pre>
 *   java SqlToApiGenerator schema.sql
 *   java SqlToApiGenerator schema.sql /path/to/project com.mycompany.api
 * </pre>
 */
@SuppressWarnings("java:S2629")
// S2629: Los argumentos de logging son metodos getter simples (getSize, getMessage, etc.)
//        que tienen O(1) complejidad y no justifican evaluacion condicional
public class SqlToApiGenerator {

    private static final Logger log = LoggerFactory.getLogger(SqlToApiGenerator.class);
    private static final String DEFAULT_BASE_PACKAGE = "com.jnzader.apigen";
    private static final String SEPARATOR_LINE = "============================================================";

    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }

        String sqlFile = args[0];
        String projectRoot = args.length > 1 ? args[1] : ".";
        String basePackage = args.length > 2 ? args[2] : DEFAULT_BASE_PACKAGE;

        try {
            generate(sqlFile, projectRoot, basePackage);
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    /**
     * Generates API code from a SQL schema file.
     *
     * @param sqlFilePath  Path to the SQL file
     * @param projectRoot  Root directory of the project
     * @param basePackage  Base package for generated code
     * @throws IOException If file operations fail
     */
    public static void generate(String sqlFilePath, String projectRoot, String basePackage) throws IOException {
        Path sqlPath = Paths.get(sqlFilePath);
        Path rootPath = Paths.get(projectRoot);

        log.info(SEPARATOR_LINE);
        log.info("APiGen - SQL to API Generator");
        log.info(SEPARATOR_LINE);
        log.info("SQL File:     {}", sqlPath.toAbsolutePath());
        log.info("Project Root: {}", rootPath.toAbsolutePath());
        log.info("Base Package: {}", basePackage);
        log.info("");

        // Parse SQL file
        log.info("[1/3] Parsing SQL schema...");
        SqlSchemaParser parser = new SqlSchemaParser();
        SqlSchema schema = parser.parseFile(sqlPath);

        // Show parse results
        log.info("      Found {} tables", schema.getTables().size());
        log.info("      Found {} functions/procedures", schema.getFunctions().size());
        log.info("      Found {} junction tables (many-to-many)", schema.getJunctionTables().size());

        if (!schema.getParseErrors().isEmpty()) {
            log.info("      Warnings:");
            schema.getParseErrors().forEach(e -> log.info("        - {}", e));
        }
        log.info("");

        // Validate schema
        log.info("[2/3] Validating schema...");
        List<String> validationErrors = schema.validate();
        if (!validationErrors.isEmpty()) {
            log.info("      Validation issues:");
            validationErrors.forEach(e -> log.info("        - {}", e));
        } else {
            log.info("      Schema is valid");
        }
        log.info("");

        // Generate code
        log.info("[3/3] Generating code...");
        CodeGenerator generator = new CodeGenerator(basePackage, rootPath);
        CodeGenerator.GenerationResult result = generator.generate(schema);

        // Print results
        log.info("");
        log.info(SEPARATOR_LINE);
        log.info("Generation Complete!");
        log.info(SEPARATOR_LINE);

        if (!result.getGeneratedFiles().isEmpty()) {
            log.info("Generated Files ({}):", result.getGeneratedFiles().size());
            for (String file : result.getGeneratedFiles()) {
                log.info("  [+] {}", file);
            }
        }

        if (!result.getFunctionNotes().isEmpty()) {
            log.info("Function Methods:");
            for (String note : result.getFunctionNotes()) {
                log.info("  [F] {}", note);
            }
        }

        if (!result.getErrors().isEmpty()) {
            log.info("Errors:");
            for (String error : result.getErrors()) {
                log.info("  [!] {}", error);
            }
        }

        // Print summary per entity
        log.info("Entity Summary:");
        for (SqlTable table : schema.getEntityTables()) {
            log.info("  {}:", table.getEntityName());
            log.info("    - Columns:   {}", table.getBusinessColumns().size());
            log.info("    - Relations: {}", table.getForeignKeys().size());
            log.info("    - Endpoint:  /api/v1/{}", table.getModuleName());
        }

        log.info("Next steps:");
        log.info("  1. Review the generated files");
        log.info("  2. Add custom validations to DTOs");
        log.info("  3. Run: ./gradlew compileJava");
        log.info("  4. Start the application");
        log.info(SEPARATOR_LINE);
    }

    private static void printUsage() {
        log.info("APiGen - SQL to API Generator");
        log.info("");
        log.info("Usage:");
        log.info("  java SqlToApiGenerator <sql-file> [project-root] [base-package]");
        log.info("");
        log.info("Arguments:");
        log.info("  sql-file      Path to the SQL schema file (required)");
        log.info("  project-root  Project root directory (default: current directory)");
        log.info("  base-package  Base Java package (default: com.jnzader.apigen)");
        log.info("");
        log.info("Examples:");
        log.info("  java SqlToApiGenerator schema.sql");
        log.info("  java SqlToApiGenerator db/schema.sql . com.mycompany.api");
        log.info("");
        log.info("Supported SQL features:");
        log.info("  - CREATE TABLE with columns, constraints, and indexes");
        log.info("  - FOREIGN KEY constraints (inline and ALTER TABLE)");
        log.info("  - PRIMARY KEY, UNIQUE, NOT NULL, DEFAULT");
        log.info("  - CREATE INDEX (BTREE, GIN, GIST, HASH, BRIN)");
        log.info("  - CREATE FUNCTION / PROCEDURE (PostgreSQL syntax)");
        log.info("  - Many-to-many relationships via junction tables");
        log.info("");
        log.info("Generated output per table:");
        log.info("  - Entity.java         (JPA entity with relationships)");
        log.info("  - DTO.java            (Data transfer object with validations)");
        log.info("  - Mapper.java         (MapStruct mapper)");
        log.info("  - Repository.java     (Spring Data JPA repository)");
        log.info("  - Service.java        (Business service interface)");
        log.info("  - ServiceImpl.java    (Service implementation)");
        log.info("  - Controller.java     (REST controller interface)");
        log.info("  - ControllerImpl.java (Controller implementation)");
        log.info("  - Migration.sql       (Flyway migration)");
        log.info("  - Test.java           (Unit test)");
    }
}
