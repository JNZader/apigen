package com.jnzader.apigen.core.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de SpringDoc para generar la documentación OpenAPI/Swagger de la API.
 * Proporciona metadatos personalizados para la interfaz de usuario de Swagger.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Genérica de Microservicios")
                        .version("1.0.0")
                        .description("Plantilla de API RESTful genérica construida con Spring Boot 4, Java 25, Spring Security y MapStruct. " +
                                     "Proporciona funcionalidades CRUD básicas para entidades con auditoría.")
                        .termsOfService("http://swagger.io/terms/")
                        .contact(new Contact()
                                .name("Javier N. Zader")
                                .url("https://github.com/JNZader")
                                .email("tu_email@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")));
    }
}
