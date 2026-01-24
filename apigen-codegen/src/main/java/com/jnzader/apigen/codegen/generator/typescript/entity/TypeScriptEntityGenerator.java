package com.jnzader.apigen.codegen.generator.typescript.entity;

import com.jnzader.apigen.codegen.generator.typescript.TypeScriptTypeMapper;
import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.List;

/**
 * Generates TypeORM entity classes for NestJS projects.
 *
 * <p>Generates:
 *
 * <ul>
 *   <li>Entity classes with TypeORM decorators
 *   <li>Relationship mappings
 *   <li>Base entity with audit fields
 * </ul>
 */
public class TypeScriptEntityGenerator {

    private final TypeScriptTypeMapper typeMapper;

    public TypeScriptEntityGenerator() {
        this.typeMapper = new TypeScriptTypeMapper();
    }

    /**
     * Generates the base entity class with common audit fields.
     *
     * @return the base.entity.ts content
     */
    public String generateBaseEntity() {
        return """
        import {
          PrimaryGeneratedColumn,
          Column,
          CreateDateColumn,
          UpdateDateColumn,
          DeleteDateColumn,
        } from 'typeorm';

        export abstract class BaseEntity {
          @PrimaryGeneratedColumn()
          id: number;

          @Column({ name: 'activo', default: true })
          activo: boolean;

          @CreateDateColumn({ name: 'created_at' })
          createdAt: Date;

          @UpdateDateColumn({ name: 'updated_at' })
          updatedAt: Date;

          @Column({ name: 'created_by', nullable: true })
          createdBy: string | null;

          @Column({ name: 'updated_by', nullable: true })
          updatedBy: string | null;

          @DeleteDateColumn({ name: 'deleted_at', nullable: true })
          deletedAt: Date | null;

          @Column({ name: 'deleted_by', nullable: true })
          deletedBy: string | null;
        }
        """;
    }

