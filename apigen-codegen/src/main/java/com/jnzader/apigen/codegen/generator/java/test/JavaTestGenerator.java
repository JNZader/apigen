package com.jnzader.apigen.codegen.generator.java.test;

import com.jnzader.apigen.codegen.model.SqlTable;

/** Facade for generating all test classes for a table in Java/Spring Boot. */
public class JavaTestGenerator {

    private final JavaServiceTestGenerator serviceTestGenerator;
    private final JavaDTOTestGenerator dtoTestGenerator;
    private final JavaControllerTestGenerator controllerTestGenerator;
    private final JavaIntegrationTestGenerator integrationTestGenerator;

    public JavaTestGenerator(String basePackage) {
        this.serviceTestGenerator = new JavaServiceTestGenerator(basePackage);
        this.dtoTestGenerator = new JavaDTOTestGenerator(basePackage);
        this.controllerTestGenerator = new JavaControllerTestGenerator(basePackage);
        this.integrationTestGenerator = new JavaIntegrationTestGenerator(basePackage);
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
