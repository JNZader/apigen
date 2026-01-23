package com.example.myapi.orders.infrastructure.controller;

import com.jnzader.apigen.core.infrastructure.controller.BaseController;
import com.example.myapi.orders.application.dto.OrderDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Orders", description = "Order management API")
@RequestMapping("/api/v1/orders")
public interface OrderController extends BaseController<OrderDTO, Long> {

    // Add custom endpoints here
}
