package com.example.myapi.orderitems.application.service;

import com.jnzader.apigen.core.application.service.BaseServiceImpl;
import com.jnzader.apigen.core.application.service.CacheEvictionService;
import com.example.myapi.orderitems.domain.entity.OrderItem;
import com.example.myapi.orderitems.infrastructure.repository.OrderItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional
public class OrderItemServiceImpl
        extends BaseServiceImpl<OrderItem, Long>
        implements OrderItemService {

    public OrderItemServiceImpl(
            OrderItemRepository repository,
            CacheEvictionService cacheEvictionService,
            ApplicationEventPublisher eventPublisher,
            AuditorAware<String> auditorAware) {
        super(repository, cacheEvictionService, eventPublisher, auditorAware);
    }

    @Override
    protected Class<OrderItem> getEntityClass() {
        return OrderItem.class;
    }
}
