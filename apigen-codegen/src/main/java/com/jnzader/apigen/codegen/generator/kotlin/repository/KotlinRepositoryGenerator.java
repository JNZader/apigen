package com.jnzader.apigen.codegen.generator.kotlin.repository;

import com.jnzader.apigen.codegen.generator.kotlin.KotlinTypeMapper;
import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlFunction;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.List;

/** Generates Spring Data JPA Repository interfaces for Kotlin/Spring Boot. */
public class KotlinRepositoryGenerator {

    private static final String APIGEN_CORE_PKG = "com.jnzader.apigen.core";

    private final String basePackage;
    private final KotlinTypeMapper typeMapper = new KotlinTypeMapper();

    public KotlinRepositoryGenerator(String basePackage) {
        this.basePackage = basePackage;
    }

    /** Generates the Repository interface code in Kotlin. */
    public String generate(SqlTable table, List<SqlFunction> functions) {
        String entityName = table.getEntityName();
        String moduleName = table.getModuleName();

        StringBuilder customMethods = new StringBuilder();

        // Add methods for unique columns
        for (SqlColumn col : table.getColumns()) {
            if (col.isUnique() && !col.isPrimaryKey()) {
                String capitalField =
                        Character.toUpperCase(col.getJavaFieldName().charAt(0))
                                + col.getJavaFieldName().substring(1);
                String kotlinType = typeMapper.mapColumnType(col);
                customMethods
                        .append("\n\n    fun findBy")
                        .append(capitalField)
                        .append("(")
                        .append(col.getJavaFieldName())
                        .append(": ")
                        .append(kotlinType)
                        .append("): ")
                        .append(entityName)
                        .append("?");
            }
        }

        // Add function call methods
        for (SqlFunction func : functions) {
            customMethods
                    .append("\n\n    @Query(value = \"SELECT * FROM ")
                    .append(func.getName())
                    .append("(");
            List<String> params =
                    func.getParameters().stream().map(p -> ":" + p.getName()).toList();
            customMethods.append(String.join(", ", params));
            customMethods.append(")\", nativeQuery = true)");
            customMethods.append("\n    ").append(toKotlinMethodSignature(func));
        }

        String imports =
                functions.isEmpty() ? "" : "\nimport org.springframework.data.jpa.repository.Query";

        return
"""
package %s.%s.infrastructure.repository

import %s.domain.repository.BaseRepository
import %s.%s.domain.entity.%s
import org.springframework.stereotype.Repository%s

@Repository
interface %sRepository : BaseRepository<%s, Long> {

    // Custom query methods%s
}
"""
                .formatted(
                        basePackage,
                        moduleName,
                        APIGEN_CORE_PKG,
                        basePackage,
                        moduleName,
                        entityName,
                        imports,
                        entityName,
                        entityName,
                        customMethods);
    }

    /** Converts a SQL function to a Kotlin method signature. */
    private String toKotlinMethodSignature(SqlFunction func) {
        // Get the Java method signature and convert to Kotlin
        String javaSignature = func.toJavaMethodSignature();
        return javaMethodToKotlin(javaSignature);
    }

    /** Converts a Java method signature to Kotlin syntax. */
    private String javaMethodToKotlin(String javaSignature) {
        // Parse Java signature: "ReturnType methodName(ParamType param, ...)"
        // Convert to Kotlin: "fun methodName(param: ParamType, ...): ReturnType"

        // Find the return type (everything before the first space before method name)
        int firstSpace = javaSignature.indexOf(' ');
        if (firstSpace == -1) {
            return "fun " + javaSignature;
        }

        String returnType = javaSignature.substring(0, firstSpace);
        String rest = javaSignature.substring(firstSpace + 1);

        // Find method name and params
        int parenStart = rest.indexOf('(');
        int parenEnd = rest.lastIndexOf(')');
        if (parenStart == -1 || parenEnd == -1) {
            return "fun " + rest;
        }

        String methodName = rest.substring(0, parenStart);
        String paramsStr = rest.substring(parenStart + 1, parenEnd);

        StringBuilder sb = new StringBuilder();
        sb.append("fun ").append(methodName).append("(");

        // Convert parameters
        if (!paramsStr.trim().isEmpty()) {
            String[] params = paramsStr.split(",");
            for (int i = 0; i < params.length; i++) {
                if (i > 0) sb.append(", ");
                String param = params[i].trim();
                // Java: "Type name" -> Kotlin: "name: Type"
                int lastSpace = param.lastIndexOf(' ');
                if (lastSpace != -1) {
                    String paramType = param.substring(0, lastSpace).trim();
                    String paramName = param.substring(lastSpace + 1).trim();
                    sb.append(paramName)
                            .append(": ")
                            .append(typeMapper.mapJavaTypeToKotlin(paramType));
                } else {
                    sb.append(param);
                }
            }
        }

        sb.append("): ");

        // Convert return type
        String kotlinReturnType = typeMapper.mapJavaTypeToKotlin(returnType);
        if ("void".equals(returnType)) {
            sb.setLength(sb.length() - 2); // Remove ": " for Unit return
        } else {
            sb.append(kotlinReturnType).append("?");
        }

        return sb.toString();
    }
}
