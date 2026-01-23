package com.example.myapi.orders.infrastructure.controller;

import com.jnzader.apigen.core.infrastructure.controller.BaseControllerImpl;
import com.example.myapi.orders.application.dto.OrderDTO;
import com.example.myapi.orders.application.mapper.OrderMapper;
import com.example.myapi.orders.application.service.OrderService;
import com.example.myapi.orders.domain.entity.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class OrderControllerImpl
        extends BaseControllerImpl<Order, OrderDTO, Long>
        implements OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    public OrderControllerImpl(OrderService service, OrderMapper mapper) {
        super(service, mapper);
        this.orderService = service;
        this.orderMapper = mapper;
    }
}
