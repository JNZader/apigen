package com.jnzader.apigen.codegen.generator.gochi;

import com.jnzader.apigen.codegen.generator.api.LanguageTypeMapper;
import com.jnzader.apigen.codegen.model.SqlColumn;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Type mapper for Go/Chi with pgx driver.
 *
 * <p>Maps SQL types to Go types optimized for pgx (pgtype package for nullable types).
 */
public class GoChiTypeMapper implements LanguageTypeMapper {

    private static final Map<String, String> JAVA_TO_GO =
            Map.ofEntries(
                    Map.entry("String", "string"),
                    Map.entry("Integer", "int32"),
                    Map.entry("Long", "int64"),
                    Map.entry("Boolean", "bool"),
                    Map.entry("BigDecimal", "decimal.Decimal"),
                    Map.entry("Double", "float64"),
                    Map.entry("Float", "float32"),
                    Map.entry("LocalDate", "time.Time"),
                    Map.entry("LocalDateTime", "time.Time"),
                    Map.entry("LocalTime", "time.Time"),
                    Map.entry("Instant", "time.Time"),
                    Map.entry("UUID", "uuid.UUID"),
                    Map.entry("byte[]", "[]byte"),
                    Map.entry("Date", "time.Time"),
                    Map.entry("Timestamp", "time.Time"));

    // pgx nullable types (using pgtype)
    private static final Map<String, String> JAVA_TO_GO_NULLABLE =
            Map.ofEntries(
                    Map.entry("String", "pgtype.Text"),
                    Map.entry("Integer", "pgtype.Int4"),
                    Map.entry("Long", "pgtype.Int8"),
                    Map.entry("Boolean", "pgtype.Bool"),
                    Map.entry("BigDecimal", "pgtype.Numeric"),
                    Map.entry("Double", "pgtype.Float8"),
                    Map.entry("Float", "pgtype.Float4"),
                    Map.entry("LocalDate", "pgtype.Date"),
                    Map.entry("LocalDateTime", "pgtype.Timestamptz"),
                    Map.entry("LocalTime", "pgtype.Time"),
                    Map.entry("Instant", "pgtype.Timestamptz"),
                    Map.entry("UUID", "pgtype.UUID"),
                    Map.entry("byte[]", "[]byte"),
                    Map.entry("Date", "pgtype.Date"),
                    Map.entry("Timestamp", "pgtype.Timestamptz"));

    private static final Set<String> GO_KEYWORDS =
            Set.of(
                    "break",
                    "case",
                    "chan",
                    "const",
                    "continue",
                    "default",
                    "defer",
                    "else",
                    "fallthrough",
                    "for",
                    "func",
                    "go",
                    "goto",
                    "if",
                    "import",
                    "interface",
                    "map",
                    "package",
                    "range",
                    "return",
                    "select",
                    "struct",
                    "switch",
                    "type",
                    "var");

    @Override
    public String mapColumnType(SqlColumn column) {
        String javaType = column.getJavaType();
        if (column.isNullable()) {
            return JAVA_TO_GO_NULLABLE.getOrDefault(javaType, "any");
        }
        return JAVA_TO_GO.getOrDefault(javaType, "any");
    }

    /**
     * Maps Java type to Go type for non-nullable fields.
     *
     * @param javaType the Java type
     * @return the Go type
     */
    public String mapJavaTypeToGo(String javaType) {
        return JAVA_TO_GO.getOrDefault(javaType, "any");
    }

    /**
     * Maps Java type to Go type with nullable support.
     *
     * @param javaType the Java type
     * @param nullable whether the field is nullable
     * @return the Go type (pgtype for nullable)
     */
    public String mapJavaTypeToGo(String javaType, boolean nullable) {
        if (nullable) {
            return JAVA_TO_GO_NULLABLE.getOrDefault(javaType, "any");
        }
        return JAVA_TO_GO.getOrDefault(javaType, "any");
    }

    /**
     * Maps Java type to pointer type for Go (used in update DTOs).
     *
     * @param javaType the Java type
     * @return the Go pointer type
     */
    public String mapJavaTypeToGoPointer(String javaType) {
        String goType = JAVA_TO_GO.getOrDefault(javaType, "any");
        return "*" + goType;
    }

