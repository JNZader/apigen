package com.example.myapi.categories.application.mapper;

import com.jnzader.apigen.core.application.mapper.BaseMapper;
import com.example.myapi.categories.application.dto.CategoryDTO;
import com.example.myapi.categories.domain.entity.Category;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CategoryMapper extends BaseMapper<Category, CategoryDTO> {
    // Inherits toDTO, toEntity, updateEntityFromDTO, updateDTOFromEntity from BaseMapper
    // MapStruct will generate implementations automatically
    @Override
    @Mapping(source = "estado", target = "activo")
    CategoryDTO toDTO(Category entity);

    @Override
    @InheritInverseConfiguration(name = "toDTO")
    @Mapping(target = "products", ignore = true)
    Category toEntity(CategoryDTO dto);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "activo", target = "estado")
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "fechaEliminacion", ignore = true)
    @Mapping(target = "eliminadoPor", ignore = true)
    @Mapping(target = "creadoPor", ignore = true)
    @Mapping(target = "modificadoPor", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "domainEvents", ignore = true)
    @Mapping(target = "products", ignore = true)
    void updateEntityFromDTO(CategoryDTO dto, @MappingTarget Category entity);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "estado", target = "activo")
    void updateDTOFromEntity(Category entity, @MappingTarget CategoryDTO dto);

}