    /**
     * Generates an entity class for a table.
     *
     * @param table the SQL table
     * @param relationships relationships where this table is the source
     * @param inverseRelationships relationships where this table is the target
     * @return the entity.ts content
     */
    public String generate(
            SqlTable table,
            List<SqlSchema.TableRelationship> relationships,
            List<SqlSchema.TableRelationship> inverseRelationships) {

        StringBuilder sb = new StringBuilder();
        String className = table.getEntityName();
        String tableName = table.getName();

        // Imports
        sb.append("import {\n");
        sb.append("  Entity,\n");
        sb.append("  Column,\n");
        if (!relationships.isEmpty()) {
            sb.append("  ManyToOne,\n");
            sb.append("  JoinColumn,\n");
        }
        if (!inverseRelationships.isEmpty()) {
            sb.append("  OneToMany,\n");
        }
        sb.append("} from 'typeorm';\n");
        sb.append("import { BaseEntity } from './base.entity';\n");

        // Import related entities
        for (SqlSchema.TableRelationship rel : relationships) {
            String targetEntity = rel.getTargetTable().getEntityName();
            String targetKebab = typeMapper.toKebabCase(targetEntity);
            sb.append("import { ")
                    .append(targetEntity)
                    .append(" } from './")
                    .append(targetKebab)
                    .append(".entity';\n");
        }
        for (SqlSchema.TableRelationship rel : inverseRelationships) {
            String sourceEntity = rel.getSourceTable().getEntityName();
            String sourceKebab = typeMapper.toKebabCase(sourceEntity);
            sb.append("import { ")
                    .append(sourceEntity)
                    .append(" } from './")
                    .append(sourceKebab)
                    .append(".entity';\n");
        }

        sb.append("\n");

        // Entity decorator
        sb.append("@Entity('").append(tableName).append("')\n");
        sb.append("export class ").append(className).append(" extends BaseEntity {\n");

        // Entity-specific columns
        for (SqlColumn column : table.getColumns()) {
            if (column.isPrimaryKey() || isAuditField(column.getName())) {
                continue;
            }

            // Skip FK columns - they'll be handled by relationships
            boolean isFkColumn =
                    relationships.stream()
                            .anyMatch(
                                    r ->
                                            r.getForeignKey()
                                                    .getColumnName()
                                                    .equalsIgnoreCase(column.getName()));
            if (isFkColumn) {
                continue;
            }

            String tsType = typeMapper.mapColumnType(column);
            String fieldName = typeMapper.toCamelCase(column.getName());
            String columnName = typeMapper.toSnakeCase(column.getName());

            sb.append("\n");
            sb.append("  @Column({\n");
            sb.append("    name: '").append(columnName).append("',\n");

            // Type for varchar with length
            if ("String".equals(column.getJavaType())) {
                int length = column.getLength() > 0 ? column.getLength() : 255;
                if (length <= 255) {
                    sb.append("    length: ").append(length).append(",\n");
                } else if (length <= 65535) {
                    sb.append("    type: 'text',\n");
                } else {
                    sb.append("    type: 'text',\n");
                }
            }

            // Precision and scale for decimals
            if ("BigDecimal".equals(column.getJavaType())) {
                int precision = column.getPrecision() > 0 ? column.getPrecision() : 19;
                int scale = column.getScale() > 0 ? column.getScale() : 2;
                sb.append("    type: 'decimal',\n");
                sb.append("    precision: ").append(precision).append(",\n");
                sb.append("    scale: ").append(scale).append(",\n");
            }

            if (column.isNullable()) {
                sb.append("    nullable: true,\n");
            }
            if (column.isUnique()) {
                sb.append("    unique: true,\n");
            }
            sb.append("  })\n");

            if (column.isNullable()) {
                sb.append("  ").append(fieldName).append(": ").append(tsType).append(" | null;\n");
            } else {
                sb.append("  ").append(fieldName).append(": ").append(tsType).append(";\n");
            }
        }

        // ManyToOne relationships
        for (SqlSchema.TableRelationship rel : relationships) {
            String targetEntity = rel.getTargetTable().getEntityName();
            String fieldName = typeMapper.toCamelCase(targetEntity);
            String fkColumn = typeMapper.toSnakeCase(rel.getForeignKey().getColumnName());
            String fkFieldName = typeMapper.toCamelCase(rel.getForeignKey().getColumnName());

            sb.append("\n");
            sb.append("  @ManyToOne(() => ").append(targetEntity).append(", { lazy: true })\n");
            sb.append("  @JoinColumn({ name: '").append(fkColumn).append("' })\n");
            sb.append("  ").append(fieldName).append(": ").append(targetEntity).append(";\n");

            // Also add the FK field
            sb.append("\n");
            sb.append("  @Column({ name: '").append(fkColumn).append("', nullable: true })\n");
            sb.append("  ").append(fkFieldName).append(": number | null;\n");
        }

        // OneToMany relationships (inverse)
        for (SqlSchema.TableRelationship rel : inverseRelationships) {
            String sourceEntity = rel.getSourceTable().getEntityName();
            String fieldName = typeMapper.toCamelCase(typeMapper.pluralize(sourceEntity));
            String inverseField = typeMapper.toCamelCase(className);

            sb.append("\n");
            sb.append("  @OneToMany(() => ")
                    .append(sourceEntity)
                    .append(", (")
                    .append(typeMapper.toCamelCase(sourceEntity))
                    .append(") => ")
                    .append(typeMapper.toCamelCase(sourceEntity))
                    .append(".")
                    .append(inverseField)
                    .append(")\n");
            sb.append("  ").append(fieldName).append(": ").append(sourceEntity).append("[];\n");
        }

        sb.append("}\n");

        return sb.toString();
    }

    private boolean isAuditField(String columnName) {
        String lower = columnName.toLowerCase();
        return lower.equals("id")
                || lower.equals("activo")
                || lower.equals("created_at")
                || lower.equals("updated_at")
                || lower.equals("created_by")
                || lower.equals("updated_by")
                || lower.equals("deleted_at")
                || lower.equals("deleted_by");
    }
}
