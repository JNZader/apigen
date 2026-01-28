package com.jnzader.apigen.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

/**
 * Main entry point for the APiGen Server. Provides REST API for generating Spring Boot projects
 * from SQL schemas.
 *
 * <p>DataSource and JPA auto-configuration are excluded since this server doesn't use a database.
 */
@SpringBootApplication(
        exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class ApiGenServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGenServerApplication.class, args);
    }
}
