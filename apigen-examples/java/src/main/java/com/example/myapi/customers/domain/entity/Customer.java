package com.example.myapi.customers.domain.entity;

import com.example.myapi.orders.domain.entity.Order;
import com.example.myapi.reviews.domain.entity.Review;
import com.jnzader.apigen.core.domain.entity.Base;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "customers")
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer extends Base {

    @NotBlank // @Unique
    @Column(name = "email", nullable = false, unique = true)
    private String email;
    @NotBlank
    @Column(name = "first_name", nullable = false)
    private String firstName;
    @NotBlank
    @Column(name = "last_name", nullable = false)
    private String lastName;
    @Column(name = "phone")
    private String phone;
    @Column(name = "address")
    private String address;
@OneToMany(mappedBy = "customer", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
@Builder.Default
private List<Order> orders = new ArrayList<>();
@OneToMany(mappedBy = "customer", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
@Builder.Default
private List<Review> reviews = new ArrayList<>();
}
