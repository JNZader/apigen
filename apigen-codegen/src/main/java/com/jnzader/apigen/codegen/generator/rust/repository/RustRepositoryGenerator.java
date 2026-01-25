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
package com.jnzader.apigen.codegen.generator.rust.repository;

import com.jnzader.apigen.codegen.generator.rust.RustAxumOptions;
import com.jnzader.apigen.codegen.generator.rust.RustTypeMapper;
import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates Rust repository implementations using SQLx.
 *
 * @author APiGen
 * @since 2.12.0
 */
@SuppressWarnings({"java:S1068", "java:S1192"}) // S1068: reserved fields; S1192: template strings
public class RustRepositoryGenerator {

    private final RustTypeMapper typeMapper;
    private final RustAxumOptions options;

    public RustRepositoryGenerator(RustTypeMapper typeMapper, RustAxumOptions options) {
        this.typeMapper = typeMapper;
        this.options = options;
    }

    /**
     * Generates the repository/mod.rs file with module declarations.
     *
     * @param tables the tables to generate repositories for
     * @return the mod.rs content
     */
    public String generateModRs(List<SqlTable> tables) {
        StringBuilder sb = new StringBuilder();
        sb.append("//! Database repositories.\n\n");

        for (SqlTable table : tables) {
            String moduleName = typeMapper.toModuleName(table.getName());
            sb.append("mod ").append(moduleName).append("_repository;\n");
        }

        sb.append("\n");

        for (SqlTable table : tables) {
            String moduleName = typeMapper.toModuleName(table.getName());
            String structName = typeMapper.toStructName(table.getName());
            sb.append("pub use ")
                    .append(moduleName)
                    .append("_repository::")
                    .append(structName)
                    .append("Repository;\n");
        }

        return sb.toString();
    }

