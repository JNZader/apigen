package com.jnzader.apigen.codegen.generator.php;

import com.jnzader.apigen.codegen.generator.api.AbstractLanguageTypeMapper;
import com.jnzader.apigen.codegen.model.SqlColumn;
import java.util.HashSet;
import java.util.Set;

/**
 * Maps SQL types to PHP types for Laravel projects.
 *
 * <p>Provides mappings for:
 *
 * <ul>
 *   <li>Eloquent model types
 *   <li>Migration column types
 *   <li>PHP native types
 * </ul>
 */
@SuppressWarnings({
    "java:S1192",
    "java:S3776"
}) // S1192: Type mapping strings; S3776: complex type mapping logic
public class PhpTypeMapper extends AbstractLanguageTypeMapper {

    private static final Set<String> PHP_KEYWORDS =
            Set.of(
                    "abstract",
                    "and",
                    "array",
                    "as",
                    "break",
                    "callable",
                    "case",
                    "catch",
                    "class",
                    "clone",
                    "const",
                    "continue",
                    "declare",
                    "default",
                    "die",
                    "do",
                    "echo",
                    "else",
                    "elseif",
                    "empty",
                    "enddeclare",
                    "endfor",
                    "endforeach",
                    "endif",
                    "endswitch",
                    "endwhile",
                    "eval",
                    "exit",
                    "extends",
                    "final",
                    "finally",
                    "fn",
                    "for",
                    "foreach",
                    "function",
                    "global",
                    "goto",
                    "if",
                    "implements",
                    "include",
                    "include_once",
                    "instanceof",
                    "insteadof",
                    "interface",
                    "isset",
                    "list",
                    "match",
                    "namespace",
                    "new",
                    "or",
                    "print",
                    "private",
                    "protected",
                    "public",
                    "readonly",
                    "require",
                    "require_once",
                    "return",
                    "static",
                    "switch",
                    "throw",
                    "trait",
                    "try",
                    "unset",
                    "use",
                    "var",
                    "while",
                    "xor",
                    "yield",
                    "__CLASS__",
                    "__DIR__",
                    "__FILE__",
                    "__FUNCTION__",
                    "__LINE__",
                    "__METHOD__",
                    "__NAMESPACE__",
                    "__TRAIT__");

    @Override
    public String mapJavaType(String javaType) {
        if (javaType == null) {
            return "string";
        }

        return switch (javaType) {
            case "String" -> "string";
            case "Integer", "int", "Long", "long" -> "int";
            case "Double", "double", "Float", "float", "BigDecimal" -> "float";
            case "Boolean", "boolean" -> "bool";
            case "LocalDate", "LocalDateTime", "Instant", "ZonedDateTime", "LocalTime" -> "Carbon";
            case "UUID" -> "string";
            case "byte[]", "Byte[]" -> "string";
            default -> "string";
        };
    }

    @Override
    protected String getListTypeFormat() {
        return "array";
    }

    @Override
    public String getListType(String elementType) {
        // PHP uses untyped arrays, so we just return "array"
        return "array";
    }

    @Override
    public String getNullableType(String type) {
        if (type.startsWith("?")) {
            return type;
        }
        return "?" + type;
    }

    @Override
    public Set<String> getRequiredImports(SqlColumn column) {
        Set<String> imports = new HashSet<>();
        String javaType = column.getJavaType();

        switch (javaType) {
            case "LocalDate", "LocalDateTime", "Instant", "ZonedDateTime", "LocalTime" ->
                    imports.add("use Carbon\\Carbon;");
            case "BigDecimal" -> imports.add("use Brick\\Math\\BigDecimal;");
            case "UUID" -> imports.add("use Illuminate\\Support\\Str;");
            default -> {
                // No import needed for basic types
            }
        }

        return imports;
    }

    @Override
    public String getDefaultValue(SqlColumn column) {
        String phpType = mapColumnType(column);
        return switch (phpType) {
            case "string" -> "''";
            case "int" -> "0";
            case "float" -> "0.0";
            case "bool" -> "false";
            case "array" -> "[]";
            default -> "null";
        };
    }

    @Override
    public String getPrimaryKeyType() {
        return "int";
    }

