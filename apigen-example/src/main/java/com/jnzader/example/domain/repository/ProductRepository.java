package com.jnzader.example.domain.repository;

import com.jnzader.apigen.core.domain.repository.BaseRepository;
import com.jnzader.example.domain.entity.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Repository for Product entity.
 *
 * <p>Extends {@link BaseRepository} which provides:
 *
 * <ul>
 *   <li>All JpaRepository methods (save, findById, findAll, delete, etc.)
 *   <li>JpaSpecificationExecutor for dynamic queries
 *   <li>Soft delete operations (softDeleteAllByIds, restoreAllByIds)
 *   <li>Hard delete (hardDeleteById)
 * </ul>
 *
 * <p>Add custom query methods here using Spring Data JPA naming conventions or @Query annotations.
 */
@Repository
public interface ProductRepository extends BaseRepository<Product, Long> {

    /** Find products by category. */
    List<Product> findByCategory(String category);

    /** Find product by SKU. */
    Optional<Product> findBySku(String sku);

    /** Find products by name containing (case-insensitive). */
    List<Product> findByNameContainingIgnoreCase(String name);

    /** Count products in a category. */
    long countByCategory(String category);
}
