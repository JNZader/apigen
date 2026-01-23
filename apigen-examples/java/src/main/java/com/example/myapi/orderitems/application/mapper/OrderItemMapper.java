package com.example.myapi.orderitems.application.mapper;

import com.jnzader.apigen.core.application.mapper.BaseMapper;
import com.example.myapi.orderitems.application.dto.OrderItemDTO;
import com.example.myapi.orderitems.domain.entity.OrderItem;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface OrderItemMapper extends BaseMapper<OrderItem, OrderItemDTO> {
    // Inherits toDTO, toEntity, updateEntityFromDTO, updateDTOFromEntity from BaseMapper
    // MapStruct will generate implementations automatically
    @Override
    @Mapping(source = "estado", target = "activo")
    @Mapping(source = "order.id", target = "orderId")
    @Mapping(source = "product.id", target = "productId")
    OrderItemDTO toDTO(OrderItem entity);

    @Override
    @InheritInverseConfiguration(name = "toDTO")
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "product", ignore = true)
    OrderItem toEntity(OrderItemDTO dto);

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
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "product", ignore = true)
    void updateEntityFromDTO(OrderItemDTO dto, @MappingTarget OrderItem entity);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "estado", target = "activo")
    @Mapping(source = "order.id", target = "orderId")
    @Mapping(source = "product.id", target = "productId")
    void updateDTOFromEntity(OrderItem entity, @MappingTarget OrderItemDTO dto);

}
