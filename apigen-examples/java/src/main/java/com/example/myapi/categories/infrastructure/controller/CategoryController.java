package com.example.myapi.categories.infrastructure.controller;

import com.jnzader.apigen.core.infrastructure.controller.BaseController;
import com.example.myapi.categories.application.dto.CategoryDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Categorys", description = "Category management API")
@RequestMapping("/api/v1/categories")
public interface CategoryController extends BaseController<CategoryDTO, Long> {

    // Add custom endpoints here
}
