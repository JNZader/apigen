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
package com.jnzader.apigen.codegen.generator.rust;

import com.jnzader.apigen.codegen.generator.api.LanguageTypeMapper;
import com.jnzader.apigen.codegen.model.SqlColumn;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps SQL types to Rust types for SQLx-based code generation.
 *
 * <p>Handles nullable types with Option<T>, and provides proper imports for chrono, uuid,
 * rust_decimal, and serde_json types.
 *
 * @author APiGen
 * @since 2.12.0
 */
@SuppressWarnings({
    "java:S1192",
    "java:S3776"
}) // S1192: Type mapping strings; S3776: complex type mapping logic
public class RustTypeMapper implements LanguageTypeMapper {

    private static final Pattern ACRONYM_PATTERN = Pattern.compile("(Id|Url|Api|Uuid|Json|Http)");

    /** SQL type to Rust type mappings. */
    private static final Map<String, String> TYPE_MAPPINGS =
            Map.ofEntries(
                    // String types
                    Map.entry("VARCHAR", "String"),
                    Map.entry("CHARACTER VARYING", "String"),
                    Map.entry("CHAR", "String"),
                    Map.entry("CHARACTER", "String"),
                    Map.entry("TEXT", "String"),
                    Map.entry("CLOB", "String"),

                    // Integer types
                    Map.entry("SMALLINT", "i16"),
                    Map.entry("INT2", "i16"),
                    Map.entry("INTEGER", "i32"),
                    Map.entry("INT", "i32"),
                    Map.entry("INT4", "i32"),
                    Map.entry("BIGINT", "i64"),
                    Map.entry("INT8", "i64"),
                    Map.entry("SERIAL", "i32"),
                    Map.entry("BIGSERIAL", "i64"),
                    Map.entry("SMALLSERIAL", "i16"),

                    // Boolean
                    Map.entry("BOOLEAN", "bool"),
                    Map.entry("BOOL", "bool"),

                    // Decimal/Numeric
                    Map.entry("DECIMAL", "Decimal"),
                    Map.entry("NUMERIC", "Decimal"),
                    Map.entry("MONEY", "Decimal"),

                    // Floating point
                    Map.entry("REAL", "f32"),
                    Map.entry("FLOAT4", "f32"),
                    Map.entry("DOUBLE PRECISION", "f64"),
                    Map.entry("DOUBLE", "f64"),
                    Map.entry("FLOAT8", "f64"),
                    Map.entry("FLOAT", "f64"),

                    // Date/Time types
                    Map.entry("DATE", "NaiveDate"),
                    Map.entry("TIME", "NaiveTime"),
                    Map.entry("TIME WITHOUT TIME ZONE", "NaiveTime"),
                    Map.entry("TIME WITH TIME ZONE", "NaiveTime"),
                    Map.entry("TIMESTAMP", "NaiveDateTime"),
                    Map.entry("TIMESTAMP WITHOUT TIME ZONE", "NaiveDateTime"),
                    Map.entry("TIMESTAMPTZ", "DateTime<Utc>"),
                    Map.entry("TIMESTAMP WITH TIME ZONE", "DateTime<Utc>"),
                    Map.entry("DATETIME", "NaiveDateTime"),

                    // UUID
                    Map.entry("UUID", "Uuid"),

                    // Binary
                    Map.entry("BYTEA", "Vec<u8>"),
                    Map.entry("BLOB", "Vec<u8>"),
                    Map.entry("BINARY", "Vec<u8>"),
                    Map.entry("VARBINARY", "Vec<u8>"),

                    // JSON
                    Map.entry("JSON", "Value"),
                    Map.entry("JSONB", "Value"));

    /** Crate imports required for specific types. */
    private static final Map<String, String> TYPE_IMPORTS =
            Map.of(
                    "Decimal", "rust_decimal::Decimal",
                    "NaiveDate", "chrono::NaiveDate",
                    "NaiveTime", "chrono::NaiveTime",
                    "NaiveDateTime", "chrono::NaiveDateTime",
                    "DateTime<Utc>", "chrono::{DateTime, Utc}",
                    "Uuid", "uuid::Uuid",
                    "Value", "serde_json::Value");

    /** Rust keywords that need escaping. */
    private static final Set<String> RUST_KEYWORDS =
            Set.of(
                    "as",
                    "async",
                    "await",
                    "break",
                    "const",
                    "continue",
                    "crate",
                    "dyn",
                    "else",
                    "enum",
                    "extern",
                    "false",
                    "fn",
                    "for",
                    "if",
                    "impl",
                    "in",
                    "let",
                    "loop",
                    "match",
                    "mod",
                    "move",
                    "mut",
                    "pub",
                    "ref",
                    "return",
                    "self",
                    "Self",
                    "static",
                    "struct",
                    "super",
                    "trait",
                    "true",
                    "type",
                    "unsafe",
                    "use",
                    "where",
                    "while",
                    "abstract",
                    "become",
                    "box",
                    "do",
                    "final",
                    "macro",
                    "override",
                    "priv",
                    "try",
                    "typeof",
                    "unsized",
                    "virtual",
                    "yield");

    @Override
    public String mapColumnType(SqlColumn column) {
        String sqlType = column.getSqlType().toUpperCase(Locale.ROOT);
        String rustType = TYPE_MAPPINGS.getOrDefault(sqlType, "String");

        if (column.isNullable()) {
            return "Option<" + rustType + ">";
        }
        return rustType;
    }

    @Override
    public Set<String> getRequiredImports(SqlColumn column) {
        Set<String> imports = new HashSet<>();
        String sqlType = column.getSqlType().toUpperCase(Locale.ROOT);
        String rustType = TYPE_MAPPINGS.getOrDefault(sqlType, "String");

        String importPath = TYPE_IMPORTS.get(rustType);
        if (importPath != null) {
            imports.add(importPath);
        }

        return imports;
    }

