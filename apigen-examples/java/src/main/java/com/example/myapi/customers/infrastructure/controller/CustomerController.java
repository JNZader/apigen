package com.example.myapi.customers.infrastructure.controller;

import com.jnzader.apigen.core.infrastructure.controller.BaseController;
import com.example.myapi.customers.application.dto.CustomerDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Customers", description = "Customer management API")
@RequestMapping("/api/v1/customers")
public interface CustomerController extends BaseController<CustomerDTO, Long> {

    // Add custom endpoints here
}
