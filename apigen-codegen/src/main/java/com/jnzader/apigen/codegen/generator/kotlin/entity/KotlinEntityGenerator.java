package com.jnzader.apigen.codegen.generator.kotlin.entity;

import static com.jnzader.apigen.codegen.generator.util.CodeGenerationUtils.pluralize;
import static com.jnzader.apigen.codegen.generator.util.CodeGenerationUtils.safeKotlinFieldName;
import static com.jnzader.apigen.codegen.generator.util.CodeGenerationUtils.toSnakeCase;

import com.jnzader.apigen.codegen.generator.common.ManyToManyRelation;
import com.jnzader.apigen.codegen.generator.kotlin.KotlinImportManager;
import com.jnzader.apigen.codegen.generator.kotlin.KotlinTypeMapper;
import com.jnzader.apigen.codegen.model.RelationType;
import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlIndex;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.List;

/** Generates JPA Entity classes from SQL table definitions for Kotlin/Spring Boot. */
public class KotlinEntityGenerator {

    private static final String APIGEN_CORE_PKG = "com.jnzader.apigen.core";
    private static final String DOMAIN_ENTITY_SUFFIX = ".domain.entity.";

    private final String basePackage;
    private final KotlinTypeMapper typeMapper = new KotlinTypeMapper();

    public KotlinEntityGenerator(String basePackage) {
        this.basePackage = basePackage;
    }

    /** Generates the Entity class code in Kotlin. */
    public String generate(
            SqlTable table,
            List<SqlSchema.TableRelationship> outgoingRelations,
            List<SqlSchema.TableRelationship> incomingRelations,
            List<ManyToManyRelation> manyToManyRelations) {
        String entityName = table.getEntityName();
        String moduleName = table.getModuleName();

        KotlinImportManager imports = new KotlinImportManager();
        imports.addEntityImports(APIGEN_CORE_PKG);

        StringBuilder constructorParams = new StringBuilder();
        StringBuilder properties = new StringBuilder();

        // Generate constructor parameters and properties for business columns
        boolean isFirstParam = true;
        for (SqlColumn column : table.getBusinessColumns()) {
            imports.addImportsForColumn(column);
            String kotlinType = typeMapper.mapColumnType(column);
            String fieldName = safeKotlinFieldName(column.getJavaFieldName());
            boolean isNullable = column.isNullable();

            // Add comma separator
            if (!isFirstParam) {
                constructorParams.append(",\n");
            }
            isFirstParam = false;

            // Validation annotations
            String validations = column.getValidationAnnotations();
            if (!validations.isEmpty()) {
                constructorParams.append("\n    ").append(validations);
            }

            // Column annotation
            constructorParams.append("\n    @Column(name = \"");
            constructorParams.append(toSnakeCase(column.getJavaFieldName()));
            constructorParams.append("\"");
            if (!isNullable) constructorParams.append(", nullable = false");
            if (column.isUnique()) constructorParams.append(", unique = true");
            if (column.getLength() != null && "String".equals(column.getJavaType())) {
                constructorParams.append(", length = ").append(column.getLength());
            }
            constructorParams.append(")");

            // Property declaration
            constructorParams.append("\n    var ").append(fieldName).append(": ");
            constructorParams.append(kotlinType);
            if (isNullable) {
                constructorParams.append("? = null");
            }
        }

        // Generate ManyToOne/OneToOne relationships (outgoing)
        for (SqlSchema.TableRelationship rel : outgoingRelations) {
            String targetEntity = rel.getTargetTable().getEntityName();
            String targetModule = rel.getTargetTable().getModuleName();
            String fieldName = rel.getForeignKey().getJavaFieldName();

            imports.addImportForClass(
                    basePackage + "." + targetModule + DOMAIN_ENTITY_SUFFIX + targetEntity);

            if (!isFirstParam) {
                constructorParams.append(",\n");
            }
            isFirstParam = false;

            if (rel.getRelationType() == RelationType.ONE_TO_ONE) {
                constructorParams.append(
                        """

                        @OneToOne(fetch = FetchType.LAZY)
                        @JoinColumn(name = "%s", unique = true)
                        var %s: %s? = null\
                        """
                                .formatted(
                                        rel.getForeignKey().getColumnName(),
                                        fieldName,
                                        targetEntity));
            } else {
                constructorParams.append(
                        """

                        @ManyToOne(fetch = FetchType.LAZY)
                        @JoinColumn(name = "%s")
                        var %s: %s? = null\
                        """
                                .formatted(
                                        rel.getForeignKey().getColumnName(),
                                        fieldName,
                                        targetEntity));
            }
        }

        // Generate OneToMany relationships (incoming - inverse side)
        for (SqlSchema.TableRelationship rel : incomingRelations) {
            if (rel.getRelationType() == RelationType.ONE_TO_ONE) continue;

            String sourceEntity = rel.getSourceTable().getEntityName();
            String sourceModule = rel.getSourceTable().getModuleName();
            String fieldName = pluralize(rel.getSourceTable().getEntityVariableName());
            String mappedBy = rel.getForeignKey().getJavaFieldName();

            imports.addImportForClass(
                    basePackage + "." + sourceModule + DOMAIN_ENTITY_SUFFIX + sourceEntity);

            if (!isFirstParam) {
                constructorParams.append(",\n");
            }
            isFirstParam = false;

            constructorParams.append(
                    """

                    @OneToMany(mappedBy = "%s", cascade = [CascadeType.PERSIST, CascadeType.MERGE], orphanRemoval = true)
                    var %s: MutableList<%s> = mutableListOf()\
                    """
                            .formatted(mappedBy, fieldName, sourceEntity));
        }

        // Generate ManyToMany relationships
        for (ManyToManyRelation rel : manyToManyRelations) {
            String targetEntity = rel.targetTable().getEntityName();
            String targetModule = rel.targetTable().getModuleName();
            String fieldName = pluralize(rel.targetTable().getEntityVariableName());

            imports.addImportForClass(
                    basePackage + "." + targetModule + DOMAIN_ENTITY_SUFFIX + targetEntity);

            if (!isFirstParam) {
                constructorParams.append(",\n");
            }
            isFirstParam = false;

            constructorParams.append(
                    """

                    @ManyToMany
                    @JoinTable(
                        name = "%s",
                        joinColumns = [JoinColumn(name = "%s")],
                        inverseJoinColumns = [JoinColumn(name = "%s")]
                    )
                    var %s: MutableList<%s> = mutableListOf()\
                    """
                            .formatted(
                                    rel.junctionTable(),
                                    rel.sourceColumn(),
                                    rel.targetColumn(),
                                    fieldName,
                                    targetEntity));
        }

        // Generate index annotations
        StringBuilder tableAnnotation = new StringBuilder();
        if (!table.getIndexes().isEmpty()) {
            tableAnnotation
                    .append("@Table(name = \"")
                    .append(table.getName())
                    .append("\", indexes = [\n");
            List<String> indexStrings =
                    table.getIndexes().stream().map(SqlIndex::toJpaAnnotation).toList();
            tableAnnotation.append("    ").append(String.join(",\n    ", indexStrings));
            tableAnnotation.append("\n])");
        } else {
            tableAnnotation.append("@Table(name = \"").append(table.getName()).append("\")");
        }

        return
"""
package %s.%s.domain.entity

%s

@Entity
%s
@Audited
open class %s(%s
) : Base()
"""
                .formatted(
                        basePackage,
                        moduleName,
                        imports.build(),
                        tableAnnotation,
                        entityName,
                        constructorParams);
    }
}
