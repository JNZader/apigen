package com.jnzader.apigen.codegen.generator.kotlin.controller;

import com.jnzader.apigen.codegen.model.SqlTable;

/** Generates Controller interface and implementation classes for Kotlin/Spring Boot. */
public class KotlinControllerGenerator {

    private static final String APIGEN_CORE_PKG = "com.jnzader.apigen.core";

    private final String basePackage;

    public KotlinControllerGenerator(String basePackage) {
        this.basePackage = basePackage;
    }

    /** Generates the Controller interface code in Kotlin. */
    public String generateInterface(SqlTable table) {
        String entityName = table.getEntityName();
        String moduleName = table.getModuleName();

        return
"""
package %s.%s.infrastructure.controller

import %s.infrastructure.controller.BaseController
import %s.%s.application.dto.%sDTO
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping

@Tag(name = "%ss", description = "%s management API")
@RequestMapping("/api/v1/%s")
interface %sController : BaseController<%sDTO, Long> {

    // Add custom endpoints here
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
                        entityName,
                        moduleName,
                        entityName,
                        entityName);
    }

    /** Generates the Controller implementation class code in Kotlin. */
    public String generateImpl(SqlTable table) {
        String entityName = table.getEntityName();
        String entityVarName = table.getEntityVariableName();
        String moduleName = table.getModuleName();

        return
"""
package %s.%s.infrastructure.controller

import %s.infrastructure.controller.BaseControllerImpl
import %s.%s.application.dto.%sDTO
import %s.%s.application.mapper.%sMapper
import %s.%s.application.service.%sService
import %s.%s.domain.entity.%s
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.RestController

@RestController
open class %sControllerImpl(
    private val %sService: %sService,
    private val %sMapper: %sMapper
) : BaseControllerImpl<%s, %sDTO, Long>(%sService, %sMapper), %sController {

    private val log = LoggerFactory.getLogger(%sControllerImpl::class.java)
}
"""
                .formatted(
                        basePackage,
                        moduleName,
                        APIGEN_CORE_PKG,
                        basePackage,
                        moduleName,
                        entityName,
                        basePackage,
                        moduleName,
                        entityName,
                        basePackage,
                        moduleName,
                        entityName,
                        basePackage,
                        moduleName,
                        entityName,
                        entityName,
                        entityVarName,
                        entityName,
                        entityVarName,
                        entityName,
                        entityName,
                        entityName,
                        entityVarName,
                        entityVarName,
                        entityName,
                        entityName);
    }
}
