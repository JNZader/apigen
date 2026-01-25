package com.jnzader.apigen.codegen.generator.gochi.dto;

import com.jnzader.apigen.codegen.generator.gochi.GoChiTypeMapper;
import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Generates DTO structs for Go/Chi with validation tags. */
@SuppressWarnings({
    "java:S1192",
    "java:S2479",
    "java:S3776"
}) // S1192: template strings; S2479: tabs for Go; S3776: complex DTO generation
public class GoChiDtoGenerator {

    private final GoChiTypeMapper typeMapper;

    public GoChiDtoGenerator(GoChiTypeMapper typeMapper) {
        this.typeMapper = typeMapper;
    }

    public String generate(SqlTable table, List<SqlSchema.TableRelationship> relationships) {
        StringBuilder sb = new StringBuilder();
        String entityName = typeMapper.toExportedName(table.getEntityName());

        Set<String> imports = collectImports(table);

        sb.append("package dto\n\n");

        if (!imports.isEmpty()) {
            sb.append("import (\n");
            for (String imp : imports) {
                sb.append("\t\"").append(imp).append("\"\n");
            }
            sb.append(")\n\n");
        }

        // Create Request DTO
        sb.append("// Create")
                .append(entityName)
                .append("Request is the request body for creating a ")
                .append(entityName.toLowerCase(Locale.ROOT))
                .append(".\n");
        sb.append("type Create").append(entityName).append("Request struct {\n");
        for (SqlColumn column : table.getColumns()) {
            if (isBaseField(column.getName()) || column.isPrimaryKey()) {
                continue;
            }
            appendDtoField(sb, column, false);
        }
        // FK fields
        for (SqlSchema.TableRelationship rel : relationships) {
            String fkColumn = rel.getForeignKey().getColumnName();
            String fieldName = typeMapper.toExportedName(fkColumn);
            String jsonName = typeMapper.toSnakeCase(fkColumn);
            sb.append("\t")
                    .append(fieldName)
                    .append(" ".repeat(Math.max(1, 10 - fieldName.length())))
                    .append("*int64")
                    .append(" ".repeat(12))
                    .append("`json:\"")
                    .append(jsonName)
                    .append(",omitempty\"`\n");
        }
        sb.append("}\n\n");

        // Update Request DTO (all fields are pointers for partial update)
        sb.append("// Update")
                .append(entityName)
                .append("Request is the request body for updating a ")
                .append(entityName.toLowerCase(Locale.ROOT))
                .append(".\n");
        sb.append("type Update").append(entityName).append("Request struct {\n");
        for (SqlColumn column : table.getColumns()) {
            if (isBaseField(column.getName()) || column.isPrimaryKey()) {
                continue;
            }
            appendDtoField(sb, column, true);
        }
        // FK fields
        for (SqlSchema.TableRelationship rel : relationships) {
            String fkColumn = rel.getForeignKey().getColumnName();
            String fieldName = typeMapper.toExportedName(fkColumn);
            String jsonName = typeMapper.toSnakeCase(fkColumn);
            sb.append("\t")
                    .append(fieldName)
                    .append(" ".repeat(Math.max(1, 10 - fieldName.length())))
                    .append("*int64")
                    .append(" ".repeat(12))
                    .append("`json:\"")
                    .append(jsonName)
                    .append(",omitempty\"`\n");
        }
        sb.append("}\n\n");

        // Response DTO
        sb.append("// ")
                .append(entityName)
                .append("Response is the response body for a ")
                .append(entityName.toLowerCase(Locale.ROOT))
                .append(".\n");
        sb.append("type ").append(entityName).append("Response struct {\n");
        sb.append("\tID        int64     `json:\"id\"`\n");
        for (SqlColumn column : table.getColumns()) {
            if (isBaseField(column.getName()) || column.isPrimaryKey()) {
                continue;
            }
            appendResponseField(sb, column);
        }
        // FK fields
        for (SqlSchema.TableRelationship rel : relationships) {
            String fkColumn = rel.getForeignKey().getColumnName();
            String fieldName = typeMapper.toExportedName(fkColumn);
            String jsonName = typeMapper.toSnakeCase(fkColumn);
            sb.append("\t")
                    .append(fieldName)
                    .append(" ".repeat(Math.max(1, 10 - fieldName.length())))
                    .append("*int64")
                    .append(" ".repeat(12))
                    .append("`json:\"")
                    .append(jsonName)
                    .append(",omitempty\"`\n");
        }
        sb.append("\tCreatedAt time.Time  `json:\"created_at\"`\n");
        sb.append("\tUpdatedAt time.Time  `json:\"updated_at\"`\n");
        sb.append("}\n");

        return sb.toString();
    }

