package com.jnzader.apigen.core.infrastructure.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotación para marcar métodos que deben ser medidos con métricas detalladas.
 * <p>
 * Solo los métodos anotados con @Measured serán instrumentados por MetricsAspect.
 * Esto reduce el overhead de instrumentación automática en toda la aplicación.
 * <p>
 * Uso:
 * <pre>
 * {@code
 * @Measured(name = "process-order", description = "Procesamiento de órdenes")
 * public Result<Order, Exception> processOrder(OrderRequest request) { }
 * }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Measured {

    /**
     * Nombre de la métrica (opcional).
     * Si no se especifica, se usa el nombre del método.
     */
    String name() default "";

    /**
     * Descripción de la métrica (opcional).
     */
    String description() default "";

    /**
     * Si true, también registra el histograma de distribución.
     */
    boolean histogram() default false;

    /**
     * Umbral en milisegundos para logging de métodos lentos.
     * -1 significa usar el valor por defecto del aspecto.
     */
    long slowThresholdMs() default -1;
}
