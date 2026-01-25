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
package com.jnzader.apigen.codegen.generator.rust.router;

import com.jnzader.apigen.codegen.generator.rust.RustAxumOptions;
import com.jnzader.apigen.codegen.generator.rust.RustTypeMapper;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.List;

/**
 * Generates Axum router configuration.
 *
 * @author APiGen
 * @since 2.12.0
 */
@SuppressWarnings({
    "java:S1192",
    "java:S3776"
}) // S1192: Template strings; S3776: complex router generation logic
public class RustRouterGenerator {

    private final RustTypeMapper typeMapper;
    private final RustAxumOptions options;

    public RustRouterGenerator(RustTypeMapper typeMapper, RustAxumOptions options) {
        this.typeMapper = typeMapper;
        this.options = options;
    }

    /**
     * Generates the router.rs file.
     *
     * @param tables the tables to create routes for
     * @return the router file content
     */
    public String generate(List<SqlTable> tables) {
        StringBuilder sb = new StringBuilder();
        sb.append("//! Router configuration.\n\n");

        // Imports
        sb.append("use axum::{\n");
        sb.append("    routing::{delete, get, post, put},\n");
        sb.append("    Router,\n");
        sb.append("};\n");
        if (options.usePostgres()) {
            sb.append("use sqlx::PgPool;\n");
        }
        sb.append("use tower_http::{\n");
        sb.append("    cors::{Any, CorsLayer},\n");
        if (options.useTracing()) {
            sb.append("    trace::TraceLayer,\n");
        }
        sb.append("};\n");

        sb.append("use crate::handlers;\n");
        sb.append("use crate::repository::*;\n");
        sb.append("use crate::service::*;\n");
        if (options.useJwt()) {
            sb.append("use crate::middleware::jwt_auth;\n");
        }
        sb.append("\n");

        // Generate AppState struct
        sb.append("/// Application state containing all services.\n");
        sb.append("#[derive(Clone)]\n");
        sb.append("pub struct AppState {\n");
        if (options.usePostgres()) {
            sb.append("    pub pool: PgPool,\n");
        }
        for (SqlTable table : tables) {
            String varName = typeMapper.toSnakeCase(table.getName());
            String structName = typeMapper.toStructName(table.getName());
            sb.append("    pub ")
                    .append(varName)
                    .append("_service: ")
                    .append(structName)
                    .append("Service,\n");
        }
        sb.append("}\n\n");

        // Create router function
        sb.append("/// Creates the application router.\n");
        if (options.usePostgres()) {
            sb.append("pub fn create_router(pool: PgPool) -> Router {\n");
        } else {
            sb.append("pub fn create_router() -> Router {\n");
        }

        // Create repositories and services
        if (!tables.isEmpty()) {
            sb.append("    // Initialize repositories\n");
            for (SqlTable table : tables) {
                String structName = typeMapper.toStructName(table.getName());
                String varName = typeMapper.toSnakeCase(table.getName());
                sb.append("    let ")
                        .append(varName)
                        .append("_repo = ")
                        .append(structName)
                        .append("Repository::new(pool.clone());\n");
            }
            sb.append("\n");

            sb.append("    // Initialize services\n");
            for (SqlTable table : tables) {
                String structName = typeMapper.toStructName(table.getName());
                String varName = typeMapper.toSnakeCase(table.getName());
                sb.append("    let ")
                        .append(varName)
                        .append("_service = ")
                        .append(structName)
                        .append("Service::new(")
                        .append(varName)
                        .append("_repo);\n");
            }
            sb.append("\n");
        }

        // Create AppState
        sb.append("    // Create application state\n");
        sb.append("    let state = AppState {\n");
        if (options.usePostgres()) {
            sb.append("        pool: pool.clone(),\n");
        }
        for (SqlTable table : tables) {
            String varName = typeMapper.toSnakeCase(table.getName());
            sb.append("        ").append(varName).append("_service,\n");
        }
        sb.append("    };\n\n");

        // CORS layer
        sb.append("    // CORS configuration\n");
        sb.append("    let cors = CorsLayer::new()\n");
        sb.append("        .allow_origin(Any)\n");
        sb.append("        .allow_methods(Any)\n");
        sb.append("        .allow_headers(Any);\n\n");

        // Build router
        sb.append("    // Build router\n");
        sb.append("    Router::new()\n");
        sb.append("        // Health check\n");
        sb.append("        .route(\"/health\", get(handlers::health_check))\n");

        // API routes
        if (!tables.isEmpty()) {
            sb.append("        // API v1 routes\n");

            for (SqlTable table : tables) {
                String structName = typeMapper.toStructName(table.getName());
                String snakeName = typeMapper.toSnakeCase(table.getName());
                String pluralSnakeName = typeMapper.pluralize(snakeName);
                String routePath =
                        "/api/v1/" + typeMapper.pluralize(typeMapper.toSnakeCase(table.getName()));

                sb.append("        // ").append(structName).append(" routes\n");
                sb.append("        .route(\n");
                sb.append("            \"").append(routePath).append("\",\n");
                sb.append("            get(handlers::get_all_")
                        .append(pluralSnakeName)
                        .append(")\n");
                sb.append("                .post(handlers::create_")
                        .append(snakeName)
                        .append("),\n");
                sb.append("        )\n");
                sb.append("        .route(\n");
                sb.append("            \"").append(routePath).append("/{id}\",\n");
                sb.append("            get(handlers::get_").append(snakeName).append("_by_id)\n");
                sb.append("                .put(handlers::update_").append(snakeName).append(")\n");
                sb.append("                .delete(handlers::delete_")
                        .append(snakeName)
                        .append("),\n");
                sb.append("        )\n");
            }
        }

        // Add layers and state
        if (options.useTracing()) {
            sb.append("        .layer(TraceLayer::new_for_http())\n");
        }
        sb.append("        .layer(cors)\n");
        sb.append("        .with_state(state)\n");
        sb.append("}\n");

        return sb.toString();
    }
}
