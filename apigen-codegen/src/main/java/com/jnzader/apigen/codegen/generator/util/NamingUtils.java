package com.jnzader.apigen.codegen.generator.util;

import com.jnzader.apigen.codegen.model.SqlSchema;
import java.util.List;
import java.util.Set;

/**
 * Utility class for naming conventions and string transformations.
 *
 * <p>Consolidates common naming operations used across all 9 language generators, reducing
 * duplication. Methods include case conversions, pluralization, and field detection.
 */
public final class NamingUtils {

    /** Audit field names that are typically managed by the framework, not user code. */
    private static final Set<String> AUDIT_FIELDS =
            Set.of(
                    "id",
                    "estado",
                    "activo",
                    "created_at",
                    "updated_at",
                    "created_by",
                    "updated_by",
                    "deleted_at",
                    "deleted_by");

    private NamingUtils() {
        // Utility class
    }

    // ========== Case Conversion Methods ==========

    /**
     * Converts snake_case or kebab-case to PascalCase.
     *
     * <p>Examples:
     *
     * <ul>
     *   <li>"user_name" → "UserName"
     *   <li>"created-at" → "CreatedAt"
     *   <li>"simple" → "Simple"
     * </ul>
     *
     * @param name the input string in snake_case or kebab-case
     * @return the PascalCase version
     */
    public static String toPascalCase(String name) {
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
     * Converts PascalCase or camelCase to snake_case.
     *
     * <p>Examples:
     *
     * <ul>
     *   <li>"UserName" → "user_name"
     *   <li>"createdAt" → "created_at"
     *   <li>"ID" → "id"
     * </ul>
     *
     * @param name the input string in PascalCase or camelCase
     * @return the snake_case version
     */
    public static String toSnakeCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Converts PascalCase or camelCase to kebab-case.
     *
     * <p>Examples:
     *
     * <ul>
     *   <li>"UserName" → "user-name"
     *   <li>"createdAt" → "created-at"
     * </ul>
     *
     * @param name the input string in PascalCase or camelCase
     * @return the kebab-case version
     */
    public static String toKebabCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append('-');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Converts snake_case to camelCase.
     *
     * <p>Examples:
     *
     * <ul>
     *   <li>"user_name" → "userName"
     *   <li>"created_at" → "createdAt"
     * </ul>
     *
     * @param snake the input string in snake_case
     * @return the camelCase version
     */
    public static String toCamelCase(String snake) {
        if (snake == null || snake.isEmpty()) {
            return snake;
        }
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;
        boolean firstChar = true;
        for (char c : snake.toLowerCase().toCharArray()) {
            if (c == '_') {
                capitalizeNext = !firstChar;
            } else {
                result.append(capitalizeNext ? Character.toUpperCase(c) : c);
                capitalizeNext = false;
                firstChar = false;
            }
        }
        return result.toString();
    }

    /**
     * Capitalizes the first letter of a string.
     *
     * @param str the input string
     * @return the string with first letter capitalized
     */
    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    // ========== Pluralization ==========

    /**
     * Converts a singular noun to its plural form.
     *
     * <p>Handles common English pluralization rules:
     *
     * <ul>
     *   <li>"Category" → "Categories" (y → ies)
     *   <li>"Status" → "Statuses" (s → ses)
     *   <li>"User" → "Users" (default + s)
     * </ul>
     *
     * @param name the singular noun
     * @return the plural form
     */
    public static String toPlural(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.endsWith("y") && name.length() > 1) {
            char beforeY = name.charAt(name.length() - 2);
            // Only change y to ies if preceded by a consonant
            if (!isVowel(beforeY)) {
                return name.substring(0, name.length() - 1) + "ies";
            }
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

    private static boolean isVowel(char c) {
        return "aeiouAEIOU".indexOf(c) >= 0;
    }

    // ========== Property Name Utilities ==========

    /**
     * Converts a foreign key column name to a property name by removing the _id suffix.
     *
     * <p>Examples:
     *
     * <ul>
     *   <li>"user_id" → "user"
     *   <li>"category_id" → "category"
     *   <li>"name" → "name" (no _id suffix)
     * </ul>
     *
     * @param fkColumnName the foreign key column name
     * @return the property name without _id suffix
     */
    public static String toPropertyName(String fkColumnName) {
        if (fkColumnName == null || fkColumnName.isEmpty()) {
            return fkColumnName;
        }
        if (fkColumnName.toLowerCase().endsWith("_id")) {
            return fkColumnName.substring(0, fkColumnName.length() - 3);
        }
        return fkColumnName;
    }

    // ========== Field Detection ==========

    /**
     * Checks if a column name is an audit field (managed by framework).
     *
     * <p>Audit fields include: id, estado, activo, created_at, updated_at, created_by, updated_by,
     * deleted_at, deleted_by.
     *
     * @param columnName the column name to check
     * @return true if the column is an audit field
     */
    public static boolean isAuditField(String columnName) {
        if (columnName == null) {
            return false;
        }
        return AUDIT_FIELDS.contains(columnName.toLowerCase());
    }

    /**
     * Checks if a column is a foreign key column in the given relationships.
     *
     * @param columnName the column name to check
     * @param relations the list of relationships to search
     * @return true if the column is a foreign key
     */
    public static boolean isForeignKeyColumn(
            String columnName, List<SqlSchema.TableRelationship> relations) {
        if (columnName == null || relations == null) {
            return false;
        }
        for (SqlSchema.TableRelationship rel : relations) {
            if (rel.getForeignKey().getColumnName().equalsIgnoreCase(columnName)) {
                return true;
            }
        }
        return false;
    }
}
