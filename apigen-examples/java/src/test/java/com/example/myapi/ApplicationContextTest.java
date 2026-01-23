package com.example.myapi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test to verify the Spring application context loads correctly.
 *
 * <p>This test catches configuration issues like:
 * <ul>
 *   <li>Duplicate bean definitions (BeanDefinitionOverrideException)
 *   <li>Missing required beans
 *   <li>Circular dependencies
 *   <li>Invalid configuration properties
 * </ul>
 */
@SpringBootTest(classes = MyApiApplication.class)
@ActiveProfiles("test")
@DisplayName("Application Context Tests")
class ApplicationContextTest {

    @Test
    @DisplayName("Application context should load successfully")
    void contextLoads() {
        // If we get here, the context loaded successfully
    }
}
