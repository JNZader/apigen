package com.jnzader.apigen.codegen.generator.kotlin.test;

import com.jnzader.apigen.codegen.model.SqlTable;

/** Facade for generating all test classes for a table in Kotlin/Spring Boot. */
public class KotlinTestGenerator {

    private final KotlinServiceTestGenerator serviceTestGenerator;
    private final KotlinDTOTestGenerator dtoTestGenerator;
    private final KotlinControllerTestGenerator controllerTestGenerator;
    private final KotlinIntegrationTestGenerator integrationTestGenerator;

    public KotlinTestGenerator(String basePackage) {
        this.serviceTestGenerator = new KotlinServiceTestGenerator(basePackage);
        this.dtoTestGenerator = new KotlinDTOTestGenerator(basePackage);
        this.controllerTestGenerator = new KotlinControllerTestGenerator(basePackage);
        this.integrationTestGenerator = new KotlinIntegrationTestGenerator(basePackage);
    }

    /** Generates the service test class. */
    public String generateServiceTest(SqlTable table) {
        return serviceTestGenerator.generate(table);
    }

    /** Generates the DTO test class. */
    public String generateDTOTest(SqlTable table) {
        return dtoTestGenerator.generate(table);
    }

    /** Generates the controller test class. */
    public String generateControllerTest(SqlTable table) {
        return controllerTestGenerator.generate(table);
    }

    /** Generates the integration test class. */
    public String generateIntegrationTest(SqlTable table) {
        return integrationTestGenerator.generate(table);
    }
}
