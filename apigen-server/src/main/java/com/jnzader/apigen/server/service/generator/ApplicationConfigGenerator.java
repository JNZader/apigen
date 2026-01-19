package com.jnzader.apigen.server.service.generator;

import com.jnzader.apigen.server.dto.GenerateRequest;
import org.springframework.stereotype.Component;

/** Generates application configuration files (application.yml, application-docker.yml). */
@Component
public class ApplicationConfigGenerator {

    /**
     * Generates the main application.yml file content.
     *
     * @param config the project configuration
     * @return the application.yml content
     */
    public String generateApplicationYml(GenerateRequest.ProjectConfig config) {
        return
"""
spring:
  application:
    name: %s

  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password:

  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true

  h2:
    console:
      enabled: true
      path: /h2-console

server:
  port: 8080

# APiGen Configuration
app:
  api:
    version: v1
    base-path: /api

# OpenAPI
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    operationsSorter: method
  enable-hateoas: false

# Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
"""
                .formatted(config.getArtifactId());
    }

    /**
     * Generates application-docker.yml for Docker-specific configuration.
     *
     * @param config the project configuration
     * @return the application-docker.yml content
     */
    public String generateApplicationDockerYml(GenerateRequest.ProjectConfig config) {
        GenerateRequest.DatabaseConfig db =
                config.getDatabase() != null
                        ? config.getDatabase()
                        : new GenerateRequest.DatabaseConfig();

        return
"""
# =============================================================================
# Docker Profile Configuration
# =============================================================================
# This profile is automatically activated when running in Docker.
# Database connection uses the Docker network hostname 'db'.
# =============================================================================

spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:%s}
    username: ${SPRING_DATASOURCE_USERNAME:%s}
    password: ${SPRING_DATASOURCE_PASSWORD:%s}
    driver-class-name: %s
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        format_sql: false
        dialect: %s
        jdbc:
          batch_size: 50
        order_inserts: true
        order_updates: true

  # Disable H2 console in Docker
  h2:
    console:
      enabled: false

# Actuator configuration for container health checks
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true

# Logging for production
logging:
  level:
    root: INFO
    %s: INFO
    org.hibernate.SQL: WARN
    org.hibernate.type.descriptor.sql: WARN
  pattern:
    console: "%%d{yyyy-MM-dd HH:mm:ss} [%%thread] %%-5level %%logger{36} - %%msg%%n"
"""
                .formatted(
                        db.getJdbcUrl(),
                        db.getUsername(),
                        db.getPassword(),
                        db.getDriverClassName(),
                        db.getHibernateDialect(),
                        config.getBasePackage());
    }
}
