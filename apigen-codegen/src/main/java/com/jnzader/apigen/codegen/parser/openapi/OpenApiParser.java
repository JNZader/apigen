package com.jnzader.apigen.codegen.parser.openapi;

import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlForeignKey;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for OpenAPI specifications.
 *
 * <p>This parser converts OpenAPI 3.x specifications (YAML or JSON) into the internal SqlSchema
 * model that can be processed by the code generators.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * OpenApiParser parser = new OpenApiParser();
 * SqlSchema schema = parser.parse(openApiSpec);
 * }</pre>
 */
@SuppressWarnings({
    "java:S3776" // Complex parsing logic inherent to OpenAPI conversion
})
public class OpenApiParser {

    private static final Logger log = LoggerFactory.getLogger(OpenApiParser.class);

    private static final List<String> EXCLUDED_SCHEMAS =
            List.of(
                    "Error",
                    "ErrorResponse",
                    "ValidationError",
                    "ApiResponse",
                    "PageRequest",
                    "PageResponse",
                    "Pageable",
                    "Sort",
                    "Link",
                    "Links");

    /**
     * Parses an OpenAPI specification string (YAML or JSON).
     *
     * @param openApiSpec the OpenAPI specification content
     * @return the parsed SqlSchema
     * @throws OpenApiParseException if parsing fails
     */
    public SqlSchema parse(String openApiSpec) {
        if (openApiSpec == null || openApiSpec.isBlank()) {
            throw new OpenApiParseException("OpenAPI specification cannot be empty");
        }

        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveFully(true);

        SwaggerParseResult result =
                new OpenAPIV3Parser().readContents(openApiSpec, null, parseOptions);

        if (result.getOpenAPI() == null) {
            List<String> errors = result.getMessages();
            String errorMsg =
                    errors != null && !errors.isEmpty()
                            ? String.join("; ", errors)
                            : "Unknown parsing error";
            throw new OpenApiParseException("Failed to parse OpenAPI specification: " + errorMsg);
        }

        return convertToSchema(result.getOpenAPI(), result.getMessages());
    }

    /**
     * Parses an OpenAPI specification from a file path or URL.
     *
     * @param location the file path or URL to the OpenAPI specification
     * @return the parsed SqlSchema
     * @throws OpenApiParseException if parsing fails
     */
    public SqlSchema parseFromLocation(String location) {
        if (location == null || location.isBlank()) {
            throw new OpenApiParseException("OpenAPI specification location cannot be empty");
        }

        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveFully(true);

        SwaggerParseResult result =
                new OpenAPIV3Parser().readLocation(location, null, parseOptions);

        if (result.getOpenAPI() == null) {
            List<String> errors = result.getMessages();
            String errorMsg =
                    errors != null && !errors.isEmpty()
                            ? String.join("; ", errors)
                            : "Unknown parsing error";
            throw new OpenApiParseException(
                    "Failed to parse OpenAPI specification from " + location + ": " + errorMsg);
        }

        return convertToSchema(result.getOpenAPI(), result.getMessages());
    }

    /**
     * Converts an OpenAPI object to SqlSchema.
     *
     * @param openAPI the parsed OpenAPI object
     * @param warnings any warnings from parsing
     * @return the SqlSchema
     */
    private SqlSchema convertToSchema(OpenAPI openAPI, List<String> warnings) {
        List<SqlTable> tables = new ArrayList<>();
        List<String> parseErrors = new ArrayList<>();

        // Add any warnings as parse errors
        if (warnings != null) {
            parseErrors.addAll(warnings);
        }

        // Get schemas from components
        if (openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null) {
            @SuppressWarnings("unchecked")
            Map<String, Schema<?>> schemas =
                    (Map<String, Schema<?>>) (Map<?, ?>) openAPI.getComponents().getSchemas();

            for (Map.Entry<String, Schema<?>> entry : schemas.entrySet()) {
                String schemaName = entry.getKey();
                Schema<?> schema = entry.getValue();

                // Skip excluded schemas and schemas without properties (enums/simple types)
                if (shouldExcludeSchema(schemaName)
                        || schema.getProperties() == null
                        || schema.getProperties().isEmpty()) {
                    log.debug("Skipping schema: {}", schemaName);
                    continue;
                }

                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Schema<?>> allSchemas =
                            (Map<String, Schema<?>>) (Map<?, ?>) schemas;
                    SqlTable table = OpenApiSchemaConverter.convert(schemaName, schema, allSchemas);
                    tables.add(table);
                    log.debug("Converted schema '{}' to table '{}'", schemaName, table.getName());
                } catch (Exception e) {
                    String error =
                            "Failed to convert schema '" + schemaName + "': " + e.getMessage();
                    parseErrors.add(error);
                    log.warn(error, e);
                }
            }
        }

        // Detect and create junction tables for many-to-many relationships
        List<SqlTable> junctionTables = detectManyToManyRelationships(openAPI);
        tables.addAll(junctionTables);

        String title =
                openAPI.getInfo() != null && openAPI.getInfo().getTitle() != null
                        ? openAPI.getInfo().getTitle()
                        : "OpenAPI Schema";

