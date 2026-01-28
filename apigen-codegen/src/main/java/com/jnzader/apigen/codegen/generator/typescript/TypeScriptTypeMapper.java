package com.jnzader.apigen.codegen.generator.typescript;

import com.jnzader.apigen.codegen.generator.api.AbstractLanguageTypeMapper;
import com.jnzader.apigen.codegen.model.SqlColumn;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Maps SQL types to TypeScript types for NestJS projects.
 *
 * <p>Provides mappings for:
 *
 * <ul>
 *   <li>TypeORM entity types
 *   <li>class-validator decorators
 *   <li>TypeScript native types
 * </ul>
 */
@SuppressWarnings("java:S1192") // Type mapping strings intentional for readability
public class TypeScriptTypeMapper extends AbstractLanguageTypeMapper {

    private static final Set<String> TS_KEYWORDS =
            Set.of(
                    "abstract",
                    "any",
                    "as",
                    "async",
                    "await",
                    "boolean",
                    "break",
                    "case",
                    "catch",
                    "class",
                    "const",
                    "constructor",
                    "continue",
                    "debugger",
                    "declare",
                    "default",
                    "delete",
                    "do",
                    "else",
                    "enum",
                    "export",
                    "extends",
                    "false",
                    "finally",
                    "for",
                    "from",
                    "function",
                    "get",
                    "if",
                    "implements",
                    "import",
                    "in",
                    "instanceof",
                    "interface",
                    "is",
                    "let",
                    "module",
                    "namespace",
                    "never",
                    "new",
                    "null",
                    "number",
                    "object",
                    "of",
                    "package",
                    "private",
                    "protected",
                    "public",
                    "readonly",
                    "require",
                    "return",
                    "set",
                    "static",
                    "string",
                    "super",
                    "switch",
                    "symbol",
                    "this",
                    "throw",
                    "true",
                    "try",
                    "type",
                    "typeof",
                    "undefined",
                    "unique",
                    "unknown",
                    "var",
                    "void",
                    "while",
                    "with",
                    "yield");

    @Override
    public String mapJavaType(String javaType) {
        if (javaType == null) {
            return "string";
        }

        return switch (javaType) {
            case "String" -> "string";
            case "Integer", "int", "Long", "long", "Double", "double", "Float", "float" -> "number";
            case "BigDecimal" -> "number";
            case "Boolean", "boolean" -> "boolean";
            case "LocalDate", "LocalDateTime", "Instant", "ZonedDateTime", "LocalTime" -> "Date";
            case "UUID" -> "string";
            case "byte[]", "Byte[]" -> "Buffer";
            default -> "string";
        };
    }

    @Override
    protected String getListTypeFormat() {
        return "%s[]";
    }

    @Override
    public String getNullableType(String type) {
        if (type.endsWith(" | null")) {
            return type;
        }
        return type + " | null";
    }

    @Override
    public Set<String> getRequiredImports(SqlColumn column) {
        Set<String> imports = new HashSet<>();
        String javaType = column.getJavaType();

        switch (javaType) {
            case "LocalDate", "LocalDateTime", "Instant", "ZonedDateTime", "LocalTime" ->
                    imports.add("Date");
            case "BigDecimal" -> imports.add("number");
            case "UUID" -> imports.add("string");
            default -> {
                // No special imports needed for basic types
            }
        }

        return imports;
    }

    @Override
    public String getDefaultValue(SqlColumn column) {
        String tsType = mapColumnType(column);
        return switch (tsType) {
            case "string" -> "''";
            case "number" -> "0";
            case "boolean" -> "false";
            case "Date" -> "new Date()";
            default -> "null";
        };
    }

    @Override
    public String getPrimaryKeyType() {
        return "number";
    }

    /**
     * Gets the TypeORM column type for a SQL column.
     *
     * @param column the SQL column
     * @return the TypeORM column type string
     */
    public String getTypeOrmColumnType(SqlColumn column) {
        String javaType = column.getJavaType();

        return switch (javaType) {
            case "String" -> {
                int length =
                        column.getLength() != null && column.getLength() > 0
                                ? column.getLength()
                                : 255;
                if (length > 65535) {
                    yield "text";
                } else if (length > 255) {
                    yield "text";
                } else {
                    yield "varchar";
                }
            }
            case "Integer", "int" -> "int";
            case "Long", "long" -> "bigint";
            case "Double", "double" -> "double precision";
            case "Float", "float" -> "float";
            case "BigDecimal" -> "decimal";
            case "Boolean", "boolean" -> "boolean";
            case "LocalDate" -> "date";
            case "LocalDateTime", "Instant", "ZonedDateTime" -> "timestamp";
            case "LocalTime" -> "time";
            case "UUID" -> "uuid";
            case "byte[]", "Byte[]" -> "bytea";
            default -> "varchar";
        };
    }