    /**
     * Generates a repository for a single table.
     *
     * @param table the table to generate a repository for
     * @return the repository file content
     */
    public String generate(SqlTable table) {
        StringBuilder sb = new StringBuilder();
        String structName = typeMapper.toStructName(table.getName());
        String tableName = table.getName();

        // Check for soft delete support
        boolean hasSoftDelete =
                table.getColumns().stream()
                        .anyMatch(c -> c.getName().equalsIgnoreCase("deleted_at"));

        // Imports
        sb.append("use crate::error::AppResult;\n");
        sb.append("use crate::models::").append(structName).append(";\n");
        sb.append("use sqlx::PgPool;\n");
        if (hasSoftDelete) {
            sb.append("use chrono::Utc;\n");
        }
        sb.append("\n");

        // Get column info
        SqlColumn pkColumn = findPrimaryKeyColumn(table);
        String pkType = typeMapper.mapColumnType(pkColumn);

        List<SqlColumn> insertColumns =
                table.getColumns().stream()
                        .filter(
                                c ->
                                        !c.getName().equalsIgnoreCase("id")
                                                && !c.getName().equalsIgnoreCase("created_at")
                                                && !c.getName().equalsIgnoreCase("updated_at")
                                                && !c.getName().equalsIgnoreCase("deleted_at"))
                        .toList();

        List<SqlColumn> updateColumns =
                table.getColumns().stream()
                        .filter(
                                c ->
                                        !c.getName().equalsIgnoreCase("id")
                                                && !c.getName().equalsIgnoreCase("created_at")
                                                && !c.getName().equalsIgnoreCase("deleted_at"))
                        .toList();

        // Build column lists for queries
        String allColumns =
                table.getColumns().stream()
                        .map(SqlColumn::getName)
                        .collect(Collectors.joining(", "));

        // Repository struct
        sb.append("/// Repository for ").append(structName).append(" operations.\n");
        sb.append("#[derive(Clone)]\n");
        sb.append("pub struct ").append(structName).append("Repository {\n");
        sb.append("    pool: PgPool,\n");
        sb.append("}\n\n");

        sb.append("impl ").append(structName).append("Repository {\n");

        // Constructor
        sb.append("    /// Creates a new repository instance.\n");
        sb.append("    pub fn new(pool: PgPool) -> Self {\n");
        sb.append("        Self { pool }\n");
        sb.append("    }\n\n");

        // Find by ID
        sb.append("    /// Finds a record by ID.\n");
        sb.append("    pub async fn find_by_id(&self, id: ")
                .append(pkType)
                .append(") -> AppResult<Option<")
                .append(structName)
                .append(">> {\n");
        sb.append("        let result = sqlx::query_as::<_, ").append(structName).append(">(\n");
        sb.append("            r#\"\n");
        sb.append("            SELECT ").append(allColumns).append("\n");
        sb.append("            FROM ").append(tableName).append("\n");
        sb.append("            WHERE ").append(pkColumn.getName()).append(" = $1");
        if (hasSoftDelete) {
            sb.append(" AND deleted_at IS NULL");
        }
        sb.append("\n");
        sb.append("            \"#\n");
        sb.append("        )\n");
        sb.append("        .bind(id)\n");
        sb.append("        .fetch_optional(&self.pool)\n");
        sb.append("        .await?;\n\n");
        sb.append("        Ok(result)\n");
        sb.append("    }\n\n");

        // Find all with pagination
        sb.append("    /// Finds all records with pagination.\n");
        sb.append("    pub async fn find_all(&self, limit: i64, offset: i64) -> AppResult<Vec<")
                .append(structName)
                .append(">> {\n");
        sb.append("        let results = sqlx::query_as::<_, ").append(structName).append(">(\n");
        sb.append("            r#\"\n");
        sb.append("            SELECT ").append(allColumns).append("\n");
        sb.append("            FROM ").append(tableName).append("\n");
        if (hasSoftDelete) {
            sb.append("            WHERE deleted_at IS NULL\n");
        }
        sb.append("            ORDER BY ").append(pkColumn.getName()).append("\n");
        sb.append("            LIMIT $1 OFFSET $2\n");
        sb.append("            \"#\n");
        sb.append("        )\n");
        sb.append("        .bind(limit)\n");
        sb.append("        .bind(offset)\n");
        sb.append("        .fetch_all(&self.pool)\n");
        sb.append("        .await?;\n\n");
        sb.append("        Ok(results)\n");
        sb.append("    }\n\n");

        // Count
        sb.append("    /// Counts all records.\n");
        sb.append("    pub async fn count(&self) -> AppResult<i64> {\n");
        sb.append("        let count: (i64,) = sqlx::query_as(\n");
        sb.append("            r#\"SELECT COUNT(*) FROM ").append(tableName);
        if (hasSoftDelete) {
            sb.append(" WHERE deleted_at IS NULL");
        }
        sb.append("\"#\n");
        sb.append("        )\n");
        sb.append("        .fetch_one(&self.pool)\n");
        sb.append("        .await?;\n\n");
        sb.append("        Ok(count.0)\n");
        sb.append("    }\n\n");

        // Create
        sb.append("    /// Creates a new record.\n");
        sb.append("    pub async fn create(&self, entity: &")
                .append(structName)
                .append(") -> AppResult<")
                .append(structName)
                .append("> {\n");

        String insertCols =
                insertColumns.stream().map(SqlColumn::getName).collect(Collectors.joining(", "));

        String insertParams = buildInsertParams(insertColumns.size());

        sb.append("        let result = sqlx::query_as::<_, ").append(structName).append(">(\n");
        sb.append("            r#\"\n");
        sb.append("            INSERT INTO ")
                .append(tableName)
                .append(" (")
                .append(insertCols)
                .append(")\n");
        sb.append("            VALUES (").append(insertParams).append(")\n");
        sb.append("            RETURNING ").append(allColumns).append("\n");
        sb.append("            \"#\n");
        sb.append("        )\n");

        for (SqlColumn col : insertColumns) {
            String fieldName = typeMapper.toFieldName(col.getName());
            sb.append("        .bind(&entity.").append(fieldName).append(")\n");
        }

        sb.append("        .fetch_one(&self.pool)\n");
        sb.append("        .await?;\n\n");
        sb.append("        Ok(result)\n");
        sb.append("    }\n\n");

        // Update
        sb.append("    /// Updates an existing record.\n");
        sb.append("    pub async fn update(&self, id: ")
                .append(pkType)
                .append(", entity: &")
                .append(structName)
                .append(") -> AppResult<")
                .append(structName)
                .append("> {\n");

        String updateSet = buildUpdateSet(updateColumns);
        int lastParamIndex = updateColumns.size() + 1;

        sb.append("        let result = sqlx::query_as::<_, ").append(structName).append(">(\n");
        sb.append("            r#\"\n");
        sb.append("            UPDATE ").append(tableName).append("\n");
        sb.append("            SET ").append(updateSet).append("\n");
        sb.append("            WHERE ")
                .append(pkColumn.getName())
                .append(" = $")
                .append(lastParamIndex);
        if (hasSoftDelete) {
            sb.append(" AND deleted_at IS NULL");
        }
        sb.append("\n");
        sb.append("            RETURNING ").append(allColumns).append("\n");
        sb.append("            \"#\n");
        sb.append("        )\n");

        for (SqlColumn col : updateColumns) {
            String fieldName = typeMapper.toFieldName(col.getName());
            if (col.getName().equalsIgnoreCase("updated_at")) {
                sb.append("        .bind(Utc::now())\n");
            } else {
                sb.append("        .bind(&entity.").append(fieldName).append(")\n");
            }
        }
        sb.append("        .bind(id)\n");

        sb.append("        .fetch_one(&self.pool)\n");
        sb.append("        .await?;\n\n");
        sb.append("        Ok(result)\n");
        sb.append("    }\n\n");

        // Soft delete (if supported)
        if (hasSoftDelete) {
            sb.append("    /// Soft deletes a record by ID.\n");
            sb.append("    pub async fn delete(&self, id: ")
                    .append(pkType)
                    .append(") -> AppResult<bool> {\n");
            sb.append("        let result = sqlx::query(\n");
            sb.append("            r#\"UPDATE ")
                    .append(tableName)
                    .append(" SET deleted_at = NOW() WHERE ")
                    .append(pkColumn.getName())
                    .append(" = $1 AND deleted_at IS NULL\"#\n");
            sb.append("        )\n");
            sb.append("        .bind(id)\n");
            sb.append("        .execute(&self.pool)\n");
            sb.append("        .await?;\n\n");
            sb.append("        Ok(result.rows_affected() > 0)\n");
            sb.append("    }\n\n");

            sb.append("    /// Hard deletes a record by ID.\n");
            sb.append("    pub async fn hard_delete(&self, id: ")
                    .append(pkType)
                    .append(") -> AppResult<bool> {\n");
            sb.append("        let result = sqlx::query(\n");
            sb.append("            r#\"DELETE FROM ")
                    .append(tableName)
                    .append(" WHERE ")
                    .append(pkColumn.getName())
                    .append(" = $1\"#\n");
            sb.append("        )\n");
            sb.append("        .bind(id)\n");
            sb.append("        .execute(&self.pool)\n");
            sb.append("        .await?;\n\n");
            sb.append("        Ok(result.rows_affected() > 0)\n");
            sb.append("    }\n");
        } else {
            sb.append("    /// Deletes a record by ID.\n");
            sb.append("    pub async fn delete(&self, id: ")
                    .append(pkType)
                    .append(") -> AppResult<bool> {\n");
            sb.append("        let result = sqlx::query(\n");
            sb.append("            r#\"DELETE FROM ")
                    .append(tableName)
                    .append(" WHERE ")
                    .append(pkColumn.getName())
                    .append(" = $1\"#\n");
            sb.append("        )\n");
            sb.append("        .bind(id)\n");
            sb.append("        .execute(&self.pool)\n");
            sb.append("        .await?;\n\n");
            sb.append("        Ok(result.rows_affected() > 0)\n");
            sb.append("    }\n");
        }

        sb.append("}\n");

        return sb.toString();
    }

    private SqlColumn findPrimaryKeyColumn(SqlTable table) {
        return table.getColumns().stream()
                .filter(SqlColumn::isPrimaryKey)
                .findFirst()
                .orElse(table.getColumns().get(0));
    }

    private String buildInsertParams(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= count; i++) {
            if (i > 1) sb.append(", ");
            sb.append("$").append(i);
        }
        return sb.toString();
    }

    private String buildUpdateSet(List<SqlColumn> columns) {
        StringBuilder sb = new StringBuilder();
        int paramIndex = 1;
        for (SqlColumn col : columns) {
            if (paramIndex > 1) sb.append(", ");
            sb.append(col.getName()).append(" = $").append(paramIndex);
            paramIndex++;
        }
        return sb.toString();
    }
}
