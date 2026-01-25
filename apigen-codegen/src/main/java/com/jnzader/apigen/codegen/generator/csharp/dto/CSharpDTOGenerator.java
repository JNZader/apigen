package com.jnzader.apigen.codegen.generator.csharp.dto;

import static com.jnzader.apigen.codegen.generator.util.NamingUtils.*;

import com.jnzader.apigen.codegen.generator.common.ManyToManyRelation;
import com.jnzader.apigen.codegen.generator.csharp.CSharpTypeMapper;
import com.jnzader.apigen.codegen.model.RelationType;
import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.ArrayList;
import java.util.List;

/** Generates DTO record classes for C#/ASP.NET Core. */
@SuppressWarnings("java:S1192") // Duplicate strings intentional for code generation templates
public class CSharpDTOGenerator {

    private final String baseNamespace;
    private final CSharpTypeMapper typeMapper = new CSharpTypeMapper();

    public CSharpDTOGenerator(String baseNamespace) {
        this.baseNamespace = baseNamespace;
    }

    /** Generates the DTO code in C#. */
    public String generate(
            SqlTable table,
            List<SqlSchema.TableRelationship> relations,
            List<ManyToManyRelation> manyToManyRelations) {

        String entityName = table.getEntityName();
        String moduleName = table.getModuleName();
        String namespace = baseNamespace + "." + toPascalCase(moduleName) + ".Application.DTOs";

        StringBuilder sb = new StringBuilder();

        // Using statements
        sb.append("using System.ComponentModel.DataAnnotations;\n\n");

        sb.append("namespace ").append(namespace).append(";\n\n");

        // Response DTO (full entity data)
        generateResponseDto(sb, entityName, table, relations, manyToManyRelations);

        sb.append("\n");

        // Create DTO (for creating new entities)
        generateCreateDto(sb, entityName, table, relations);

        sb.append("\n");

        // Update DTO (for updating entities)
        generateUpdateDto(sb, entityName, table, relations);

        return sb.toString();
    }

    private void generateResponseDto(
            StringBuilder sb,
            String entityName,
            SqlTable table,
            List<SqlSchema.TableRelationship> relations,
            List<ManyToManyRelation> manyToManyRelations) {

        sb.append("/// <summary>\n");
        sb.append("/// Response DTO for ").append(entityName).append(" entity.\n");
        sb.append("/// </summary>\n");
        sb.append("public record ").append(entityName).append("Dto(\n");

        List<String> properties = new ArrayList<>();

        // Id from base
        properties.add("    long Id");

        // Regular columns
        for (SqlColumn col : table.getColumns()) {
            if (col.isPrimaryKey()
                    || isAuditField(col.getName())
                    || isForeignKeyColumn(col.getName(), relations)) {
                continue;
            }

            String fieldName = toPascalCase(col.getJavaFieldName());
            String csharpType = typeMapper.mapColumnType(col);

            if (col.isNullable()) {
                properties.add("    " + csharpType + "? " + fieldName);
            } else {
                properties.add("    " + csharpType + " " + fieldName);
            }
        }

        // FK IDs from relations
        for (SqlSchema.TableRelationship rel : relations) {
            if (rel.getRelationType() == RelationType.MANY_TO_ONE
                    || rel.getRelationType() == RelationType.ONE_TO_ONE) {
                String fkColumnName = rel.getForeignKey().getColumnName();
                String propertyName = toPascalCase(toPropertyName(fkColumnName));
                properties.add("    long? " + propertyName + "Id");
            }
        }

        // ManyToMany collection IDs
        for (ManyToManyRelation rel : manyToManyRelations) {
            String targetEntityName = rel.targetTable().getEntityName();
            String collectionName = toPlural(targetEntityName) + "Ids";
            properties.add("    IEnumerable<long>? " + collectionName);
        }

        // Audit fields from base
        properties.add("    bool Estado");
        properties.add("    DateTime CreatedAt");
        properties.add("    DateTime? UpdatedAt");
        properties.add("    string? CreatedBy");
        properties.add("    string? UpdatedBy");

        sb.append(String.join(",\n", properties));
        sb.append("\n);\n");
    }

    private void generateCreateDto(
            StringBuilder sb,
            String entityName,
            SqlTable table,
            List<SqlSchema.TableRelationship> relations) {

        sb.append("/// <summary>\n");
        sb.append("/// DTO for creating a new ").append(entityName).append(".\n");
        sb.append("/// </summary>\n");
        sb.append("public record Create").append(entityName).append("Dto(\n");

        List<String> properties = new ArrayList<>();

        // Regular columns (excluding PK and audit fields)
        for (SqlColumn col : table.getColumns()) {
            if (col.isPrimaryKey()
                    || isAuditField(col.getName())
                    || isForeignKeyColumn(col.getName(), relations)) {
                continue;
            }

            String fieldName = toPascalCase(col.getJavaFieldName());
            String csharpType = typeMapper.mapColumnType(col);

            StringBuilder prop = new StringBuilder();

            // Add Required attribute for non-nullable fields
            if (!col.isNullable()) {
                prop.append("    [Required] ");
                prop.append(csharpType).append(" ").append(fieldName);
            } else {
                prop.append("    ").append(csharpType).append("? ").append(fieldName);
            }

            properties.add(prop.toString());
        }

        // FK IDs from relations
        for (SqlSchema.TableRelationship rel : relations) {
            if (rel.getRelationType() == RelationType.MANY_TO_ONE
                    || rel.getRelationType() == RelationType.ONE_TO_ONE) {
                String fkColumnName = rel.getForeignKey().getColumnName();
                String propertyName = toPascalCase(toPropertyName(fkColumnName));
                properties.add("    long? " + propertyName + "Id");
            }
        }

        sb.append(String.join(",\n", properties));
        sb.append("\n);\n");
    }

    private void generateUpdateDto(
            StringBuilder sb,
            String entityName,
            SqlTable table,
            List<SqlSchema.TableRelationship> relations) {

        sb.append("/// <summary>\n");
        sb.append("/// DTO for updating an existing ").append(entityName).append(".\n");
        sb.append("/// </summary>\n");
        sb.append("public record Update").append(entityName).append("Dto(\n");

        List<String> properties = new ArrayList<>();

        // All fields are optional for partial updates
        for (SqlColumn col : table.getColumns()) {
            if (col.isPrimaryKey()
                    || isAuditField(col.getName())
                    || isForeignKeyColumn(col.getName(), relations)) {
                continue;
            }

            String fieldName = toPascalCase(col.getJavaFieldName());
            String csharpType = typeMapper.mapColumnType(col);

            // All fields nullable for partial updates
            properties.add("    " + csharpType + "? " + fieldName);
        }

        // FK IDs from relations
        for (SqlSchema.TableRelationship rel : relations) {
            if (rel.getRelationType() == RelationType.MANY_TO_ONE
                    || rel.getRelationType() == RelationType.ONE_TO_ONE) {
                String fkColumnName = rel.getForeignKey().getColumnName();
                String propertyName = toPascalCase(toPropertyName(fkColumnName));
                properties.add("    long? " + propertyName + "Id");
            }
        }

        sb.append(String.join(",\n", properties));
        sb.append("\n);\n");
    }
}
