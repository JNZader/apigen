package com.example.myapi.products.infrastructure.repository;

import com.jnzader.apigen.core.domain.repository.BaseRepository;
import com.example.myapi.products.domain.entity.Product;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ProductRepository extends BaseRepository<Product, Long> {

    // Custom query methods

    Optional<Product> findBySku(String sku);
}
