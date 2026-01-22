package com.jnzader.apigen.codegen.generator.entity;

import static com.jnzader.apigen.codegen.generator.util.CodeGenerationUtils.pluralize;
import static com.jnzader.apigen.codegen.generator.util.CodeGenerationUtils.safeFieldName;
import static com.jnzader.apigen.codegen.generator.util.CodeGenerationUtils.toSnakeCase;

import com.jnzader.apigen.codegen.generator.util.ImportManager;
import com.jnzader.apigen.codegen.model.RelationType;
import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlIndex;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.List;

/** Generates JPA Entity classes from SQL table definitions. */
public class EntityGenerator {

    private static final String APIGEN_CORE_PKG = "com.jnzader.apigen.core";
    private static final String DOMAIN_ENTITY_SUFFIX = ".domain.entity.";

    private final String basePackage;

    public EntityGenerator(String basePackage) {
        this.basePackage = basePackage;
    }

    /** Generates the Entity class code. */
    public String generate(
            SqlTable table,
            List<SqlSchema.TableRelationship> outgoingRelations,
            List<SqlSchema.TableRelationship> incomingRelations,
            List<ManyToManyRelation> manyToManyRelations) {
        String entityName = table.getEntityName();
        String moduleName = table.getModuleName();

        ImportManager imports = new ImportManager();
        imports.addEntityImports(APIGEN_CORE_PKG);

        StringBuilder fields = new StringBuilder();

        // Generate fields for business columns
        for (SqlColumn column : table.getBusinessColumns()) {
            imports.addImportsForColumn(column);
            fields.append(generateColumnField(column));
        }

        // Generate ManyToOne/OneToOne relationships (outgoing)
        for (SqlSchema.TableRelationship rel : outgoingRelations) {
            String targetEntity = rel.getTargetTable().getEntityName();
            String targetModule = rel.getTargetTable().getModuleName();
            String fieldName = rel.getForeignKey().getJavaFieldName();

            imports.addImportForClass(
                    basePackage + "." + targetModule + DOMAIN_ENTITY_SUFFIX + targetEntity);

            if (rel.getRelationType() == RelationType.ONE_TO_ONE) {
                fields.append(
                        """

                        @OneToOne(fetch = FetchType.LAZY)
                        @JoinColumn(name = "%s", unique = true)
                        private %s %s;\
                        """
                                .formatted(
                                        rel.getForeignKey().getColumnName(),
                                        targetEntity,
                                        fieldName));
            } else {
                fields.append(
                        """

                        @ManyToOne(fetch = FetchType.LAZY)
                        @JoinColumn(name = "%s")
                        private %s %s;\
                        """
                                .formatted(
                                        rel.getForeignKey().getColumnName(),
                                        targetEntity,
                                        fieldName));
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
            imports.addListImport();
            imports.addArrayListImport();

            // Using PERSIST and MERGE instead of ALL to prevent accidental cascading deletes
            // Developers should explicitly manage REMOVE operations
            fields.append(
                    """

                    @OneToMany(mappedBy = "%s", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
                    @Builder.Default
                    private List<%s> %s = new ArrayList<>();\
                    """
                            .formatted(mappedBy, sourceEntity, fieldName));
        }

        // Generate ManyToMany relationships
        for (ManyToManyRelation rel : manyToManyRelations) {
            String targetEntity = rel.targetTable().getEntityName();
            String targetModule = rel.targetTable().getModuleName();
            String fieldName = pluralize(rel.targetTable().getEntityVariableName());

            imports.addImportForClass(
                    basePackage + "." + targetModule + DOMAIN_ENTITY_SUFFIX + targetEntity);
            imports.addListImport();
            imports.addArrayListImport();

            fields.append(
                    """

                    @ManyToMany
                    @JoinTable(
                        name = "%s",
                        joinColumns = @JoinColumn(name = "%s"),
                        inverseJoinColumns = @JoinColumn(name = "%s")
                    )
                    @Builder.Default
                    private List<%s> %s = new ArrayList<>();\
                    """
                            .formatted(
                                    rel.junctionTable(),
                                    rel.sourceColumn(),
                                    rel.targetColumn(),
                                    targetEntity,
                                    fieldName));
        }

        // Generate index annotations
        StringBuilder indexAnnotations = new StringBuilder();
        if (!table.getIndexes().isEmpty()) {
            indexAnnotations
                    .append("@Table(name = \"")
                    .append(table.getName())
                    .append("\", indexes = {\n");
            List<String> indexStrings =
                    table.getIndexes().stream().map(SqlIndex::toJpaAnnotation).toList();
            indexAnnotations.append("    ").append(String.join(",\n    ", indexStrings));
            indexAnnotations.append("\n})");
        } else {
            indexAnnotations.append("@Table(name = \"").append(table.getName()).append("\")");
        }

        return
"""
package %s.%s.domain.entity;

%s

@Entity
%s
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class %s extends Base {
%s
}
"""
                .formatted(
                        basePackage,
                        moduleName,
                        imports.build(),
                        indexAnnotations,
                        entityName,
                        fields);
    }

    private String generateColumnField(SqlColumn column) {
        StringBuilder field = new StringBuilder("\n");

        String validations = column.getValidationAnnotations();
        if (!validations.isEmpty()) {
            field.append("    ").append(validations).append("\n");
        }

        field.append("    @Column(name = \"")
                .append(toSnakeCase(column.getJavaFieldName()))
                .append("\"");
        if (!column.isNullable()) field.append(", nullable = false");
        if (column.isUnique()) field.append(", unique = true");
        if (column.getLength() != null && "String".equals(column.getJavaType())) {
            field.append(", length = ").append(column.getLength());
        }
        field.append(")\n");

        field.append("    private ")
                .append(column.getJavaType())
                .append(" ")
                .append(safeFieldName(column.getJavaFieldName()))
                .append(";");

        return field.toString();
    }

    /** Represents a many-to-many relationship through a junction table. */
    public record ManyToManyRelation(
            String junctionTable, String sourceColumn, String targetColumn, SqlTable targetTable) {}
}
