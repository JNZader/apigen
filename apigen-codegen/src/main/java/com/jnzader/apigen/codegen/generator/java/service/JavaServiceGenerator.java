package com.jnzader.apigen.codegen.generator.java.service;

import com.jnzader.apigen.codegen.model.SqlTable;

/** Generates Service interface and implementation classes for Java/Spring Boot. */
public class JavaServiceGenerator {

    private static final String APIGEN_CORE_PKG = "com.jnzader.apigen.core";

    private final String basePackage;

    public JavaServiceGenerator(String basePackage) {
        this.basePackage = basePackage;
    }

    /** Generates the Service interface code. */
    public String generateInterface(SqlTable table) {
        String entityName = table.getEntityName();
        String moduleName = table.getModuleName();

        return
"""
package %s.%s.application.service;

import %s.application.service.BaseService;
import %s.%s.domain.entity.%s;

public interface %sService extends BaseService<%s, Long> {

    // Add custom business methods here
}
"""
                .formatted(
                        basePackage,
                        moduleName,
                        APIGEN_CORE_PKG,
                        basePackage,
                        moduleName,
                        entityName,
                        entityName,
                        entityName);
    }

    /** Generates the Service implementation class code. */
    public String generateImpl(SqlTable table) {
        String entityName = table.getEntityName();
        String moduleName = table.getModuleName();

        return
"""
package %s.%s.application.service;

import %s.application.service.BaseServiceImpl;
import %s.application.service.CacheEvictionService;
import %s.%s.domain.entity.%s;
import %s.%s.infrastructure.repository.%sRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional
public class %sServiceImpl
        extends BaseServiceImpl<%s, Long>
        implements %sService {

    public %sServiceImpl(
            %sRepository repository,
            CacheEvictionService cacheEvictionService,
            ApplicationEventPublisher eventPublisher,
            AuditorAware<String> auditorAware) {
        super(repository, cacheEvictionService, eventPublisher, auditorAware);
    }

    @Override
    protected Class<%s> getEntityClass() {
        return %s.class;
    }
}
"""
                .formatted(
                        basePackage,
                        moduleName,
                        APIGEN_CORE_PKG,
                        APIGEN_CORE_PKG,
                        basePackage,
                        moduleName,
                        entityName,
                        basePackage,
                        moduleName,
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        entityName);
    }
}
