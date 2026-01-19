package com.jnzader.example.application.dto;

import com.jnzader.apigen.core.application.dto.BaseDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Product Data Transfer Object.
 *
 * <p>Implements {@link BaseDTO} which requires id() and activo() methods. Using Java records for
 * immutability and automatic equals/hashCode/toString.
 */
public record ProductDTO(
        Long id,
        Boolean activo,
        @NotBlank(message = "Product name is required") String name,
        String description,
        @Positive(message = "Price must be positive") BigDecimal price,
        @PositiveOrZero(message = "Stock cannot be negative") Integer stock,
        String category,
        String sku)
        implements BaseDTO {

    /** Factory method for creating a ProductDTO. */
    public static ProductDTO of(
            Long id,
            Boolean activo,
            String name,
            String description,
            BigDecimal price,
            Integer stock,
            String category,
            String sku) {
        return new ProductDTO(id, activo, name, description, price, stock, category, sku);
    }

    /** Factory method for creating a new product (without id). */
    public static ProductDTO create(
            String name, String description, BigDecimal price, Integer stock) {
        return new ProductDTO(null, true, name, description, price, stock, null, null);
    }
}
