package com.jnzader.apigen.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the APiGen Server. Provides REST API for generating Spring Boot projects
 * from SQL schemas.
 *
 * <p>All database-related auto-configurations are excluded since this server doesn't use a
 * database. Using excludeName instead of exclude to avoid compilation errors when classes are not
 * on classpath.
 */
@SpringBootApplication(
        excludeName = {
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
            "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration",
            "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
            "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"
        })
public class ApiGenServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGenServerApplication.class, args);
    }
}
