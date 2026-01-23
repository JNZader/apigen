package com.jnzader.apigen.codegen.generator.kotlin.service;

import com.jnzader.apigen.codegen.model.SqlTable;

/** Generates Service interface and implementation classes for Kotlin/Spring Boot. */
public class KotlinServiceGenerator {

    private static final String APIGEN_CORE_PKG = "com.jnzader.apigen.core";

    private final String basePackage;

    public KotlinServiceGenerator(String basePackage) {
        this.basePackage = basePackage;
    }

    /** Generates the Service interface code in Kotlin. */
    public String generateInterface(SqlTable table) {
        String entityName = table.getEntityName();
        String moduleName = table.getModuleName();

        return
"""
package %s.%s.application.service

import %s.application.service.BaseService
import %s.%s.domain.entity.%s

interface %sService : BaseService<%s, Long> {

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

    /** Generates the Service implementation class code in Kotlin. */
    public String generateImpl(SqlTable table) {
        String entityName = table.getEntityName();
        String entityVarName = table.getEntityVariableName();
        String moduleName = table.getModuleName();

        return
"""
package %s.%s.application.service

import %s.application.service.BaseServiceImpl
import %s.application.service.CacheEvictionService
import %s.%s.domain.entity.%s
import %s.%s.infrastructure.repository.%sRepository
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.AuditorAware
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
open class %sServiceImpl(
    repository: %sRepository,
    cacheEvictionService: CacheEvictionService,
    eventPublisher: ApplicationEventPublisher,
    auditorAware: AuditorAware<String>
) : BaseServiceImpl<%s, Long>(repository, cacheEvictionService, eventPublisher, auditorAware),
    %sService {

    private val log = LoggerFactory.getLogger(%sServiceImpl::class.java)

    override fun getEntityClass(): Class<%s> = %s::class.java
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
