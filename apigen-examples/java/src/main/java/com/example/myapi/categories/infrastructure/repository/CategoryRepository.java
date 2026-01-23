package com.example.myapi.categories.infrastructure.repository;

import com.jnzader.apigen.core.domain.repository.BaseRepository;
import com.example.myapi.categories.domain.entity.Category;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CategoryRepository extends BaseRepository<Category, Long> {

    // Custom query methods

    Optional<Category> findByName(String name);

    Optional<Category> findBySlug(String slug);
}
