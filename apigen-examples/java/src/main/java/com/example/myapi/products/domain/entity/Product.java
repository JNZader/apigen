package com.example.myapi.products.domain.entity;

import com.example.myapi.categories.domain.entity.Category;
import com.example.myapi.orderitems.domain.entity.OrderItem;
import com.example.myapi.reviews.domain.entity.Review;
import com.jnzader.apigen.core.domain.entity.Base;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "products")
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends Base {

    @NotBlank
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "description")
    private String description;
    @NotNull
    @Column(name = "price", nullable = false)
    private BigDecimal price;
    @NotNull
    @Column(name = "stock", nullable = false)
    private Integer stock;
    @NotBlank // @Unique
    @Column(name = "sku", nullable = false, unique = true)
    private String sku;
    @Column(name = "image_url")
    private String imageUrl;
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "category_id")
private Category category;
@OneToMany(mappedBy = "product", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
@Builder.Default
private List<OrderItem> orderItems = new ArrayList<>();
@OneToMany(mappedBy = "product", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
@Builder.Default
private List<Review> reviews = new ArrayList<>();
}
