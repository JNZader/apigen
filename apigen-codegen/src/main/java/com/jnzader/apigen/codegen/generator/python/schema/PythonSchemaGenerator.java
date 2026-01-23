package com.jnzader.apigen.codegen.generator.python.schema;

import com.jnzader.apigen.codegen.generator.python.PythonTypeMapper;
import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates Pydantic schema classes (DTOs) for Python/FastAPI projects.
 *
 * <p>Generates:
 *
 * <ul>
 *   <li>Base schema with common fields
 *   <li>Create schema for POST requests
 *   <li>Update schema for PUT/PATCH requests
 *   <li>Response schema for API responses
 * </ul>
 */
public class PythonSchemaGenerator {

    private final PythonTypeMapper typeMapper;

    public PythonSchemaGenerator() {
        this.typeMapper = new PythonTypeMapper();
    }

    /**
     * Generates Pydantic schemas for a table.
     *
     * @param table the SQL table
     * @param relationships relationships involving this table
     * @return the schemas.py content
     */
    public String generate(SqlTable table, List<SqlSchema.TableRelationship> relationships) {
        StringBuilder sb = new StringBuilder();
        String className = table.getEntityName();

        // Collect imports
        Set<String> imports = new HashSet<>();
        imports.add("from pydantic import BaseModel, ConfigDict, Field");

        boolean hasEmail = false;
        for (SqlColumn column : table.getColumns()) {
            String typeImport = typeMapper.getTypeImport(column.getJavaType());
            if (typeImport != null) {
                imports.add(typeImport);
            }
            if (column.getName().toLowerCase().contains("email")) {
                hasEmail = true;
            }
        }

        if (hasEmail) {
            imports.add("from pydantic import EmailStr");
        }

        // Write imports
        for (String imp : imports.stream().sorted().toList()) {
            sb.append(imp).append("\n");
        }
        sb.append("\n\n");

        // Base schema (common fields)
        sb.append("class ").append(className).append("Base(BaseModel):\n");
        sb.append("    \"\"\"Base schema with common fields for ")
                .append(className)
                .append(".\"\"\"\n\n");

        for (SqlColumn column : table.getColumns()) {
            if (isBaseField(column.getName()) || column.isPrimaryKey()) {
                continue;
            }
            generateSchemaField(sb, column, false);
        }

        // Add FK IDs for relationships
        for (SqlSchema.TableRelationship rel : relationships) {
            String fkFieldName = typeMapper.toSnakeCase(rel.getForeignKey().getColumnName());
            sb.append("    ").append(fkFieldName).append(": int | None = None\n");
        }

        sb.append("\n\n");

        // Create schema
        sb.append("class ")
                .append(className)
                .append("Create(")
                .append(className)
                .append("Base):\n");
        sb.append("    \"\"\"Schema for creating a new ").append(className).append(".\"\"\"\n\n");
        sb.append("    pass\n");
        sb.append("\n\n");

        // Update schema (all fields optional)
        sb.append("class ").append(className).append("Update(BaseModel):\n");
        sb.append("    \"\"\"Schema for updating a ").append(className).append(".\"\"\"\n\n");

        for (SqlColumn column : table.getColumns()) {
            if (isBaseField(column.getName()) || column.isPrimaryKey()) {
                continue;
            }
            generateSchemaField(sb, column, true);
        }

        // Add FK IDs for relationships (optional for update)
        for (SqlSchema.TableRelationship rel : relationships) {
            String fkFieldName = typeMapper.toSnakeCase(rel.getForeignKey().getColumnName());
            sb.append("    ").append(fkFieldName).append(": int | None = None\n");
        }

        sb.append("\n\n");

        // Response schema
        sb.append("class ")
                .append(className)
                .append("Response(")
                .append(className)
                .append("Base):\n");
        sb.append("    \"\"\"Schema for ").append(className).append(" API responses.\"\"\"\n\n");
        sb.append("    id: int\n");
        sb.append("    activo: bool = True\n");
        sb.append("    created_at: datetime | None = None\n");
        sb.append("    updated_at: datetime | None = None\n");
        sb.append("\n");
        sb.append("    model_config = ConfigDict(from_attributes=True)\n");
        sb.append("\n\n");

        // List response schema with pagination
        sb.append("class ").append(className).append("ListResponse(BaseModel):\n");
        sb.append("    \"\"\"Paginated list response for ").append(className).append(".\"\"\"\n\n");
        sb.append("    items: list[").append(className).append("Response]\n");
        sb.append("    total: int\n");
        sb.append("    page: int\n");
        sb.append("    size: int\n");
        sb.append("    pages: int\n");

        return sb.toString();
    }

    private void generateSchemaField(StringBuilder sb, SqlColumn column, boolean allOptional) {
        String fieldName = typeMapper.toSnakeCase(column.getName());
        fieldName = typeMapper.safePythonFieldName(fieldName);

        String pythonType = typeMapper.getPydanticType(column);

        // For update schema, all fields are optional
        if (allOptional && !pythonType.contains("| None")) {
            pythonType = pythonType + " | None";
        }

        sb.append("    ").append(fieldName).append(": ").append(pythonType);

        // Add default value
        if (column.isNullable() || allOptional) {
            sb.append(" = None");
        } else if (column.getJavaType().equals("Boolean")) {
            sb.append(" = True");
        }

        sb.append("\n");
    }

    /**
     * Generates common schemas used across the application.
     *
     * @return the common.py content
     */
    public String generateCommonSchemas() {
        return """
        from pydantic import BaseModel
        from typing import Generic, TypeVar

        T = TypeVar("T")


        class PagedResponse(BaseModel, Generic[T]):
            \"\"\"Generic paginated response schema.\"\"\"

            items: list[T]
            total: int
            page: int
            size: int
            pages: int

            @property
            def has_next(self) -> bool:
                return self.page < self.pages - 1

            @property
            def has_previous(self) -> bool:
                return self.page > 0


        class MessageResponse(BaseModel):
            \"\"\"Simple message response.\"\"\"

            message: str
            success: bool = True


        class ErrorResponse(BaseModel):
            \"\"\"Error response schema.\"\"\"

            detail: str
            code: str | None = None
        """;
    }

    private boolean isBaseField(String columnName) {
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
