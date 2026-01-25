package com.jnzader.apigen.codegen.generator.java.controller;

import com.jnzader.apigen.codegen.model.SqlTable;

/** Generates Controller interface and implementation classes for Java/Spring Boot. */
@SuppressWarnings("java:S1192") // Duplicate strings intentional for code generation templates
public class JavaControllerGenerator {

    private static final String APIGEN_CORE_PKG = "com.jnzader.apigen.core";

    private final String basePackage;

    public JavaControllerGenerator(String basePackage) {
        this.basePackage = basePackage;
    }

    /** Generates the Controller interface code. */
    public String generateInterface(SqlTable table) {
        String entityName = table.getEntityName();
        String moduleName = table.getModuleName();

        return
"""
package %s.%s.infrastructure.controller;

import %s.infrastructure.controller.BaseController;
import %s.%s.application.dto.%sDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "%ss", description = "%s management API")
@RequestMapping("/api/v1/%s")
public interface %sController extends BaseController<%sDTO, Long> {

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

    /** Generates the Controller implementation class code. */
    public String generateImpl(SqlTable table) {
        String entityName = table.getEntityName();
        String entityVarName = table.getEntityVariableName();
        String moduleName = table.getModuleName();

        return
"""
package %s.%s.infrastructure.controller;

import %s.infrastructure.controller.BaseControllerImpl;
import %s.%s.application.dto.%sDTO;
import %s.%s.application.mapper.%sMapper;
import %s.%s.application.service.%sService;
import %s.%s.domain.entity.%s;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class %sControllerImpl
        extends BaseControllerImpl<%s, %sDTO, Long>
        implements %sController {

    private final %sService %sService;
    private final %sMapper %sMapper;

    public %sControllerImpl(%sService service, %sMapper mapper) {
        super(service, mapper);
        this.%sService = service;
        this.%sMapper = mapper;
    }
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
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        entityVarName,
                        entityName,
                        entityVarName,
                        entityName,
                        entityName,
                        entityName,
                        entityVarName,
                        entityVarName);
    }
}