    @Override
    public String getDefaultValue(SqlColumn column) {
        String sqlType = column.getSqlType().toUpperCase(Locale.ROOT);
        String rustType = TYPE_MAPPINGS.getOrDefault(sqlType, "String");

        if (column.isNullable()) {
            return "None";
        }

        return switch (rustType) {
            case "String" -> "String::new()";
            case "i16", "i32", "i64" -> "0";
            case "f32", "f64" -> "0.0";
            case "bool" -> "false";
            case "Decimal" -> "Decimal::ZERO";
            case "Uuid" -> "Uuid::nil()";
            case "Vec<u8>" -> "Vec::new()";
            case "Value" -> "Value::Null";
            default -> "Default::default()";
        };
    }

    @Override
    public String getPrimaryKeyType() {
        return "i64";
    }

    @Override
    public Set<String> getPrimaryKeyImports() {
        return Set.of();
    }

    @Override
    public String getListType(String elementType) {
        return "Vec<" + elementType + ">";
    }

    @Override
    public String getNullableType(String type) {
        if (type.startsWith("Option<")) {
            return type;
        }
        return "Option<" + type + ">";
    }

    /**
     * Gets the base type without Option wrapper.
     *
     * @param rustType the Rust type
     * @return the unwrapped type
     */
    public String getBaseType(String rustType) {
        if (rustType.startsWith("Option<") && rustType.endsWith(">")) {
            return rustType.substring(7, rustType.length() - 1);
        }
        return rustType;
    }

    /**
     * Converts a name to snake_case (Rust convention for variables and modules).
     *
     * @param name the name to convert
     * @return snake_case name
     */
    public String toSnakeCase(String name) {
        if (name == null || name.isEmpty()) return name;

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    char prev = name.charAt(i - 1);
                    if (!Character.isUpperCase(prev) && prev != '_') {
                        result.append('_');
                    }
                }
                result.append(Character.toLowerCase(c));
            } else if (c == '-' || c == ' ') {
                result.append('_');
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Converts a name to PascalCase (Rust convention for types and structs).
     *
     * @param name the name to convert
     * @return PascalCase name
     */
    public String toPascalCase(String name) {
        if (name == null || name.isEmpty()) return name;

        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '_' || c == '-' || c == ' ') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }

        return fixAcronyms(result.toString());
    }

    /**
     * Converts a name to camelCase.
     *
     * @param name the name to convert
     * @return camelCase name
     */
    public String toCamelCase(String name) {
        String pascal = toPascalCase(name);
        if (pascal.isEmpty()) return pascal;
        return Character.toLowerCase(pascal.charAt(0)) + pascal.substring(1);
    }

    /**
     * Escapes a Rust keyword by appending underscore.
     *
     * @param name the name to check
     * @return escaped name if keyword, original otherwise
     */
    public String escapeKeyword(String name) {
        if (RUST_KEYWORDS.contains(name)) {
            return "r#" + name;
        }
        return name;
    }

    /**
     * Makes a field name safe for Rust.
     *
     * @param name the field name
     * @return safe field name
     */
    public String toFieldName(String name) {
        String snake = toSnakeCase(name);
        return escapeKeyword(snake);
    }

    /**
     * Converts a table name to a struct name.
     *
     * @param tableName the table name
     * @return struct name in PascalCase
     */
    public String toStructName(String tableName) {
        return toPascalCase(tableName);
    }

    /**
     * Converts a table name to a module name.
     *
     * @param tableName the table name
     * @return module name in snake_case
     */
    public String toModuleName(String tableName) {
        return toSnakeCase(tableName);
    }

    /**
     * Simple English pluralization.
     *
     * @param word the word to pluralize
     * @return pluralized word
     */
    public String pluralize(String word) {
        if (word == null || word.isEmpty()) return word;

        String lower = word.toLowerCase(Locale.ROOT);
        if (lower.endsWith("s")
                || lower.endsWith("x")
                || lower.endsWith("z")
                || lower.endsWith("ch")
                || lower.endsWith("sh")) {
            return word + "es";
        }
        if (lower.endsWith("y") && word.length() > 1) {
            char beforeY = lower.charAt(lower.length() - 2);
            if (beforeY != 'a'
                    && beforeY != 'e'
                    && beforeY != 'i'
                    && beforeY != 'o'
                    && beforeY != 'u') {
                return word.substring(0, word.length() - 1) + "ies";
            }
        }
        return word + "s";
    }

    /**
     * Converts common acronyms to uppercase (Id -> ID, Url -> URL, etc.).
     *
     * @param name the name to fix
     * @return name with uppercase acronyms
     */
    private String fixAcronyms(String name) {
        Matcher matcher = ACRONYM_PATTERN.matcher(name);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(result, matcher.group(1).toUpperCase(Locale.ROOT));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Gets all imports needed for a set of columns.
     *
     * @param columns the columns
     * @return set of import statements
     */
    public Set<String> getAllImports(Iterable<SqlColumn> columns) {
        Set<String> imports = new HashSet<>();
        for (SqlColumn column : columns) {
            imports.addAll(getRequiredImports(column));
        }
        return imports;
    }

    /**
     * Formats imports as Rust use statements.
     *
     * @param imports the import paths
     * @return formatted use statements
     */
    public String formatImports(Set<String> imports) {
        if (imports.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (String imp : imports.stream().sorted().toList()) {
            sb.append("use ").append(imp).append(";\n");
        }
        return sb.toString();
    }
}
