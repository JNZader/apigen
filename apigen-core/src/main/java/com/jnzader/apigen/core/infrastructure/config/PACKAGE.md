# Configuration

Este paquete contiene las **configuraciones Spring** del sistema.

## Archivos

| Archivo | Descripción |
|---------|-------------|
| `CacheConfig.java` | Configuración de Caffeine cache |
| `AsyncConfig.java` | Configuración de procesamiento async |
| `OpenApiConfig.java` | Configuración de Swagger/OpenAPI |
| `WebConfig.java` | CORS, interceptors, formatters |
| `JpaConfig.java` | Auditoría JPA, transacciones |
| `MetricsConfig.java` | Micrometer, métricas custom |

## CacheConfig

Configura **tres niveles de cache** con Caffeine:

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(caffeineCacheBuilder());
        manager.setCacheNames(List.of("entities", "lists", "counts"));
        return manager;
    }

    // Cache "entities": 1000 items, 10 min TTL
    // Cache "lists": 100 items, 5 min TTL
    // Cache "counts": 50 items, 2 min TTL
}
```

**Uso:**
```java
@Cacheable(value = "entities", key = "'Product:' + #id")
public Result<Product, Exception> findById(Long id) { }
```

## AsyncConfig

Configura el **executor para @Async**:

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("async-");
        return executor;
    }
}
```

**Uso:**
```java
@Async
@EventListener
public void onProductCreated(EntityCreatedEvent<Product> event) {
    // Ejecuta en thread separado
}
```

## OpenApiConfig

Configura **Swagger UI y OpenAPI spec**:

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("APiGen API")
                .version("1.0.0")
                .description("Generic REST API Template"))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
```

**Endpoints generados:**
- `/swagger-ui.html` - UI interactiva
- `/v3/api-docs` - JSON spec

## WebConfig

Configura **CORS, interceptors y formatters**:

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000")
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
            .allowedHeaders("*")
            .exposedHeaders("ETag", "Location")
            .allowCredentials(true);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestIdInterceptor());
    }
}
```

## JpaConfig

Configura **auditoría automática**:

```java
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext())
            .map(SecurityContext::getAuthentication)
            .filter(Authentication::isAuthenticated)
            .map(Authentication::getName)
            .or(() -> Optional.of("system"));
    }
}
```

**Resultado:**
- `@CreatedBy` → Usuario actual
- `@LastModifiedBy` → Usuario actual
- `@CreatedDate` → Timestamp actual
- `@LastModifiedDate` → Timestamp actual

## Perfiles de Configuración

Las configuraciones pueden variar por perfil:

```java
@Configuration
@Profile("dev")
public class DevConfig {
    // Config específica para desarrollo
}

@Configuration
@Profile("prod")
public class ProdConfig {
    // Config específica para producción
}
```

**Activar perfil:**
```bash
./gradlew bootRun -Dspring.profiles.active=dev
```

## Propiedades Externalizadas

Usar `@ConfigurationProperties` para type-safety:

```java
@Configuration
@ConfigurationProperties(prefix = "app.cache")
@Data
public class CacheProperties {
    private int entitiesMaxSize = 1000;
    private Duration entitiesExpire = Duration.ofMinutes(10);
    private int listsMaxSize = 100;
    private Duration listsExpire = Duration.ofMinutes(5);
}
```

```yaml
app:
  cache:
    entities-max-size: 2000
    entities-expire: 15m
```

## Buenas Prácticas

1. **Una clase por concern** - No mezclar cache con CORS
2. **Externalizar valores** - No hardcodear en @Bean
3. **Documentar beans** - Javadoc en métodos @Bean
4. **Usar perfiles** - Dev vs prod vs test
5. **Conditional beans** - @ConditionalOnProperty para features opcionales
