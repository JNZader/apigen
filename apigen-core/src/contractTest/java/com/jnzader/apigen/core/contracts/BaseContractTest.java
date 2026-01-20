package com.jnzader.apigen.core.contracts;

import com.jnzader.apigen.core.config.TestSecurityConfig;
import com.jnzader.apigen.core.fixtures.TestEntity;
import com.jnzader.apigen.core.fixtures.TestEntityControllerImpl;
import com.jnzader.apigen.core.fixtures.TestEntityRepository;
import com.jnzader.apigen.core.infrastructure.config.JpaConfig;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Base class for Spring Cloud Contract generated tests.
 *
 * <p>This class sets up the MockMvc context with: - Test security configuration (permissive for
 * contract tests) - JPA configuration with H2 in-memory database - Test entity fixtures for
 * contract verification
 *
 * <p>Contract tests verify that the API conforms to the defined contracts, ensuring API stability
 * and preventing breaking changes.
 */
@SpringBootTest
@Import({TestSecurityConfig.class, JpaConfig.class})
@ActiveProfiles("test")
public abstract class BaseContractTest {

    @Autowired private WebApplicationContext context;

    @Autowired private TestEntityRepository testEntityRepository;

    @Autowired private TestEntityControllerImpl testEntityController;

    protected TestEntity savedEntity;

    @BeforeEach
    public void setup() {
        // Clean and prepare test data
        testEntityRepository.deleteAll();

        TestEntity entity = new TestEntity();
        entity.setName("Contract Test Entity");
        entity.setEstado(true);
        savedEntity = testEntityRepository.save(entity);

        // Create additional entities for list tests
        for (int i = 1; i <= 5; i++) {
            TestEntity additional = new TestEntity();
            additional.setName("Entity " + i);
            additional.setEstado(i % 2 == 0);
            testEntityRepository.save(additional);
        }

        // Configure RestAssured to use MockMvc with full Spring context
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    /** Returns the ID of the saved test entity. Used by contracts that need a valid entity ID. */
    protected Long getEntityId() {
        return savedEntity.getId();
    }
}
