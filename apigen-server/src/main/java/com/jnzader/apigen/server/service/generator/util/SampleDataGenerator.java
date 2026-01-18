package com.jnzader.apigen.server.service.generator.util;

import com.jnzader.apigen.codegen.model.SqlColumn;

import static com.jnzader.apigen.server.service.generator.util.StringTransformationUtil.capitalize;

/**
 * Generates sample data values for API testing files.
 */
public final class SampleDataGenerator {

    private SampleDataGenerator() {
        // Utility class
    }

    /**
     * Gets a sample value for a column based on its type.
     *
     * @param col the SQL column
     * @return a sample JSON value
     */
    public static String getSampleValue(SqlColumn col) {
        return getSampleValue(col, null);
    }

    /**
     * Gets a sample value for a column based on its type with optional prefix.
     *
     * @param col    the SQL column
     * @param prefix optional prefix for string values
     * @return a sample JSON value
     */
    public static String getSampleValue(SqlColumn col, String prefix) {
        String javaType = col.getJavaType();
        String fieldName = col.getJavaFieldName();
        String displayName = prefix != null ? prefix + " " + fieldName : fieldName;

        return switch (javaType) {
            case "String" -> "\"" + capitalize(displayName) + " example\"";
            case "Integer", "int" -> "100";
            case "Long", "long" -> "1000";
            case "Double", "double", "Float", "float" -> "99.99";
            case "BigDecimal" -> "199.99";
            case "Boolean", "boolean" -> "true";
            case "LocalDate" -> "\"2024-01-15\"";
            case "LocalDateTime" -> "\"2024-01-15T10:30:00\"";
            case "LocalTime" -> "\"10:30:00\"";
            case "UUID" -> "\"550e8400-e29b-41d4-a716-446655440000\"";
            default -> "null";
        };
    }
}
