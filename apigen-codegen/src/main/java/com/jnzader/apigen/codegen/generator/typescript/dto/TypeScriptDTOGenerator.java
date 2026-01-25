package com.jnzader.apigen.codegen.generator.typescript.dto;

import static com.jnzader.apigen.codegen.generator.util.NamingUtils.*;

import com.jnzader.apigen.codegen.generator.typescript.TypeScriptTypeMapper;
import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates DTO classes with class-validator decorators for NestJS projects.
 *
 * <p>Generates:
 *
 * <ul>
 *   <li>CreateDto for POST operations
 *   <li>UpdateDto for PUT/PATCH operations
 *   <li>ResponseDto for API responses
 *   <li>PaginatedResponseDto for paginated results
 * </ul>
 */
@SuppressWarnings({
    "java:S1192",
    "java:S3776",
    "java:S6541"
}) // S1192: Template strings; S3776/S6541: complex DTO generation logic
public class TypeScriptDTOGenerator {

    private final TypeScriptTypeMapper typeMapper;

    public TypeScriptDTOGenerator() {
        this.typeMapper = new TypeScriptTypeMapper();
    }

    /**
     * Generates the base paginated response DTO.
     *
     * @return the paginated-response.dto.ts content
     */
    public String generatePaginatedResponseDto() {
        return """
        import { ApiProperty } from '@nestjs/swagger';

        export class PaginatedResponseDto<T> {
          @ApiProperty({ description: 'Array of items' })
          items: T[];

          @ApiProperty({ description: 'Total number of items' })
          total: number;

          @ApiProperty({ description: 'Current page (0-indexed)' })
          page: number;

          @ApiProperty({ description: 'Items per page' })
          size: number;

          @ApiProperty({ description: 'Total number of pages' })
          pages: number;

          constructor(items: T[], total: number, page: number, size: number) {
            this.items = items;
            this.total = total;
            this.page = page;
            this.size = size;
            this.pages = Math.ceil(total / size);
          }
        }
        """;
    }

    /**
     * Generates the base response DTO with audit fields.
     *
     * @return the base-response.dto.ts content
     */
    public String generateBaseResponseDto() {
        return """
        import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

        export class BaseResponseDto {
          @ApiProperty({ description: 'Unique identifier' })
          id: number;

          @ApiProperty({ description: 'Active status' })
          activo: boolean;

          @ApiProperty({ description: 'Creation timestamp' })
          createdAt: Date;

          @ApiProperty({ description: 'Last update timestamp' })
          updatedAt: Date;

          @ApiPropertyOptional({ description: 'Created by user' })
          createdBy: string | null;

          @ApiPropertyOptional({ description: 'Updated by user' })
          updatedBy: string | null;
        }
        """;
    }

    /**
     * Generates the Create DTO for an entity.
     *
     * @param table the SQL table
     * @param relationships relationships involving this table
     * @return the create.dto.ts content
     */
    public String generateCreateDto(
            SqlTable table, List<SqlSchema.TableRelationship> relationships) {
        return generateDto(table, relationships, "Create", false);
    }

    /**
     * Generates the Update DTO for an entity.
     *
     * @param table the SQL table
     * @param relationships relationships involving this table
     * @return the update.dto.ts content
     */
    public String generateUpdateDto(
            SqlTable table, List<SqlSchema.TableRelationship> relationships) {
        return generateDto(table, relationships, "Update", true);
    }

    /**
     * Generates the Response DTO for an entity.
     *
     * @param table the SQL table
     * @param relationships relationships involving this table
     * @return the response.dto.ts content
     */
    public String generateResponseDto(
            SqlTable table, List<SqlSchema.TableRelationship> relationships) {

        StringBuilder sb = new StringBuilder();
        String className = table.getEntityName();

        // Collect validators needed
        Set<String> apiDecorators = new HashSet<>();
        apiDecorators.add("ApiProperty");

        boolean hasOptionalFields = false;
        for (SqlColumn column : table.getColumns()) {
            if (column.isPrimaryKey() || isAuditField(column.getName())) {
                continue;
            }
            if (column.isNullable()) {
                hasOptionalFields = true;
            }
        }

        if (hasOptionalFields || !relationships.isEmpty()) {
            apiDecorators.add("ApiPropertyOptional");
        }

        // Imports
        sb.append("import { ");
        sb.append(String.join(", ", apiDecorators));
        sb.append(" } from '@nestjs/swagger';\n");
        sb.append("import { BaseResponseDto } from '../../../dto/base-response.dto';\n");
        sb.append("\n");

        // Class definition
        sb.append("export class ")
                .append(className)
                .append("ResponseDto extends BaseResponseDto {\n");

        // Entity-specific fields
        for (SqlColumn column : table.getColumns()) {
            if (column.isPrimaryKey() || isAuditField(column.getName())) {
                continue;
            }

            String tsType = typeMapper.mapColumnType(column);
            String fieldName = typeMapper.toCamelCase(column.getName());

            sb.append("\n");
            if (column.isNullable()) {
                sb.append("  @ApiPropertyOptional({ description: '")
                        .append(column.getName())
                        .append("' })\n");
                sb.append("  ").append(fieldName).append(": ").append(tsType).append(" | null;\n");
            } else {
                sb.append("  @ApiProperty({ description: '")
                        .append(column.getName())
                        .append("' })\n");
                sb.append("  ").append(fieldName).append(": ").append(tsType).append(";\n");
            }
        }

        // Relationship fields (FK IDs)
        for (SqlSchema.TableRelationship rel : relationships) {
            String fkFieldName = typeMapper.toCamelCase(rel.getForeignKey().getColumnName());

            sb.append("\n");
            sb.append("  @ApiPropertyOptional({ description: 'Related ")
                    .append(rel.getTargetTable().getEntityName())
                    .append(" ID' })\n");
            sb.append("  ").append(fkFieldName).append(": number | null;\n");
        }

        sb.append("}\n");

        return sb.toString();
    }

