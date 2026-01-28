package com.jnzader.apigen.codegen.generator.csharp.test;

import com.jnzader.apigen.codegen.model.SqlTable;

/** Facade for generating all test classes for a table in C#/ASP.NET Core. */
@SuppressWarnings("java:S1192") // Duplicate strings intentional for code generation templates
public class CSharpTestGenerator {

    private final CSharpEntityTestGenerator entityTestGenerator;
    private final CSharpRepositoryTestGenerator repositoryTestGenerator;
    private final CSharpServiceTestGenerator serviceTestGenerator;
    private final CSharpDTOTestGenerator dtoTestGenerator;
    private final CSharpControllerTestGenerator controllerTestGenerator;
    private final CSharpIntegrationTestGenerator integrationTestGenerator;

    public CSharpTestGenerator(String baseNamespace) {
        this.entityTestGenerator = new CSharpEntityTestGenerator(baseNamespace);
        this.repositoryTestGenerator = new CSharpRepositoryTestGenerator(baseNamespace);
        this.serviceTestGenerator = new CSharpServiceTestGenerator(baseNamespace);
        this.dtoTestGenerator = new CSharpDTOTestGenerator(baseNamespace);
        this.controllerTestGenerator = new CSharpControllerTestGenerator(baseNamespace);
        this.integrationTestGenerator = new CSharpIntegrationTestGenerator(baseNamespace);
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

    /** Generates the controller test class. */
    public String generateControllerTest(SqlTable table) {
        return controllerTestGenerator.generate(table);
    }

    /** Generates the integration test class. */
    public String generateIntegrationTest(SqlTable table) {
        return integrationTestGenerator.generate(table);
    }
}
