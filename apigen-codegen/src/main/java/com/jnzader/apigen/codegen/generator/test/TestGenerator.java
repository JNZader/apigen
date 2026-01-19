package com.jnzader.apigen.codegen.generator.test;

import com.jnzader.apigen.codegen.model.SqlTable;

/** Facade for generating all test classes for a table. */
public class TestGenerator {

    private final ServiceTestGenerator serviceTestGenerator;
    private final DTOTestGenerator dtoTestGenerator;
    private final ControllerTestGenerator controllerTestGenerator;
    private final IntegrationTestGenerator integrationTestGenerator;

    public TestGenerator(String basePackage) {
        this.serviceTestGenerator = new ServiceTestGenerator(basePackage);
        this.dtoTestGenerator = new DTOTestGenerator(basePackage);
        this.controllerTestGenerator = new ControllerTestGenerator(basePackage);
        this.integrationTestGenerator = new IntegrationTestGenerator(basePackage);
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
