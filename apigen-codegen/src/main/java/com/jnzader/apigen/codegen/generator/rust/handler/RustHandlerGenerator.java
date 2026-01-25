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
package com.jnzader.apigen.codegen.generator.rust.handler;

import com.jnzader.apigen.codegen.generator.rust.RustAxumOptions;
import com.jnzader.apigen.codegen.generator.rust.RustTypeMapper;
import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.List;

/**
 * Generates Axum HTTP handlers.
 *
 * @author APiGen
 * @since 2.12.0
 */
@SuppressWarnings("java:S1192") // Template string literals intentional for readability
public class RustHandlerGenerator {

    private final RustTypeMapper typeMapper;
    private final RustAxumOptions options;

    public RustHandlerGenerator(RustTypeMapper typeMapper, RustAxumOptions options) {
        this.typeMapper = typeMapper;
        this.options = options;
    }

    /**
     * Generates the handlers/mod.rs file with module declarations.
     *
     * @param tables the tables to generate handlers for
     * @return the mod.rs content
     */
    public String generateModRs(List<SqlTable> tables) {
        StringBuilder sb = new StringBuilder();
        sb.append("//! HTTP handlers.\n\n");

        sb.append("mod health;\n");
        for (SqlTable table : tables) {
            String moduleName = typeMapper.toModuleName(table.getName());
            sb.append("mod ").append(moduleName).append("_handler;\n");
        }

        sb.append("\n");
        sb.append("pub use health::*;\n");

        for (SqlTable table : tables) {
            String moduleName = typeMapper.toModuleName(table.getName());
            sb.append("pub use ").append(moduleName).append("_handler::*;\n");
        }

        return sb.toString();
    }

    /**
     * Generates the health check handler.
     *
     * @return the health handler content
     */
    public String generateHealthHandler() {
        StringBuilder sb = new StringBuilder();
        sb.append("//! Health check handler.\n\n");

        sb.append("use axum::{\n");
        sb.append("    http::StatusCode,\n");
        sb.append("    response::IntoResponse,\n");
        sb.append("    Json,\n");
        if (options.usePostgres()) {
            sb.append("    extract::State,\n");
        }
        sb.append("};\n");
        sb.append("use serde::Serialize;\n");
        if (options.usePostgres()) {
            sb.append("use crate::router::AppState;\n");
        }
        sb.append("\n");

        sb.append("#[derive(Serialize)]\n");
        sb.append("pub struct HealthResponse {\n");
        sb.append("    pub status: String,\n");
        sb.append("    pub version: String,\n");
        if (options.hasDatabase()) {
            sb.append("    pub database: String,\n");
        }
        sb.append("}\n\n");

        if (options.usePostgres()) {
            sb.append("/// Health check endpoint.\n");
            sb.append(
                    "pub async fn health_check(State(state): State<AppState>) -> impl IntoResponse"
                            + " {\n");
            sb.append(
                    "    let db_status = match sqlx::query(\"SELECT"
                            + " 1\").fetch_one(&state.pool).await {\n");
            sb.append("        Ok(_) => \"healthy\".to_string(),\n");
            sb.append("        Err(e) => format!(\"unhealthy: {}\", e),\n");
            sb.append("    };\n\n");
            sb.append("    let response = HealthResponse {\n");
            sb.append("        status: \"ok\".to_string(),\n");
            sb.append("        version: env!(\"CARGO_PKG_VERSION\").to_string(),\n");
            sb.append("        database: db_status,\n");
            sb.append("    };\n\n");
            sb.append("    (StatusCode::OK, Json(response))\n");
            sb.append("}\n");
        } else {
            sb.append("/// Health check endpoint.\n");
            sb.append("pub async fn health_check() -> impl IntoResponse {\n");
            sb.append("    let response = HealthResponse {\n");
            sb.append("        status: \"ok\".to_string(),\n");
            sb.append("        version: env!(\"CARGO_PKG_VERSION\").to_string(),\n");
            sb.append("    };\n\n");
            sb.append("    (StatusCode::OK, Json(response))\n");
            sb.append("}\n");
        }

        return sb.toString();
    }

