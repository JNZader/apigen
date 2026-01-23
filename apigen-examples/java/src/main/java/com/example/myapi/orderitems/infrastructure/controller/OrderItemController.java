package com.example.myapi.orderitems.infrastructure.controller;

import com.jnzader.apigen.core.infrastructure.controller.BaseController;
import com.example.myapi.orderitems.application.dto.OrderItemDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "OrderItems", description = "OrderItem management API")
@RequestMapping("/api/v1/orderitems")
public interface OrderItemController extends BaseController<OrderItemDTO, Long> {

    // Add custom endpoints here
}
