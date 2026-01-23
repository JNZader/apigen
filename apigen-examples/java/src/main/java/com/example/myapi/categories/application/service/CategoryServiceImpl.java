package com.example.myapi.categories.application.service;

import com.jnzader.apigen.core.application.service.BaseServiceImpl;
import com.jnzader.apigen.core.application.service.CacheEvictionService;
import com.example.myapi.categories.domain.entity.Category;
import com.example.myapi.categories.infrastructure.repository.CategoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional
public class CategoryServiceImpl
        extends BaseServiceImpl<Category, Long>
        implements CategoryService {

    public CategoryServiceImpl(
            CategoryRepository repository,
            CacheEvictionService cacheEvictionService,
            ApplicationEventPublisher eventPublisher,
            AuditorAware<String> auditorAware) {
        super(repository, cacheEvictionService, eventPublisher, auditorAware);
    }

    @Override
    protected Class<Category> getEntityClass() {
        return Category.class;
    }
}
