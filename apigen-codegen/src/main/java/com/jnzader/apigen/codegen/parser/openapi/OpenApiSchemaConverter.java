package com.jnzader.apigen.codegen.parser.openapi;

import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlForeignKey;
import com.jnzader.apigen.codegen.model.SqlTable;
import io.swagger.v3.oas.models.media.Schema;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Converts OpenAPI schemas to SqlTable/SqlColumn models.
 *
 * <p>This converter transforms OpenAPI component schemas into the internal SQL model format that
 * can be processed by the code generators.
 */
public class OpenApiSchemaConverter {

    private static final List<String> AUDIT_FIELDS =
            List.of(
                    "created_at",
                    "createdAt",
                    "updated_at",
                    "updatedAt",
                    "deleted_at",
                    "deletedAt",
                    "created_by",
                    "createdBy",
                    "updated_by",
                    "updatedBy",
                    "deleted_by",
                    "deletedBy");

    private OpenApiSchemaConverter() {
        // Utility class
    }

    /**
     * Converts an OpenAPI schema to an SqlTable.
     *
     * @param schemaName the name of the schema (typically the entity name)
     * @param schema the OpenAPI schema object
     * @param allSchemas all schemas in the OpenAPI spec (for resolving references)
     * @return the converted SqlTable
     */
    public static SqlTable convert(
            String schemaName, Schema<?> schema, Map<String, Schema<?>> allSchemas) {

        String tableName = toTableName(schemaName);
        List<SqlColumn> columns = new ArrayList<>();
        List<SqlForeignKey> foreignKeys = new ArrayList<>();
        List<String> primaryKeyColumns = new ArrayList<>();

        // Add ID column if not present in schema
        boolean hasId = false;

        Map<String, Schema> properties = schema.getProperties();
        if (properties != null) {
            for (Map.Entry<String, Schema> entry : properties.entrySet()) {
                String propertyName = entry.getKey();
                Schema<?> propertySchema = entry.getValue();

                // Check if this is the ID field
                if ("id".equalsIgnoreCase(propertyName)) {
                    hasId = true;
                    SqlColumn idColumn = createIdColumn(propertyName, propertySchema);
                    columns.add(idColumn);
                    primaryKeyColumns.add(idColumn.getName());
                    continue;
                }

                // Check if this is a reference to another schema
                if (isReference(propertySchema)) {
                    String refSchemaName = extractRefSchemaName(propertySchema);
                    if (refSchemaName != null && allSchemas.containsKey(refSchemaName)) {
                        // Create foreign key column
                        String fkColumnName = toColumnName(propertyName) + "_id";
                        columns.add(createForeignKeyColumn(fkColumnName, propertySchema));
                        foreignKeys.add(createForeignKey(fkColumnName, refSchemaName));
                        continue;
                    }
                }

                // Check if this is an array of references (many-to-many)
                if ("array".equals(propertySchema.getType())) {
                    Schema<?> itemsSchema = propertySchema.getItems();
                    if (itemsSchema != null && isReference(itemsSchema)) {
                        // Skip many-to-many for now, will be handled separately
                        continue;
                    }
                }

                // Regular column
                columns.add(createColumn(propertyName, propertySchema, schema));
            }
        }

        // Add default ID if none found
        if (!hasId) {
            SqlColumn idColumn =
                    SqlColumn.builder()
                            .name("id")
                            .sqlType("BIGSERIAL")
                            .javaType("Long")
                            .nullable(false)
                            .primaryKey(true)
                            .autoIncrement(true)
                            .build();
            columns.add(0, idColumn);
            primaryKeyColumns.add("id");
        }

        return SqlTable.builder()
                .name(tableName)
                .columns(columns)
                .foreignKeys(foreignKeys)
                .primaryKeyColumns(primaryKeyColumns)
                .comment(schema.getDescription())
                .build();
    }

    /**
     * Creates a column from an OpenAPI property schema.
     *
     * @param propertyName the property name
     * @param propertySchema the property schema
     * @param parentSchema the parent schema (for required fields check)
     * @return the SqlColumn
     */
    private static SqlColumn createColumn(
            String propertyName, Schema<?> propertySchema, Schema<?> parentSchema) {

        String columnName = toColumnName(propertyName);
        String sqlType = OpenApiTypeMapper.toSqlType(propertySchema);
        String javaType = OpenApiTypeMapper.toJavaType(propertySchema);

        boolean nullable = !isRequired(propertyName, parentSchema);
        boolean unique = Boolean.TRUE.equals(propertySchema.getUniqueItems());

        Integer length = OpenApiTypeMapper.extractLength(sqlType);
        Integer precision = null;
        Integer scale = null;

        int[] precisionScale = OpenApiTypeMapper.extractPrecisionScale(sqlType);
        if (precisionScale != null) {
            precision = precisionScale[0];
            scale = precisionScale[1];
        }

        // Extract enum values if present
        List<String> enumValues = null;
        if (propertySchema.getEnum() != null) {
            enumValues = propertySchema.getEnum().stream().map(Object::toString).toList();
        }

        return SqlColumn.builder()
                .name(columnName)
                .sqlType(sqlType)
                .javaType(javaType)
                .nullable(nullable)
                .primaryKey(false)
                .unique(unique)
                .autoIncrement(false)
                .length(length)
                .precision(precision)
                .scale(scale)
                .enumValues(enumValues)
                .comment(propertySchema.getDescription())
                .defaultValue(
                        propertySchema.getDefault() != null
                                ? propertySchema.getDefault().toString()
                                : null)
                .build();
    }

