package com.jnzader.apigen.codegen.model;

import static com.jnzader.apigen.codegen.generator.util.NamingUtils.toCamelCase;

import java.util.List;
import java.util.Locale;
import lombok.Builder;
import lombok.Data;

/** Represents a SQL function or stored procedure. */
@Data
@Builder
public class SqlFunction {
    private String name;
    private FunctionType type;
    private List<SqlParameter> parameters;
    private String returnType;
    private String body;
    private String language; // plpgsql, sql, etc.
    private boolean isVolatile;
    private String comment;

    public enum FunctionType {
        FUNCTION,
        PROCEDURE,
        TRIGGER
    }

    @Data
    @Builder
    public static class SqlParameter {
        private String name;
        private String sqlType;
        private String javaType;
        private ParameterMode mode;
        private String defaultValue;

        public enum ParameterMode {
            IN,
            OUT,
            INOUT
        }
    }

    /** Generates a Spring Data JPA @Query annotation. */
    public String toJpaCallQuery() {
        StringBuilder sb = new StringBuilder();
        if (type == FunctionType.FUNCTION) {
            sb.append("SELECT ").append(name).append("(");
        } else {
            sb.append("CALL ").append(name).append("(");
        }

        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(":").append(parameters.get(i).getName());
        }
        sb.append(")");

        return sb.toString();
    }

    /** Generates a Java method signature for calling this function. */
    public String toJavaMethodSignature() {
        StringBuilder sb = new StringBuilder();

        // Return type
        if (returnType != null && !returnType.equals("void")) {
            sb.append(mapSqlTypeToJava(returnType));
        } else {
            sb.append("void");
        }

        sb.append(" ").append(toCamelCase(name)).append("(");

        // Parameters
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) sb.append(", ");
            SqlParameter param = parameters.get(i);
            sb.append(
                    param.getJavaType() != null
                            ? param.getJavaType()
                            : mapSqlTypeToJava(param.getSqlType()));
            sb.append(" ").append(toCamelCase(param.getName()));
        }

        sb.append(")");
        return sb.toString();
    }

    private String mapSqlTypeToJava(String sqlType) {
        if (sqlType == null) return "Object";
        return switch (sqlType.toLowerCase(Locale.ROOT).split("\\(")[0].split(" ")[0]) {
            case "integer", "int", "int4" -> "Integer";
            case "bigint", "int8" -> "Long";
            case "smallint", "int2" -> "Short";
            case "decimal", "numeric" -> "BigDecimal";
            case "real", "float4" -> "Float";
            case "double", "float8" -> "Double";
            case "boolean", "bool" -> "Boolean";
            case "varchar", "character", "text", "char" -> "String";
            case "date" -> "LocalDate";
            case "timestamp", "timestamptz" -> "LocalDateTime";
            case "time", "timetz" -> "LocalTime";
            case "uuid" -> "UUID";
            case "json", "jsonb" -> "String";
            case "bytea" -> "byte[]";
            case "void" -> "void";
            default -> "Object";
        };
    }
}
