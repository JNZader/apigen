package com.jnzader.apigen.codegen.generator.csharp;

import com.jnzader.apigen.codegen.generator.api.AbstractLanguageTypeMapper;
import com.jnzader.apigen.codegen.model.SqlColumn;

/**
 * C#-specific implementation of {@link AbstractLanguageTypeMapper}.
 *
 * <p>Maps SQL types to C# types with appropriate imports for Entity Framework entities, DTOs, and
 * other C# constructs. Handles nullable types using C# nullable reference types.
 */
@SuppressWarnings("java:S1192") // Type mapping strings intentional for readability
public class CSharpTypeMapper extends AbstractLanguageTypeMapper {

    @Override
    public String mapJavaType(String javaType) {
        return switch (javaType) {
            case "int", "Integer" -> "int";
            case "long", "Long" -> "long";
            case "double", "Double" -> "double";
            case "float", "Float" -> "float";
            case "boolean", "Boolean" -> "bool";
            case "byte", "Byte" -> "byte";
            case "short", "Short" -> "short";
            case "char", "Character" -> "char";
            case "String" -> "string";
            case "BigDecimal" -> "decimal";
            case "LocalDate" -> "DateOnly";
            case "LocalDateTime" -> "DateTime";
            case "LocalTime" -> "TimeOnly";
            case "Instant" -> "DateTimeOffset";
            case "Duration" -> "TimeSpan";
            case "UUID" -> "Guid";
            case "byte[]" -> "byte[]";
            default -> javaType;
        };
    }

    @Override
    protected String getListTypeFormat() {
        return "IEnumerable<%s>";
    }

    @Override
    public String getNullableType(String type) {
        // In C#, nullable types use the ? suffix
        if (type.endsWith("?")) {
            return type;
        }
        return type + "?";
    }

    @Override
    public String getDefaultValue(SqlColumn column) {
        String csharpType = mapColumnType(column);
        return switch (csharpType) {
            case "string" -> "string.Empty";
            case "int" -> "0";
            case "long" -> "0L";
            case "double" -> "0.0";
            case "float" -> "0.0f";
            case "decimal" -> "0m";
            case "bool" -> "false";
            case "DateTime" -> "DateTime.MinValue";
            case "DateOnly" -> "DateOnly.MinValue";
            case "TimeOnly" -> "TimeOnly.MinValue";
            case "DateTimeOffset" -> "DateTimeOffset.MinValue";
            case "Guid" -> "Guid.Empty";
            default -> "default!";
        };
    }

    @Override
    public String getPrimaryKeyType() {
        return "long";
    }

    /**
     * Returns the List type for C# collections.
     *
     * @param elementType the element type
     * @return the List type string
     */
    public String getListTypeForCollection(String elementType) {
        return "List<" + elementType + ">";
    }

    /**
     * Returns the ICollection type for C# navigation properties.
     *
     * @param elementType the element type
     * @return the ICollection type string
     */
    public String getCollectionType(String elementType) {
        return "ICollection<" + elementType + ">";
    }

    /**
     * Checks if a type is a C# value type (struct).
     *
     * @param csharpType the C# type
     * @return true if value type
     */
    public boolean isValueType(String csharpType) {
        return switch (csharpType) {
            case "int",
                    "long",
                    "short",
                    "byte",
                    "float",
                    "double",
                    "decimal",
                    "bool",
                    "char",
                    "DateTime",
                    "DateOnly",
                    "TimeOnly",
                    "DateTimeOffset",
                    "TimeSpan",
                    "Guid" ->
                    true;
            default -> false;
        };
    }
}
