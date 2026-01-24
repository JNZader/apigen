package com.jnzader.apigen.codegen.parser.openapi;

import io.swagger.v3.oas.models.media.Schema;

/**
 * Maps OpenAPI schema types to SQL and Java types.
 *
 * <p>This mapper handles the conversion of OpenAPI data types to their SQL equivalents for schema
 * generation and Java types for code generation.
 */
public class OpenApiTypeMapper {

    private OpenApiTypeMapper() {
        // Utility class
    }

    /**
     * Maps an OpenAPI schema to its SQL type equivalent.
     *
     * @param schema the OpenAPI schema
     * @return the SQL type string
     */
    public static String toSqlType(Schema<?> schema) {
        if (schema == null) {
            return "VARCHAR(255)";
        }

        String type = schema.getType();
        String format = schema.getFormat();

        if (type == null) {
            // Check for $ref or allOf/oneOf/anyOf
            if (schema.get$ref() != null) {
                return "BIGINT"; // Assume reference to another entity (FK)
            }
            return "VARCHAR(255)";
        }

        return switch (type) {
            case "string" -> mapStringType(schema, format);
            case "integer" -> mapIntegerType(format);
            case "number" -> mapNumberType(format);
            case "boolean" -> "BOOLEAN";
            case "array" -> "JSONB"; // Store arrays as JSON
            case "object" -> "JSONB"; // Store objects as JSON
            default -> "VARCHAR(255)";
        };
    }

    /**
     * Maps an OpenAPI schema to its Java type equivalent.
     *
     * @param schema the OpenAPI schema
     * @return the Java type string
     */
    public static String toJavaType(Schema<?> schema) {
        if (schema == null) {
            return "String";
        }

        String type = schema.getType();
        String format = schema.getFormat();

        if (type == null) {
            if (schema.get$ref() != null) {
                return "Long"; // Reference to another entity
            }
            return "String";
        }

        return switch (type) {
            case "string" -> mapStringToJavaType(format);
            case "integer" -> mapIntegerToJavaType(format);
            case "number" -> mapNumberToJavaType(format);
            case "boolean" -> "Boolean";
            case "array" -> "List<Object>";
            case "object" -> "Map<String, Object>";
            default -> "String";
        };
    }

    private static String mapStringType(Schema<?> schema, String format) {
        if (format == null) {
            Integer maxLength = schema.getMaxLength();
            if (maxLength != null && maxLength > 0) {
                if (maxLength > 65535) {
                    return "TEXT";
                }
                return "VARCHAR(" + maxLength + ")";
            }
            return "VARCHAR(255)";
        }

        return switch (format) {
            case "date" -> "DATE";
            case "date-time" -> "TIMESTAMP";
            case "time" -> "TIME";
            case "uuid" -> "UUID";
            case "email" -> "VARCHAR(320)";
            case "uri", "url" -> "VARCHAR(2048)";
            case "byte", "binary" -> "BYTEA";
            case "password" -> "VARCHAR(255)";
            default -> "VARCHAR(255)";
        };
    }

    private static String mapIntegerType(String format) {
        if (format == null) {
            return "INTEGER";
        }

        return switch (format) {
            case "int32" -> "INTEGER";
            case "int64" -> "BIGINT";
            default -> "INTEGER";
        };
    }

    private static String mapNumberType(String format) {
        if (format == null) {
            return "DECIMAL(19,4)";
        }

        return switch (format) {
            case "float" -> "REAL";
            case "double" -> "DOUBLE PRECISION";
            default -> "DECIMAL(19,4)";
        };
    }

    private static String mapStringToJavaType(String format) {
        if (format == null) {
            return "String";
        }

        return switch (format) {
            case "date" -> "LocalDate";
            case "date-time" -> "LocalDateTime";
            case "time" -> "LocalTime";
            case "uuid" -> "UUID";
            case "byte", "binary" -> "byte[]";
            default -> "String";
        };
    }

    private static String mapIntegerToJavaType(String format) {
        if (format == null) {
            return "Integer";
        }

        return switch (format) {
            case "int32" -> "Integer";
            case "int64" -> "Long";
            default -> "Integer";
        };
    }

    private static String mapNumberToJavaType(String format) {
        if (format == null) {
            return "BigDecimal";
        }

        return switch (format) {
            case "float" -> "Float";
            case "double" -> "Double";
            default -> "BigDecimal";
        };
    }

    /**
     * Extracts the length from a VARCHAR SQL type.
     *
     * @param sqlType the SQL type string
     * @return the length or null if not a VARCHAR
     */
    public static Integer extractLength(String sqlType) {
        if (sqlType == null || !sqlType.toUpperCase().startsWith("VARCHAR")) {
            return null;
        }

        int start = sqlType.indexOf('(');
        int end = sqlType.indexOf(')');
        if (start > 0 && end > start) {
            try {
                return Integer.parseInt(sqlType.substring(start + 1, end).trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static final int[] EMPTY_INT_ARRAY = new int[0];

    /**
     * Extracts precision and scale from a DECIMAL SQL type.
     *
     * @param sqlType the SQL type string
     * @return int array [precision, scale] or empty array if not a DECIMAL
     */
    public static int[] extractPrecisionScale(String sqlType) {
        if (sqlType == null || !sqlType.toUpperCase().startsWith("DECIMAL")) {
            return EMPTY_INT_ARRAY;
        }

        int start = sqlType.indexOf('(');
        int end = sqlType.indexOf(')');
        if (start > 0 && end > start) {
            String[] parts = sqlType.substring(start + 1, end).split(",");
            try {
                int precision = Integer.parseInt(parts[0].trim());
                int scale = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
                return new int[] {precision, scale};
            } catch (NumberFormatException e) {
                return EMPTY_INT_ARRAY;
            }
        }
        return EMPTY_INT_ARRAY;
    }
}
