package com.jnzader.apigen.codegen.generator.kotlin;

import com.jnzader.apigen.codegen.generator.api.LanguageTypeMapper;
import com.jnzader.apigen.codegen.model.SqlColumn;
import java.util.Set;

/**
 * Kotlin-specific implementation of {@link LanguageTypeMapper}.
 *
 * <p>Maps SQL types to Kotlin types with appropriate imports for JPA entities, DTOs, and other
 * Kotlin constructs. Handles nullable types using Kotlin's built-in null-safety features.
 */
@SuppressWarnings("java:S1192") // Type mapping strings intentional for readability
public class KotlinTypeMapper implements LanguageTypeMapper {

    @Override
    public String mapColumnType(SqlColumn column) {
        return mapJavaTypeToKotlin(column.getJavaType());
    }

    /**
     * Maps a Java type to its Kotlin equivalent.
     *
     * @param javaType the Java type name
     * @return the Kotlin type name
     */
    public String mapJavaTypeToKotlin(String javaType) {
        return switch (javaType) {
            case "int", "Integer" -> "Int";
            case "long", "Long" -> "Long";
            case "double", "Double" -> "Double";
            case "float", "Float" -> "Float";
            case "boolean", "Boolean" -> "Boolean";
            case "byte", "Byte" -> "Byte";
            case "short", "Short" -> "Short";
            case "char", "Character" -> "Char";
            default -> javaType; // String, BigDecimal, LocalDate, etc. stay the same
        };
    }

    @Override
    public Set<String> getRequiredImports(SqlColumn column) {
        return switch (column.getJavaType()) {
            case "BigDecimal" -> Set.of("import java.math.BigDecimal");
            case "LocalDate" -> Set.of("import java.time.LocalDate");
            case "LocalDateTime" -> Set.of("import java.time.LocalDateTime");
            case "LocalTime" -> Set.of("import java.time.LocalTime");
            case "Instant" -> Set.of("import java.time.Instant");
            case "Duration" -> Set.of("import java.time.Duration");
            case "UUID" -> Set.of("import java.util.UUID");
            default -> Set.of();
        };
    }

    @Override
    public String getDefaultValue(SqlColumn column) {
        String kotlinType = mapColumnType(column);
        return switch (kotlinType) {
            case "String" -> "\"\"";
            case "Int" -> "0";
            case "Long" -> "0L";
            case "Double" -> "0.0";
            case "Float" -> "0.0f";
            case "Boolean" -> "false";
            case "BigDecimal" -> "BigDecimal.ZERO";
            default -> "null";
        };
    }

    @Override
    public String getPrimaryKeyType() {
        return "Long";
    }

    @Override
    public Set<String> getPrimaryKeyImports() {
        return Set.of();
    }

    @Override
    public String getListType(String elementType) {
        return "List<" + elementType + ">";
    }

    @Override
    public String getNullableType(String type) {
        // In Kotlin, nullable types use the ? suffix
        if (type.endsWith("?")) {
            return type;
        }
        return type + "?";
    }

    /**
     * Returns the mutable list type for Kotlin.
     *
     * @param elementType the element type
     * @return the MutableList type string
     */
    public String getMutableListType(String elementType) {
        return "MutableList<" + elementType + ">";
    }
}
