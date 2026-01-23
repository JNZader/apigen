package com.example.myapi.products.application.mapper;

import com.jnzader.apigen.core.application.mapper.BaseMapper;
import com.example.myapi.products.application.dto.ProductDTO;
import com.example.myapi.products.domain.entity.Product;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ProductMapper extends BaseMapper<Product, ProductDTO> {
    // Inherits toDTO, toEntity, updateEntityFromDTO, updateDTOFromEntity from BaseMapper
    // MapStruct will generate implementations automatically
    @Override
    @Mapping(source = "estado", target = "activo")
    @Mapping(source = "category.id", target = "categoryId")
    ProductDTO toDTO(Product entity);

    @Override
    @InheritInverseConfiguration(name = "toDTO")
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "orderItems", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    Product toEntity(ProductDTO dto);

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
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "orderItems", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    void updateEntityFromDTO(ProductDTO dto, @MappingTarget Product entity);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "estado", target = "activo")
    @Mapping(source = "category.id", target = "categoryId")
    void updateDTOFromEntity(Product entity, @MappingTarget ProductDTO dto);

}
