package com.jnzader.apigen.codegen.generator.kotlin.test;

import com.jnzader.apigen.codegen.model.SqlTable;

/** Facade for generating all test classes for a table in Kotlin/Spring Boot. */
@SuppressWarnings("java:S1192") // Duplicate strings intentional for code generation templates
public class KotlinTestGenerator {

    private final KotlinEntityTestGenerator entityTestGenerator;
    private final KotlinRepositoryTestGenerator repositoryTestGenerator;
    private final KotlinServiceTestGenerator serviceTestGenerator;
    private final KotlinDTOTestGenerator dtoTestGenerator;
    private final KotlinMapperTestGenerator mapperTestGenerator;
    private final KotlinControllerTestGenerator controllerTestGenerator;
    private final KotlinIntegrationTestGenerator integrationTestGenerator;

    public KotlinTestGenerator(String basePackage) {
        this.entityTestGenerator = new KotlinEntityTestGenerator(basePackage);
        this.repositoryTestGenerator = new KotlinRepositoryTestGenerator(basePackage);
        this.serviceTestGenerator = new KotlinServiceTestGenerator(basePackage);
        this.dtoTestGenerator = new KotlinDTOTestGenerator(basePackage);
        this.mapperTestGenerator = new KotlinMapperTestGenerator(basePackage);
        this.controllerTestGenerator = new KotlinControllerTestGenerator(basePackage);
        this.integrationTestGenerator = new KotlinIntegrationTestGenerator(basePackage);
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
