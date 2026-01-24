package com.jnzader.apigen.codegen.generator.php;

import com.jnzader.apigen.codegen.generator.api.Feature;
import com.jnzader.apigen.codegen.generator.api.LanguageTypeMapper;
import com.jnzader.apigen.codegen.generator.api.ProjectConfig;
import com.jnzader.apigen.codegen.generator.api.ProjectGenerator;
import com.jnzader.apigen.codegen.generator.php.config.PhpConfigGenerator;
import com.jnzader.apigen.codegen.generator.php.controller.PhpControllerGenerator;
import com.jnzader.apigen.codegen.generator.php.migration.PhpMigrationGenerator;
import com.jnzader.apigen.codegen.generator.php.model.PhpModelGenerator;
import com.jnzader.apigen.codegen.generator.php.request.PhpRequestGenerator;
import com.jnzader.apigen.codegen.generator.php.resource.PhpResourceGenerator;
import com.jnzader.apigen.codegen.generator.php.service.PhpServiceGenerator;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Project generator for PHP/Laravel applications.
 *
 * <p>This generator creates complete Laravel API projects from SQL schemas, including:
 *
 * <ul>
 *   <li>Eloquent models with relationships
 *   <li>Database migrations
 *   <li>API Resources (DTOs)
 *   <li>Form Requests (validation)
 *   <li>Service classes with business logic
 *   <li>API Controllers with REST endpoints
 *   <li>Project configuration files (composer.json, routes, etc.)
 * </ul>
 */
public class PhpLaravelProjectGenerator implements ProjectGenerator {

    private static final String LANGUAGE = "php";
    private static final String FRAMEWORK = "laravel";
    private static final String DEFAULT_PHP_VERSION = "8.4";
    private static final String DEFAULT_FRAMEWORK_VERSION = "12.0";

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
                    Feature.ONE_TO_MANY);

    private final PhpTypeMapper typeMapper = new PhpTypeMapper();

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
        return "PHP / Laravel 11.x";
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
        return DEFAULT_PHP_VERSION;
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
        PhpModelGenerator modelGenerator = new PhpModelGenerator();
        PhpMigrationGenerator migrationGenerator = new PhpMigrationGenerator();
        PhpResourceGenerator resourceGenerator = new PhpResourceGenerator();
        PhpRequestGenerator requestGenerator = new PhpRequestGenerator();
        PhpServiceGenerator serviceGenerator = new PhpServiceGenerator();
        PhpControllerGenerator controllerGenerator = new PhpControllerGenerator();
        PhpConfigGenerator configGenerator = new PhpConfigGenerator(projectName);

        // Collect all relationships for bidirectional mapping
        Map<String, List<SqlSchema.TableRelationship>> relationshipsByTable = new HashMap<>();
        for (SqlSchema.TableRelationship rel : schema.getAllRelationships()) {
            relationshipsByTable
                    .computeIfAbsent(rel.getSourceTable().getName(), k -> new ArrayList<>())
                    .add(rel);
        }

        // Generate base trait for models
        files.put("app/Traits/BaseModelTrait.php", modelGenerator.generateBaseTrait());

        // Generate base service
        files.put("app/Services/BaseService.php", serviceGenerator.generateBase());

        // Generate paginated response resource
        files.put(
                "app/Http/Resources/PaginatedResponse.php",
                resourceGenerator.generatePaginatedResponse());

        // Create directories placeholder files (Laravel convention)
        files.put("app/Http/Controllers/Api/V1/.gitkeep", "");
        files.put("app/Http/Requests/.gitkeep", "");
        files.put("app/Http/Resources/.gitkeep", "");
        files.put("app/Models/.gitkeep", "");
        files.put("app/Services/.gitkeep", "");
        files.put("database/migrations/.gitkeep", "");

        // Generate code for each entity table
        for (SqlTable table : schema.getEntityTables()) {
            String className = table.getEntityName();
            List<SqlSchema.TableRelationship> tableRelations =
                    relationshipsByTable.getOrDefault(table.getName(), Collections.emptyList());

            // Find inverse relationships (where this table is the target)
            List<SqlSchema.TableRelationship> inverseRelations =
                    schema.getAllRelationships().stream()
                            .filter(r -> r.getTargetTable().getName().equals(table.getName()))
                            .filter(r -> !r.getSourceTable().isJunctionTable())
                            .toList();

            // 1. Generate Model
            String modelCode = modelGenerator.generate(table, tableRelations, inverseRelations);
            files.put("app/Models/" + className + ".php", modelCode);

            // 2. Generate Migration
            String migrationCode = migrationGenerator.generate(table, tableRelations);
            String migrationFileName = migrationGenerator.getMigrationFileName(table.getName());
            files.put("database/migrations/" + migrationFileName, migrationCode);

            // 3. Generate Resource (DTO)
            String resourceCode = resourceGenerator.generate(table, tableRelations);
            files.put("app/Http/Resources/" + className + "Resource.php", resourceCode);

            // 4. Generate Resource Collection
            String collectionCode = resourceGenerator.generateCollection(table);
            files.put("app/Http/Resources/" + className + "Collection.php", collectionCode);

            // 5. Generate Create Request
            String createRequestCode =
                    requestGenerator.generateCreateRequest(table, tableRelations);
            files.put("app/Http/Requests/Create" + className + "Request.php", createRequestCode);

            // 6. Generate Update Request
            String updateRequestCode =
                    requestGenerator.generateUpdateRequest(table, tableRelations);
            files.put("app/Http/Requests/Update" + className + "Request.php", updateRequestCode);

            // 7. Generate Service
            String serviceCode = serviceGenerator.generate(table);
            files.put("app/Services/" + className + "Service.php", serviceCode);

            // 8. Generate Controller
            String controllerCode = controllerGenerator.generate(table);
            files.put(
                    "app/Http/Controllers/Api/V1/" + className + "Controller.php", controllerCode);
        }

        // Generate configuration files
        files.putAll(configGenerator.generate(schema, config));

        // Generate tests directory structure
        files.put("tests/Feature/.gitkeep", "");
        files.put("tests/Unit/.gitkeep", "");

        return files;
    }

    @Override
    public List<String> validateConfig(ProjectConfig config) {
        List<String> errors = new ArrayList<>();

        if (config.getProjectName() == null || config.getProjectName().isBlank()) {
            errors.add("Project name is required for PHP/Laravel projects");
        }

        return errors;
    }
}
