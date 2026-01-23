package com.example.myapi.products.infrastructure.controller;

import com.jnzader.apigen.core.infrastructure.controller.BaseController;
import com.example.myapi.products.application.dto.ProductDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Products", description = "Product management API")
@RequestMapping("/api/v1/products")
public interface ProductController extends BaseController<ProductDTO, Long> {

    // Add custom endpoints here
}
