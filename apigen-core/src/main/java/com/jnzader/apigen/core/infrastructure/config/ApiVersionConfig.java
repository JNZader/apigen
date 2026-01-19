package com.jnzader.apigen.core.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración de versionado de API.
 *
 * <p>Agrega automáticamente el prefijo /api/v1 a todos los controladores en el paquete de
 * controllers.
 *
 * <p>Estrategias de versionado soportadas: - URL path: /api/v1/resource (implementada) - Header:
 * Accept-Version: v1 (puede agregarse) - Query param: ?version=1 (puede agregarse)
 */
@Configuration
public class ApiVersionConfig implements WebMvcConfigurer {

    @Value("${app.api.version:v1}")
    private String apiVersion;

    @Value("${app.api.base-path:/api}")
    private String basePath;

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(
                basePath + "/" + apiVersion,
                clazz -> clazz.getPackageName().startsWith("com.jnzader.apigen.core.controller"));
    }
}
