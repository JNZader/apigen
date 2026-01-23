package com.example.myapi.orders.infrastructure.repository;

import com.jnzader.apigen.core.domain.repository.BaseRepository;
import com.example.myapi.orders.domain.entity.Order;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OrderRepository extends BaseRepository<Order, Long> {

    // Custom query methods

    Optional<Order> findByOrderNumber(String orderNumber);
}
