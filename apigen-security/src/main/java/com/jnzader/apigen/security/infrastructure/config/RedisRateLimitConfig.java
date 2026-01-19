package com.jnzader.apigen.security.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

/**
 * Configuración de Redis para Rate Limiting.
 *
 * <p>Proporciona un RedisConnectionFactory por defecto cuando no existe uno configurado. Esto
 * permite que el rate limiting funcione sin configuración explícita de Redis.
 */
@Configuration
@ConditionalOnProperty(
        name = "apigen.security.rate-limit.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class RedisRateLimitConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimitConfig.class);

    /**
     * Proporciona un RedisConnectionFactory por defecto si no hay uno configurado.
     *
     * <p>Esto es útil para desarrollo local donde Redis puede no estar disponible. El
     * RateLimitService detectará si Redis no está disponible y usará almacenamiento en memoria.
     */
    @Bean
    @ConditionalOnMissingBean(RedisConnectionFactory.class)
    public RedisConnectionFactory redisConnectionFactory() {
        log.info("Creating default Redis connection factory (localhost:6379)");
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("localhost", 6379);
        return new LettuceConnectionFactory(config);
    }
}
