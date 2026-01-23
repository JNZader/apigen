package com.jnzader.apigen.codegen.generator.java.dto;

import static com.jnzader.apigen.codegen.generator.util.CodeGenerationUtils.pluralize;
import static com.jnzader.apigen.codegen.generator.util.CodeGenerationUtils.safeFieldName;

import com.jnzader.apigen.codegen.generator.common.ManyToManyRelation;
import com.jnzader.apigen.codegen.generator.java.JavaImportManager;
import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.List;

/** Generates DTO classes from SQL table definitions for Java/Spring Boot. */
public class JavaDTOGenerator {

    private static final String APIGEN_CORE_PKG = "com.jnzader.apigen.core";

    private final String basePackage;

    public JavaDTOGenerator(String basePackage) {
        this.basePackage = basePackage;
    }

    /** Generates the DTO class code. */
    public String generate(
            SqlTable table,
            List<SqlSchema.TableRelationship> relations,
            List<ManyToManyRelation> manyToManyRelations) {
        String entityName = table.getEntityName();
        String moduleName = table.getModuleName();

        JavaImportManager imports = new JavaImportManager();
        imports.addDTOImports(APIGEN_CORE_PKG);

        StringBuilder fields = new StringBuilder();

        // Generate fields for business columns with validations
        for (SqlColumn column : table.getBusinessColumns()) {
            imports.addImportsForColumn(column);
            String validations = column.getValidationAnnotations();
            if (!validations.isEmpty()) {
                fields.append("\n    ").append(validations);
            }
            fields.append("\n    private ")
                    .append(column.getJavaType())
                    .append(" ")
                    .append(safeFieldName(column.getJavaFieldName()))
                    .append(";");
        }

        // Add relation IDs (not full objects to avoid circular refs)
        for (SqlSchema.TableRelationship rel : relations) {
            String fieldName = rel.getForeignKey().getJavaFieldName() + "Id";
            fields.append("\n    private Long ").append(fieldName).append(";");
        }

        // Add ManyToMany IDs
        for (ManyToManyRelation rel : manyToManyRelations) {
            imports.addListImport();
            String fieldName = pluralize(rel.targetTable().getEntityVariableName()) + "Ids";
            fields.append("\n    private List<Long> ").append(fieldName).append(";");
        }

        return
"""
package %s.%s.application.dto;

%s

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class %sDTO implements BaseDTO {

    @Null(groups = ValidationGroups.Create.class, message = "ID debe ser nulo al crear")
    @NotNull(groups = ValidationGroups.Update.class, message = "ID es requerido al actualizar")
    private Long id;

    @Builder.Default
    private Boolean activo = true;
%s

    // BaseDTO interface methods
    @Override
    public Long id() {
        return this.id;
    }

    @Override
    public Boolean activo() {
        return this.activo;
    }
}
"""
                .formatted(basePackage, moduleName, imports.build(), entityName, fields);
    }
}
