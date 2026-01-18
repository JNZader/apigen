package com.jnzader.example.infrastructure.controller;

import com.jnzader.apigen.core.domain.specification.FilterSpecificationBuilder;
import com.jnzader.apigen.core.infrastructure.controller.BaseControllerImpl;
import com.jnzader.example.application.dto.ProductDTO;
import com.jnzader.example.application.mapper.ProductMapper;
import com.jnzader.example.application.service.ProductService;
import com.jnzader.example.domain.entity.Product;
import com.jnzader.example.infrastructure.hateoas.ProductResourceAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for Product entity.
 * <p>
 * Extends {@link BaseControllerImpl} which provides:
 * <ul>
 *     <li>GET / - List with pagination, filtering, and sparse fieldsets</li>
 *     <li>GET /{id} - Get by ID with ETag caching</li>
 *     <li>HEAD / - Count</li>
 *     <li>HEAD /{id} - Check existence</li>
 *     <li>POST / - Create with Location header</li>
 *     <li>PUT /{id} - Full update with optimistic locking</li>
 *     <li>PATCH /{id} - Partial update</li>
 *     <li>DELETE /{id} - Soft delete (or hard with ?permanent=true)</li>
 *     <li>POST /{id}/restore - Restore soft-deleted</li>
 *     <li>GET /cursor - Cursor-based pagination</li>
 * </ul>
 * <p>
 * Filtering examples:
 * <ul>
 *     <li>GET /api/products?filter=name:like:phone</li>
 *     <li>GET /api/products?filter=price:gte:100,category:eq:electronics</li>
 *     <li>GET /api/products?fields=id,name,price</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Product management API")
public class ProductController extends BaseControllerImpl<Product, ProductDTO, Long> {

    private final ProductService productService;
    private final ProductMapper productMapper;

    public ProductController(
            ProductService service,
            ProductMapper mapper,
            ProductResourceAssembler assembler,
            FilterSpecificationBuilder filterBuilder
    ) {
        super(service, mapper, assembler, filterBuilder);
        this.productService = service;
        this.productMapper = mapper;
    }

    @Override
    protected String getResourceName() {
        return "Product";
    }

    @Override
    protected Class<Product> getEntityClass() {
        return Product.class;
    }

    // ==================== Custom Endpoints ====================

    /**
     * Find products by category.
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<ProductDTO>> findByCategory(@PathVariable String category) {
        List<Product> products = productService.findByCategory(category);
        return ResponseEntity.ok(products.stream()
                .map(productMapper::toDTO)
                .toList());
    }

    /**
     * Search products by name.
     */
    @GetMapping("/search")
    public ResponseEntity<List<ProductDTO>> searchByName(@RequestParam String name) {
        List<Product> products = productService.searchByName(name);
        return ResponseEntity.ok(products.stream()
                .map(productMapper::toDTO)
                .toList());
    }
}
