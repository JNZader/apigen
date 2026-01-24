package com.jnzader.apigen.codegen.generator.go;

import com.jnzader.apigen.codegen.generator.api.LanguageTypeMapper;
import com.jnzader.apigen.codegen.model.SqlColumn;
import java.util.HashSet;
import java.util.Set;

/**
 * Maps SQL types to Go types for Gin/GORM projects.
 *
 * <p>Provides mappings for:
 *
 * <ul>
 *   <li>GORM model types
 *   <li>go-playground/validator tags
 *   <li>Go native types
 * </ul>
 */
public class GoTypeMapper implements LanguageTypeMapper {

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

    private static final Set<String> GO_PREDECLARED =
            Set.of(
                    "bool",
                    "byte",
                    "complex64",
                    "complex128",
                    "error",
                    "float32",
                    "float64",
                    "int",
                    "int8",
                    "int16",
                    "int32",
                    "int64",
                    "rune",
                    "string",
                    "uint",
                    "uint8",
                    "uint16",
                    "uint32",
                    "uint64",
                    "uintptr",
                    "true",
                    "false",
                    "nil",
                    "append",
                    "cap",
                    "close",
                    "complex",
                    "copy",
                    "delete",
                    "imag",
                    "len",
                    "make",
                    "new",
                    "panic",
                    "print",
                    "println",
                    "real",
                    "recover");

    @Override
    public String mapColumnType(SqlColumn column) {
        return mapJavaTypeToGo(column.getJavaType(), column.isNullable());
    }

    @Override
    public Set<String> getRequiredImports(SqlColumn column) {
        Set<String> imports = new HashSet<>();
        String javaType = column.getJavaType();

        switch (javaType) {
            case "LocalDate", "LocalDateTime", "Instant", "ZonedDateTime", "LocalTime" ->
                    imports.add("time");
            case "BigDecimal" -> imports.add("github.com/shopspring/decimal");
            case "UUID" -> imports.add("github.com/google/uuid");
            default -> {
                // No special imports needed
            }
        }

        return imports;
    }

