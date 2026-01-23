package com.jnzader.apigen.codegen.generator.java;

import com.jnzader.apigen.codegen.generator.api.LanguageTypeMapper;
import com.jnzader.apigen.codegen.model.SqlColumn;
import java.util.Set;

/**
 * Java-specific implementation of {@link LanguageTypeMapper}.
 *
 * <p>Maps SQL types to Java types with appropriate imports for JPA entities, DTOs, and other Java
 * constructs.
 */
public class JavaTypeMapper implements LanguageTypeMapper {

    @Override
    public String mapColumnType(SqlColumn column) {
        return column.getJavaType();
    }

    @Override
    public Set<String> getRequiredImports(SqlColumn column) {
        return switch (column.getJavaType()) {
            case "BigDecimal" -> Set.of("import java.math.BigDecimal;");
            case "LocalDate" -> Set.of("import java.time.LocalDate;");
            case "LocalDateTime" -> Set.of("import java.time.LocalDateTime;");
            case "LocalTime" -> Set.of("import java.time.LocalTime;");
            case "Instant" -> Set.of("import java.time.Instant;");
            case "Duration" -> Set.of("import java.time.Duration;");
            case "UUID" -> Set.of("import java.util.UUID;");
            default -> Set.of();
        };
    }

    @Override
    public String getDefaultValue(SqlColumn column) {
        return switch (column.getJavaType()) {
            case "String" -> "\"\"";
            case "Integer", "int" -> "0";
            case "Long", "long" -> "0L";
            case "Double", "double" -> "0.0";
            case "Float", "float" -> "0.0f";
            case "Boolean", "boolean" -> "false";
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
        return switch (type) {
            case "int" -> "Integer";
            case "long" -> "Long";
            case "double" -> "Double";
            case "float" -> "Float";
            case "boolean" -> "Boolean";
            case "byte" -> "Byte";
            case "short" -> "Short";
            case "char" -> "Character";
            default -> type;
        };
    }
}
