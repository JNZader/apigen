package com.example.myapi.reviews.infrastructure.repository;

import com.jnzader.apigen.core.domain.repository.BaseRepository;
import com.example.myapi.reviews.domain.entity.Review;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends BaseRepository<Review, Long> {

    // Custom query methods
}
