package com.jnzader.example.application.dto;

import com.jnzader.apigen.core.application.dto.BaseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Producto del catálogo")
public record ProductDTO(
        @Schema(
                        description = "ID único del producto",
                        example = "1",
                        accessMode = Schema.AccessMode.READ_ONLY)
                Long id,
        @Schema(description = "Estado activo del producto", example = "true", defaultValue = "true")
                Boolean activo,
        @Schema(
                        description = "Nombre del producto",
                        example = "MacBook Pro 16\"",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank(message = "Product name is required")
                String name,
        @Schema(
                        description = "Descripción detallada del producto",
                        example = "Laptop profesional con chip M3 Pro, 18GB RAM, 512GB SSD")
                String description,
        @Schema(
                        description = "Precio unitario",
                        example = "2499.99",
                        minimum = "0.01",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                @Positive(message = "Price must be positive")
                BigDecimal price,
        @Schema(description = "Cantidad en stock", example = "50", minimum = "0")
                @PositiveOrZero(message = "Stock cannot be negative")
                Integer stock,
        @Schema(description = "Categoría del producto", example = "Electronics") String category,
        @Schema(description = "Código SKU único", example = "MBP-16-M3-512") String sku)
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
