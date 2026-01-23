package com.example.myapi.reviews.infrastructure.controller;

import com.jnzader.apigen.core.infrastructure.controller.BaseController;
import com.example.myapi.reviews.application.dto.ReviewDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Reviews", description = "Review management API")
@RequestMapping("/api/v1/reviews")
public interface ReviewController extends BaseController<ReviewDTO, Long> {

    // Add custom endpoints here
}
