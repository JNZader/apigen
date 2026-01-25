/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jnzader.apigen.codegen.generator.rust.dto;

import com.jnzader.apigen.codegen.generator.rust.RustTypeMapper;
import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Generates Rust DTO (Data Transfer Object) structs for requests and responses.
 *
 * @author APiGen
 * @since 2.12.0
 */
@SuppressWarnings({
    "java:S1192",
    "java:S3776"
}) // S1192: Template strings; S3776: complex DTO generation logic
public class RustDtoGenerator {

    private final RustTypeMapper typeMapper;

    private static final Set<String> EXCLUDED_CREATE_FIELDS =
            Set.of("id", "created_at", "updated_at", "deleted_at");

    private static final Set<String> EXCLUDED_RESPONSE_FIELDS = Set.of("deleted_at");

    public RustDtoGenerator(RustTypeMapper typeMapper) {
        this.typeMapper = typeMapper;
    }

    /**
     * Generates the dto/mod.rs file with module declarations.
     *
     * @param tables the tables to generate DTOs for
     * @return the mod.rs content
     */
    public String generateModRs(List<SqlTable> tables) {
        StringBuilder sb = new StringBuilder();
        sb.append("//! Request and Response DTOs.\n\n");

        sb.append("mod pagination;\n");
        for (SqlTable table : tables) {
            String moduleName = typeMapper.toModuleName(table.getName());
            sb.append("mod ").append(moduleName).append("_dto;\n");
        }

        sb.append("\n");
        sb.append("pub use pagination::*;\n");

        for (SqlTable table : tables) {
            String moduleName = typeMapper.toModuleName(table.getName());
            sb.append("pub use ").append(moduleName).append("_dto::*;\n");
        }

        return sb.toString();
    }

