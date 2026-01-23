package com.jnzader.apigen.codegen.generator.kotlin.dto;

import static com.jnzader.apigen.codegen.generator.util.CodeGenerationUtils.pluralize;
import static com.jnzader.apigen.codegen.generator.util.CodeGenerationUtils.safeKotlinFieldName;

import com.jnzader.apigen.codegen.generator.common.ManyToManyRelation;
import com.jnzader.apigen.codegen.generator.kotlin.KotlinImportManager;
import com.jnzader.apigen.codegen.generator.kotlin.KotlinTypeMapper;
import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.List;

/** Generates DTO classes from SQL table definitions for Kotlin/Spring Boot. */
public class KotlinDTOGenerator {

    private static final String APIGEN_CORE_PKG = "com.jnzader.apigen.core";

    private final String basePackage;
    private final KotlinTypeMapper typeMapper = new KotlinTypeMapper();

    public KotlinDTOGenerator(String basePackage) {
        this.basePackage = basePackage;
    }

    /** Generates the DTO data class code in Kotlin. */
    public String generate(
            SqlTable table,
            List<SqlSchema.TableRelationship> relations,
            List<ManyToManyRelation> manyToManyRelations) {
        String entityName = table.getEntityName();
        String moduleName = table.getModuleName();

        KotlinImportManager imports = new KotlinImportManager();
        imports.addDTOImports(APIGEN_CORE_PKG);

        StringBuilder fields = new StringBuilder();

        // Generate fields for business columns with validations
        for (SqlColumn column : table.getBusinessColumns()) {
            imports.addImportsForColumn(column);
            String kotlinType = typeMapper.mapColumnType(column);
            String fieldName = safeKotlinFieldName(column.getJavaFieldName());
            boolean isNullable = column.isNullable();

            String validations = column.getValidationAnnotations();
            if (!validations.isEmpty()) {
                fields.append("\n    ").append(validations);
            }
            // Use var for MapStruct compatibility (needs write accessors)
            fields.append("\n    var ").append(fieldName).append(": ");
            fields.append(kotlinType);
            if (isNullable) {
                fields.append("? = null,");
            } else {
                fields.append(",");
            }
        }

        // Add relation IDs (not full objects to avoid circular refs)
        for (SqlSchema.TableRelationship rel : relations) {
            String fieldName = rel.getForeignKey().getJavaFieldName() + "Id";
            fields.append("\n    var ").append(fieldName).append(": Long? = null,");
        }

        // Add ManyToMany IDs
        for (ManyToManyRelation rel : manyToManyRelations) {
            String fieldName = pluralize(rel.targetTable().getEntityVariableName()) + "Ids";
            fields.append("\n    var ").append(fieldName).append(": List<Long>? = null,");
        }

        return
"""
package %s.%s.application.dto

%s

data class %sDTO(
    @field:Null(groups = [ValidationGroups.Create::class], message = "ID debe ser nulo al crear")
    @field:NotNull(groups = [ValidationGroups.Update::class], message = "ID es requerido al actualizar")
    override var id: Long? = null,

    var activo: Boolean = true,%s
) : BaseDTO {

    override fun id(): Long? = this.id

    override fun activo(): Boolean = this.activo
}
"""
                .formatted(basePackage, moduleName, imports.build(), entityName, fields);
    }
}