    /**
     * Gets class-validator decorators for a column.
     *
     * @param column the SQL column
     * @param isUpdate whether this is for an update DTO
     * @return list of decorator strings
     */
    public String getValidationDecorators(SqlColumn column, boolean isUpdate) {
        StringBuilder decorators = new StringBuilder();
        String javaType = column.getJavaType();

        // Optional decorator for update DTOs
        if (isUpdate) {
            decorators.append("  @IsOptional()\n");
        }

        // Type-specific decorators
        switch (javaType) {
            case "String" -> {
                decorators.append("  @IsString()\n");
                int maxLength =
                        column.getLength() != null && column.getLength() > 0
                                ? column.getLength()
                                : 255;
                decorators.append("  @MaxLength(").append(maxLength).append(")\n");

                // Email validation
                if (column.getName().toLowerCase(Locale.ROOT).contains("email")) {
                    decorators.append("  @IsEmail()\n");
                }

                // URL validation
                if (column.getName().toLowerCase(Locale.ROOT).contains("url")
                        || column.getName().toLowerCase(Locale.ROOT).contains("website")) {
                    decorators.append("  @IsUrl()\n");
                }
            }
            case "Integer", "int", "Long", "long" -> decorators.append("  @IsInt()\n");
            case "Double", "double", "Float", "float", "BigDecimal" ->
                    decorators.append("  @IsNumber()\n");
            case "Boolean", "boolean" -> decorators.append("  @IsBoolean()\n");
            case "LocalDate", "LocalDateTime", "Instant", "ZonedDateTime" ->
                    decorators.append("  @IsDate()\n");
            case "UUID" -> decorators.append("  @IsUUID()\n");
            default -> {
                // Other types use default validation
            }
        }

        // Not empty for required strings
        if (!isUpdate && !column.isNullable() && "String".equals(javaType)) {
            decorators.append("  @IsNotEmpty()\n");
        }

        return decorators.toString();
    }

    /**
     * Checks if a name is a TypeScript keyword.
     *
     * @param name the name to check
     * @return true if it's a TypeScript keyword
     */
    public boolean isTypeScriptKeyword(String name) {
        return TS_KEYWORDS.contains(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Makes a field name safe for TypeScript (prefixes with underscore if keyword).
     *
     * @param name the field name
     * @return the safe field name
     */
    public String safeTypeScriptFieldName(String name) {
        if (isTypeScriptKeyword(name)) {
            return "_" + name;
        }
        return name;
    }

    /**
     * Converts a name to camelCase (TypeScript convention for variables/properties).
     *
     * @param name the name to convert
     * @return the camelCase name
     */
    public String toCamelCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '_' || c == '-') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else if (i == 0) {
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Converts a name to PascalCase (for TypeScript class names).
     *
     * @param name the name to convert
     * @return the PascalCase name
     */
    public String toPascalCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '_' || c == '-') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }

    /**
     * Converts a name to kebab-case (for TypeScript file names).
     *
     * @param name the name to convert
     * @return the kebab-case name
     */
    public String toKebabCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return name.replaceAll("([a-z])([A-Z])", "$1-$2")
                .replace("_", "-")
                .toLowerCase(Locale.ROOT);
    }

    /**
     * Converts a name to snake_case.
     *
     * @param name the name to convert
     * @return the snake_case name
     */
    public String toSnakeCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    /**
     * Gets the plural form of a name (simple English pluralization).
     *
     * @param name the singular name
     * @return the plural name
     */
    public String pluralize(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.endsWith("y")) {
            return name.substring(0, name.length() - 1) + "ies";
        }
        if (name.endsWith("s")
                || name.endsWith("x")
                || name.endsWith("ch")
                || name.endsWith("sh")) {
            return name + "es";
        }
        return name + "s";
    }
}
