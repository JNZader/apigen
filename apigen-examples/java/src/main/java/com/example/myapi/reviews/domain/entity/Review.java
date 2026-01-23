package com.example.myapi.reviews.domain.entity;

import com.example.myapi.customers.domain.entity.Customer;
import com.example.myapi.products.domain.entity.Product;
import com.jnzader.apigen.core.domain.entity.Base;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "reviews")
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review extends Base {

    @NotNull
    @Column(name = "rating", nullable = false)
    private Integer rating;
    @Column(name = "comment")
    private String comment;
    @NotNull
    @Column(name = "review_date", nullable = false)
    private LocalDateTime reviewDate;
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "product_id")
private Product product;
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "customer_id")
private Customer customer;
}
