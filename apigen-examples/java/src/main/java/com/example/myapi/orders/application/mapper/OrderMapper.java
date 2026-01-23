package com.example.myapi.orders.application.mapper;

import com.jnzader.apigen.core.application.mapper.BaseMapper;
import com.example.myapi.orders.application.dto.OrderDTO;
import com.example.myapi.orders.domain.entity.Order;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface OrderMapper extends BaseMapper<Order, OrderDTO> {
    // Inherits toDTO, toEntity, updateEntityFromDTO, updateDTOFromEntity from BaseMapper
    // MapStruct will generate implementations automatically
    @Override
    @Mapping(source = "estado", target = "activo")
    @Mapping(source = "customer.id", target = "customerId")
    OrderDTO toDTO(Order entity);

    @Override
    @InheritInverseConfiguration(name = "toDTO")
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "orderItems", ignore = true)
    Order toEntity(OrderDTO dto);

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
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "orderItems", ignore = true)
    void updateEntityFromDTO(OrderDTO dto, @MappingTarget Order entity);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "estado", target = "activo")
    @Mapping(source = "customer.id", target = "customerId")
    void updateDTOFromEntity(Order entity, @MappingTarget OrderDTO dto);

}
