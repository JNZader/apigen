package com.jnzader.apigen.codegen.generator.util;

import com.jnzader.apigen.codegen.model.SqlColumn;

/** Provides sample test values for generated tests. */
public final class TestValueProvider {

    private TestValueProvider() {
        // Utility class
    }

    /**
     * Gets a sample test value for a column.
     *
     * @param col the SQL column
     * @return a sample value as a Java expression string
     */
    public static String getSampleTestValue(SqlColumn col) {
        return getSampleTestValue(col, null);
    }

    /**
     * Gets a sample test value for a column with optional prefix.
     *
     * @param col the SQL column
     * @param prefix optional prefix for string values
     * @return a sample value as a Java expression string
     */
    public static String getSampleTestValue(SqlColumn col, String prefix) {
        String javaType = col.getJavaType();
        String fieldName = col.getJavaFieldName();
        String displayValue = prefix != null ? prefix + " " + fieldName : "Test " + fieldName;

        return switch (javaType) {
            case "String" -> "\"" + displayValue + "\"";
            case "Integer", "int" -> "100";
            case "Long", "long" -> "1000L";
            case "Double", "double" -> "99.99";
            case "Float", "float" -> "99.99f";
            case "BigDecimal" -> "new java.math.BigDecimal(\"199.99\")";
            case "Boolean", "boolean" -> "true";
            case "LocalDate" -> "java.time.LocalDate.now()";
            case "LocalDateTime" -> "java.time.LocalDateTime.now()";
            case "LocalTime" -> "java.time.LocalTime.now()";
            case "UUID" -> "java.util.UUID.randomUUID()";
            default -> "null";
        };
    }

    /**
     * Gets a sample JSON value for a column.
     *
     * @param col the SQL column
     * @return a sample value as a JSON string
     */
    public static String getSampleJsonValue(SqlColumn col) {
        return getSampleJsonValue(col, null);
    }

    /**
     * Gets a sample JSON value for a column with optional prefix.
     *
     * @param col the SQL column
     * @param prefix optional prefix for string values
     * @return a sample value as a JSON string
     */
    public static String getSampleJsonValue(SqlColumn col, String prefix) {
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

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
