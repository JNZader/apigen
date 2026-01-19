package com.jnzader.apigen.core.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración de Spring MVC para la API REST.
 *
 * <p>Configura: - Interceptores (Request ID) - Negociación de contenido - Otros aspectos de
 * configuración web
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RequestIdInterceptor requestIdInterceptor;

    public WebConfig(RequestIdInterceptor requestIdInterceptor) {
        this.requestIdInterceptor = requestIdInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Request ID interceptor para todas las requests de la API
        registry.addInterceptor(requestIdInterceptor).addPathPatterns("/api/**");
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
                // No usar parámetros para determinar tipo de contenido
                .favorParameter(false)
                // Tipo de contenido por defecto
                .defaultContentType(MediaType.APPLICATION_JSON)
                // Tipos soportados
                .mediaType("json", MediaType.APPLICATION_JSON)
                .mediaType("xml", MediaType.APPLICATION_XML);
    }
}
