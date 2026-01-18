package com.jnzader.apigen.codegen.model;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents the complete parsed SQL schema.
 */
@Data
@Builder
public class SqlSchema {
    private String name;
    private String sourceFile;

    @Builder.Default
    private List<SqlTable> tables = new ArrayList<>();

    @Builder.Default
    private List<SqlFunction> functions = new ArrayList<>();

    @Builder.Default
    private List<SqlIndex> standaloneIndexes = new ArrayList<>();

    @Builder.Default
    private List<String> extensions = new ArrayList<>();

    @Builder.Default
    private List<String> parseErrors = new ArrayList<>();

    /**
     * Gets all tables that should generate entities (excludes junction tables).
     */
    public List<SqlTable> getEntityTables() {
        return tables.stream()
                .filter(t -> !t.isJunctionTable())
                .filter(t -> !isAuditTable(t.getName()))
                .toList();
    }

    /**
     * Gets all junction tables (for many-to-many relationships).
     */
    public List<SqlTable> getJunctionTables() {
        return tables.stream()
                .filter(SqlTable::isJunctionTable)
                .toList();
    }

    /**
     * Gets a table by name.
     */
    public SqlTable getTableByName(String name) {
        return tables.stream()
                .filter(t -> t.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Checks if a table is an audit table (ends with _aud or _audit).
     */
    private boolean isAuditTable(String tableName) {
        String lower = tableName.toLowerCase();
        return lower.endsWith("_aud") || lower.endsWith("_audit") || lower.equals("revision_info");
    }

    /**
     * Gets all relationships between tables.
     */
    public List<TableRelationship> getAllRelationships() {
        List<TableRelationship> relationships = new ArrayList<>();

        for (SqlTable table : tables) {
            for (SqlForeignKey fk : table.getForeignKeys()) {
                SqlTable referencedTable = getTableByName(fk.getReferencedTable());
                if (referencedTable != null) {
                    RelationType relationType = fk.inferRelationType(table, referencedTable);
                    relationships.add(TableRelationship.builder()
                            .sourceTable(table)
                            .targetTable(referencedTable)
                            .foreignKey(fk)
                            .relationType(relationType)
                            .build());
                }
            }
        }

        return relationships;
    }

    /**
     * Groups tables by module (based on naming patterns or prefixes).
     */
    public Map<String, List<SqlTable>> getTablesByModule() {
        return tables.stream()
                .filter(t -> !t.isJunctionTable())
                .filter(t -> !isAuditTable(t.getName()))
                .collect(Collectors.groupingBy(SqlTable::getModuleName));
    }

    /**
     * Gets functions grouped by their related table (if any).
     */
    public Map<String, List<SqlFunction>> getFunctionsByTable() {
        return functions.stream()
                .collect(Collectors.groupingBy(f -> inferTableFromFunctionName(f.getName())));
    }

    private String inferTableFromFunctionName(String functionName) {
        // Try to infer table from function name patterns like:
        // get_user_by_id -> users
        // create_product -> products
        // update_category -> categories
        for (SqlTable table : tables) {
            String tableName = table.getName().toLowerCase();
            String singular = tableName.endsWith("s") ? tableName.substring(0, tableName.length() - 1) : tableName;

            if (functionName.toLowerCase().contains(singular)) {
                return tableName;
            }
        }
        return "_global";
    }

    /**
     * Validates the schema for common issues.
     */
    public List<String> validate() {
        List<String> issues = new ArrayList<>();

        // Check for tables without primary keys
        for (SqlTable table : tables) {
            if (table.getPrimaryKeyColumns().isEmpty()) {
                issues.add("Table '" + table.getName() + "' has no primary key");
            }
        }

        // Check for dangling foreign keys
        for (SqlTable table : tables) {
            for (SqlForeignKey fk : table.getForeignKeys()) {
                if (getTableByName(fk.getReferencedTable()) == null) {
                    issues.add("Foreign key in '" + table.getName() + "' references non-existent table '" + fk.getReferencedTable() + "'");
                }
            }
        }

        // Check for potential naming conflicts
        Map<String, Long> entityNameCounts = tables.stream()
                .collect(Collectors.groupingBy(SqlTable::getEntityName, Collectors.counting()));

        entityNameCounts.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .forEach(e -> issues.add("Multiple tables would generate entity name '" + e.getKey() + "'"));

        return issues;
    }

    /**
     * Represents a relationship between two tables.
     */
    @Data
    @Builder
    public static class TableRelationship {
        private SqlTable sourceTable;
        private SqlTable targetTable;
        private SqlForeignKey foreignKey;
        private RelationType relationType;
    }
}
