package com.jnzader.apigen.core.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration for the REST API.
 *
 * <p>Configures: - Interceptors (Request ID) - Content negotiation - Other web configuration
 * aspects
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RequestIdInterceptor requestIdInterceptor;

    public WebConfig(RequestIdInterceptor requestIdInterceptor) {
        this.requestIdInterceptor = requestIdInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Request ID interceptor for all API requests
        registry.addInterceptor(requestIdInterceptor).addPathPatterns("/api/**");
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
                // Do not use parameters to determine content type
                .favorParameter(false)
                // Default content type
                .defaultContentType(MediaType.APPLICATION_JSON)
                // Supported types
                .mediaType("json", MediaType.APPLICATION_JSON)
                .mediaType("xml", MediaType.APPLICATION_XML);
    }
}
