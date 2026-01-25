package com.jnzader.apigen.codegen.generator.go.model;

import com.jnzader.apigen.codegen.generator.go.GoTypeMapper;
import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Generates GORM model structs from SQL table definitions for Go/Gin. */
@SuppressWarnings({
    "java:S1068",
    "java:S1192",
    "java:S2479",
    "java:S3776"
}) // S1068: moduleName reserved; S1192: template strings; S2479: tabs for Go; S3776: complex model
// gen
public class GoModelGenerator {

    private final GoTypeMapper typeMapper;
    private final String moduleName;

    public GoModelGenerator(GoTypeMapper typeMapper, String moduleName) {
        this.typeMapper = typeMapper;
        this.moduleName = moduleName;
    }

    /**
     * Generates a GORM model struct for a table.
     *
     * @param table the SQL table
     * @param relationships relationships where this table is the source
     * @param inverseRelationships relationships where this table is the target
     * @return the generated Go code
     */
    public String generate(
            SqlTable table,
            List<SqlSchema.TableRelationship> relationships,
            List<SqlSchema.TableRelationship> inverseRelationships) {

        StringBuilder sb = new StringBuilder();
        String structName = typeMapper.toExportedName(table.getEntityName());
        String tableName = table.getName();

        // Collect imports
        Set<String> imports = collectImports(table);

        // Package declaration
        sb.append("package models\n\n");

        // Imports
        if (!imports.isEmpty()) {
            sb.append("import (\n");
            // Standard library imports first
            if (imports.contains("time")) {
                sb.append("\t\"time\"\n");
            }
            // External imports
            sb.append("\n");
            if (imports.contains("gorm")) {
                sb.append("\t\"gorm.io/gorm\"\n");
            }
            if (imports.contains("uuid")) {
                sb.append("\t\"github.com/google/uuid\"\n");
            }
            if (imports.contains("decimal")) {
                sb.append("\t\"github.com/shopspring/decimal\"\n");
            }
            sb.append(")\n\n");
        }

        // Struct definition
        sb.append("// ")
                .append(structName)
                .append(" represents the ")
                .append(tableName)
                .append(" table.\n");
        sb.append("type ").append(structName).append(" struct {\n");

        // ID field
        sb.append("\tID        int64          `gorm:\"primaryKey;autoIncrement\" json:\"id\"`\n");

        // Regular columns (skip id, created_at, updated_at, deleted_at)
        for (SqlColumn column : table.getColumns()) {
            if (isBaseField(column.getName())) {
                continue;
            }

            String fieldName = typeMapper.toExportedName(column.getName());
            String fieldType =
                    typeMapper.mapJavaTypeToGo(column.getJavaType(), column.isNullable());
            String jsonName = typeMapper.toSnakeCase(column.getName());

            sb.append("\t").append(fieldName);
            // Pad field name for alignment
            sb.append(" ".repeat(Math.max(1, 10 - fieldName.length())));
            sb.append(fieldType);
            // Pad type for alignment
            sb.append(" ".repeat(Math.max(1, 16 - fieldType.length())));

            // GORM and JSON tags
            sb.append("`gorm:\"");
            sb.append("column:").append(column.getName());
            if (!column.isNullable()) {
                sb.append(";not null");
            }
            if (column.isUnique()) {
                sb.append(";uniqueIndex");
            }
            if (column.getLength() != null
                    && column.getLength() > 0
                    && "String".equals(column.getJavaType())) {
                sb.append(";size:").append(column.getLength());
            }
            if (column.getDefaultValue() != null && !column.getDefaultValue().isEmpty()) {
                sb.append(";default:").append(formatDefaultValue(column));
            }
            sb.append("\" json:\"").append(jsonName);
            if (column.isNullable()) {
                sb.append(",omitempty");
            }
            sb.append("\"`\n");
        }

        // ManyToOne relationships (foreign keys)
        for (SqlSchema.TableRelationship rel : relationships) {
            String relatedName = typeMapper.toExportedName(rel.getTargetTable().getEntityName());
            String fieldName = relatedName;
            String fkColumn = rel.getForeignKey().getColumnName();

            // Foreign key ID field
            String fkFieldName = typeMapper.toExportedName(fkColumn);
            sb.append("\t").append(fkFieldName);
            sb.append(" ".repeat(Math.max(1, 10 - fkFieldName.length())));
            sb.append("*int64");
            sb.append(" ".repeat(10));
            sb.append("`gorm:\"column:").append(fkColumn).append("\" json:\"");
            sb.append(typeMapper.toSnakeCase(fkColumn)).append(",omitempty\"`\n");

            // Related entity
            sb.append("\t").append(fieldName);
            sb.append(" ".repeat(Math.max(1, 10 - fieldName.length())));
            sb.append("*").append(relatedName);
            sb.append(" ".repeat(Math.max(1, 16 - relatedName.length() - 1)));
            sb.append("`gorm:\"foreignKey:").append(fkFieldName);
            sb.append(";references:ID\" json:\"");
            sb.append(typeMapper.toSnakeCase(fieldName)).append(",omitempty\"`\n");
        }

        // OneToMany relationships (inverse)
        for (SqlSchema.TableRelationship rel : inverseRelationships) {
            String relatedName = typeMapper.toExportedName(rel.getSourceTable().getEntityName());
            String fieldName = typeMapper.pluralize(relatedName);
            String fkColumn = rel.getForeignKey().getColumnName();

            sb.append("\t").append(fieldName);
            sb.append(" ".repeat(Math.max(1, 10 - fieldName.length())));
            sb.append("[]").append(relatedName);
            sb.append(" ".repeat(Math.max(1, 14 - relatedName.length())));
            sb.append("`gorm:\"foreignKey:").append(typeMapper.toExportedName(fkColumn));
            sb.append("\" json:\"")
                    .append(typeMapper.toSnakeCase(fieldName))
                    .append(",omitempty\"`\n");
        }

        // Audit fields
        sb.append("\tCreatedAt time.Time      `gorm:\"autoCreateTime\" json:\"created_at\"`\n");
        sb.append("\tUpdatedAt time.Time      `gorm:\"autoUpdateTime\" json:\"updated_at\"`\n");
        sb.append("\tDeletedAt gorm.DeletedAt `gorm:\"index\" json:\"-\"`\n");

        sb.append("}\n\n");

        // TableName method
        sb.append("// TableName specifies the table name for GORM.\n");
        sb.append("func (").append(structName).append(") TableName() string {\n");
        sb.append("\treturn \"").append(tableName).append("\"\n");
        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Generates the base model with common fields.
     *
     * @return the generated Go code for base model
     */
    public String generateBaseModel() {
        return """
        package models

        import (
        \t"time"

        \t"gorm.io/gorm"
        )

        // BaseModel contains common fields for all models.
        type BaseModel struct {
        \tID        int64          `gorm:"primaryKey;autoIncrement" json:"id"`
        \tCreatedAt time.Time      `gorm:"autoCreateTime" json:"created_at"`
        \tUpdatedAt time.Time      `gorm:"autoUpdateTime" json:"updated_at"`
        \tDeletedAt gorm.DeletedAt `gorm:"index" json:"-"`
        }

        // IsDeleted returns true if the record is soft-deleted.
        func (b *BaseModel) IsDeleted() bool {
        \treturn b.DeletedAt.Valid
        }
        """;
    }

    private Set<String> collectImports(SqlTable table) {

        Set<String> imports = new HashSet<>();
        imports.add("time"); // For CreatedAt, UpdatedAt
        imports.add("gorm"); // For DeletedAt

        for (SqlColumn column : table.getColumns()) {
            switch (column.getJavaType()) {
                case "UUID" -> imports.add("uuid");
                case "BigDecimal" -> imports.add("decimal");
                default -> {
                    // Other types don't require additional imports
                }
            }
        }

        return imports;
    }

    private boolean isBaseField(String columnName) {
        String lower = columnName.toLowerCase();
        return lower.equals("id")
                || lower.equals("created_at")
                || lower.equals("updated_at")
                || lower.equals("deleted_at")
                || lower.equals("createdat")
                || lower.equals("updatedat")
                || lower.equals("deletedat");
    }

    private String formatDefaultValue(SqlColumn column) {
        String defaultValue = column.getDefaultValue();
        if (defaultValue == null) {
            return "";
        }
        // Clean up common default value formats
        defaultValue = defaultValue.trim();
        if (defaultValue.startsWith("'") && defaultValue.endsWith("'")) {
            return defaultValue.substring(1, defaultValue.length() - 1);
        }
        if (defaultValue.equalsIgnoreCase("true") || defaultValue.equalsIgnoreCase("false")) {
            return defaultValue.toLowerCase();
        }
        return defaultValue;
    }
}