    @Override
    public String getDefaultValue(SqlColumn column) {
        String goType = mapColumnType(column);

        // Pointer types default to nil
        if (goType.startsWith("*")) {
            return "nil";
        }

        return switch (goType) {
            case "string" -> "\"\"";
            case "int", "int32", "int64" -> "0";
            case "float64", "float32" -> "0.0";
            case "bool" -> "false";
            case "time.Time" -> "time.Time{}";
            case "uuid.UUID" -> "uuid.UUID{}";
            case "decimal.Decimal" -> "decimal.Zero";
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
        if (type.startsWith("*")) {
            return type;
        }
        return "*" + type;
    }

    /**
     * Maps a Java type to its Go equivalent.
     *
     * @param javaType the Java type name
     * @param nullable whether the field is nullable
     * @return the Go type name
     */
    public String mapJavaTypeToGo(String javaType, boolean nullable) {
        if (javaType == null) {
            return "string";
        }

        String baseType =
                switch (javaType) {
                    case "String" -> "string";
                    case "Integer", "int" -> "int";
                    case "Long", "long" -> "int64";
                    case "Short", "short" -> "int16";
                    case "Byte", "byte" -> "int8";
                    case "Double", "double" -> "float64";
                    case "Float", "float" -> "float32";
                    case "BigDecimal" -> "decimal.Decimal";
                    case "Boolean", "boolean" -> "bool";
                    case "LocalDate", "LocalDateTime", "Instant", "ZonedDateTime", "LocalTime" ->
                            "time.Time";
                    case "UUID" -> "uuid.UUID";
                    case "byte[]", "Byte[]" -> "[]byte";
                    default -> "string";
                };

        // Make pointer type for nullable fields (except slices)
        if (nullable && !baseType.startsWith("[]")) {
            return "*" + baseType;
        }

        return baseType;
    }

    /**
     * Gets the GORM column type for a SQL column.
     *
     * @param column the SQL column
     * @return the GORM column type string
     */
    public String getGormColumnType(SqlColumn column) {
        String javaType = column.getJavaType();

        return switch (javaType) {
            case "String" -> {
                int length =
                        column.getLength() != null && column.getLength() > 0
                                ? column.getLength()
                                : 255;
                if (length > 65535) {
                    yield "text";
                } else {
                    yield "varchar(" + length + ")";
                }
            }
            case "Integer", "int" -> "int";
            case "Long", "long" -> "bigint";
            case "Double", "double" -> "double precision";
            case "Float", "float" -> "real";
            case "BigDecimal" -> {
                int precision =
                        column.getPrecision() != null && column.getPrecision() > 0
                                ? column.getPrecision()
                                : 10;
                int scale =
                        column.getScale() != null && column.getScale() > 0 ? column.getScale() : 2;
                yield "decimal(" + precision + "," + scale + ")";
            }
            case "Boolean", "boolean" -> "boolean";
            case "LocalDate" -> "date";
            case "LocalDateTime", "Instant", "ZonedDateTime" -> "timestamp with time zone";
            case "LocalTime" -> "time";
            case "UUID" -> "uuid";
            case "byte[]", "Byte[]" -> "bytea";
            default -> "varchar(255)";
        };
    }

    /**
     * Gets validator tags for a column.
     *
     * @param column the SQL column
     * @param isUpdate whether this is for an update request
     * @return the validation tag string
     */
    public String getValidatorTag(SqlColumn column, boolean isUpdate) {
        StringBuilder tag = new StringBuilder();
        String javaType = column.getJavaType();

        // Required validation
        if (!isUpdate && !column.isNullable()) {
            tag.append("required");
        }

        // Type-specific validations
        switch (javaType) {
            case "String" -> {
                int maxLength =
                        column.getLength() != null && column.getLength() > 0
                                ? column.getLength()
                                : 255;
                if (tag.length() > 0) tag.append(",");
                tag.append("max=").append(maxLength);

                // Email validation
                if (column.getName().toLowerCase().contains("email")) {
                    if (tag.length() > 0) tag.append(",");
                    tag.append("email");
                }

                // URL validation
                if (column.getName().toLowerCase().contains("url")
                        || column.getName().toLowerCase().contains("website")) {
                    if (tag.length() > 0) tag.append(",");
                    tag.append("url");
                }
            }
            case "Integer", "int", "Long", "long", "Short", "short" -> {
                // Numeric validation (optional)
            }
            case "UUID" -> {
                if (tag.length() > 0) tag.append(",");
                tag.append("uuid");
            }
        }

        // Omitempty for optional fields
        if (isUpdate || column.isNullable()) {
            if (tag.length() > 0) {
                return "omitempty," + tag;
            }
            return "omitempty";
        }

        return tag.toString();
    }

    /**
     * Checks if a name is a Go keyword or predeclared identifier.
     *
     * @param name the name to check
     * @return true if it's a Go keyword or predeclared identifier
     */
    public boolean isGoKeyword(String name) {
        String lower = name.toLowerCase();
        return GO_KEYWORDS.contains(lower) || GO_PREDECLARED.contains(lower);
    }

    /**
     * Makes a field name safe for Go (prefixes with underscore if keyword).
     *
     * @param name the field name
     * @return the safe field name
     */
    public String safeGoFieldName(String name) {
        if (isGoKeyword(name)) {
            return name + "_";
        }
        return name;
    }

    /**
     * Converts a name to Go exported format (PascalCase).
     *
     * @param name the name to convert
     * @return the PascalCase name (exported)
     */
    public String toExportedName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : name.toCharArray()) {
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
     * Converts a name to Go unexported format (camelCase).
     *
     * @param name the name to convert
     * @return the camelCase name (unexported)
     */
    public String toUnexportedName(String name) {
        String exported = toExportedName(name);
        if (exported == null || exported.isEmpty()) {
            return exported;
        }
        return Character.toLowerCase(exported.charAt(0)) + exported.substring(1);
    }

    /**
     * Converts a name to snake_case (for JSON and DB columns).
     *
     * @param name the name to convert
     * @return the snake_case name
     */
    public String toSnakeCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        // Replace spaces and hyphens with underscores, then convert camelCase to snake_case
        return name.replaceAll("[\\s-]+", "_")
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toLowerCase()
                .replaceAll("_+", "_"); // Normalize multiple underscores
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
        String lower = name.toLowerCase();
        if (lower.endsWith("y")
                && !lower.endsWith("ay")
                && !lower.endsWith("ey")
                && !lower.endsWith("oy")
                && !lower.endsWith("uy")) {
            return name.substring(0, name.length() - 1) + "ies";
        }
        if (lower.endsWith("s")
                || lower.endsWith("x")
                || lower.endsWith("ch")
                || lower.endsWith("sh")) {
            return name + "es";
        }
        return name + "s";
    }

    /**
     * Gets the singular form of a name (simple English singularization).
     *
     * @param name the plural name
     * @return the singular name
     */
    public String singularize(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.endsWith("ies")) {
            return name.substring(0, name.length() - 3) + "y";
        }
        if (name.endsWith("es")
                && (name.endsWith("sses")
                        || name.endsWith("xes")
                        || name.endsWith("ches")
                        || name.endsWith("shes"))) {
            return name.substring(0, name.length() - 2);
        }
        if (name.endsWith("s") && !name.endsWith("ss")) {
            return name.substring(0, name.length() - 1);
        }
        return name;
    }
}
