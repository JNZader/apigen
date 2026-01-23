package com.example.myapi.orderitems.infrastructure.repository;

import com.jnzader.apigen.core.domain.repository.BaseRepository;
import com.example.myapi.orderitems.domain.entity.OrderItem;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends BaseRepository<OrderItem, Long> {

    // Custom query methods
}