    /**
     * Gets the Laravel migration column type for a SQL column.
     *
     * @param column the SQL column
     * @return the migration method string
     */
    public String getMigrationColumnType(SqlColumn column) {
        String javaType = column.getJavaType();
        String columnName = toSnakeCase(column.getName());

        StringBuilder method = new StringBuilder();

        switch (javaType) {
            case "String" -> {
                int length =
                        column.getLength() != null && column.getLength() > 0
                                ? column.getLength()
                                : 255;
                if (length > 65535) {
                    method.append("$table->longText('").append(columnName).append("')");
                } else if (length > 255) {
                    method.append("$table->text('").append(columnName).append("')");
                } else {
                    method.append("$table->string('")
                            .append(columnName)
                            .append("', ")
                            .append(length)
                            .append(")");
                }
            }
            case "Integer", "int" ->
                    method.append("$table->integer('").append(columnName).append("')");
            case "Long", "long" ->
                    method.append("$table->bigInteger('").append(columnName).append("')");
            case "Double", "double" ->
                    method.append("$table->double('").append(columnName).append("')");
            case "Float", "float" ->
                    method.append("$table->float('").append(columnName).append("')");
            case "Boolean", "boolean" ->
                    method.append("$table->boolean('").append(columnName).append("')");
            case "BigDecimal" -> {
                int precision =
                        column.getPrecision() != null && column.getPrecision() > 0
                                ? column.getPrecision()
                                : 19;
                int scale =
                        column.getScale() != null && column.getScale() > 0 ? column.getScale() : 2;
                method.append("$table->decimal('")
                        .append(columnName)
                        .append("', ")
                        .append(precision)
                        .append(", ")
                        .append(scale)
                        .append(")");
            }
            case "LocalDate" -> method.append("$table->date('").append(columnName).append("')");
            case "LocalDateTime", "Instant", "ZonedDateTime" ->
                    method.append("$table->dateTime('").append(columnName).append("')");
            case "LocalTime" -> method.append("$table->time('").append(columnName).append("')");
            case "UUID" -> method.append("$table->uuid('").append(columnName).append("')");
            case "byte[]", "Byte[]" ->
                    method.append("$table->binary('").append(columnName).append("')");
            default -> method.append("$table->string('").append(columnName).append("')");
        }

        // Add modifiers
        if (column.isNullable()) {
            method.append("->nullable()");
        }
        if (column.isUnique()) {
            method.append("->unique()");
        }

        return method.toString();
    }

    /**
     * Gets the Eloquent cast type for a column.
     *
     * @param column the SQL column
     * @return the Eloquent cast type
     */
    public String getEloquentCast(SqlColumn column) {
        String javaType = column.getJavaType();

        return switch (javaType) {
            case "Integer", "int", "Long", "long" -> "integer";
            case "Double", "double", "Float", "float" -> "float";
            case "BigDecimal" ->
                    "decimal:"
                            + (column.getScale() != null && column.getScale() > 0
                                    ? column.getScale()
                                    : 2);
            case "Boolean", "boolean" -> "boolean";
            case "LocalDate" -> "date";
            case "LocalDateTime", "Instant", "ZonedDateTime" -> "datetime";
            case "LocalTime" -> "string";
            case "UUID" -> "string";
            case "byte[]", "Byte[]" -> "string";
            default -> null; // No cast needed for strings
        };
    }

    /**
     * Checks if a name is a PHP keyword.
     *
     * @param name the name to check
     * @return true if it's a PHP keyword
     */
    public boolean isPhpKeyword(String name) {
        return PHP_KEYWORDS.contains(name.toLowerCase());
    }

    /**
     * Makes a field name safe for PHP (prefixes with underscore if keyword).
     *
     * @param name the field name
     * @return the safe field name
     */
    public String safePhpFieldName(String name) {
        if (isPhpKeyword(name)) {
            return "_" + name;
        }
        return name;
    }

    /**
     * Converts a name to snake_case (PHP/Laravel convention for database).
     *
     * @param name the name to convert
     * @return the snake_case name
     */
    public String toSnakeCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    /**
     * Converts a name to PascalCase (for PHP class names).
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
     * Converts a name to camelCase (for PHP variable names).
     *
     * @param name the name to convert
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
