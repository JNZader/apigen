package com.jnzader.apigen.server.service.generator.util;

import java.util.Locale;

/** Utility methods for string transformations. */
public final class StringTransformationUtil {

    private StringTransformationUtil() {
        // Utility class
    }

    /**
     * Capitalizes the first letter of a string.
     *
     * @param str the string to capitalize
     * @return the capitalized string
     */
    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * Converts snake_case to camelCase.
     *
     * @param snake the snake_case string
     * @return the camelCase string
     */
    public static String toCamelCase(String snake) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;
        boolean firstChar = true;
        String lowerSnake = snake.toLowerCase(Locale.ROOT);
        for (int i = 0; i < lowerSnake.length(); i++) {
            char c = lowerSnake.charAt(i);
            if (c == '_') {
                capitalizeNext = !firstChar; // Only capitalize after underscore if not at start
            } else {
                result.append(capitalizeNext ? Character.toUpperCase(c) : c);
                capitalizeNext = false;
                firstChar = false;
            }
        }
        return result.toString();
    }

    /**
     * Converts kebab-case to PascalCase.
     *
     * @param kebab the kebab-case string
     * @return the PascalCase string
     */
    public static String toPascalCase(String kebab) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (int i = 0; i < kebab.length(); i++) {
            char c = kebab.charAt(i);
            if (c == '-') {
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
     * Converts snake_case or PascalCase to kebab-case.
     *
     * @param str the string to convert
     * @return the kebab-case string
     */
    public static String toKebabCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        // Replace underscores with hyphens and handle camelCase/PascalCase
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '_') {
                result.append('-');
            } else if (Character.isUpperCase(c) && i > 0) {
                result.append('-').append(Character.toLowerCase(c));
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }

    /**
     * Escapes a string for use as a JSON string value.
     *
     * @param s the string to escape
     * @return the escaped JSON string (including surrounding quotes)
     */
    public static String escapeJsonString(String s) {
        return "\""
                + s.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t")
                + "\"";
    }
}
