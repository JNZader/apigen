package com.example.myapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example.myapi", "com.jnzader.apigen.core"})
@EnableJpaRepositories(basePackages = {"com.example.myapi", "com.jnzader.apigen.security.domain.repository"})
@EntityScan(basePackages = {"com.example.myapi", "com.jnzader.apigen.security.domain.entity"})
@EnableCaching
public class MyApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyApiApplication.class, args);
    }
}
