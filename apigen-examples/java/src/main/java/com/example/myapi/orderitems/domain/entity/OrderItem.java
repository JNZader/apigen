package com.example.myapi.orderitems.domain.entity;

import com.example.myapi.orders.domain.entity.Order;
import com.example.myapi.products.domain.entity.Product;
import com.jnzader.apigen.core.domain.entity.Base;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.*;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "order_items")
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem extends Base {

    @NotNull
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    @NotNull
    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "order_id")
private Order order;
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "product_id")
private Product product;
}
