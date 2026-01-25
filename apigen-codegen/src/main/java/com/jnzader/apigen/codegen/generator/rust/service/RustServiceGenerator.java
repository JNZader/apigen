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
package com.jnzader.apigen.codegen.generator.rust.service;

import com.jnzader.apigen.codegen.generator.rust.RustAxumOptions;
import com.jnzader.apigen.codegen.generator.rust.RustTypeMapper;
import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Generates Rust service implementations with business logic.
 *
 * @author APiGen
 * @since 2.12.0
 */
@SuppressWarnings({
    "java:S1192",
    "java:S3776"
}) // S1192: Template strings; S3776: complex service generation logic
public class RustServiceGenerator {

    private final RustTypeMapper typeMapper;
    private final RustAxumOptions options;

    private static final Set<String> EXCLUDED_CREATE_FIELDS =
            Set.of("id", "created_at", "updated_at", "deleted_at");

    private static final Set<String> EXCLUDED_RESPONSE_FIELDS = Set.of("deleted_at");

    public RustServiceGenerator(RustTypeMapper typeMapper, RustAxumOptions options) {
        this.typeMapper = typeMapper;
        this.options = options;
    }

    /**
     * Generates the service/mod.rs file with module declarations.
     *
     * @param tables the tables to generate services for
     * @return the mod.rs content
     */
    public String generateModRs(List<SqlTable> tables) {
        StringBuilder sb = new StringBuilder();
        sb.append("//! Business logic services.\n\n");

        for (SqlTable table : tables) {
            String moduleName = typeMapper.toModuleName(table.getName());
            sb.append("mod ").append(moduleName).append("_service;\n");
        }

        sb.append("\n");

        for (SqlTable table : tables) {
            String moduleName = typeMapper.toModuleName(table.getName());
            String structName = typeMapper.toStructName(table.getName());
            sb.append("pub use ")
                    .append(moduleName)
                    .append("_service::")
                    .append(structName)
                    .append("Service;\n");
        }

        return sb.toString();
    }

