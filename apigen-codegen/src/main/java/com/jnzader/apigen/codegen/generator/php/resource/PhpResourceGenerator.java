package com.jnzader.apigen.codegen.generator.php.resource;

import static com.jnzader.apigen.codegen.generator.util.NamingUtils.*;

import com.jnzader.apigen.codegen.generator.php.PhpTypeMapper;
import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.List;

/**
 * Generates Laravel API Resource classes (DTOs) for Laravel projects.
 *
 * <p>Generates:
 *
 * <ul>
 *   <li>JsonResource classes for single entities
 *   <li>ResourceCollection classes for paginated results
 * </ul>
 */
@SuppressWarnings("java:S1192") // Duplicate strings intentional for code generation templates
public class PhpResourceGenerator {

    private final PhpTypeMapper typeMapper;

    public PhpResourceGenerator() {
        this.typeMapper = new PhpTypeMapper();
    }

    /**
     * Generates the base paginated response resource.
     *
     * @return the PaginatedResponse.php content
     */
    public String generatePaginatedResponse() {
        return """
        <?php

        namespace App\\Http\\Resources;

        use Illuminate\\Http\\Request;
        use Illuminate\\Http\\Resources\\Json\\ResourceCollection;

        class PaginatedResponse extends ResourceCollection
        {
            /**
             * The resource class to use for collection items.
             */
            public string $collects;

            /**
             * Transform the resource collection into an array.
             */
            public function toArray(Request $request): array
            {
                return [
                    'items' => $this->collection,
                    'total' => $this->resource->total(),
                    'page' => $this->resource->currentPage() - 1,
                    'size' => $this->resource->perPage(),
                    'pages' => $this->resource->lastPage(),
                ];
            }
        }
        """;
    }

    /**
     * Generates a JSON Resource for a table.
     *
     * @param table the SQL table
     * @param relationships relationships involving this table
     * @return the Resource.php content
     */
    public String generate(SqlTable table, List<SqlSchema.TableRelationship> relationships) {
        StringBuilder sb = new StringBuilder();
        String className = table.getEntityName();

        sb.append("<?php\n\n");
        sb.append("namespace App\\Http\\Resources;\n\n");
        sb.append("use Illuminate\\Http\\Request;\n");
        sb.append("use Illuminate\\Http\\Resources\\Json\\JsonResource;\n\n");

        sb.append("class ").append(className).append("Resource extends JsonResource\n");
        sb.append("{\n");

        sb.append("    /**\n");
        sb.append("     * Transform the resource into an array.\n");
        sb.append("     */\n");
        sb.append("    public function toArray(Request $request): array\n");
        sb.append("    {\n");
        sb.append("        return [\n");

        // Primary key
        sb.append("            'id' => $this->id,\n");

        // Entity-specific fields
        for (SqlColumn column : table.getColumns()) {
            if (column.isPrimaryKey() || isAuditField(column.getName())) {
                continue;
            }

            String snakeName = typeMapper.toSnakeCase(column.getName());
            String camelName = typeMapper.toCamelCase(column.getName());
            String javaType = column.getJavaType();

            // Format dates
            if (isDateType(javaType)) {
                sb.append("            '")
                        .append(camelName)
                        .append("' => $this->")
                        .append(snakeName)
                        .append("?->toIso8601String(),\n");
            } else {
                sb.append("            '")
                        .append(camelName)
                        .append("' => $this->")
                        .append(snakeName)
                        .append(",\n");
            }
        }

        // Relationships
        for (SqlSchema.TableRelationship rel : relationships) {
            String targetEntity = rel.getTargetTable().getEntityName();
            String methodName = typeMapper.toCamelCase(targetEntity);
            String fkColumn = typeMapper.toSnakeCase(rel.getForeignKey().getColumnName());

            sb.append("            '")
                    .append(fkColumn)
                    .append("' => $this->")
                    .append(fkColumn)
                    .append(",\n");
            sb.append("            '")
                    .append(methodName)
                    .append("' => new ")
                    .append(targetEntity)
                    .append("Resource($this->whenLoaded('")
                    .append(methodName)
                    .append("')),\n");
        }

        // Base audit fields
        sb.append("            'activo' => $this->activo,\n");
        sb.append("            'createdAt' => $this->created_at?->toIso8601String(),\n");
        sb.append("            'updatedAt' => $this->updated_at?->toIso8601String(),\n");
        sb.append("            'createdBy' => $this->created_by,\n");
        sb.append("            'updatedBy' => $this->updated_by,\n");

        sb.append("        ];\n");
        sb.append("    }\n");
        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Generates a Resource Collection for paginated responses.
     *
     * @param table the SQL table
     * @return the ResourceCollection.php content
     */
    public String generateCollection(SqlTable table) {
        String className = table.getEntityName();

        return """
        <?php

        namespace App\\Http\\Resources;

        class %sCollection extends PaginatedResponse
        {
            /**
             * The resource that this resource collects.
             */
            public string $collects = %sResource::class;
        }
        """
                .formatted(className, className);
    }

    private boolean isDateType(String javaType) {
        return "LocalDate".equals(javaType)
                || "LocalDateTime".equals(javaType)
                || "Instant".equals(javaType)
                || "ZonedDateTime".equals(javaType)
                || "LocalTime".equals(javaType);
    }
}
