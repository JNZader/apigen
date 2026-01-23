package com.jnzader.apigen.codegen.generator.java.repository;

import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlFunction;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.List;

/** Generates Spring Data JPA Repository interfaces for Java/Spring Boot. */
public class JavaRepositoryGenerator {

    private static final String APIGEN_CORE_PKG = "com.jnzader.apigen.core";

    private final String basePackage;

    public JavaRepositoryGenerator(String basePackage) {
        this.basePackage = basePackage;
    }

    /** Generates the Repository interface code. */
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
                customMethods
                        .append("\n\n    Optional<")
                        .append(entityName)
                        .append("> findBy")
                        .append(capitalField)
                        .append("(")
                        .append(col.getJavaType())
                        .append(" ")
                        .append(col.getJavaFieldName())
                        .append(");");
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
            customMethods.append("\n    ").append(func.toJavaMethodSignature()).append(";");
        }

        String imports =
                functions.isEmpty()
                        ? ""
                        : "\nimport org.springframework.data.jpa.repository.Query;";
        String optionalImport =
                table.getColumns().stream().anyMatch(c -> c.isUnique() && !c.isPrimaryKey())
                        ? "\nimport java.util.Optional;"
                        : "";

        return
"""
package %s.%s.infrastructure.repository;

import %s.domain.repository.BaseRepository;
import %s.%s.domain.entity.%s;
import org.springframework.stereotype.Repository;%s%s

@Repository
public interface %sRepository extends BaseRepository<%s, Long> {

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
                        optionalImport,
                        entityName,
                        entityName,
                        customMethods);
    }
}
