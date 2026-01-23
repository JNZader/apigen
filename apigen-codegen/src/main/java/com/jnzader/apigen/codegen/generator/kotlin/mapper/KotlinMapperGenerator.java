package com.jnzader.apigen.codegen.generator.kotlin.mapper;

import static com.jnzader.apigen.codegen.generator.util.CodeGenerationUtils.pluralize;

import com.jnzader.apigen.codegen.generator.common.ManyToManyRelation;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.List;

/** Generates MapStruct Mapper interfaces for Kotlin/Spring Boot. */
public class KotlinMapperGenerator {

    private static final String APIGEN_CORE_PKG = "com.jnzader.apigen.core";
    private static final String MAPPING_TARGET = "\n    @Mapping(target = \"";
    private static final String MAPPING_SOURCE = "\n    @Mapping(source = \"";
    private static final String IGNORE_TRUE = "\", ignore = true)";

    // Audit fields from Base class that should be ignored in updateEntityFromDTO
    private static final String AUDIT_FIELD_IGNORES =
            MAPPING_TARGET
                    + "fechaCreacion"
                    + IGNORE_TRUE
                    + MAPPING_TARGET
                    + "fechaActualizacion"
                    + IGNORE_TRUE
                    + MAPPING_TARGET
                    + "fechaEliminacion"
                    + IGNORE_TRUE
                    + MAPPING_TARGET
                    + "eliminadoPor"
                    + IGNORE_TRUE
                    + MAPPING_TARGET
                    + "creadoPor"
                    + IGNORE_TRUE
                    + MAPPING_TARGET
                    + "modificadoPor"
                    + IGNORE_TRUE
                    + MAPPING_TARGET
                    + "version"
                    + IGNORE_TRUE
                    + MAPPING_TARGET
                    + "domainEvents"
                    + IGNORE_TRUE;

    private final String basePackage;

    public KotlinMapperGenerator(String basePackage) {
        this.basePackage = basePackage;
    }

    /** Generates the Mapper interface code in Kotlin. */
    public String generate(
            SqlTable table,
            List<SqlSchema.TableRelationship> outgoingRelations,
            List<SqlSchema.TableRelationship> incomingRelations,
            List<ManyToManyRelation> manyToManyRelations) {
        String entityName = table.getEntityName();
        String moduleName = table.getModuleName();

        StringBuilder toDTOMappings = new StringBuilder();
        StringBuilder toEntityMappings = new StringBuilder();
        StringBuilder updateEntityMappings = new StringBuilder();
        StringBuilder updateDTOMappings = new StringBuilder();

        // Generate mappings for ManyToOne/OneToOne relationships (outgoing)
        for (SqlSchema.TableRelationship rel : outgoingRelations) {
            String fieldName = rel.getForeignKey().getJavaFieldName();
            String dtoFieldName = fieldName + "Id";

            // Entity -> DTO: Extract ID from relationship
            toDTOMappings
                    .append(MAPPING_SOURCE)
                    .append(fieldName)
                    .append(".id\", target = \"")
                    .append(dtoFieldName)
                    .append("\")");

            // DTO -> Entity: Ignore relationship (handled by service)
            toEntityMappings.append(MAPPING_TARGET).append(fieldName).append(IGNORE_TRUE);

            // updateEntityFromDTO: Ignore relationship
            updateEntityMappings.append(MAPPING_TARGET).append(fieldName).append(IGNORE_TRUE);

            // updateDTOFromEntity: Map relationship ID
            updateDTOMappings
                    .append(MAPPING_SOURCE)
                    .append(fieldName)
                    .append(".id\", target = \"")
                    .append(dtoFieldName)
                    .append("\")");
        }

        // Generate mappings for inverse OneToMany relationships (incoming)
        for (SqlSchema.TableRelationship rel : incomingRelations) {
            String sourceEntityVar = rel.getSourceTable().getEntityVariableName();
            String fieldName = pluralize(sourceEntityVar);

            // toEntity: Ignore inverse collections
            toEntityMappings.append(MAPPING_TARGET).append(fieldName).append(IGNORE_TRUE);

            // updateEntityFromDTO: Ignore inverse collections
            updateEntityMappings.append(MAPPING_TARGET).append(fieldName).append(IGNORE_TRUE);
        }

        // Generate mappings for ManyToMany relationships
        for (ManyToManyRelation rel : manyToManyRelations) {
            String fieldName = pluralize(rel.targetTable().getEntityVariableName());
            String dtoFieldName = fieldName + "Ids";

            // Entity -> DTO: Ignore collection IDs (not directly mappable)
            toDTOMappings.append(MAPPING_TARGET).append(dtoFieldName).append(IGNORE_TRUE);

            // DTO -> Entity: Ignore relationship (handled by service)
            toEntityMappings.append(MAPPING_TARGET).append(fieldName).append(IGNORE_TRUE);

            // updateEntityFromDTO: Ignore relationship
            updateEntityMappings.append(MAPPING_TARGET).append(fieldName).append(IGNORE_TRUE);

            // updateDTOFromEntity: Ignore collection IDs
            updateDTOMappings.append(MAPPING_TARGET).append(dtoFieldName).append(IGNORE_TRUE);
        }

        // Build methods section - always generate if there are any relationships
        boolean hasRelationships =
                !outgoingRelations.isEmpty()
                        || !incomingRelations.isEmpty()
                        || !manyToManyRelations.isEmpty();

        String methods = "";
        if (hasRelationships) {
            methods =
"""

    @Mapping(source = "estado", target = "activo")%s
    override fun toDTO(entity: %s): %sDTO

    @InheritInverseConfiguration(name = "toDTO")%s
    override fun toEntity(dto: %sDTO): %s

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "activo", target = "estado")%s
    override fun updateEntityFromDTO(dto: %sDTO, @MappingTarget entity: %s)

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "estado", target = "activo")%s
    override fun updateDTOFromEntity(entity: %s, @MappingTarget dto: %sDTO)
"""
                            .formatted(
                                    toDTOMappings,
                                    entityName,
                                    entityName,
                                    toEntityMappings,
                                    entityName,
                                    entityName,
                                    AUDIT_FIELD_IGNORES + updateEntityMappings,
                                    entityName,
                                    entityName,
                                    updateDTOMappings,
                                    entityName,
                                    entityName);
        }

        return
"""
package %s.%s.application.mapper

import %s.application.mapper.BaseMapper
import %s.%s.application.dto.%sDTO
import %s.%s.domain.entity.%s
import org.mapstruct.*

@Mapper(componentModel = "spring")
interface %sMapper : BaseMapper<%s, %sDTO> {
    // Inherits toDTO, toEntity, updateEntityFromDTO, updateDTOFromEntity from BaseMapper
    // MapStruct will generate implementations automatically%s
}
"""
                .formatted(
                        basePackage,
                        moduleName,
                        APIGEN_CORE_PKG,
                        basePackage,
                        moduleName,
                        entityName,
                        basePackage,
                        moduleName,
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        methods);
    }
}
