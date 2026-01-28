package com.jnzader.apigen.codegen.generator.java.test;

import com.jnzader.apigen.codegen.model.SqlTable;

/** Facade for generating all test classes for a table in Java/Spring Boot. */
@SuppressWarnings("java:S1192") // Duplicate strings intentional for code generation templates
public class JavaTestGenerator {

    private final JavaEntityTestGenerator entityTestGenerator;
    private final JavaRepositoryTestGenerator repositoryTestGenerator;
    private final JavaServiceTestGenerator serviceTestGenerator;
    private final JavaDTOTestGenerator dtoTestGenerator;
    private final JavaMapperTestGenerator mapperTestGenerator;
    private final JavaControllerTestGenerator controllerTestGenerator;
    private final JavaIntegrationTestGenerator integrationTestGenerator;

    public JavaTestGenerator(String basePackage) {
        this.entityTestGenerator = new JavaEntityTestGenerator(basePackage);
        this.repositoryTestGenerator = new JavaRepositoryTestGenerator(basePackage);
        this.serviceTestGenerator = new JavaServiceTestGenerator(basePackage);
        this.dtoTestGenerator = new JavaDTOTestGenerator(basePackage);
        this.mapperTestGenerator = new JavaMapperTestGenerator(basePackage);
        this.controllerTestGenerator = new JavaControllerTestGenerator(basePackage);
        this.integrationTestGenerator = new JavaIntegrationTestGenerator(basePackage);
    }

    /** Generates the entity test class. */
    public String generateEntityTest(SqlTable table) {
        return entityTestGenerator.generate(table);
    }

    /** Generates the repository test class. */
    public String generateRepositoryTest(SqlTable table) {
        return repositoryTestGenerator.generate(table);
    }

    /** Generates the service test class. */
    public String generateServiceTest(SqlTable table) {
        return serviceTestGenerator.generate(table);
    }

    /** Generates the DTO test class. */
    public String generateDTOTest(SqlTable table) {
        return dtoTestGenerator.generate(table);
    }

    /** Generates the mapper test class. */
    public String generateMapperTest(SqlTable table) {
        return mapperTestGenerator.generate(table);
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
