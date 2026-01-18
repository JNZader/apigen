package com.jnzader.apigen.codegen.generator.util;

import java.util.Set;

/**
 * Utility methods for code generation.
 */
public final class CodeGenerationUtils {

    private static final Set<String> JAVA_KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new",
            "package", "private", "protected", "public", "return", "short", "static",
            "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while", "var", "yield", "record",
            "sealed", "permits", "non-sealed"
    );

    private CodeGenerationUtils() {
        // Utility class
    }

    /**
     * Ensures a field name is safe (not a Java keyword).
     *
     * @param name the field name to check
     * @return the safe field name, with "Field" suffix if it's a keyword
     */
    public static String safeFieldName(String name) {
        if (JAVA_KEYWORDS.contains(name.toLowerCase())) {
            return name + "Field";
        }
        return name;
    }

    /**
     * Converts camelCase to snake_case.
     *
     * @param camelCase the camelCase string
     * @return the snake_case string
     */
    public static String toSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    /**
     * Pluralizes a singular English word.
     *
     * @param singular the singular word
     * @return the plural form
     */
    public static String pluralize(String singular) {
        if (singular.endsWith("y") && !singular.endsWith("ay") && !singular.endsWith("ey") &&
                !singular.endsWith("oy") && !singular.endsWith("uy")) {
            return singular.substring(0, singular.length() - 1) + "ies";
        }
        if (singular.endsWith("s") || singular.endsWith("x") || singular.endsWith("ch") ||
                singular.endsWith("sh")) {
            return singular + "es";
        }
        return singular + "s";
    }

    /**
     * Checks if a string is a Java keyword.
     *
     * @param name the string to check
     * @return true if it's a Java keyword
     */
    public static boolean isJavaKeyword(String name) {
        return JAVA_KEYWORDS.contains(name.toLowerCase());
    }
}
