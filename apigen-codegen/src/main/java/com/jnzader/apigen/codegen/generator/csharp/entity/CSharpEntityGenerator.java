package com.jnzader.apigen.codegen.generator.csharp.entity;

import com.jnzader.apigen.codegen.generator.common.ManyToManyRelation;
import com.jnzader.apigen.codegen.generator.csharp.CSharpTypeMapper;
import com.jnzader.apigen.codegen.model.RelationType;
import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlIndex;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Generates Entity Framework Core entity classes for C#/ASP.NET Core. */
public class CSharpEntityGenerator {

    private final String baseNamespace;
    private final CSharpTypeMapper typeMapper = new CSharpTypeMapper();

    public CSharpEntityGenerator(String baseNamespace) {
        this.baseNamespace = baseNamespace;
    }

    /** Generates the Entity class code in C#. */
    public String generate(
            SqlTable table,
            List<SqlSchema.TableRelationship> relations,
            List<SqlSchema.TableRelationship> inverseRelations,
            List<ManyToManyRelation> manyToManyRelations) {

        String entityName = table.getEntityName();
        String moduleName = table.getModuleName();
        String namespace = baseNamespace + "." + toPascalCase(moduleName) + ".Domain.Entities";

        StringBuilder sb = new StringBuilder();

        // Using statements
        sb.append("using System.ComponentModel.DataAnnotations;\n");
        sb.append("using System.ComponentModel.DataAnnotations.Schema;\n");

        // Collect related entity namespaces
        Set<String> relatedNamespaces = new HashSet<>();
        for (SqlSchema.TableRelationship rel : relations) {
            String targetModule = rel.getTargetTable().getModuleName();
            if (!targetModule.equals(moduleName)) {
                relatedNamespaces.add(
                        baseNamespace + "." + toPascalCase(targetModule) + ".Domain.Entities");
            }
        }
        for (SqlSchema.TableRelationship rel : inverseRelations) {
            String sourceModule = rel.getSourceTable().getModuleName();
            if (!sourceModule.equals(moduleName)) {
                relatedNamespaces.add(
                        baseNamespace + "." + toPascalCase(sourceModule) + ".Domain.Entities");
            }
        }
        for (ManyToManyRelation rel : manyToManyRelations) {
            String targetModule = rel.targetTable().getModuleName();
            if (!targetModule.equals(moduleName)) {
                relatedNamespaces.add(
                        baseNamespace + "." + toPascalCase(targetModule) + ".Domain.Entities");
            }
        }
        for (String ns : relatedNamespaces) {
            sb.append("using ").append(ns).append(";\n");
        }

        sb.append("\n");
        sb.append("namespace ").append(namespace).append(";\n\n");

        // Table attribute with indexes
        sb.append("[Table(\"").append(table.getName()).append("\")]\n");

        // Index attributes
        for (SqlIndex index : table.getIndexes()) {
            if (!index.getColumns().isEmpty()) {
                sb.append("[Index(");
                sb.append(
                        String.join(
                                ", ",
                                index.getColumns().stream()
                                        .map(c -> "nameof(" + toPascalCase(c) + ")")
                                        .toList()));
                if (index.isUnique()) {
                    sb.append(", IsUnique = true");
                }
                sb.append(", Name = \"").append(index.getName()).append("\")]\n");
            }
        }

        // Class declaration
        sb.append("public class ").append(entityName).append(" : BaseEntity\n");
        sb.append("{\n");

        // Generate fields for columns (excluding id which is in BaseEntity)
        for (SqlColumn col : table.getColumns()) {
            if (col.isPrimaryKey()) {
                continue; // Skip PK, it's in BaseEntity
            }
            if (isAuditField(col.getName())) {
                continue; // Skip audit fields, they're in BaseEntity
            }
            if (isForeignKeyColumn(col.getName(), relations)) {
                continue; // Skip FK columns, they'll be generated with relationships
            }

            generateColumnField(sb, col);
        }

        // Generate ManyToOne relationships
        for (SqlSchema.TableRelationship rel : relations) {
            if (rel.getRelationType() == RelationType.MANY_TO_ONE) {
                generateManyToOneRelation(sb, rel);
            } else if (rel.getRelationType() == RelationType.ONE_TO_ONE) {
                generateOneToOneRelation(sb, rel);
            }
        }

        // Generate OneToMany inverse relationships
        for (SqlSchema.TableRelationship rel : inverseRelations) {
            generateOneToManyInverse(sb, rel);
        }

        // Generate ManyToMany relationships
        for (ManyToManyRelation rel : manyToManyRelations) {
            generateManyToManyRelation(sb, rel);
        }

        sb.append("}\n");
        return sb.toString();
    }

