package com.jnzader.example.application.mapper;

import com.jnzader.apigen.core.application.mapper.BaseMapper;
import com.jnzader.example.application.dto.ProductDTO;
import com.jnzader.example.domain.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.BeanMapping;

/**
 * MapStruct mapper for Product entity.
 * <p>
 * Extends {@link BaseMapper} which provides:
 * <ul>
 *     <li>toDTO(entity) - Convert entity to DTO</li>
 *     <li>toEntity(dto) - Convert DTO to entity</li>
 *     <li>toDTOList(entities) - Convert list of entities</li>
 *     <li>toEntityList(dtos) - Convert list of DTOs</li>
 *     <li>updateEntityFromDTO(dto, entity) - Partial update for PATCH</li>
 * </ul>
 * <p>
 * The 'estado' field in entity is automatically mapped to 'activo' in DTO.
 * <p>
 * Note: Since ProductDTO is a record (immutable), updateDTOFromEntity is not applicable.
 */
@Mapper(componentModel = "spring")
public interface ProductMapper extends BaseMapper<Product, ProductDTO> {

    @Override
    @Mapping(source = "estado", target = "activo")
    ProductDTO toDTO(Product entity);

    @Override
    @Mapping(source = "activo", target = "estado")
    Product toEntity(ProductDTO dto);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "activo", target = "estado")
    void updateEntityFromDTO(ProductDTO dto, @MappingTarget Product entity);

    /**
     * Not applicable for records (immutable).
     * This implementation does nothing as records cannot be modified.
     */
    @Override
    default void updateDTOFromEntity(Product entity, @MappingTarget ProductDTO dto) {
        // Records are immutable - this operation is not supported
        // Use toDTO() to create a new DTO instance instead
    }
}
