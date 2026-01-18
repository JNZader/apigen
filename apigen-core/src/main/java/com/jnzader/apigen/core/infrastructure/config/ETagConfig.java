package com.jnzader.apigen.core.infrastructure.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/**
 * Configuración de ETag para caché HTTP.
 * <p>
 * ShallowEtagHeaderFilter calcula un hash MD5 del contenido de la respuesta
 * y lo usa como ETag. Esto permite que los clientes usen If-None-Match
 * para validar su caché y recibir 304 Not Modified si el contenido no ha cambiado.
 * <p>
 * Beneficios:
 * - Reduce el ancho de banda (no se envía el body si no cambió)
 * - Mejora el tiempo de respuesta percibido
 * - Permite validación de caché del lado del cliente
 * <p>
 * Limitaciones:
 * - El servidor aún procesa la request completamente (solo ahorra bandwidth)
 * - Para verdadero ahorro de procesamiento, usar caché del lado del servidor
 */
@Configuration
public class ETagConfig {

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagHeaderFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> filterRegistrationBean =
                new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());

        // Aplicar a todos los endpoints de la API
        filterRegistrationBean.addUrlPatterns("/api/*");

        // Establecer orden (después del logging, antes del rate limiting)
        filterRegistrationBean.setOrder(2);

        // Nombre para identificación en logs
        filterRegistrationBean.setName("etagFilter");

        return filterRegistrationBean;
    }
}
