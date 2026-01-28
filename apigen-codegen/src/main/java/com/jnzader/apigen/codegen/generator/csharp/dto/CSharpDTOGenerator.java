package com.jnzader.apigen.codegen.generator.csharp.dto;

import static com.jnzader.apigen.codegen.generator.util.NamingUtils.*;

import com.jnzader.apigen.codegen.generator.common.ManyToManyRelation;
import com.jnzader.apigen.codegen.generator.csharp.CSharpTypeMapper;
import com.jnzader.apigen.codegen.model.RelationType;
import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.List;

/** Generates DTO classes with init properties for C#/ASP.NET Core. */
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
        sb.append("public class ").append(entityName).append("Dto\n");
        sb.append("{\n");

        // Id from base
        sb.append("    public long Id { get; init; }\n");

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
                sb.append("    public ")
                        .append(csharpType)
                        .append("? ")
                        .append(fieldName)
                        .append(" { get; init; }\n");
            } else {
                sb.append("    public ")
                        .append(csharpType)
                        .append(" ")
                        .append(fieldName)
                        .append(" { get; init; } = default!;\n");
            }
        }

        // FK IDs from relations
        for (SqlSchema.TableRelationship rel : relations) {
            if (rel.getRelationType() == RelationType.MANY_TO_ONE
                    || rel.getRelationType() == RelationType.ONE_TO_ONE) {
                String fkColumnName = rel.getForeignKey().getColumnName();
                String propertyName = toPascalCase(toPropertyName(fkColumnName));
                sb.append("    public long? ").append(propertyName).append("Id { get; init; }\n");
            }
        }

        // ManyToMany collection IDs
        for (ManyToManyRelation rel : manyToManyRelations) {
            String targetEntityName = rel.targetTable().getEntityName();
            String collectionName = toPlural(targetEntityName) + "Ids";
            sb.append("    public IEnumerable<long>? ")
                    .append(collectionName)
                    .append(" { get; init; }\n");
        }

        // Audit fields from base
        sb.append("    public bool Estado { get; init; }\n");
        sb.append("    public DateTime CreatedAt { get; init; }\n");
        sb.append("    public DateTime? UpdatedAt { get; init; }\n");
        sb.append("    public string? CreatedBy { get; init; }\n");
        sb.append("    public string? UpdatedBy { get; init; }\n");

        sb.append("}\n");
    }

    private void generateCreateDto(
            StringBuilder sb,
            String entityName,
            SqlTable table,
            List<SqlSchema.TableRelationship> relations) {

        sb.append("/// <summary>\n");
        sb.append("/// DTO for creating a new ").append(entityName).append(".\n");
        sb.append("/// </summary>\n");
        sb.append("public class Create").append(entityName).append("Dto\n");
        sb.append("{\n");

        // Regular columns (excluding PK and audit fields)
        for (SqlColumn col : table.getColumns()) {
            if (col.isPrimaryKey()
                    || isAuditField(col.getName())
                    || isForeignKeyColumn(col.getName(), relations)) {
                continue;
            }

            String fieldName = toPascalCase(col.getJavaFieldName());
            String csharpType = typeMapper.mapColumnType(col);

            // Add Required attribute for non-nullable fields
            if (!col.isNullable()) {
                sb.append("    [Required]\n");
                sb.append("    public ")
                        .append(csharpType)
                        .append(" ")
                        .append(fieldName)
                        .append(" { get; init; } = default!;\n\n");
            } else {
                sb.append("    public ")
                        .append(csharpType)
                        .append("? ")
                        .append(fieldName)
                        .append(" { get; init; }\n\n");
            }
        }

        // FK IDs from relations
        for (SqlSchema.TableRelationship rel : relations) {
            if (rel.getRelationType() == RelationType.MANY_TO_ONE
                    || rel.getRelationType() == RelationType.ONE_TO_ONE) {
                String fkColumnName = rel.getForeignKey().getColumnName();
                String propertyName = toPascalCase(toPropertyName(fkColumnName));
                sb.append("    public long? ").append(propertyName).append("Id { get; init; }\n\n");
            }
        }

        sb.append("}\n");
    }

    private void generateUpdateDto(
            StringBuilder sb,
            String entityName,
            SqlTable table,
            List<SqlSchema.TableRelationship> relations) {

        sb.append("/// <summary>\n");
        sb.append("/// DTO for updating an existing ").append(entityName).append(".\n");
        sb.append("/// </summary>\n");
        sb.append("public class Update").append(entityName).append("Dto\n");
        sb.append("{\n");

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
            sb.append("    public ")
                    .append(csharpType)
                    .append("? ")
                    .append(fieldName)
                    .append(" { get; init; }\n\n");
        }

        // FK IDs from relations
        for (SqlSchema.TableRelationship rel : relations) {
            if (rel.getRelationType() == RelationType.MANY_TO_ONE
                    || rel.getRelationType() == RelationType.ONE_TO_ONE) {
                String fkColumnName = rel.getForeignKey().getColumnName();
                String propertyName = toPascalCase(toPropertyName(fkColumnName));
                sb.append("    public long? ").append(propertyName).append("Id { get; init; }\n\n");
            }
        }

        sb.append("}\n");
    }
}
