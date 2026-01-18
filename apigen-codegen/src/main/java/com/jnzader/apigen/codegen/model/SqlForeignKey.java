package com.jnzader.apigen.codegen.model;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a foreign key constraint parsed from SQL.
 */
@Data
@Builder
public class SqlForeignKey {
    private String name;
    private String columnName;
    private String referencedTable;
    private String referencedColumn;
    private ForeignKeyAction onDelete;
    private ForeignKeyAction onUpdate;

    public enum ForeignKeyAction {
        CASCADE,
        SET_NULL,
        SET_DEFAULT,
        RESTRICT,
        NO_ACTION
    }

    /**
     * Determines the JPA relationship type based on FK characteristics.
     * Note: referencedTable reserved for future bidirectional relationship detection.
     */
    @SuppressWarnings("java:S1172") // referencedTable reserved for future use
    public RelationType inferRelationType(SqlTable parentTable, SqlTable referencedTable) {
        // Check if this is a junction table (many-to-many)
        if (isJunctionTableFk(parentTable)) {
            return RelationType.MANY_TO_MANY;
        }

        // Check if unique constraint on FK column (one-to-one)
        SqlColumn column = parentTable.getColumnByName(columnName);
        if (column != null && column.isUnique()) {
            return RelationType.ONE_TO_ONE;
        }

        // Default to many-to-one
        return RelationType.MANY_TO_ONE;
    }

    private boolean isJunctionTableFk(SqlTable table) {
        // A junction table typically has:
        // 1. Only 2 columns that are both FKs (+ possible audit fields)
        // 2. Composite primary key on both FK columns
        long fkCount = table.getForeignKeys().size();
        long pkColumnCount = table.getColumns().stream()
                .filter(SqlColumn::isPrimaryKey)
                .count();

        return fkCount == 2 && pkColumnCount == 2;
    }

    /**
     * Converts table name to Java entity name.
     */
    public String getReferencedEntityName() {
        if (referencedTable == null) return null;

        String singular = toSingular(referencedTable);
        return snakeToPascalCase(singular);
    }

    /**
     * Converts a plural table name to singular form.
     */
    private String toSingular(String name) {
        if (name.endsWith("ies")) {
            return name.substring(0, name.length() - 3) + "y";
        }
        if (name.endsWith("sses")) {
            return name.substring(0, name.length() - 2);
        }
        if (endsWithAny(name, "xes", "ches", "shes")) {
            return name.substring(0, name.length() - 2);
        }
        if (endsWithAny(name, "uses", "ases", "ises", "oses")) {
            return name.substring(0, name.length() - 2);
        }
        if (name.endsWith("s") && !name.endsWith("ss")) {
            return name.substring(0, name.length() - 1);
        }
        return name;
    }

    private boolean endsWithAny(String str, String... suffixes) {
        for (String suffix : suffixes) {
            if (str.endsWith(suffix)) return true;
        }
        return false;
    }

    /**
     * Converts snake_case to PascalCase.
     */
    private String snakeToPascalCase(String input) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : input.toLowerCase().toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else {
                result.append(capitalizeNext ? Character.toUpperCase(c) : c);
                capitalizeNext = false;
            }
        }
        return result.toString();
    }

    /**
     * Gets the Java field name for this relationship.
     */
    public String getJavaFieldName() {
        // Remove _id suffix if present
        String fieldName = columnName;
        if (fieldName.endsWith("_id")) {
            fieldName = fieldName.substring(0, fieldName.length() - 3);
        }

        // Convert to camelCase
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;
        for (char c : fieldName.toLowerCase().toCharArray()) {
            if (c == '_') {
                capitalizeNext = true;
            } else {
                result.append(capitalizeNext ? Character.toUpperCase(c) : c);
                capitalizeNext = false;
            }
        }
        return result.toString();
    }
}