    /**
     * Generates the pagination.rs file.
     *
     * @return the pagination module content
     */
    public String generatePaginationRs() {
        StringBuilder sb = new StringBuilder();
        sb.append("//! Pagination DTOs.\n\n");

        sb.append("use serde::{Deserialize, Serialize};\n\n");

        // Pagination params
        sb.append("/// Query parameters for pagination.\n");
        sb.append("#[derive(Debug, Clone, Deserialize)]\n");
        sb.append("pub struct PaginationParams {\n");
        sb.append("    #[serde(default = \"default_page\")]\n");
        sb.append("    pub page: i64,\n");
        sb.append("    #[serde(default = \"default_page_size\")]\n");
        sb.append("    pub page_size: i64,\n");
        sb.append("}\n\n");

        sb.append("fn default_page() -> i64 { 1 }\n");
        sb.append("fn default_page_size() -> i64 { 20 }\n\n");

        sb.append("impl PaginationParams {\n");
        sb.append("    /// Calculate offset for SQL query.\n");
        sb.append("    pub fn offset(&self) -> i64 {\n");
        sb.append("        (self.page.max(1) - 1) * self.page_size\n");
        sb.append("    }\n\n");
        sb.append("    /// Get limit for SQL query.\n");
        sb.append("    pub fn limit(&self) -> i64 {\n");
        sb.append("        self.page_size.min(100).max(1)\n");
        sb.append("    }\n");
        sb.append("}\n\n");

        sb.append("impl Default for PaginationParams {\n");
        sb.append("    fn default() -> Self {\n");
        sb.append("        Self {\n");
        sb.append("            page: 1,\n");
        sb.append("            page_size: 20,\n");
        sb.append("        }\n");
        sb.append("    }\n");
        sb.append("}\n\n");

        // Paginated response
        sb.append("/// Paginated response wrapper.\n");
        sb.append("#[derive(Debug, Clone, Serialize)]\n");
        sb.append("pub struct PaginatedResponse<T> {\n");
        sb.append("    pub data: Vec<T>,\n");
        sb.append("    pub page: i64,\n");
        sb.append("    pub page_size: i64,\n");
        sb.append("    pub total: i64,\n");
        sb.append("    pub total_pages: i64,\n");
        sb.append("}\n\n");

        sb.append("impl<T> PaginatedResponse<T> {\n");
        sb.append("    /// Create a new paginated response.\n");
        sb.append(
                "    pub fn new(data: Vec<T>, page: i64, page_size: i64, total: i64) -> Self {\n");
        sb.append("        let total_pages = (total as f64 / page_size as f64).ceil() as i64;\n");
        sb.append("        Self {\n");
        sb.append("            data,\n");
        sb.append("            page,\n");
        sb.append("            page_size,\n");
        sb.append("            total,\n");
        sb.append("            total_pages,\n");
        sb.append("        }\n");
        sb.append("    }\n");
        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Generates DTOs for a single table.
     *
     * @param table the table to generate DTOs for
     * @return the DTO file content
     */
    public String generate(SqlTable table) {
        StringBuilder sb = new StringBuilder();
        String structName = typeMapper.toStructName(table.getName());

        // Collect imports from all columns used in DTOs
        Set<String> imports = new HashSet<>();
        imports.add("serde::{Deserialize, Serialize}");
        imports.add("validator::Validate");

        for (SqlColumn column : table.getColumns()) {
            String colNameLower = column.getName().toLowerCase(Locale.ROOT);
            // Include imports for create/update request fields
            if (!EXCLUDED_CREATE_FIELDS.contains(colNameLower)) {
                imports.addAll(typeMapper.getRequiredImports(column));
            }
            // Include imports for response fields (includes created_at, updated_at)
            if (!EXCLUDED_RESPONSE_FIELDS.contains(colNameLower)) {
                imports.addAll(typeMapper.getRequiredImports(column));
            }
        }

        // Write imports
        for (String imp : imports.stream().sorted().toList()) {
            sb.append("use ").append(imp).append(";\n");
        }
        sb.append("\n");

        // Create request DTO
        sb.append("/// Request DTO for creating a ").append(structName).append(".\n");
        sb.append("#[derive(Debug, Clone, Deserialize, Validate)]\n");
        sb.append("pub struct Create").append(structName).append("Request {\n");

        for (SqlColumn column : table.getColumns()) {
            String fieldName = column.getName().toLowerCase(Locale.ROOT);
            if (EXCLUDED_CREATE_FIELDS.contains(fieldName)) {
                continue;
            }

            String rustFieldName = typeMapper.toFieldName(column.getName());
            String fieldType = typeMapper.mapColumnType(column);

            // Add validation attributes
            appendValidationAttributes(sb, column);

            sb.append("    pub ")
                    .append(rustFieldName)
                    .append(": ")
                    .append(fieldType)
                    .append(",\n");
        }
        sb.append("}\n\n");

        // Update request DTO (all fields optional)
        sb.append("/// Request DTO for updating a ").append(structName).append(".\n");
        sb.append("#[derive(Debug, Clone, Deserialize, Validate)]\n");
        sb.append("pub struct Update").append(structName).append("Request {\n");

        for (SqlColumn column : table.getColumns()) {
            String fieldName = column.getName().toLowerCase(Locale.ROOT);
            if (EXCLUDED_CREATE_FIELDS.contains(fieldName)) {
                continue;
            }

            String rustFieldName = typeMapper.toFieldName(column.getName());
            String baseType = typeMapper.getBaseType(typeMapper.mapColumnType(column));

            sb.append("    #[serde(skip_serializing_if = \"Option::is_none\")]\n");
            sb.append("    pub ")
                    .append(rustFieldName)
                    .append(": Option<")
                    .append(baseType)
                    .append(">,\n");
        }
        sb.append("}\n\n");

        // Response DTO
        sb.append("/// Response DTO for ").append(structName).append(".\n");
        sb.append("#[derive(Debug, Clone, Serialize)]\n");
        sb.append("pub struct ").append(structName).append("Response {\n");

        for (SqlColumn column : table.getColumns()) {
            String fieldName = column.getName().toLowerCase(Locale.ROOT);
            if (EXCLUDED_RESPONSE_FIELDS.contains(fieldName)) {
                continue;
            }

            String rustFieldName = typeMapper.toFieldName(column.getName());
            String fieldType = typeMapper.mapColumnType(column);

            if (column.isNullable()) {
                sb.append("    #[serde(skip_serializing_if = \"Option::is_none\")]\n");
            }

            sb.append("    pub ")
                    .append(rustFieldName)
                    .append(": ")
                    .append(fieldType)
                    .append(",\n");
        }
        sb.append("}\n");

        return sb.toString();
    }

    private void appendValidationAttributes(StringBuilder sb, SqlColumn column) {
        // Required validation
        if (!column.isNullable()) {
            // No explicit validation needed - field is required by type
        }

        // Length validation for strings
        if (column.getLength() != null && column.getLength() > 0) {
            String typeName = column.getSqlType().toUpperCase(Locale.ROOT);
            if (typeName.contains("VARCHAR")
                    || typeName.contains("CHAR")
                    || typeName.equals("TEXT")) {
                sb.append("    #[validate(length(max = ")
                        .append(column.getLength())
                        .append("))]\n");
            }
        }

        // Email validation for email-like fields
        String fieldName = column.getName().toLowerCase(Locale.ROOT);
        if (fieldName.contains("email")) {
            sb.append("    #[validate(email)]\n");
        }

        // URL validation for url-like fields
        if (fieldName.contains("url") || fieldName.contains("website")) {
            sb.append("    #[validate(url)]\n");
        }
    }
}
