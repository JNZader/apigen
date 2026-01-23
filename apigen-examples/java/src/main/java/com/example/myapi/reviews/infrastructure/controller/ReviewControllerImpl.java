package com.example.myapi.reviews.infrastructure.controller;

import com.jnzader.apigen.core.infrastructure.controller.BaseControllerImpl;
import com.example.myapi.reviews.application.dto.ReviewDTO;
import com.example.myapi.reviews.application.mapper.ReviewMapper;
import com.example.myapi.reviews.application.service.ReviewService;
import com.example.myapi.reviews.domain.entity.Review;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class ReviewControllerImpl
        extends BaseControllerImpl<Review, ReviewDTO, Long>
        implements ReviewController {

    private final ReviewService reviewService;
    private final ReviewMapper reviewMapper;

    public ReviewControllerImpl(ReviewService service, ReviewMapper mapper) {
        super(service, mapper);
        this.reviewService = service;
        this.reviewMapper = mapper;
    }
}
