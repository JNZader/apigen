package com.jnzader.apigen.codegen.generator.mapper;

import static com.jnzader.apigen.codegen.generator.util.CodeGenerationUtils.pluralize;

import com.jnzader.apigen.codegen.generator.entity.EntityGenerator.ManyToManyRelation;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.List;

/** Generates MapStruct Mapper interfaces from SQL table definitions. */
public class MapperGenerator {

    private static final String APIGEN_CORE_PKG = "com.jnzader.apigen.core";
    private static final String MAPPING_TARGET = "\n    @Mapping(target = \"";
    private static final String IGNORE_TRUE = "\", ignore = true)";

    private final String basePackage;

    public MapperGenerator(String basePackage) {
        this.basePackage = basePackage;
    }

    /** Generates the Mapper interface code. */
    public String generate(
            SqlTable table,
            List<SqlSchema.TableRelationship> relations,
            List<ManyToManyRelation> manyToManyRelations) {
        String entityName = table.getEntityName();
        String moduleName = table.getModuleName();

        StringBuilder toDTOMappings = new StringBuilder();
        StringBuilder toEntityMappings = new StringBuilder();

        // Generate mappings for ManyToOne/OneToOne relationships
        for (SqlSchema.TableRelationship rel : relations) {
            String fieldName = rel.getForeignKey().getJavaFieldName();
            String dtoFieldName = fieldName + "Id";

            // Entity -> DTO: Extract ID from relationship
            toDTOMappings
                    .append("\n    @Mapping(source = \"")
                    .append(fieldName)
                    .append(".id\", target = \"")
                    .append(dtoFieldName)
                    .append("\")");

            // DTO -> Entity: Ignore relationship (handled by service)
            toEntityMappings.append(MAPPING_TARGET).append(fieldName).append(IGNORE_TRUE);
        }

        // Generate mappings for ManyToMany relationships
        for (ManyToManyRelation rel : manyToManyRelations) {
            String fieldName = pluralize(rel.targetTable().getEntityVariableName());
            String dtoFieldName = fieldName + "Ids";

            // Entity -> DTO: Ignore collection IDs (not directly mappable)
            toDTOMappings.append(MAPPING_TARGET).append(dtoFieldName).append(IGNORE_TRUE);

            // DTO -> Entity: Ignore relationship (handled by service)
            toEntityMappings.append(MAPPING_TARGET).append(fieldName).append(IGNORE_TRUE);
        }

        // Only generate custom methods if there are relationships
        String methods = "";
        if (!relations.isEmpty() || !manyToManyRelations.isEmpty()) {
            methods =
"""

    @Override%s
    %sDTO toDTO(%s entity);

    @Override%s
    %s toEntity(%sDTO dto);
"""
                            .formatted(
                                    toDTOMappings,
                                    entityName,
                                    entityName,
                                    toEntityMappings,
                                    entityName,
                                    entityName);
        }

        return
"""
package %s.%s.application.mapper;

import %s.application.mapper.BaseMapper;
import %s.%s.application.dto.%sDTO;
import %s.%s.domain.entity.%s;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface %sMapper extends BaseMapper<%s, %sDTO> {
    // Inherits toDTO, toEntity, updateEntityFromDTO from BaseMapper
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