    /**
     * Converts a name to exported (public) Go naming convention (PascalCase).
     *
     * @param name the input name
     * @return the exported name
     */
    public String toExportedName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        String pascal = toPascalCase(name);
        // Handle common acronyms
        pascal = pascal.replace("Id", "ID").replace("Url", "URL").replace("Api", "API");
        return pascal;
    }

    /**
     * Converts a name to unexported (private) Go naming convention (camelCase).
     *
     * @param name the input name
     * @return the unexported name
     */
    public String toUnexportedName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        String camel = toCamelCase(name);
        if (GO_KEYWORDS.contains(camel)) {
            return camel + "_";
        }
        return camel;
    }

    /**
     * Converts a name to snake_case.
     *
     * @param name the input name
     * @return the snake_case name
     */
    public String toSnakeCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return name.replaceAll("([a-z])([A-Z])", "$1_$2")
                .replaceAll("-", "_")
                .toLowerCase(Locale.ROOT);
    }

    /**
     * Converts a name to PascalCase.
     *
     * @param name the input name
     * @return the PascalCase name
     */
    public String toPascalCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : name.toCharArray()) {
            if (c == '_' || c == '-' || c == ' ') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Converts a name to camelCase.
     *
     * @param name the input name
     * @return the camelCase name
     */
    public String toCamelCase(String name) {
        String pascal = toPascalCase(name);
        if (pascal == null || pascal.isEmpty()) {
            return pascal;
        }
        return Character.toLowerCase(pascal.charAt(0)) + pascal.substring(1);
    }

    /**
     * Pluralizes a name using simple English rules.
     *
     * @param name the singular name
     * @return the pluralized name
     */
    public String pluralize(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.endsWith("y")
                && !name.endsWith("ay")
                && !name.endsWith("ey")
                && !name.endsWith("oy")
                && !name.endsWith("uy")) {
            return name.substring(0, name.length() - 1) + "ies";
        }
        if (name.endsWith("s")
                || name.endsWith("x")
                || name.endsWith("z")
                || name.endsWith("ch")
                || name.endsWith("sh")) {
            return name + "es";
        }
        return name + "s";
    }

    /**
     * Gets the required imports for a type.
     *
     * @param javaType the Java type
     * @param nullable whether nullable
     * @return the import path or null if built-in
     */
    public String getImportForType(String javaType, boolean nullable) {
        if (nullable) {
            // pgtype requires github.com/jackc/pgx/v5/pgtype
            if (JAVA_TO_GO_NULLABLE.containsKey(javaType) && !javaType.equals("byte[]")) {
                return "github.com/jackc/pgx/v5/pgtype";
            }
        }
        return switch (javaType) {
            case "LocalDate", "LocalDateTime", "LocalTime", "Instant", "Date", "Timestamp" ->
                    "time";
            case "UUID" -> "github.com/google/uuid";
            case "BigDecimal" -> "github.com/shopspring/decimal";
            default -> null;
        };
    }

    @Override
    public Set<String> getRequiredImports(SqlColumn column) {
        Set<String> imports = new HashSet<>();
        String importPath = getImportForType(column.getJavaType(), column.isNullable());
        if (importPath != null) {
            imports.add(importPath);
        }
        return imports;
    }

    @Override
    public String getDefaultValue(SqlColumn column) {
        String goType = mapColumnType(column);
        return switch (goType) {
            case "string", "pgtype.Text" -> "\"\"";
            case "int32", "int64", "pgtype.Int4", "pgtype.Int8" -> "0";
            case "float32", "float64", "pgtype.Float4", "pgtype.Float8" -> "0.0";
            case "bool", "pgtype.Bool" -> "false";
            case "[]byte" -> "nil";
            default -> "nil";
        };
    }

    @Override
    public String getPrimaryKeyType() {
        return "int64";
    }

    @Override
    public Set<String> getPrimaryKeyImports() {
        return Set.of();
    }

    @Override
    public String getListType(String elementType) {
        return "[]" + elementType;
    }

    @Override
    public String getNullableType(String type) {
        // In Go, we use pgtype for nullable types or pointers
        return switch (type) {
            case "string" -> "pgtype.Text";
            case "int32" -> "pgtype.Int4";
            case "int64" -> "pgtype.Int8";
            case "bool" -> "pgtype.Bool";
            case "float32" -> "pgtype.Float4";
            case "float64" -> "pgtype.Float8";
            case "time.Time" -> "pgtype.Timestamptz";
            case "uuid.UUID" -> "pgtype.UUID";
            case "decimal.Decimal" -> "pgtype.Numeric";
            default -> "*" + type; // Use pointer for custom types
        };
    }
}
