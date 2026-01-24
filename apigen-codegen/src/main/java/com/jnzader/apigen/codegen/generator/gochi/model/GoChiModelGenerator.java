package com.jnzader.apigen.codegen.generator.gochi.model;

import com.jnzader.apigen.codegen.generator.gochi.GoChiTypeMapper;
import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates Go model structs for pgx (no GORM decorators).
 *
 * <p>Models are plain Go structs with JSON tags, designed for use with pgx and manual SQL queries.
 */
public class GoChiModelGenerator {

    private final GoChiTypeMapper typeMapper;

    public GoChiModelGenerator(GoChiTypeMapper typeMapper) {
        this.typeMapper = typeMapper;
    }

    public String generate(
            SqlTable table,
            List<SqlSchema.TableRelationship> relationships,
            List<SqlSchema.TableRelationship> inverseRelationships) {

        StringBuilder sb = new StringBuilder();
        String structName = typeMapper.toExportedName(table.getEntityName());

        // Collect imports
        Set<String> imports = collectImports(table);

        sb.append("package model\n\n");

        if (!imports.isEmpty()) {
            sb.append("import (\n");
            for (String imp : imports) {
                sb.append("\t\"").append(imp).append("\"\n");
            }
            sb.append(")\n\n");
        }

        // Struct definition
        sb.append("// ")
                .append(structName)
                .append(" represents the ")
                .append(table.getName())
                .append(" table.\n");
        sb.append("type ").append(structName).append(" struct {\n");

        // ID field
        sb.append("\tID        int64     `json:\"id\" db:\"id\"`\n");

        // Regular columns
        for (SqlColumn column : table.getColumns()) {
            if (isBaseField(column.getName())) {
                continue;
            }

            String fieldName = typeMapper.toExportedName(column.getName());
            String fieldType =
                    typeMapper.mapJavaTypeToGo(column.getJavaType(), column.isNullable());
            String jsonName = typeMapper.toSnakeCase(column.getName());
            String dbName = column.getName().toLowerCase();

            sb.append("\t").append(fieldName);
            sb.append(" ".repeat(Math.max(1, 10 - fieldName.length())));
            sb.append(fieldType);
            sb.append(" ".repeat(Math.max(1, 18 - fieldType.length())));

            // Tags
            sb.append("`json:\"").append(jsonName);
            if (column.isNullable()) {
                sb.append(",omitempty");
            }
            sb.append("\" db:\"").append(dbName).append("\"`\n");
        }

        // FK fields for relationships (store IDs)
        for (SqlSchema.TableRelationship rel : relationships) {
            String fkColumn = rel.getForeignKey().getColumnName();
            String fkFieldName = typeMapper.toExportedName(fkColumn);
            String jsonName = typeMapper.toSnakeCase(fkColumn);

            sb.append("\t").append(fkFieldName);
            sb.append(" ".repeat(Math.max(1, 10 - fkFieldName.length())));
            sb.append("*int64");
            sb.append(" ".repeat(12));
            sb.append("`json:\"")
                    .append(jsonName)
                    .append(",omitempty\" db:\"")
                    .append(fkColumn.toLowerCase())
                    .append("\"`\n");
        }

        // Audit fields
        sb.append("\tCreatedAt time.Time  `json:\"created_at\" db:\"created_at\"`\n");
        sb.append("\tUpdatedAt time.Time  `json:\"updated_at\" db:\"updated_at\"`\n");
        sb.append("\tDeletedAt *time.Time `json:\"-\" db:\"deleted_at\"`\n");

        sb.append("}\n\n");

        // TableName method
        sb.append("// TableName returns the table name.\n");
        sb.append("func (").append(structName).append(") TableName() string {\n");
        sb.append("\treturn \"").append(table.getName()).append("\"\n");
        sb.append("}\n\n");

        // IsDeleted method
        sb.append("// IsDeleted returns true if soft-deleted.\n");
        sb.append("func (m *").append(structName).append(") IsDeleted() bool {\n");
        sb.append("\treturn m.DeletedAt != nil\n");
        sb.append("}\n");

        return sb.toString();
    }

    private Set<String> collectImports(SqlTable table) {
        Set<String> imports = new HashSet<>();
        imports.add("time"); // Always need time for audit fields

        for (SqlColumn column : table.getColumns()) {
            String imp = typeMapper.getImportForType(column.getJavaType(), column.isNullable());
            if (imp != null) {
                imports.add(imp);
            }
        }

        return imports;
    }

    private boolean isBaseField(String columnName) {
        String lower = columnName.toLowerCase();
        return lower.equals("id")
                || lower.equals("created_at")
                || lower.equals("updated_at")
                || lower.equals("deleted_at");
    }
}
