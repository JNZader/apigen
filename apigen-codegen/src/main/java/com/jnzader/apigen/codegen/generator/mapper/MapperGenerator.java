package com.jnzader.apigen.codegen.generator.mapper;

import com.jnzader.apigen.codegen.model.SqlTable;

/**
 * Generates MapStruct Mapper interfaces from SQL table definitions.
 */
public class MapperGenerator {

    private static final String APIGEN_CORE_PKG = "com.jnzader.apigen.core";

    private final String basePackage;

    public MapperGenerator(String basePackage) {
        this.basePackage = basePackage;
    }

    /**
     * Generates the Mapper interface code.
     */
    public String generate(SqlTable table) {
        String entityName = table.getEntityName();
        String moduleName = table.getModuleName();

        return """
package %s.%s.application.mapper;

import %s.application.mapper.BaseMapper;
import %s.%s.application.dto.%sDTO;
import %s.%s.domain.entity.%s;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface %sMapper extends BaseMapper<%s, %sDTO> {
    // Inherits toDTO, toEntity, updateEntityFromDTO from BaseMapper
    // MapStruct will generate implementations automatically
}
""".formatted(
                basePackage, moduleName,
                APIGEN_CORE_PKG, basePackage, moduleName, entityName,
                basePackage, moduleName, entityName,
                entityName, entityName, entityName
        );
    }
}
