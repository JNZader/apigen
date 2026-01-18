package com.jnzader.apigen.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the APiGen Server.
 * Provides REST API for generating Spring Boot projects from SQL schemas.
 */
@SpringBootApplication
public class ApiGenServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGenServerApplication.class, args);
    }
}
