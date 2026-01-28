package com.jnzader.apigen.codegen.generator.go.dto;

import com.jnzader.apigen.codegen.generator.go.GoTypeMapper;
import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Generates DTO structs (request/response) from SQL table definitions for Go/Gin. */
@SuppressWarnings({
    "java:S2479",
    "java:S1192"
}) // Literal tabs intentional for Go code; duplicate strings for templates
public class GoDtoGenerator {

    private final GoTypeMapper typeMapper;

    public GoDtoGenerator(GoTypeMapper typeMapper) {
        this.typeMapper = typeMapper;
    }

    /**
     * Generates the Create request DTO.
     *
     * @param table the SQL table
     * @param relationships relationships for this table
     * @return the generated Go code
     */
    public String generateCreateRequest(
            SqlTable table, List<SqlSchema.TableRelationship> relationships) {
        StringBuilder sb = new StringBuilder();
        String structName = "Create" + typeMapper.toExportedName(table.getEntityName()) + "Request";

        // Collect imports
        Set<String> imports = collectImports(table);

        // Package declaration
        sb.append("package dto\n\n");

        // Imports
        appendImports(sb, imports);

        // Struct definition
        sb.append("// ")
                .append(structName)
                .append(" represents the request body for creating a ")
                .append(table.getEntityName())
                .append(".\n");
        sb.append("type ").append(structName).append(" struct {\n");

        // Fields
        for (SqlColumn column : table.getColumns()) {
            if (isBaseField(column.getName()) || column.isPrimaryKey()) {
                continue;
            }

            appendDtoField(sb, column, false);
        }

        // Foreign key IDs for relationships
        for (SqlSchema.TableRelationship rel : relationships) {
            String fkField = typeMapper.toExportedName(rel.getForeignKey().getColumnName());
            String jsonName = typeMapper.toSnakeCase(rel.getForeignKey().getColumnName());

            sb.append("\t").append(fkField);
            sb.append(" ".repeat(Math.max(1, 12 - fkField.length())));
            sb.append("*int64");
            sb.append(" ".repeat(10));
            sb.append("`json:\"").append(jsonName).append(",omitempty\" ");
            sb.append("binding:\"omitempty\"`\n");
        }

        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Generates the Update request DTO.
     *
     * @param table the SQL table
     * @param relationships relationships for this table
     * @return the generated Go code
     */
    public String generateUpdateRequest(
            SqlTable table, List<SqlSchema.TableRelationship> relationships) {
        StringBuilder sb = new StringBuilder();
        String structName = "Update" + typeMapper.toExportedName(table.getEntityName()) + "Request";

        // Collect imports
        Set<String> imports = collectImports(table);

        // Package declaration
        sb.append("package dto\n\n");

        // Imports
        appendImports(sb, imports);

        // Struct definition
        sb.append("// ")
                .append(structName)
                .append(" represents the request body for updating a ")
                .append(table.getEntityName())
                .append(".\n");
        sb.append("type ").append(structName).append(" struct {\n");

        // All fields are optional (pointers) for updates
        for (SqlColumn column : table.getColumns()) {
            if (isBaseField(column.getName()) || column.isPrimaryKey()) {
                continue;
            }

            appendDtoField(sb, column, true);
        }

        // Foreign key IDs for relationships
        for (SqlSchema.TableRelationship rel : relationships) {
            String fkField = typeMapper.toExportedName(rel.getForeignKey().getColumnName());
            String jsonName = typeMapper.toSnakeCase(rel.getForeignKey().getColumnName());

            sb.append("\t").append(fkField);
            sb.append(" ".repeat(Math.max(1, 12 - fkField.length())));
            sb.append("*int64");
            sb.append(" ".repeat(10));
            sb.append("`json:\"").append(jsonName).append(",omitempty\" ");
            sb.append("binding:\"omitempty\"`\n");
        }

        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Generates the Response DTO.
     *
     * @param table the SQL table
     * @param relationships relationships for this table
     * @return the generated Go code
     */
    public String generateResponse(
            SqlTable table, List<SqlSchema.TableRelationship> relationships) {
        StringBuilder sb = new StringBuilder();
        String entityName = typeMapper.toExportedName(table.getEntityName());
        String structName = entityName + "Response";

        // Collect imports
        Set<String> imports = collectImports(table);
        imports.add("time"); // For timestamps

        // Package declaration
        sb.append("package dto\n\n");

        // Imports
        appendImports(sb, imports);

        // Struct definition
        sb.append("// ")
                .append(structName)
                .append(" represents the response for a ")
                .append(table.getEntityName())
                .append(".\n");
        sb.append("type ").append(structName).append(" struct {\n");

        // ID field
        sb.append("\tID        int64     `json:\"id\"`\n");

        // Regular fields
        for (SqlColumn column : table.getColumns()) {
            if (isBaseField(column.getName()) || column.isPrimaryKey()) {
                continue;
            }

            String fieldName = typeMapper.toExportedName(column.getName());
            String fieldType =
                    typeMapper.mapJavaTypeToGo(column.getJavaType(), column.isNullable());
            String jsonName = typeMapper.toSnakeCase(column.getName());

            sb.append("\t").append(fieldName);
            sb.append(" ".repeat(Math.max(1, 12 - fieldName.length())));
            sb.append(fieldType);
            sb.append(" ".repeat(Math.max(1, 14 - fieldType.length())));
            sb.append("`json:\"").append(jsonName);
            if (column.isNullable()) {
                sb.append(",omitempty");
            }
            sb.append("\"`\n");
        }

        // Foreign key IDs
        for (SqlSchema.TableRelationship rel : relationships) {
            String fkField = typeMapper.toExportedName(rel.getForeignKey().getColumnName());
            String jsonName = typeMapper.toSnakeCase(rel.getForeignKey().getColumnName());

            sb.append("\t").append(fkField);
            sb.append(" ".repeat(Math.max(1, 12 - fkField.length())));
            sb.append("*int64");
            sb.append(" ".repeat(8));
            sb.append("`json:\"").append(jsonName).append(",omitempty\"`\n");
        }

        // Audit fields
        sb.append("\tCreatedAt time.Time `json:\"created_at\"`\n");
        sb.append("\tUpdatedAt time.Time `json:\"updated_at\"`\n");

        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Generates the paginated response DTO.
     *
     * @return the generated Go code
     */
    public String generatePaginatedResponse() {
        return """
        package dto

        // PaginatedResponse represents a paginated list response.
        type PaginatedResponse[T any] struct {
        \tContent       []T   `json:"content"`
        \tPage          int   `json:"page"`
        \tSize          int   `json:"size"`
        \tTotalElements int64 `json:"total_elements"`
        \tTotalPages    int   `json:"total_pages"`
        }

        // NewPaginatedResponse creates a new paginated response.
        func NewPaginatedResponse[T any](content []T, page, size int, total int64) *PaginatedResponse[T] {
        \ttotalPages := int(total) / size
        \tif int(total)%size != 0 {
        \t\ttotalPages++
        \t}

        \treturn &PaginatedResponse[T]{
        \t\tContent:       content,
        \t\tPage:          page,
        \t\tSize:          size,
        \t\tTotalElements: total,
        \t\tTotalPages:    totalPages,
        \t}
        }
        """;
    }

    /**
     * Generates the error response DTO.
     *
     * @return the generated Go code
     */
    public String generateErrorResponse() {
        return """
        package dto

        import "time"

        // ErrorResponse represents an error response.
        type ErrorResponse struct {
        \tStatus    int       `json:"status"`
        \tMessage   string    `json:"message"`
        \tPath      string    `json:"path,omitempty"`
        \tTimestamp time.Time `json:"timestamp"`
        }

        // NewErrorResponse creates a new error response.
        func NewErrorResponse(status int, message, path string) *ErrorResponse {
        \treturn &ErrorResponse{
        \t\tStatus:    status,
        \t\tMessage:   message,
        \t\tPath:      path,
        \t\tTimestamp: time.Now(),
        \t}
        }

        // ValidationError represents a validation error for a specific field.
        type ValidationError struct {
        \tField   string `json:"field"`
        \tMessage string `json:"message"`
        }

        // ValidationErrorResponse represents a validation error response.
        type ValidationErrorResponse struct {
        \tStatus    int               `json:"status"`
        \tMessage   string            `json:"message"`
        \tErrors    []ValidationError `json:"errors"`
        \tTimestamp time.Time         `json:"timestamp"`
        }

        // NewValidationErrorResponse creates a new validation error response.
        func NewValidationErrorResponse(errors []ValidationError) *ValidationErrorResponse {
        \treturn &ValidationErrorResponse{
        \t\tStatus:    400,
        \t\tMessage:   "Validation failed",
        \t\tErrors:    errors,
        \t\tTimestamp: time.Now(),
        \t}
        }
        """;
    }

    private void appendDtoField(StringBuilder sb, SqlColumn column, boolean isUpdate) {
        String fieldName = typeMapper.toExportedName(column.getName());
        String fieldType =
                typeMapper.mapJavaTypeToGo(column.getJavaType(), isUpdate || column.isNullable());
        String jsonName = typeMapper.toSnakeCase(column.getName());
        String validatorTag = typeMapper.getValidatorTag(column, isUpdate);

        sb.append("\t").append(fieldName);
        sb.append(" ".repeat(Math.max(1, 12 - fieldName.length())));
        sb.append(fieldType);
        sb.append(" ".repeat(Math.max(1, 14 - fieldType.length())));
        sb.append("`json:\"").append(jsonName);
        if (isUpdate || column.isNullable()) {
            sb.append(",omitempty");
        }
        sb.append("\"");
        if (!validatorTag.isEmpty()) {
            sb.append(" binding:\"").append(validatorTag).append("\"");
        }
        sb.append("`\n");
    }

    private void appendImports(StringBuilder sb, Set<String> imports) {
        if (imports.isEmpty()) {
            return;
        }

        sb.append("import (\n");
        // Standard library
        if (imports.contains("time")) {
            sb.append("\t\"time\"\n");
        }
        // External packages
        boolean hasExternal = imports.contains("uuid") || imports.contains("decimal");
        if (hasExternal) {
            sb.append("\n");
            if (imports.contains("uuid")) {
                sb.append("\t\"github.com/google/uuid\"\n");
            }
            if (imports.contains("decimal")) {
                sb.append("\t\"github.com/shopspring/decimal\"\n");
            }
        }
        sb.append(")\n\n");
    }

    private Set<String> collectImports(SqlTable table) {
        Set<String> imports = new HashSet<>();

        for (SqlColumn column : table.getColumns()) {
            if (isBaseField(column.getName()) || column.isPrimaryKey()) {
                continue;
            }
            switch (column.getJavaType()) {
                case "LocalDate", "LocalDateTime", "Instant", "ZonedDateTime", "LocalTime" ->
                        imports.add("time");
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
        String lower = columnName.toLowerCase(Locale.ROOT);
        return lower.equals("id")
                || lower.equals("created_at")
                || lower.equals("updated_at")
                || lower.equals("deleted_at")
                || lower.equals("createdat")
                || lower.equals("updatedat")
                || lower.equals("deletedat");
    }
}
