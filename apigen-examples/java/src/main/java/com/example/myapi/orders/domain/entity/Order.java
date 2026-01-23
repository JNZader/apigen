package com.example.myapi.orders.domain.entity;

import com.example.myapi.customers.domain.entity.Customer;
import com.example.myapi.orderitems.domain.entity.OrderItem;
import com.jnzader.apigen.core.domain.entity.Base;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "orders")
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends Base {

    @NotBlank // @Unique
    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;
    @NotNull
    @Column(name = "total", nullable = false)
    private BigDecimal total;
    @NotBlank
    @Column(name = "status", nullable = false)
    private String status;
    @NotBlank
    @Column(name = "shipping_address", nullable = false)
    private String shippingAddress;
    @NotNull
    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate;
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "customer_id")
private Customer customer;
@OneToMany(mappedBy = "order", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
@Builder.Default
private List<OrderItem> orderItems = new ArrayList<>();
}
