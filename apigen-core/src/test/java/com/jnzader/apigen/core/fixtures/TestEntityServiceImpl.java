package com.jnzader.apigen.core.fixtures;

import com.jnzader.apigen.core.application.service.BaseServiceImpl;
import com.jnzader.apigen.core.application.service.CacheEvictionService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;

/** Test service implementation for unit tests. */
@Service
public class TestEntityServiceImpl extends BaseServiceImpl<TestEntity, Long>
        implements TestEntityService {

    public TestEntityServiceImpl(
            TestEntityRepository repository,
            CacheEvictionService cacheEvictionService,
            ApplicationEventPublisher eventPublisher,
            AuditorAware<String> auditorAware) {
        super(repository, cacheEvictionService, eventPublisher, auditorAware);
    }

    @Override
    protected Class<TestEntity> getEntityClass() {
        return TestEntity.class;
    }
}
