package com.example.myapi.orderitems.infrastructure.controller;

import com.jnzader.apigen.core.infrastructure.controller.BaseControllerImpl;
import com.example.myapi.orderitems.application.dto.OrderItemDTO;
import com.example.myapi.orderitems.application.mapper.OrderItemMapper;
import com.example.myapi.orderitems.application.service.OrderItemService;
import com.example.myapi.orderitems.domain.entity.OrderItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class OrderItemControllerImpl
        extends BaseControllerImpl<OrderItem, OrderItemDTO, Long>
        implements OrderItemController {

    private final OrderItemService orderItemService;
    private final OrderItemMapper orderItemMapper;

    public OrderItemControllerImpl(OrderItemService service, OrderItemMapper mapper) {
        super(service, mapper);
        this.orderItemService = service;
        this.orderItemMapper = mapper;
    }
}