    private void appendDtoField(StringBuilder sb, SqlColumn column, boolean pointer) {
        String fieldName = typeMapper.toExportedName(column.getName());
        String baseType = typeMapper.mapJavaTypeToGo(column.getJavaType());
        String fieldType = pointer || column.isNullable() ? "*" + baseType : baseType;
        String jsonName = typeMapper.toSnakeCase(column.getName());

        sb.append("\t").append(fieldName);
        sb.append(" ".repeat(Math.max(1, 10 - fieldName.length())));
        sb.append(fieldType);
        sb.append(" ".repeat(Math.max(1, 18 - fieldType.length())));
        sb.append("`json:\"").append(jsonName);
        if (pointer || column.isNullable()) {
            sb.append(",omitempty");
        }
        sb.append("\"");

        // Validation tags
        if (!pointer && !column.isNullable()) {
            sb.append(" validate:\"required");
            if ("String".equals(column.getJavaType()) && column.getLength() != null) {
                sb.append(",max=").append(column.getLength());
            }
            sb.append("\"");
        } else if ("String".equals(column.getJavaType()) && column.getLength() != null) {
            sb.append(" validate:\"omitempty,max=").append(column.getLength()).append("\"");
        }

        sb.append("`\n");
    }

    private void appendResponseField(StringBuilder sb, SqlColumn column) {
        String fieldName = typeMapper.toExportedName(column.getName());
        // Use pointer types for nullable fields in DTOs (cleaner for JSON serialization)
        String baseType = typeMapper.mapJavaTypeToGo(column.getJavaType());
        String fieldType = column.isNullable() ? "*" + baseType : baseType;
        String jsonName = typeMapper.toSnakeCase(column.getName());

        sb.append("\t").append(fieldName);
        sb.append(" ".repeat(Math.max(1, 10 - fieldName.length())));
        sb.append(fieldType);
        sb.append(" ".repeat(Math.max(1, 18 - fieldType.length())));
        sb.append("`json:\"").append(jsonName);
        if (column.isNullable()) {
            sb.append(",omitempty");
        }
        sb.append("\"`\n");
    }

    private Set<String> collectImports(SqlTable table) {
        Set<String> imports = new HashSet<>();
        imports.add("time"); // For audit fields

        for (SqlColumn column : table.getColumns()) {
            // Use non-nullable types for imports since DTOs use pointer types for nullable
            String imp = typeMapper.getImportForType(column.getJavaType(), false);
            if (imp != null) {
                imports.add(imp);
            }
        }

        return imports;
    }

    private boolean isBaseField(String columnName) {
        String lower = columnName.toLowerCase(Locale.ROOT);
        return lower.equals("id")
                || lower.equals("created_at")
                || lower.equals("updated_at")
                || lower.equals("deleted_at");
    }

    public String generatePagination() {
        return """
        package dto

        // PaginatedResponse is a generic paginated response.
        type PaginatedResponse[T any] struct {
        \tItems      []T   `json:"items"`
        \tTotal      int64 `json:"total"`
        \tPage       int   `json:"page"`
        \tPageSize   int   `json:"page_size"`
        \tTotalPages int   `json:"total_pages"`
        }

        // NewPaginatedResponse creates a new paginated response.
        func NewPaginatedResponse[T any](items []T, page, pageSize int, total int64) *PaginatedResponse[T] {
        \ttotalPages := int(total) / pageSize
        \tif int(total)%pageSize > 0 {
        \t\ttotalPages++
        \t}
        \treturn &PaginatedResponse[T]{
        \t\tItems:      items,
        \t\tTotal:      total,
        \t\tPage:       page,
        \t\tPageSize:   pageSize,
        \t\tTotalPages: totalPages,
        \t}
        }

        // PaginationParams holds pagination parameters.
        type PaginationParams struct {
        \tPage     int `json:"page"`
        \tPageSize int `json:"page_size"`
        }

        // NewPaginationParams creates pagination params with defaults.
        func NewPaginationParams(page, pageSize int) PaginationParams {
        \tif page < 0 {
        \t\tpage = 0
        \t}
        \tif pageSize <= 0 {
        \t\tpageSize = 20
        \t}
        \tif pageSize > 100 {
        \t\tpageSize = 100
        \t}
        \treturn PaginationParams{Page: page, PageSize: pageSize}
        }

        // Offset returns the SQL offset.
        func (p PaginationParams) Offset() int {
        \treturn p.Page * p.PageSize
        }
        """;
    }

    public String generateResponse() {
        return """
        package dto

        // ErrorResponse is the standard error response.
        type ErrorResponse struct {
        \tStatus  int    `json:"status"`
        \tMessage string `json:"message"`
        \tDetails any    `json:"details,omitempty"`
        }

        // NewErrorResponse creates a new error response.
        func NewErrorResponse(status int, message string) *ErrorResponse {
        \treturn &ErrorResponse{Status: status, Message: message}
        }

        // WithDetails adds details to the error response.
        func (e *ErrorResponse) WithDetails(details any) *ErrorResponse {
        \te.Details = details
        \treturn e
        }

        // SuccessResponse is a generic success response.
        type SuccessResponse struct {
        \tMessage string `json:"message"`
        \tData    any    `json:"data,omitempty"`
        }

        // NewSuccessResponse creates a new success response.
        func NewSuccessResponse(message string, data any) *SuccessResponse {
        \treturn &SuccessResponse{Message: message, Data: data}
        }
        """;
    }
}