    /**
     * Generates a service for a single table.
     *
     * @param table the table to generate a service for
     * @return the service file content
     */
    public String generate(SqlTable table) {
        StringBuilder sb = new StringBuilder();
        String structName = typeMapper.toStructName(table.getName());
        String lowerName = typeMapper.toCamelCase(table.getName());

        // Get primary key info
        SqlColumn pkColumn = findPrimaryKeyColumn(table);
        String pkType = typeMapper.mapColumnType(pkColumn);

        // Imports
        sb.append("use crate::dto::{\n");
        sb.append("    Create").append(structName).append("Request,\n");
        sb.append("    Update").append(structName).append("Request,\n");
        sb.append("    ").append(structName).append("Response,\n");
        sb.append("    PaginatedResponse,\n");
        sb.append("    PaginationParams,\n");
        sb.append("};\n");
        sb.append("use crate::error::{AppError, AppResult};\n");
        sb.append("use crate::models::").append(structName).append(";\n");
        sb.append("use crate::repository::").append(structName).append("Repository;\n");
        if (options.useTracing()) {
            sb.append("use tracing::instrument;\n");
        }
        sb.append("\n");

        // Service struct
        sb.append("/// Service for ").append(structName).append(" business logic.\n");
        sb.append("#[derive(Clone)]\n");
        sb.append("pub struct ").append(structName).append("Service {\n");
        sb.append("    repository: ").append(structName).append("Repository,\n");
        sb.append("}\n\n");

        sb.append("impl ").append(structName).append("Service {\n");

        // Constructor
        sb.append("    /// Creates a new service instance.\n");
        sb.append("    pub fn new(repository: ")
                .append(structName)
                .append("Repository) -> Self {\n");
        sb.append("        Self { repository }\n");
        sb.append("    }\n\n");

        // Get by ID
        if (options.useTracing()) {
            sb.append("    #[instrument(skip(self))]\n");
        }
        sb.append("    /// Gets a ").append(lowerName).append(" by ID.\n");
        sb.append("    pub async fn get_by_id(&self, id: ")
                .append(pkType)
                .append(") -> AppResult<")
                .append(structName)
                .append("Response> {\n");
        sb.append("        let entity = self.repository.find_by_id(id).await?\n");
        sb.append("            .ok_or_else(|| AppError::NotFound(\n");
        sb.append("                format!(\"")
                .append(structName)
                .append(" with id {} not found\", id)\n");
        sb.append("            ))?;\n\n");
        sb.append("        Ok(Self::to_response(&entity))\n");
        sb.append("    }\n\n");

        // Get all with pagination
        if (options.useTracing()) {
            sb.append("    #[instrument(skip(self))]\n");
        }
        sb.append("    /// Gets all ")
                .append(typeMapper.pluralize(lowerName))
                .append(" with pagination.\n");
        sb.append("    pub async fn get_all(\n");
        sb.append("        &self,\n");
        sb.append("        params: PaginationParams,\n");
        sb.append("    ) -> AppResult<PaginatedResponse<")
                .append(structName)
                .append("Response>> {\n");
        sb.append("        let entities = self.repository\n");
        sb.append("            .find_all(params.limit(), params.offset())\n");
        sb.append("            .await?;\n\n");
        sb.append("        let total = self.repository.count().await?;\n\n");
        sb.append("        let data: Vec<").append(structName).append("Response> = entities\n");
        sb.append("            .iter()\n");
        sb.append("            .map(Self::to_response)\n");
        sb.append("            .collect();\n\n");
        sb.append(
                "        Ok(PaginatedResponse::new(data, params.page, params.page_size, total))\n");
        sb.append("    }\n\n");

        // Create
        if (options.useTracing()) {
            sb.append("    #[instrument(skip(self, request))]\n");
        }
        sb.append("    /// Creates a new ").append(lowerName).append(".\n");
        sb.append("    pub async fn create(\n");
        sb.append("        &self,\n");
        sb.append("        request: Create").append(structName).append("Request,\n");
        sb.append("    ) -> AppResult<").append(structName).append("Response> {\n");
        sb.append("        let entity = Self::from_create_request(&request);\n");
        sb.append("        let created = self.repository.create(&entity).await?;\n\n");
        sb.append("        Ok(Self::to_response(&created))\n");
        sb.append("    }\n\n");

        // Update
        if (options.useTracing()) {
            sb.append("    #[instrument(skip(self, request))]\n");
        }
        sb.append("    /// Updates an existing ").append(lowerName).append(".\n");
        sb.append("    pub async fn update(\n");
        sb.append("        &self,\n");
        sb.append("        id: ").append(pkType).append(",\n");
        sb.append("        request: Update").append(structName).append("Request,\n");
        sb.append("    ) -> AppResult<").append(structName).append("Response> {\n");
        sb.append("        let mut entity = self.repository.find_by_id(id).await?\n");
        sb.append("            .ok_or_else(|| AppError::NotFound(\n");
        sb.append("                format!(\"")
                .append(structName)
                .append(" with id {} not found\", id)\n");
        sb.append("            ))?;\n\n");
        sb.append("        Self::apply_update(&mut entity, &request);\n");
        sb.append("        let updated = self.repository.update(id, &entity).await?;\n\n");
        sb.append("        Ok(Self::to_response(&updated))\n");
        sb.append("    }\n\n");

        // Delete
        if (options.useTracing()) {
            sb.append("    #[instrument(skip(self))]\n");
        }
        sb.append("    /// Deletes a ").append(lowerName).append(" by ID.\n");
        sb.append("    pub async fn delete(&self, id: ")
                .append(pkType)
                .append(") -> AppResult<()> {\n");
        sb.append("        let deleted = self.repository.delete(id).await?;\n\n");
        sb.append("        if !deleted {\n");
        sb.append("            return Err(AppError::NotFound(\n");
        sb.append("                format!(\"")
                .append(structName)
                .append(" with id {} not found\", id)\n");
        sb.append("            ));\n");
        sb.append("        }\n\n");
        sb.append("        Ok(())\n");
        sb.append("    }\n\n");

        // Mapper: from_create_request
        sb.append("    /// Converts a create request to a model.\n");
        sb.append("    fn from_create_request(request: &Create")
                .append(structName)
                .append("Request) -> ")
                .append(structName)
                .append(" {\n");
        sb.append("        ").append(structName).append(" {\n");

        for (SqlColumn column : table.getColumns()) {
            String fieldName = typeMapper.toFieldName(column.getName());
            String colNameLower = column.getName().toLowerCase(Locale.ROOT);

            if (colNameLower.equals("id")) {
                sb.append("            ")
                        .append(fieldName)
                        .append(": 0, // Will be set by database\n");
            } else if (colNameLower.equals("created_at") || colNameLower.equals("updated_at")) {
                // Use naive_utc() for TIMESTAMP WITHOUT TIME ZONE columns
                sb.append("            ")
                        .append(fieldName)
                        .append(": chrono::Utc::now().naive_utc(),\n");
            } else if (colNameLower.equals("deleted_at")) {
                sb.append("            ").append(fieldName).append(": None,\n");
            } else {
                sb.append("            ")
                        .append(fieldName)
                        .append(": request.")
                        .append(fieldName)
                        .append(".clone(),\n");
            }
        }

        sb.append("        }\n");
        sb.append("    }\n\n");

        // Mapper: apply_update
        sb.append("    /// Applies update request to an existing model.\n");
        sb.append("    fn apply_update(entity: &mut ")
                .append(structName)
                .append(", request: &Update")
                .append(structName)
                .append("Request) {\n");

        for (SqlColumn column : table.getColumns()) {
            String fieldName = typeMapper.toFieldName(column.getName());
            String colNameLower = column.getName().toLowerCase(Locale.ROOT);

            if (EXCLUDED_CREATE_FIELDS.contains(colNameLower)) {
                continue;
            }

            sb.append("        if let Some(ref value) = request.").append(fieldName).append(" {\n");
            if (column.isNullable()) {
                sb.append("            entity.")
                        .append(fieldName)
                        .append(" = Some(value.clone());\n");
            } else {
                sb.append("            entity.").append(fieldName).append(" = value.clone();\n");
            }
            sb.append("        }\n");
        }

        sb.append("    }\n\n");

        // Mapper: to_response
        sb.append("    /// Converts a model to a response DTO.\n");
        sb.append("    fn to_response(entity: &")
                .append(structName)
                .append(") -> ")
                .append(structName)
                .append("Response {\n");
        sb.append("        ").append(structName).append("Response {\n");

        for (SqlColumn column : table.getColumns()) {
            String fieldName = typeMapper.toFieldName(column.getName());
            String colNameLower = column.getName().toLowerCase(Locale.ROOT);

            if (EXCLUDED_RESPONSE_FIELDS.contains(colNameLower)) {
                continue;
            }

            sb.append("            ")
                    .append(fieldName)
                    .append(": entity.")
                    .append(fieldName)
                    .append(".clone(),\n");
        }

        sb.append("        }\n");
        sb.append("    }\n");

        sb.append("}\n");

        return sb.toString();
    }

    private SqlColumn findPrimaryKeyColumn(SqlTable table) {
        return table.getColumns().stream()
                .filter(SqlColumn::isPrimaryKey)
                .findFirst()
                .orElse(table.getColumns().get(0));
    }
}