        return SqlSchema.builder().name(title).tables(tables).parseErrors(parseErrors).build();
    }

    /**
     * Checks if a schema should be excluded from table generation.
     *
     * @param schemaName the schema name
     * @return true if should be excluded
     */
    private boolean shouldExcludeSchema(String schemaName) {
        // Check exact match
        if (EXCLUDED_SCHEMAS.contains(schemaName)) {
            return true;
        }

        // Check patterns
        String lower = schemaName.toLowerCase(Locale.ROOT);
        return lower.endsWith("request")
                || lower.endsWith("response")
                || lower.endsWith("dto")
                || lower.endsWith("input")
                || lower.endsWith("output")
                || lower.endsWith("payload");
    }

    /**
     * Detects many-to-many relationships in the OpenAPI spec and creates junction tables.
     *
     * @param openAPI the OpenAPI object
     * @return list of junction tables
     */
    private List<SqlTable> detectManyToManyRelationships(OpenAPI openAPI) {
        List<SqlTable> junctionTables = new ArrayList<>();

        if (openAPI.getComponents() == null || openAPI.getComponents().getSchemas() == null) {
            return junctionTables;
        }

        @SuppressWarnings("unchecked")
        Map<String, Schema<?>> schemas =
                (Map<String, Schema<?>>) (Map<?, ?>) openAPI.getComponents().getSchemas();

        for (Map.Entry<String, Schema<?>> entry : schemas.entrySet()) {
            String schemaName = entry.getKey();
            Schema<?> schema = entry.getValue();

            if (schema.getProperties() == null) {
                continue;
            }

            @SuppressWarnings("unchecked")
            Map<String, Schema<?>> properties =
                    (Map<String, Schema<?>>) (Map<?, ?>) schema.getProperties();
            for (Map.Entry<String, Schema<?>> propEntry : properties.entrySet()) {
                Schema<?> propSchema = propEntry.getValue();

                // Check for array of references
                if ("array".equals(propSchema.getType()) && propSchema.getItems() != null) {
                    Schema<?> itemsSchema = propSchema.getItems();
                    if (itemsSchema.get$ref() != null) {
                        String refSchemaName = extractRefSchemaName(itemsSchema.get$ref());
                        if (refSchemaName != null && schemas.containsKey(refSchemaName)) {
                            // Check if the referenced schema also has an array pointing back
                            Schema<?> refSchema = schemas.get(refSchemaName);
                            if (hasReverseArrayRef(refSchema, schemaName)) {
                                // Create junction table
                                SqlTable junctionTable =
                                        createJunctionTable(schemaName, refSchemaName);
                                // Avoid duplicates
                                if (!hasJunctionTable(junctionTables, junctionTable.getName())) {
                                    junctionTables.add(junctionTable);
                                    log.debug(
                                            "Created junction table '{}' for many-to-many between"
                                                    + " '{}' and '{}'",
                                            junctionTable.getName(),
                                            schemaName,
                                            refSchemaName);
                                }
                            }
                        }
                    }
                }
            }
        }

        return junctionTables;
    }

    /**
     * Checks if a schema has an array property referencing back to the source schema.
     *
     * @param schema the schema to check
     * @param sourceSchemaName the source schema name
     * @return true if has reverse reference
     */
    private boolean hasReverseArrayRef(Schema<?> schema, String sourceSchemaName) {
        if (schema.getProperties() == null) {
            return false;
        }

        for (Schema<?> propSchema : schema.getProperties().values()) {
            if ("array".equals(propSchema.getType()) && propSchema.getItems() != null) {
                Schema<?> itemsSchema = propSchema.getItems();
                if (itemsSchema.get$ref() != null) {
                    String refName = extractRefSchemaName(itemsSchema.get$ref());
                    if (sourceSchemaName.equals(refName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Creates a junction table for a many-to-many relationship.
     *
     * @param schema1Name first schema name
     * @param schema2Name second schema name
     * @return the junction table
     */
    private SqlTable createJunctionTable(String schema1Name, String schema2Name) {
        // Ensure consistent ordering for junction table name
        String first;
        String second;
        if (schema1Name.compareTo(schema2Name) < 0) {
            first = schema1Name;
            second = schema2Name;
        } else {
            first = schema2Name;
            second = schema1Name;
        }

        String tableName =
                OpenApiSchemaConverter.toColumnName(first)
                        + "_"
                        + OpenApiSchemaConverter.toColumnName(second);

        String fk1Column = OpenApiSchemaConverter.toColumnName(first) + "_id";
        String fk2Column = OpenApiSchemaConverter.toColumnName(second) + "_id";

        List<String> pkColumns = List.of(fk1Column, fk2Column);

        return SqlTable.builder()
                .name(tableName)
                .columns(
                        List.of(
                                SqlColumn.builder()
                                        .name(fk1Column)
                                        .sqlType("BIGINT")
                                        .javaType("Long")
                                        .nullable(false)
                                        .primaryKey(true)
                                        .build(),
                                SqlColumn.builder()
                                        .name(fk2Column)
                                        .sqlType("BIGINT")
                                        .javaType("Long")
                                        .nullable(false)
                                        .primaryKey(true)
                                        .build()))
                .foreignKeys(
                        List.of(
                                SqlForeignKey.builder()
                                        .columnName(fk1Column)
                                        .referencedTable(OpenApiSchemaConverter.toTableName(first))
                                        .referencedColumn("id")
                                        .build(),
                                SqlForeignKey.builder()
                                        .columnName(fk2Column)
                                        .referencedTable(OpenApiSchemaConverter.toTableName(second))
                                        .referencedColumn("id")
                                        .build()))
                .primaryKeyColumns(pkColumns)
                .build();
    }

    private boolean hasJunctionTable(List<SqlTable> tables, String tableName) {
        return tables.stream().anyMatch(t -> t.getName().equals(tableName));
    }

    private String extractRefSchemaName(String ref) {
        if (ref == null) {
            return null;
        }
        int lastSlash = ref.lastIndexOf('/');
        if (lastSlash >= 0) {
            return ref.substring(lastSlash + 1);
        }
        return ref;
    }

    /** Exception thrown when OpenAPI parsing fails. */
    public static class OpenApiParseException extends RuntimeException {
        public OpenApiParseException(String message) {
            super(message);
        }

        public OpenApiParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
