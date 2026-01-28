package com.jnzader.apigen.codegen.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.Builder;
import lombok.Data;

/** Represents a table parsed from SQL CREATE TABLE statement. */
@Data
@Builder
public class SqlTable {
    private String name;
    private String schema;
    private String comment;

    @Builder.Default private List<SqlColumn> columns = new ArrayList<>();

    @Builder.Default private List<SqlForeignKey> foreignKeys = new ArrayList<>();

    @Builder.Default private List<SqlIndex> indexes = new ArrayList<>();

    @Builder.Default private List<String> primaryKeyColumns = new ArrayList<>();

    @Builder.Default private List<String> uniqueConstraints = new ArrayList<>();

    @Builder.Default private List<String> checkConstraints = new ArrayList<>();

    /** Whether this table is a junction table (for many-to-many relationships). */
    public boolean isJunctionTable() {
        // A junction table typically:
        // 1. Has exactly 2 foreign keys
        // 2. Has a composite primary key consisting of both FK columns
        // 3. May have additional audit columns but no business columns
        if (foreignKeys.size() != 2) {
            return false;
        }

        // Check if PK is composite of FK columns
        if (primaryKeyColumns.size() != 2) {
            return false;
        }

        List<String> fkColumns = foreignKeys.stream().map(SqlForeignKey::getColumnName).toList();

        return primaryKeyColumns.containsAll(fkColumns);
    }

    /** Gets the entity name (PascalCase, singular) from table name. */
    public String getEntityName() {
        if (name == null) return null;

        String singular = toSingular(name);
        return snakeToPascalCase(singular);
    }

    /** Converts a plural table name to singular form. */
    private String toSingular(String tableName) {
        if (tableName.endsWith("ies")) {
            return tableName.substring(0, tableName.length() - 3) + "y";
        }
        if (tableName.endsWith("sses")) {
            return tableName.substring(0, tableName.length() - 2);
        }
        if (endsWithAny(tableName, "xes", "ches", "shes")) {
            return tableName.substring(0, tableName.length() - 2);
        }
        if (endsWithAny(tableName, "uses", "ases", "ises", "oses")) {
            return tableName.substring(0, tableName.length() - 2);
        }
        if (tableName.endsWith("s") && !tableName.endsWith("ss")) {
            return tableName.substring(0, tableName.length() - 1);
        }
        return tableName;
    }

    private boolean endsWithAny(String str, String... suffixes) {
        for (String suffix : suffixes) {
            if (str.endsWith(suffix)) return true;
        }
        return false;
    }

    /** Converts snake_case to PascalCase. */
    private String snakeToPascalCase(String input) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : input.toLowerCase(Locale.ROOT).toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else {
                result.append(capitalizeNext ? Character.toUpperCase(c) : c);
                capitalizeNext = false;
            }
        }
        return result.toString();
    }

    /** Gets the module name (lowercase, plural) from table name. */
    public String getModuleName() {
        if (name == null) return null;
        return name.toLowerCase(Locale.ROOT).replace("_", "");
    }

    /** Gets a column by name. */
    public SqlColumn getColumnByName(String columnName) {
        return columns.stream()
                .filter(c -> c.getName().equalsIgnoreCase(columnName))
                .findFirst()
                .orElse(null);
    }

    /** Gets non-FK, non-PK business columns (for entity fields). */
    public List<SqlColumn> getBusinessColumns() {
        List<String> fkColumns = foreignKeys.stream().map(SqlForeignKey::getColumnName).toList();

        return columns.stream()
                .filter(c -> !c.isPrimaryKey())
                .filter(c -> !fkColumns.contains(c.getName()))
                .filter(c -> !isBaseColumn(c.getName()))
                .toList();
    }

    /** Checks if column is a standard Base entity column. */
    private boolean isBaseColumn(String columnName) {
        return List.of(
                        "estado",
                        "fecha_creacion",
                        "fecha_actualizacion",
                        "fecha_eliminacion",
                        "creado_por",
                        "modificado_por",
                        "eliminado_por",
                        "version",
                        "created_at",
                        "updated_at",
                        "deleted_at",
                        "created_by",
                        "updated_by",
                        "deleted_by")
                .contains(columnName.toLowerCase(Locale.ROOT));
    }

    /** Checks if this table extends Base entity (has standard audit columns). */
    public boolean extendsBase() {
        List<String> columnNames =
                columns.stream().map(c -> c.getName().toLowerCase(Locale.ROOT)).toList();

        // Check for common audit columns
        return columnNames.contains("estado") || columnNames.contains("created_at");
    }

    /** Gets the camelCase variable name for this entity. */
    public String getEntityVariableName() {
        String entityName = getEntityName();
        if (entityName == null || entityName.isEmpty()) return null;
        return Character.toLowerCase(entityName.charAt(0)) + entityName.substring(1);
    }
}