    private String generateDto(
            SqlTable table,
            List<SqlSchema.TableRelationship> relationships,
            String prefix,
            boolean isUpdate) {

        StringBuilder sb = new StringBuilder();
        String className = table.getEntityName();

        // Collect validators needed
        Set<String> validators = new HashSet<>();
        Set<String> apiDecorators = new HashSet<>();

        if (isUpdate) {
            validators.add("IsOptional");
        }

        for (SqlColumn column : table.getColumns()) {
            if (column.isPrimaryKey() || isAuditField(column.getName())) {
                continue;
            }

            String javaType = column.getJavaType();
            switch (javaType) {
                case "String" -> {
                    validators.add("IsString");
                    validators.add("MaxLength");
                    if (!isUpdate && !column.isNullable()) {
                        validators.add("IsNotEmpty");
                    }
                    if (column.getName().toLowerCase().contains("email")) {
                        validators.add("IsEmail");
                    }
                    if (column.getName().toLowerCase().contains("url")) {
                        validators.add("IsUrl");
                    }
                }
                case "Integer", "int", "Long", "long" -> validators.add("IsInt");
                case "Double", "double", "Float", "float", "BigDecimal" ->
                        validators.add("IsNumber");
                case "Boolean", "boolean" -> validators.add("IsBoolean");
                case "LocalDate", "LocalDateTime", "Instant", "ZonedDateTime" ->
                        validators.add("IsDate");
                case "UUID" -> validators.add("IsUUID");
                default -> {
                    // Other types use default validation
                }
            }
        }

        // Add validators for FK fields
        if (!relationships.isEmpty()) {
            validators.add("IsInt");
            validators.add("IsOptional");
        }

        apiDecorators.add("ApiProperty");
        apiDecorators.add("ApiPropertyOptional");

        // Imports
        sb.append("import {\n");
        for (String validator : validators.stream().sorted().toList()) {
            sb.append("  ").append(validator).append(",\n");
        }
        sb.append("} from 'class-validator';\n");

        sb.append("import { ");
        sb.append(String.join(", ", apiDecorators));
        sb.append(" } from '@nestjs/swagger';\n");
        sb.append("\n");

        // Class definition
        sb.append("export class ").append(prefix).append(className).append("Dto {\n");

        // Entity-specific fields
        for (SqlColumn column : table.getColumns()) {
            if (column.isPrimaryKey() || isAuditField(column.getName())) {
                continue;
            }

            String tsType = typeMapper.mapColumnType(column);
            String fieldName = typeMapper.toCamelCase(column.getName());
            String javaType = column.getJavaType();

            sb.append("\n");

            // Swagger decorator
            if (isUpdate || column.isNullable()) {
                sb.append("  @ApiPropertyOptional({ description: '")
                        .append(column.getName())
                        .append("' })\n");
            } else {
                sb.append("  @ApiProperty({ description: '")
                        .append(column.getName())
                        .append("' })\n");
            }

            // Validation decorators
            if (isUpdate) {
                sb.append("  @IsOptional()\n");
            }

            switch (javaType) {
                case "String" -> {
                    sb.append("  @IsString()\n");
                    int maxLength =
                            column.getLength() != null && column.getLength() > 0
                                    ? column.getLength()
                                    : 255;
                    sb.append("  @MaxLength(").append(maxLength).append(")\n");

                    if (column.getName().toLowerCase().contains("email")) {
                        sb.append("  @IsEmail()\n");
                    }
                    if (column.getName().toLowerCase().contains("url")) {
                        sb.append("  @IsUrl()\n");
                    }
                    if (!isUpdate && !column.isNullable()) {
                        sb.append("  @IsNotEmpty()\n");
                    }
                }
                case "Integer", "int", "Long", "long" -> sb.append("  @IsInt()\n");
                case "Double", "double", "Float", "float", "BigDecimal" ->
                        sb.append("  @IsNumber()\n");
                case "Boolean", "boolean" -> sb.append("  @IsBoolean()\n");
                case "LocalDate", "LocalDateTime", "Instant", "ZonedDateTime" ->
                        sb.append("  @IsDate()\n");
                case "UUID" -> sb.append("  @IsUUID()\n");
                default -> {
                    // Other types use default validation
                }
            }

            if (isUpdate || column.isNullable()) {
                sb.append("  ").append(fieldName).append("?: ").append(tsType).append(" | null;\n");
            } else {
                sb.append("  ").append(fieldName).append(": ").append(tsType).append(";\n");
            }
        }

        // FK fields from relationships
        for (SqlSchema.TableRelationship rel : relationships) {
            String fkFieldName = typeMapper.toCamelCase(rel.getForeignKey().getColumnName());

            sb.append("\n");
            sb.append("  @ApiPropertyOptional({ description: 'Related ")
                    .append(rel.getTargetTable().getEntityName())
                    .append(" ID' })\n");
            sb.append("  @IsOptional()\n");
            sb.append("  @IsInt()\n");
            sb.append("  ").append(fkFieldName).append("?: number | null;\n");
        }

        sb.append("}\n");

        return sb.toString();
    }
}
