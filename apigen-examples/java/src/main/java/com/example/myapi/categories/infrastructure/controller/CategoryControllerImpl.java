package com.example.myapi.categories.infrastructure.controller;

import com.jnzader.apigen.core.infrastructure.controller.BaseControllerImpl;
import com.example.myapi.categories.application.dto.CategoryDTO;
import com.example.myapi.categories.application.mapper.CategoryMapper;
import com.example.myapi.categories.application.service.CategoryService;
import com.example.myapi.categories.domain.entity.Category;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class CategoryControllerImpl
        extends BaseControllerImpl<Category, CategoryDTO, Long>
        implements CategoryController {

    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;

    public CategoryControllerImpl(CategoryService service, CategoryMapper mapper) {
        super(service, mapper);
        this.categoryService = service;
        this.categoryMapper = mapper;
    }
}