    /**
     * Creates an ID column.
     *
     * @param propertyName the property name
     * @param propertySchema the property schema
     * @return the SqlColumn for ID
     */
    private static SqlColumn createIdColumn(String propertyName, Schema<?> propertySchema) {
        String format = propertySchema.getFormat();
        String javaType;
        String sqlType;

        if ("uuid".equals(format)) {
            javaType = "UUID";
            sqlType = "UUID";
        } else if ("int64".equals(format)) {
            javaType = "Long";
            sqlType = "BIGSERIAL";
        } else {
            javaType = "Long";
            sqlType = "BIGSERIAL";
        }

        return SqlColumn.builder()
                .name(toColumnName(propertyName))
                .sqlType(sqlType)
                .javaType(javaType)
                .nullable(false)
                .primaryKey(true)
                .autoIncrement(!"UUID".equals(javaType))
                .build();
    }

    /**
     * Creates a foreign key column.
     *
     * @param columnName the column name
     * @param propertySchema the property schema
     * @return the SqlColumn for FK
     */
    private static SqlColumn createForeignKeyColumn(String columnName, Schema<?> propertySchema) {
        return SqlColumn.builder()
                .name(columnName)
                .sqlType("BIGINT")
                .javaType("Long")
                .nullable(true)
                .primaryKey(false)
                .unique(false)
                .comment("Foreign key to " + extractRefSchemaName(propertySchema))
                .build();
    }

    /**
     * Creates a foreign key constraint.
     *
     * @param columnName the column name
     * @param refSchemaName the referenced schema name
     * @return the SqlForeignKey
     */
    private static SqlForeignKey createForeignKey(String columnName, String refSchemaName) {
        return SqlForeignKey.builder()
                .columnName(columnName)
                .referencedTable(toTableName(refSchemaName))
                .referencedColumn("id")
                .build();
    }

    /**
     * Checks if a property is a reference to another schema.
     *
     * @param schema the schema to check
     * @return true if it's a reference
     */
    private static boolean isReference(Schema<?> schema) {
        return schema.get$ref() != null;
    }

    /**
     * Extracts the schema name from a $ref.
     *
     * @param schema the schema with $ref
     * @return the referenced schema name
     */
    private static String extractRefSchemaName(Schema<?> schema) {
        String ref = schema.get$ref();
        if (ref == null) {
            return null;
        }
        // $ref format: "#/components/schemas/SchemaName"
        int lastSlash = ref.lastIndexOf('/');
        if (lastSlash >= 0) {
            return ref.substring(lastSlash + 1);
        }
        return ref;
    }

    /**
     * Checks if a property is required in the parent schema.
     *
     * @param propertyName the property name
     * @param parentSchema the parent schema
     * @return true if required
     */
    private static boolean isRequired(String propertyName, Schema<?> parentSchema) {
        List<String> required = parentSchema.getRequired();
        return required != null && required.contains(propertyName);
    }

    /**
     * Converts a schema name to a table name (plural, snake_case).
     *
     * @param schemaName the schema name
     * @return the table name
     */
    public static String toTableName(String schemaName) {
        if (schemaName == null || schemaName.isEmpty()) {
            return schemaName;
        }
        String snakeCase = camelToSnakeCase(schemaName);
        return toPlural(snakeCase.toLowerCase(Locale.ROOT));
    }

    /**
     * Converts a property name to a column name (snake_case).
     *
     * @param propertyName the property name
     * @return the column name
     */
    public static String toColumnName(String propertyName) {
        if (propertyName == null || propertyName.isEmpty()) {
            return propertyName;
        }
        return camelToSnakeCase(propertyName).toLowerCase(Locale.ROOT);
    }

    /**
     * Converts camelCase to snake_case.
     *
     * @param input the camelCase string
     * @return the snake_case string
     */
    private static String camelToSnakeCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Converts singular to plural form.
     *
     * @param singular the singular form
     * @return the plural form
     */
    private static String toPlural(String singular) {
        if (singular == null || singular.isEmpty()) {
            return singular;
        }

        if (singular.endsWith("y")
                && singular.length() > 1
                && !isVowel(singular.charAt(singular.length() - 2))) {
            return singular.substring(0, singular.length() - 1) + "ies";
        }
        if (singular.endsWith("s")
                || singular.endsWith("x")
                || singular.endsWith("ch")
                || singular.endsWith("sh")) {
            return singular + "es";
        }
        return singular + "s";
    }

    private static boolean isVowel(char c) {
        return "aeiouAEIOU".indexOf(c) >= 0;
    }
}
