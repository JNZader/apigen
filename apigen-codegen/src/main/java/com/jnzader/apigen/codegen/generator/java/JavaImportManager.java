package com.jnzader.apigen.codegen.generator.java;

import com.jnzader.apigen.codegen.model.SqlColumn;
import java.util.Set;
import java.util.TreeSet;

/**
 * Manages imports for generated Java code.
 *
 * <p>This class is Java-specific and handles common import patterns for JPA entities, DTOs,
 * controllers, and other Java constructs.
 */
public class JavaImportManager {

    private final Set<String> imports = new TreeSet<>();

    /**
     * Adds an import statement.
     *
     * @param importStatement the full import statement (e.g., "import java.util.List;")
     * @return this JavaImportManager for chaining
     */
    public JavaImportManager addImport(String importStatement) {
        imports.add(importStatement);
        return this;
    }

    /**
     * Adds an import for a class.
     *
     * @param fullyQualifiedClassName the fully qualified class name
     * @return this JavaImportManager for chaining
     */
    public JavaImportManager addImportForClass(String fullyQualifiedClassName) {
        imports.add("import " + fullyQualifiedClassName + ";");
        return this;
    }

    /**
     * Adds imports required for a column's Java type.
     *
     * @param column the SQL column
     * @return this JavaImportManager for chaining
     */
    public JavaImportManager addImportsForColumn(SqlColumn column) {
        switch (column.getJavaType()) {
            case "BigDecimal" -> addImportForClass("java.math.BigDecimal");
            case "LocalDate" -> addImportForClass("java.time.LocalDate");
            case "LocalDateTime" -> addImportForClass("java.time.LocalDateTime");
            case "LocalTime" -> addImportForClass("java.time.LocalTime");
            case "Instant" -> addImportForClass("java.time.Instant");
            case "Duration" -> addImportForClass("java.time.Duration");
            case "UUID" -> addImportForClass("java.util.UUID");
            default -> {
                // No additional imports needed for primitive types and String
            }
        }
        return this;
    }

    /**
     * Adds common entity imports.
     *
     * @param apigenCorePkg the APiGen core package
     * @return this JavaImportManager for chaining
     */
    public JavaImportManager addEntityImports(String apigenCorePkg) {
        addImportForClass(apigenCorePkg + ".domain.entity.Base");
        addImport("import jakarta.persistence.*;");
        addImport("import jakarta.validation.constraints.*;");
        addImport("import lombok.*;");
        addImportForClass("org.hibernate.envers.Audited");
        return this;
    }

    /**
     * Adds common DTO imports.
     *
     * @param apigenCorePkg the APiGen core package
     * @return this JavaImportManager for chaining
     */
    public JavaImportManager addDTOImports(String apigenCorePkg) {
        addImportForClass(apigenCorePkg + ".application.dto.BaseDTO");
        addImportForClass(apigenCorePkg + ".application.validation.ValidationGroups");
        addImport("import jakarta.validation.constraints.*;");
        addImport("import lombok.*;");
        return this;
    }

    /**
     * Adds List import.
     *
     * @return this JavaImportManager for chaining
     */
    public JavaImportManager addListImport() {
        addImportForClass("java.util.List");
        return this;
    }

    /**
     * Adds ArrayList import.
     *
     * @return this JavaImportManager for chaining
     */
    public JavaImportManager addArrayListImport() {
        addImportForClass("java.util.ArrayList");
        return this;
    }

    /**
     * Builds the imports section as a string.
     *
     * @return the formatted imports string
     */
    public String build() {
        StringBuilder sb = new StringBuilder();
        for (String imp : imports) {
            sb.append(imp).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * Gets the raw set of imports.
     *
     * @return the set of import statements
     */
    public Set<String> getImports() {
        return imports;
    }
}