    /**
     * Generates a handler for a single table.
     *
     * @param table the table to generate a handler for
     * @return the handler file content
     */
    public String generate(SqlTable table) {
        StringBuilder sb = new StringBuilder();
        String structName = typeMapper.toStructName(table.getName());
        String lowerName = typeMapper.toCamelCase(table.getName());
        String snakeName = typeMapper.toSnakeCase(table.getName());

        // Get primary key info
        SqlColumn pkColumn = findPrimaryKeyColumn(table);
        String pkType = typeMapper.mapColumnType(pkColumn);

        // Imports
        sb.append("//! ").append(structName).append(" handlers.\n\n");

        sb.append("use axum::{\n");
        sb.append("    extract::{Path, Query, State},\n");
        sb.append("    http::StatusCode,\n");
        sb.append("    response::IntoResponse,\n");
        sb.append("    Json,\n");
        sb.append("};\n");
        sb.append("use crate::dto::{\n");
        sb.append("    Create").append(structName).append("Request,\n");
        sb.append("    Update").append(structName).append("Request,\n");
        sb.append("    PaginationParams,\n");
        sb.append("};\n");
        sb.append("use crate::error::AppError;\n");
        sb.append("use crate::router::AppState;\n");
        sb.append("use validator::Validate;\n");
        if (options.useTracing()) {
            sb.append("use tracing::instrument;\n");
        }
        sb.append("\n");

        // Get by ID
        if (options.useTracing()) {
            sb.append("#[instrument(skip(state))]\n");
        }
        sb.append("/// Get a ").append(lowerName).append(" by ID.\n");
        sb.append("pub async fn get_").append(snakeName).append("_by_id(\n");
        sb.append("    State(state): State<AppState>,\n");
        sb.append("    Path(id): Path<").append(pkType).append(">,\n");
        sb.append(") -> Result<impl IntoResponse, AppError> {\n");
        sb.append("    let response = state.")
                .append(snakeName)
                .append("_service.get_by_id(id).await?;\n");
        sb.append("    Ok((StatusCode::OK, Json(response)))\n");
        sb.append("}\n\n");

        // Get all
        if (options.useTracing()) {
            sb.append("#[instrument(skip(state))]\n");
        }
        sb.append("/// Get all ")
                .append(typeMapper.pluralize(lowerName))
                .append(" with pagination.\n");
        sb.append("pub async fn get_all_").append(typeMapper.pluralize(snakeName)).append("(\n");
        sb.append("    State(state): State<AppState>,\n");
        sb.append("    Query(params): Query<PaginationParams>,\n");
        sb.append(") -> Result<impl IntoResponse, AppError> {\n");
        sb.append("    let response = state.")
                .append(snakeName)
                .append("_service.get_all(params).await?;\n");
        sb.append("    Ok((StatusCode::OK, Json(response)))\n");
        sb.append("}\n\n");

        // Create
        if (options.useTracing()) {
            sb.append("#[instrument(skip(state, request))]\n");
        }
        sb.append("/// Create a new ").append(lowerName).append(".\n");
        sb.append("pub async fn create_").append(snakeName).append("(\n");
        sb.append("    State(state): State<AppState>,\n");
        sb.append("    Json(request): Json<Create").append(structName).append("Request>,\n");
        sb.append(") -> Result<impl IntoResponse, AppError> {\n");
        sb.append("    // Validate request\n");
        sb.append("    request.validate().map_err(|e| AppError::Validation(e.to_string()))?;\n\n");
        sb.append("    let response = state.")
                .append(snakeName)
                .append("_service.create(request).await?;\n");
        sb.append("    Ok((StatusCode::CREATED, Json(response)))\n");
        sb.append("}\n\n");

        // Update
        if (options.useTracing()) {
            sb.append("#[instrument(skip(state, request))]\n");
        }
        sb.append("/// Update an existing ").append(lowerName).append(".\n");
        sb.append("pub async fn update_").append(snakeName).append("(\n");
        sb.append("    State(state): State<AppState>,\n");
        sb.append("    Path(id): Path<").append(pkType).append(">,\n");
        sb.append("    Json(request): Json<Update").append(structName).append("Request>,\n");
        sb.append(") -> Result<impl IntoResponse, AppError> {\n");
        sb.append("    // Validate request\n");
        sb.append("    request.validate().map_err(|e| AppError::Validation(e.to_string()))?;\n\n");
        sb.append("    let response = state.")
                .append(snakeName)
                .append("_service.update(id, request).await?;\n");
        sb.append("    Ok((StatusCode::OK, Json(response)))\n");
        sb.append("}\n\n");

        // Delete
        if (options.useTracing()) {
            sb.append("#[instrument(skip(state))]\n");
        }
        sb.append("/// Delete a ").append(lowerName).append(" by ID.\n");
        sb.append("pub async fn delete_").append(snakeName).append("(\n");
        sb.append("    State(state): State<AppState>,\n");
        sb.append("    Path(id): Path<").append(pkType).append(">,\n");
        sb.append(") -> Result<impl IntoResponse, AppError> {\n");
        sb.append("    state.").append(snakeName).append("_service.delete(id).await?;\n");
        sb.append("    Ok(StatusCode::NO_CONTENT)\n");
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
