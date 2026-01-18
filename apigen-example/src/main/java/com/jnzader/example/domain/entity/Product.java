package com.jnzader.example.domain.entity;

import com.jnzader.apigen.core.domain.entity.Base;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Example Product entity demonstrating APiGen's Base entity usage.
 * <p>
 * Inherits from {@link Base} which provides:
 * <ul>
 *     <li>ID generation with sequence</li>
 *     <li>Soft delete (estado, fechaEliminacion, eliminadoPor)</li>
 *     <li>Auditing (creadoPor, modificadoPor, fechaCreacion, fechaActualizacion)</li>
 *     <li>Optimistic locking (version)</li>
 *     <li>Domain events support</li>
 * </ul>
 * <p>
 * Note: equals/hashCode are overridden to use the entity ID for JPA entity identity,
 * following best practices for Hibernate entities. This ensures consistency across
 * detached/attached entity states and collection operations.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product extends Base {

    @NotBlank(message = "Product name is required")
    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 1000)
    private String description;

    @Positive(message = "Price must be positive")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @PositiveOrZero(message = "Stock cannot be negative")
    @Column(nullable = false)
    private Integer stock = 0;

    @Column(length = 100)
    private String category;

    @Column(length = 50)
    private String sku;

    public Product(String name, BigDecimal price) {
        this.name = name;
        this.price = price;
    }

    public Product(String name, String description, BigDecimal price, Integer stock) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
    }

    /**
     * Compares this Product to another object for equality.
     * Uses ID-based equality for JPA entity identity consistency.
     * Two products are equal if they have the same non-null ID.
     *
     * @param o the object to compare
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        // Use ID for entity identity; return false if either ID is null (transient entities)
        return getId() != null && Objects.equals(getId(), product.getId());
    }

    /**
     * Returns a hash code for this Product.
     * Uses a constant hash code to ensure consistency across entity state transitions.
     * This follows Hibernate best practices for entities that may be in Sets.
     *
     * @return a constant hash code value
     */
    @Override
    public int hashCode() {
        // Use a constant hashCode for JPA entities to ensure consistency
        // when the entity transitions between transient and persistent states
        return getClass().hashCode();
    }
}
