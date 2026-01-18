package com.jnzader.apigen.core.fixtures;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.BeanMapping;

import java.util.List;

/**
 * Test mapper for unit tests.
 * Custom implementation since BaseMapper expects mutable DTOs but we use records.
 */
@Mapper(componentModel = "spring")
public interface TestEntityMapper {

    @Mapping(source = "estado", target = "activo")
    TestEntityDTO toDTO(TestEntity entity);

    @Mapping(source = "activo", target = "estado")
    TestEntity toEntity(TestEntityDTO dto);

    List<TestEntityDTO> toDTOList(List<TestEntity> entities);

    List<TestEntity> toEntityList(List<TestEntityDTO> dtos);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "activo", target = "estado")
    void updateEntityFromDTO(TestEntityDTO dto, @MappingTarget TestEntity entity);
}
