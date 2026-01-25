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
package com.jnzader.apigen.codegen.generator.rust.model;

import com.jnzader.apigen.codegen.generator.rust.RustAxumOptions;
import com.jnzader.apigen.codegen.generator.rust.RustTypeMapper;
import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates Rust model structs with serde and sqlx derives.
 *
 * @author APiGen
 * @since 2.12.0
 */
@SuppressWarnings("java:S1192") // Duplicate strings intentional for code generation templates
public class RustModelGenerator {

    private final RustTypeMapper typeMapper;
    private final RustAxumOptions options;

    public RustModelGenerator(RustTypeMapper typeMapper, RustAxumOptions options) {
        this.typeMapper = typeMapper;
        this.options = options;
    }

    /**
     * Generates the models/mod.rs file with module declarations.
     *
     * @param tables the tables to generate models for
     * @return the mod.rs content
     */
    public String generateModRs(List<SqlTable> tables) {
        StringBuilder sb = new StringBuilder();
        sb.append("//! Database models.\n\n");

        for (SqlTable table : tables) {
            String moduleName = typeMapper.toModuleName(table.getName());
            sb.append("mod ").append(moduleName).append(";\n");
        }

        sb.append("\n");

        for (SqlTable table : tables) {
            String moduleName = typeMapper.toModuleName(table.getName());
            String structName = typeMapper.toStructName(table.getName());
            sb.append("pub use ").append(moduleName).append("::").append(structName).append(";\n");
        }

        return sb.toString();
    }

    /**
     * Generates a model file for a single table.
     *
     * @param table the table to generate a model for
     * @param relationships the table's relationships
     * @return the model file content
     */
    public String generate(SqlTable table, List<SqlSchema.TableRelationship> relationships) {
        StringBuilder sb = new StringBuilder();
        String structName = typeMapper.toStructName(table.getName());

        // Collect imports
        Set<String> imports = new HashSet<>();
        imports.add("serde::{Deserialize, Serialize}");
        if (options.hasDatabase()) {
            imports.add("sqlx::FromRow");
        }

        for (SqlColumn column : table.getColumns()) {
            imports.addAll(typeMapper.getRequiredImports(column));
        }

        // Write imports
        for (String imp : imports.stream().sorted().toList()) {
            sb.append("use ").append(imp).append(";\n");
        }
        sb.append("\n");

        // Doc comment
        sb.append("/// ")
                .append(structName)
                .append(" model representing the `")
                .append(table.getName())
                .append("` table.\n");

        // Derive macros
        sb.append("#[derive(Debug, Clone, Serialize, Deserialize");
        if (options.hasDatabase()) {
            sb.append(", FromRow");
        }
        sb.append(")]\n");

        // Struct definition
        sb.append("pub struct ").append(structName).append(" {\n");

        for (SqlColumn column : table.getColumns()) {
            String fieldName = typeMapper.toFieldName(column.getName());
            String fieldType = typeMapper.mapColumnType(column);

            // Add serde attributes for special cases
            if (column.isNullable()) {
                sb.append("    #[serde(skip_serializing_if = \"Option::is_none\")]\n");
            }

            // Handle deleted_at specially (don't serialize)
            if (column.getName().equalsIgnoreCase("deleted_at")) {
                sb.append("    #[serde(skip)]\n");
            }

            sb.append("    pub ").append(fieldName).append(": ").append(fieldType).append(",\n");
        }

        sb.append("}\n\n");

        // Impl block with helper methods
        sb.append("impl ").append(structName).append(" {\n");

        // Table name method
        sb.append("    /// Returns the table name.\n");
        sb.append("    pub fn table_name() -> &'static str {\n");
        sb.append("        \"").append(table.getName()).append("\"\n");
        sb.append("    }\n");

        // Check for soft delete
        boolean hasSoftDelete =
                table.getColumns().stream()
                        .anyMatch(c -> c.getName().equalsIgnoreCase("deleted_at"));

        if (hasSoftDelete) {
            sb.append("\n");
            sb.append("    /// Returns true if the record is soft-deleted.\n");
            sb.append("    pub fn is_deleted(&self) -> bool {\n");
            sb.append("        self.deleted_at.is_some()\n");
            sb.append("    }\n");
        }

        sb.append("}\n");

        return sb.toString();
    }
}
