package com.example.myapi.categories.domain.entity;

import com.example.myapi.products.domain.entity.Product;
import com.jnzader.apigen.core.domain.entity.Base;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "categories")
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category extends Base {

    @NotBlank // @Unique
    @Column(name = "name", nullable = false, unique = true)
    private String name;
    @Column(name = "description")
    private String description;
    @NotBlank // @Unique
    @Column(name = "slug", nullable = false, unique = true)
    private String slug;
@OneToMany(mappedBy = "category", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
@Builder.Default
private List<Product> products = new ArrayList<>();
}
