package com.jnzader.apigen.codegen.generator.python;

import com.jnzader.apigen.codegen.generator.api.AbstractLanguageTypeMapper;
import com.jnzader.apigen.codegen.model.SqlColumn;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Maps SQL types to Python types for FastAPI/SQLAlchemy projects.
 *
 * <p>Provides mappings for:
 *
 * <ul>
 *   <li>SQLAlchemy column types
 *   <li>Pydantic field types
 *   <li>Python native types
 * </ul>
 */
@SuppressWarnings("java:S1192") // Type mapping strings intentional for readability
public class PythonTypeMapper extends AbstractLanguageTypeMapper {

    private static final Set<String> PYTHON_KEYWORDS =
            Set.of(
                    "and",
                    "as",
                    "assert",
                    "async",
                    "await",
                    "break",
                    "class",
                    "continue",
                    "def",
                    "del",
                    "elif",
                    "else",
                    "except",
                    "finally",
                    "for",
                    "from",
                    "global",
                    "if",
                    "import",
                    "in",
                    "is",
                    "lambda",
                    "nonlocal",
                    "not",
                    "or",
                    "pass",
                    "raise",
                    "return",
                    "try",
                    "while",
                    "with",
                    "yield",
                    "True",
                    "False",
                    "None");

    @Override
    public String mapJavaType(String javaType) {
        if (javaType == null) {
            return "str";
        }

        return switch (javaType) {
            case "String" -> "str";
            case "Integer", "int" -> "int";
            case "Long", "long" -> "int";
            case "Double", "double", "Float", "float" -> "float";
            case "Boolean", "boolean" -> "bool";
            case "BigDecimal" -> "Decimal";
            case "LocalDate" -> "date";
            case "LocalDateTime", "Instant", "ZonedDateTime" -> "datetime";
            case "LocalTime" -> "time";
            case "UUID" -> "UUID";
            case "byte[]", "Byte[]" -> "bytes";
            default -> "str";
        };
    }

    @Override
    protected String getListTypeFormat() {
        return "list[%s]";
    }

    @Override
    public String getNullableType(String type) {
        if (type.contains("| None") || type.endsWith("?")) {
            return type;
        }
        return type + " | None";
    }

    @Override
    public Set<String> getRequiredImports(SqlColumn column) {
        Set<String> imports = new HashSet<>();
        String javaType = column.getJavaType();

        switch (javaType) {
            case "BigDecimal" -> imports.add("from decimal import Decimal");
            case "LocalDate" -> imports.add("from datetime import date");
            case "LocalDateTime", "Instant", "ZonedDateTime" ->
                    imports.add("from datetime import datetime");
            case "LocalTime" -> imports.add("from datetime import time");
            case "UUID" -> imports.add("from uuid import UUID");
            default -> {
                // No import needed for basic types
            }
        }

        return imports;
    }

    @Override
    public String getDefaultValue(SqlColumn column) {
        String pythonType = mapColumnType(column);
        return switch (pythonType) {
            case "str" -> "\"\"";
            case "int" -> "0";
            case "float" -> "0.0";
            case "bool" -> "False";
            case "Decimal" -> "Decimal(\"0\")";
            case "date", "datetime", "time" -> "None";
            case "UUID" -> "None";
            case "bytes" -> "b\"\"";
            default -> "None";
        };
    }

    @Override
    public String getPrimaryKeyType() {
        return "int";
    }

    /**
     * Gets the SQLAlchemy column type for a SQL column.
     *
     * @param column the SQL column
     * @return the SQLAlchemy type string
     */
    public String getSqlAlchemyType(SqlColumn column) {
        String javaType = column.getJavaType();

        return switch (javaType) {
            case "String" -> {
                int length =
                        column.getLength() != null && column.getLength() > 0
                                ? column.getLength()
                                : 255;
                yield "String(" + length + ")";
            }
            case "Integer", "int" -> "Integer";
            case "Long", "long" -> "BigInteger";
            case "Double", "double" -> "Float";
            case "Float", "float" -> "Float";
            case "Boolean", "boolean" -> "Boolean";
            case "BigDecimal" -> {
                int precision =
                        column.getPrecision() != null && column.getPrecision() > 0
                                ? column.getPrecision()
                                : 19;
                int scale =
                        column.getScale() != null && column.getScale() > 0 ? column.getScale() : 2;
                yield "Numeric(" + precision + ", " + scale + ")";
            }
            case "LocalDate" -> "Date";
            case "LocalDateTime", "Instant", "ZonedDateTime" -> "DateTime";
            case "LocalTime" -> "Time";
            case "UUID" -> "UUID";
            case "byte[]", "Byte[]" -> "LargeBinary";
            default -> "String(255)";
        };
    }

    /**
     * Gets the Pydantic field type for a SQL column.
     *
     * @param column the SQL column
     * @return the Pydantic type with optional constraints
     */
    public String getPydanticType(SqlColumn column) {
        String baseType = mapColumnType(column);
        boolean nullable = column.isNullable();

        // Add nullable suffix if needed
        if (nullable && !baseType.equals("bool") && !baseType.contains("| None")) {
            baseType = baseType + " | None";
        }

        // Add special Pydantic types for validation
        String javaType = column.getJavaType();
        if ("String".equals(javaType)
                && column.getName().toLowerCase(Locale.ROOT).contains("email")) {
            return nullable ? "EmailStr | None" : "EmailStr";
        }

        return baseType;
    }

    /**
     * Gets the Python import statement for a type.
     *
     * @param javaType the Java type
     * @return the Python import statement or null if no import needed
     */
    public String getTypeImport(String javaType) {
        return switch (javaType) {
            case "BigDecimal" -> "from decimal import Decimal";
            case "LocalDate" -> "from datetime import date";
            case "LocalDateTime", "Instant", "ZonedDateTime" -> "from datetime import datetime";
            case "LocalTime" -> "from datetime import time";
            case "UUID" -> "from uuid import UUID";
            default -> null;
        };
    }

    /**
     * Checks if a name is a Python keyword.
     *
     * @param name the name to check
     * @return true if it's a Python keyword
     */
    public boolean isPythonKeyword(String name) {
        return PYTHON_KEYWORDS.contains(name);
    }

    /**
     * Makes a field name safe for Python (escapes keywords with underscore suffix).
     *
     * @param name the field name
     * @return the safe field name
     */
    public String safePythonFieldName(String name) {
        if (isPythonKeyword(name)) {
            return name + "_";
        }
        return name;
    }

    /**
     * Converts a name to snake_case (Python convention).
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
     * Converts a name to PascalCase (for Python class names).
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
}