    private void generateColumnField(StringBuilder sb, SqlColumn col) {
        String fieldName = toPascalCase(col.getJavaFieldName());
        String csharpType = typeMapper.mapColumnType(col);

        sb.append("\n");

        // Required attribute for non-nullable reference types
        if (!col.isNullable() && !typeMapper.isValueType(csharpType)) {
            sb.append("    [Required]\n");
        }

        // MaxLength for strings
        if (csharpType.equals("string") && col.getLength() != null && col.getLength() > 0) {
            sb.append("    [MaxLength(").append(col.getLength()).append(")]\n");
        }

        // Column attribute
        StringBuilder colAttr = new StringBuilder();
        colAttr.append("    [Column(\"").append(col.getName()).append("\"");
        if (csharpType.equals("decimal") && col.getPrecision() != null) {
            colAttr.append(", TypeName = \"decimal(")
                    .append(col.getPrecision())
                    .append(", ")
                    .append(col.getScale() != null ? col.getScale() : 2)
                    .append(")\"");
        }
        colAttr.append(")]\n");
        sb.append(colAttr);

        // Property declaration
        if (col.isNullable() && !typeMapper.isValueType(csharpType)) {
            sb.append("    public ").append(csharpType).append("? ").append(fieldName);
            sb.append(" { get; set; }\n");
        } else if (col.isNullable() && typeMapper.isValueType(csharpType)) {
            sb.append("    public ").append(csharpType).append("? ").append(fieldName);
            sb.append(" { get; set; }\n");
        } else {
            sb.append("    public ").append(csharpType).append(" ").append(fieldName);
            if (!typeMapper.isValueType(csharpType)) {
                sb.append(" { get; set; } = ")
                        .append(typeMapper.getDefaultValue(col))
                        .append(";\n");
            } else {
                sb.append(" { get; set; }\n");
            }
        }
    }

    private void generateManyToOneRelation(StringBuilder sb, SqlSchema.TableRelationship rel) {
        String targetEntityName = rel.getTargetTable().getEntityName();
        String fkColumnName = rel.getForeignKey().getColumnName();
        String propertyName = toPascalCase(toPropertyName(fkColumnName));

        sb.append("\n");
        sb.append("    [ForeignKey(nameof(").append(propertyName).append("))]\n");
        sb.append("    public long? ").append(propertyName).append("Id { get; set; }\n");
        sb.append("\n");
        sb.append("    public virtual ")
                .append(targetEntityName)
                .append("? ")
                .append(propertyName)
                .append(" { get; set; }\n");
    }

    private void generateOneToOneRelation(StringBuilder sb, SqlSchema.TableRelationship rel) {
        String targetEntityName = rel.getTargetTable().getEntityName();
        String fkColumnName = rel.getForeignKey().getColumnName();
        String propertyName = toPascalCase(toPropertyName(fkColumnName));

        sb.append("\n");
        sb.append("    [ForeignKey(nameof(").append(propertyName).append("))]\n");
        sb.append("    public long? ").append(propertyName).append("Id { get; set; }\n");
        sb.append("\n");
        sb.append("    public virtual ")
                .append(targetEntityName)
                .append("? ")
                .append(propertyName)
                .append(" { get; set; }\n");
    }

    private void generateOneToManyInverse(StringBuilder sb, SqlSchema.TableRelationship rel) {
        String sourceEntityName = rel.getSourceTable().getEntityName();
        String collectionName = toPlural(sourceEntityName);

        sb.append("\n");
        sb.append("    public virtual ICollection<")
                .append(sourceEntityName)
                .append("> ")
                .append(collectionName);
        sb.append(" { get; set; } = new List<").append(sourceEntityName).append(">();\n");
    }

    private void generateManyToManyRelation(StringBuilder sb, ManyToManyRelation rel) {
        String targetEntityName = rel.targetTable().getEntityName();
        String collectionName = toPlural(targetEntityName);

        sb.append("\n");
        sb.append("    public virtual ICollection<")
                .append(targetEntityName)
                .append("> ")
                .append(collectionName);
        sb.append(" { get; set; } = new List<").append(targetEntityName).append(">();\n");
    }

    private boolean isAuditField(String name) {
        String lower = name.toLowerCase();
        return lower.equals("estado")
                || lower.equals("created_at")
                || lower.equals("updated_at")
                || lower.equals("created_by")
                || lower.equals("updated_by")
                || lower.equals("deleted_at")
                || lower.equals("deleted_by");
    }

    private boolean isForeignKeyColumn(
            String columnName, List<SqlSchema.TableRelationship> relations) {
        for (SqlSchema.TableRelationship rel : relations) {
            if (rel.getForeignKey().getColumnName().equalsIgnoreCase(columnName)) {
                return true;
            }
        }
        return false;
    }

    private String toPropertyName(String fkColumnName) {
        // Remove _id suffix
        if (fkColumnName.toLowerCase().endsWith("_id")) {
            return fkColumnName.substring(0, fkColumnName.length() - 3);
        }
        return fkColumnName;
    }

    private String toPlural(String name) {
        if (name.endsWith("y")) {
            return name.substring(0, name.length() - 1) + "ies";
        } else if (name.endsWith("s") || name.endsWith("x") || name.endsWith("ch")) {
            return name + "es";
        }
        return name + "s";
    }

    private String toPascalCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : name.toCharArray()) {
            if (c == '_' || c == '-') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }
}
