package com.example.myapi.customers.application.mapper;

import com.jnzader.apigen.core.application.mapper.BaseMapper;
import com.example.myapi.customers.application.dto.CustomerDTO;
import com.example.myapi.customers.domain.entity.Customer;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CustomerMapper extends BaseMapper<Customer, CustomerDTO> {
    // Inherits toDTO, toEntity, updateEntityFromDTO, updateDTOFromEntity from BaseMapper
    // MapStruct will generate implementations automatically
    @Override
    @Mapping(source = "estado", target = "activo")
    CustomerDTO toDTO(Customer entity);

    @Override
    @InheritInverseConfiguration(name = "toDTO")
    @Mapping(target = "orders", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    Customer toEntity(CustomerDTO dto);

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
    @Mapping(target = "orders", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    void updateEntityFromDTO(CustomerDTO dto, @MappingTarget Customer entity);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "estado", target = "activo")
    void updateDTOFromEntity(Customer entity, @MappingTarget CustomerDTO dto);

}
