package com.example.myapi.products.infrastructure.controller;

import com.jnzader.apigen.core.infrastructure.controller.BaseControllerImpl;
import com.example.myapi.products.application.dto.ProductDTO;
import com.example.myapi.products.application.mapper.ProductMapper;
import com.example.myapi.products.application.service.ProductService;
import com.example.myapi.products.domain.entity.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class ProductControllerImpl
        extends BaseControllerImpl<Product, ProductDTO, Long>
        implements ProductController {

    private final ProductService productService;
    private final ProductMapper productMapper;

    public ProductControllerImpl(ProductService service, ProductMapper mapper) {
        super(service, mapper);
        this.productService = service;
        this.productMapper = mapper;
    }
}
