# Gateway Module

The `apigen-gateway` module provides API Gateway functionality.

## Features

- **Route Management** - Dynamic routing
- **Rate Limiting** - Request throttling
- **Circuit Breaker** - Fault tolerance
- **Load Balancing** - Service discovery

## Installation

```groovy
implementation 'com.github.jnzader.apigen:apigen-gateway'
```

## Configuration

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: products-service
          uri: lb://products-service
          predicates:
            - Path=/api/products/**
          filters:
            - name: RateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
```

## Features

### Rate Limiting

```yaml
filters:
  - name: RateLimiter
    args:
      redis-rate-limiter.replenishRate: 100
      redis-rate-limiter.burstCapacity: 200
```

### Circuit Breaker

```yaml
filters:
  - name: CircuitBreaker
    args:
      name: productsCircuitBreaker
      fallbackUri: forward:/fallback/products
```

### Load Balancing

Uses Spring Cloud LoadBalancer for service discovery:

```yaml
uri: lb://service-name
```
