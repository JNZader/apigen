package com.jnzader.apigen.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the APiGen Server. Provides REST API for generating Spring Boot projects
 * from SQL schemas.
 *
 * <p>This server doesn't use a database - JPA/Hibernate dependencies are excluded at the Gradle
 * dependency level to prevent DataSource auto-configuration.
 */
@SpringBootApplication
public class ApiGenServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGenServerApplication.class, args);
    }
}
