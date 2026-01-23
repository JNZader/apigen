package com.example.myapi.products.application.service;

import com.jnzader.apigen.core.application.service.BaseServiceImpl;
import com.jnzader.apigen.core.application.service.CacheEvictionService;
import com.example.myapi.products.domain.entity.Product;
import com.example.myapi.products.infrastructure.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional
public class ProductServiceImpl
        extends BaseServiceImpl<Product, Long>
        implements ProductService {

    public ProductServiceImpl(
            ProductRepository repository,
            CacheEvictionService cacheEvictionService,
            ApplicationEventPublisher eventPublisher,
            AuditorAware<String> auditorAware) {
        super(repository, cacheEvictionService, eventPublisher, auditorAware);
    }

    @Override
    protected Class<Product> getEntityClass() {
        return Product.class;
    }
}
