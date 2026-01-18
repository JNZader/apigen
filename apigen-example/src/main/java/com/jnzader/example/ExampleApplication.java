package com.jnzader.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

/**
 * Example application demonstrating APiGen auto-configuration.
 * <p>
 * With APiGen's Spring Boot Starter approach:
 * <ul>
 *     <li>Component scanning - auto-configured via {@code @ComponentScan}</li>
 *     <li>JPA repositories - auto-configured via {@code @EnableJpaRepositories}</li>
 *     <li>All configurations - auto-imported</li>
 * </ul>
 * <p>
 * Note: Entity scanning from external packages requires {@code @EntityScan}
 * due to Spring Boot 4.0's package restructuring.
 */
@SpringBootApplication
@EntityScan(basePackages = {
    "com.jnzader.example",
    "com.jnzader.apigen.core.domain.entity",
    "com.jnzader.apigen.security.domain.entity"
})
public class ExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExampleApplication.class, args);
    }
}
