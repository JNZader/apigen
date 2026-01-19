package com.jnzader.example.application.service;

import com.jnzader.apigen.core.application.service.BaseServiceImpl;
import com.jnzader.apigen.core.application.service.CacheEvictionService;
import com.jnzader.example.domain.entity.Product;
import com.jnzader.example.domain.repository.ProductRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;

/**
 * Service for Product entity.
 *
 * <p>Extends {@link BaseServiceImpl} which provides:
 *
 * <ul>
 *   <li>CRUD operations with Result pattern
 *   <li>Soft delete and restore
 *   <li>Hard delete
 *   <li>Batch operations
 *   <li>Cursor-based pagination
 *   <li>Specification-based queries
 *   <li>Caching support
 *   <li>Domain events
 * </ul>
 *
 * <p>Add custom business logic here.
 */
@Service
public class ProductService extends BaseServiceImpl<Product, Long> {

    private final ProductRepository productRepository;

    public ProductService(
            ProductRepository repository,
            CacheEvictionService cacheEvictionService,
            ApplicationEventPublisher eventPublisher,
            AuditorAware<String> auditorAware) {
        super(repository, cacheEvictionService, eventPublisher, auditorAware);
        this.productRepository = repository;
    }

    @Override
    protected Class<Product> getEntityClass() {
        return Product.class;
    }

    // ==================== Custom Business Methods ====================

    /** Find products by category. */
    public List<Product> findByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    /** Find product by SKU. */
    public Optional<Product> findBySku(String sku) {
        return productRepository.findBySku(sku);
    }

    /** Search products by name. */
    public List<Product> searchByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }

    /** Count products in a category. */
    public long countByCategory(String category) {
        return productRepository.countByCategory(category);
    }
}
