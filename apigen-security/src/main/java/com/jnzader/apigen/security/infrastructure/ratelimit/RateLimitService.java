package com.jnzader.apigen.security.infrastructure.ratelimit;

import com.jnzader.apigen.security.infrastructure.config.SecurityProperties;
import com.jnzader.apigen.security.infrastructure.config.SecurityProperties.RateLimitProperties;
import com.jnzader.apigen.security.infrastructure.config.SecurityProperties.RateLimitProperties.TierConfig;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.stereotype.Service;

/**
 * Servicio de Rate Limiting usando Bucket4j.
 *
 * <p>Soporta dos modos de almacenamiento:
 *
 * <ul>
 *   <li><b>in-memory</b>: Almacenamiento local con ConcurrentHashMap (single instance)
 *   <li><b>redis</b>: Almacenamiento distribuido con Redis/Lettuce (multi-instance)
 * </ul>
 *
 * <p>Uso:
 *
 * <pre>
 * if (!rateLimitService.tryConsume(clientIp)) {
 *     // Rate limit exceeded
 *     return ResponseEntity.status(429).build();
 * }
 * </pre>
 */
@Service
@ConditionalOnProperty(
        name = "apigen.security.rate-limit.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    private final SecurityProperties securityProperties;
    private final RedisConnectionFactory redisConnectionFactory;

    // In-memory storage
    private final Map<String, Bucket> localBuckets = new ConcurrentHashMap<>();

    // Redis storage (initialized lazily in @PostConstruct, volatile for thread visibility)
    private volatile ProxyManager<String> redisProxyManager;
    private volatile RedisClient redisClient;
    private volatile StatefulRedisConnection<String, byte[]> redisConnection;

    public RateLimitService(
            SecurityProperties securityProperties, RedisConnectionFactory redisConnectionFactory) {
        this.securityProperties = securityProperties;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @PostConstruct
    public void init() {
        RateLimitProperties config = securityProperties.getRateLimit();

        if (config.isRedisMode()) {
            initRedisProxyManager();
            log.info(
                    "Rate Limiting initialized with Redis storage: {} req/s, burst: {}",
                    config.getRequestsPerSecond(),
                    config.getBurstCapacity());
        } else {
            log.info(
                    "Rate Limiting initialized with in-memory storage: {} req/s, burst: {}",
                    config.getRequestsPerSecond(),
                    config.getBurstCapacity());
        }
    }

    @PreDestroy
    public void cleanup() {
        if (redisConnection != null) {
            redisConnection.close();
        }
        if (redisClient != null) {
            redisClient.shutdown();
        }
    }

    private void initRedisProxyManager() {
        try {
            if (redisConnectionFactory instanceof LettuceConnectionFactory lettuceFactory) {
                String host = lettuceFactory.getHostName();
                int port = lettuceFactory.getPort();
                String password = lettuceFactory.getPassword();

                String redisUri =
                        password != null && !password.isEmpty()
                                ? String.format("redis://:%s@%s:%d", password, host, port)
                                : String.format("redis://%s:%d", host, port);

                redisClient = RedisClient.create(redisUri);
                redisConnection =
                        redisClient.connect(
                                RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));

                RateLimitProperties config = securityProperties.getRateLimit();

                redisProxyManager =
                        Bucket4jLettuce.casBasedBuilder(redisConnection)
                                .expirationAfterWrite(
                                        ExpirationAfterWriteStrategy
                                                .basedOnTimeForRefillingBucketUpToMax(
                                                        Duration.ofSeconds(
                                                                config.getRedisTtlSeconds())))
                                .build();

                log.info("Redis ProxyManager initialized successfully");
            } else {
                log.warn(
                        "RedisConnectionFactory is not LettuceConnectionFactory, falling back to"
                                + " in-memory");
                securityProperties
                        .getRateLimit()
                        .setStorageMode(RateLimitProperties.StorageMode.IN_MEMORY);
            }
        } catch (Exception e) {
            log.error("Failed to initialize Redis ProxyManager, falling back to in-memory", e);
            securityProperties
                    .getRateLimit()
                    .setStorageMode(RateLimitProperties.StorageMode.IN_MEMORY);
        }
    }

    /**
     * Intenta consumir un token del bucket para la clave dada (rate limiting general).
     *
     * @param key Identificador único (típicamente IP del cliente)
     * @return true si se permitió el request, false si se excedió el límite
     */
    public boolean tryConsume(String key) {
        return tryConsume(key, false);
    }

    /**
     * Intenta consumir un token del bucket para la clave dada.
     *
     * @param key Identificador único (típicamente IP del cliente)
     * @param isAuthEndpoint true para usar límites más restrictivos de autenticación
     * @return true si se permitió el request, false si se excedió el límite
     */
    public boolean tryConsume(String key, boolean isAuthEndpoint) {
        Bucket bucket = resolveBucket(key, isAuthEndpoint);
        return bucket.tryConsume(1);
    }

    /**
     * Intenta consumir un token y retorna información detallada sobre el consumo.
     *
     * @param key Identificador único
     * @param isAuthEndpoint true para límites de autenticación
     * @return ConsumptionProbe con información de tokens restantes y tiempo de espera
     */
    public ConsumptionProbe tryConsumeAndReturnRemaining(String key, boolean isAuthEndpoint) {
        Bucket bucket = resolveBucket(key, isAuthEndpoint);
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    /**
     * Obtiene los tokens disponibles para una clave.
     *
     * @param key Identificador único
     * @param isAuthEndpoint true para límites de autenticación
     * @return Número de tokens disponibles
     */
    public long getAvailableTokens(String key, boolean isAuthEndpoint) {
        Bucket bucket = resolveBucket(key, isAuthEndpoint);
        return bucket.getAvailableTokens();
    }

    // ========== Tier-based rate limiting methods ==========

    /**
     * Intenta consumir un token del bucket usando límites basados en tier.
     *
     * @param userIdentifier Identificador único del usuario (user:id o ip:address)
     * @param tier el tier de rate limit del usuario
     * @return true si se permitió el request, false si se excedió el límite
     */
    public boolean tryConsumeForTier(String userIdentifier, RateLimitTier tier) {
        Bucket bucket = resolveTierBucket(userIdentifier, tier);
        return bucket.tryConsume(1);
    }

    /**
     * Intenta consumir un token y retorna información detallada usando límites basados en tier.
     *
     * @param userIdentifier Identificador único del usuario
     * @param tier el tier de rate limit del usuario
     * @return ConsumptionProbe con información de tokens restantes y tiempo de espera
     */
    public ConsumptionProbe tryConsumeForTierAndReturnRemaining(
            String userIdentifier, RateLimitTier tier) {
        Bucket bucket = resolveTierBucket(userIdentifier, tier);
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    /**
     * Obtiene los tokens disponibles para un usuario en un tier específico.
     *
     * @param userIdentifier Identificador único del usuario
     * @param tier el tier de rate limit del usuario
     * @return Número de tokens disponibles
     */
    public long getAvailableTokensForTier(String userIdentifier, RateLimitTier tier) {
        Bucket bucket = resolveTierBucket(userIdentifier, tier);
        return bucket.getAvailableTokens();
    }

    /**
     * Obtiene la configuración de tier para un tier específico.
     *
     * @param tier el tier
     * @return la configuración del tier
     */
    public TierConfig getTierConfig(RateLimitTier tier) {
        return securityProperties.getRateLimit().getTiers().getForTier(tier.getName());
    }

    /**
     * Verifica si el rate limiting basado en tiers está habilitado.
     *
     * @return true si está habilitado
     */
    public boolean isTiersEnabled() {
        return securityProperties.getRateLimit().isTiersEnabled();
    }

    private Bucket resolveTierBucket(String userIdentifier, RateLimitTier tier) {
        RateLimitProperties config = securityProperties.getRateLimit();
        String bucketKey =
                config.getRedisKeyPrefix() + "tier:" + tier.getName() + ":" + userIdentifier;

        if (config.isRedisMode() && redisProxyManager != null) {
            return resolveRedisTierBucket(bucketKey, tier);
        } else {
            return resolveLocalTierBucket(bucketKey, tier);
        }
    }

    private Bucket resolveLocalTierBucket(String key, RateLimitTier tier) {
        return localBuckets.computeIfAbsent(key, k -> createTierBucket(tier));
    }

    private Bucket resolveRedisTierBucket(String key, RateLimitTier tier) {
        Supplier<BucketConfiguration> configSupplier = () -> createTierBucketConfiguration(tier);
        return redisProxyManager.builder().build(key, configSupplier);
    }

    private Bucket createTierBucket(RateLimitTier tier) {
        return Bucket.builder().addLimit(createTierBandwidth(tier)).build();
    }

    private BucketConfiguration createTierBucketConfiguration(RateLimitTier tier) {
        return BucketConfiguration.builder().addLimit(createTierBandwidth(tier)).build();
    }

    private Bandwidth createTierBandwidth(RateLimitTier tier) {
        TierConfig tierConfig = getTierConfig(tier);
        return Bandwidth.builder()
                .capacity(tierConfig.getBurstCapacity())
                .refillGreedy(tierConfig.getRequestsPerSecond(), Duration.ofSeconds(1))
                .build();
    }

    private Bucket resolveBucket(String key, boolean isAuthEndpoint) {
        RateLimitProperties config = securityProperties.getRateLimit();
        String bucketKey = config.getRedisKeyPrefix() + (isAuthEndpoint ? "auth:" : "api:") + key;

        if (config.isRedisMode() && redisProxyManager != null) {
            return resolveRedisBucket(bucketKey, isAuthEndpoint);
        } else {
            return resolveLocalBucket(bucketKey, isAuthEndpoint);
        }
    }

    private Bucket resolveLocalBucket(String key, boolean isAuthEndpoint) {
        return localBuckets.computeIfAbsent(key, k -> createBucket(isAuthEndpoint));
    }

    private Bucket resolveRedisBucket(String key, boolean isAuthEndpoint) {
        Supplier<BucketConfiguration> configSupplier =
                () -> createBucketConfiguration(isAuthEndpoint);
        return redisProxyManager.builder().build(key, configSupplier);
    }

    private Bucket createBucket(boolean isAuthEndpoint) {
        return Bucket.builder().addLimit(createBandwidth(isAuthEndpoint)).build();
    }

    private BucketConfiguration createBucketConfiguration(boolean isAuthEndpoint) {
        return BucketConfiguration.builder().addLimit(createBandwidth(isAuthEndpoint)).build();
    }

    private Bandwidth createBandwidth(boolean isAuthEndpoint) {
        RateLimitProperties config = securityProperties.getRateLimit();

        if (isAuthEndpoint) {
            // Límite más restrictivo para endpoints de autenticación
            return Bandwidth.builder()
                    .capacity(config.getAuthBurstCapacity())
                    .refillGreedy(config.getAuthRequestsPerMinute(), Duration.ofMinutes(1))
                    .build();
        } else {
            // Límite general para la API
            return Bandwidth.builder()
                    .capacity(config.getBurstCapacity())
                    .refillGreedy(config.getRequestsPerSecond(), Duration.ofSeconds(1))
                    .build();
        }
    }

    /** Limpia los buckets locales (útil para tests). */
    public void clearLocalBuckets() {
        localBuckets.clear();
    }

    /** Verifica si el servicio está usando Redis. */
    public boolean isUsingRedis() {
        return securityProperties.getRateLimit().isRedisMode() && redisProxyManager != null;
    }
}
