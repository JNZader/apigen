package com.jnzader.apigen.codegen.generator.typescript.auth;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates TypeScript/NestJS rate limiting functionality using @nestjs/throttler.
 *
 * <p>This generator creates:
 *
 * <ul>
 *   <li>Throttler module configuration
 *   <li>Throttler guard for global rate limiting
 *   <li>Custom decorators for endpoint-specific limits
 *   <li>Redis storage support for distributed rate limiting
 * </ul>
 */
@SuppressWarnings({
    "java:S1192",
    "java:S3400"
}) // S1192: template strings; S3400: template methods return constants
public class TypeScriptRateLimitGenerator {

    private static final int DEFAULT_TTL = 60;
    private static final int DEFAULT_LIMIT = 100;
    private static final boolean DEFAULT_USE_REDIS = false;

    /**
     * Generates all rate limiting files.
     *
     * @param ttlSeconds time-to-live for rate limit window in seconds
     * @param limit maximum requests per TTL window
     * @param useRedis whether to use Redis backend for distributed limiting
     * @return map of file path to content
     */
    public Map<String, String> generate(int ttlSeconds, int limit, boolean useRedis) {
        Map<String, String> files = new LinkedHashMap<>();

        // Throttler configuration module
        files.put(
                "src/common/throttler/throttler.module.ts",
                generateThrottlerModule(ttlSeconds, limit, useRedis));

        // Custom throttler guard
        files.put("src/common/throttler/throttler-behind-proxy.guard.ts", generateThrottlerGuard());

        // Custom decorators for rate limiting
        files.put("src/common/throttler/throttle.decorator.ts", generateThrottleDecorator());

        // Rate limit exception filter
        files.put("src/common/throttler/throttler-exception.filter.ts", generateExceptionFilter());

        // Index barrel file
        files.put("src/common/throttler/index.ts", generateIndex());

        return files;
    }

    /** Generates with default settings. */
    public Map<String, String> generate() {
        return generate(DEFAULT_TTL, DEFAULT_LIMIT, DEFAULT_USE_REDIS);
    }

    private String generateThrottlerModule(int ttlSeconds, int limit, boolean useRedis) {
        if (useRedis) {
            return """
            import { Module } from '@nestjs/common';
            import { ThrottlerModule as NestThrottlerModule, ThrottlerStorageService } from '@nestjs/throttler';
            import { ThrottlerStorageRedisService } from '@nest-lab/throttler-storage-redis';
            import Redis from 'ioredis';

            @Module({
              imports: [
                NestThrottlerModule.forRootAsync({
                  useFactory: () => ({
                    throttlers: [
                      {
                        name: 'default',
                        ttl: %d * 1000, // Convert to milliseconds
                        limit: %d,
                      },
                      {
                        name: 'short',
                        ttl: 1000, // 1 second
                        limit: 5,
                      },
                      {
                        name: 'medium',
                        ttl: 10000, // 10 seconds
                        limit: 20,
                      },
                      {
                        name: 'long',
                        ttl: 60000, // 1 minute
                        limit: 100,
                      },
                    ],
                    storage: new ThrottlerStorageRedisService(
                      new Redis({
                        host: process.env.REDIS_HOST || 'localhost',
                        port: parseInt(process.env.REDIS_PORT || '6379', 10),
                        password: process.env.REDIS_PASSWORD,
                      }),
                    ),
                  }),
                }),
              ],
              exports: [NestThrottlerModule],
            })
            export class ThrottlerModule {}
            """
                    .formatted(ttlSeconds, limit);
        }

        return """
        import { Module } from '@nestjs/common';
        import { ThrottlerModule as NestThrottlerModule } from '@nestjs/throttler';

        /**
         * Rate limiting module using @nestjs/throttler.
         *
         * Provides multiple rate limit tiers:
         * - default: Standard API rate limit
         * - short: Burst protection (1 second window)
         * - medium: Short-term protection (10 seconds)
         * - long: Long-term protection (1 minute)
         */
        @Module({
          imports: [
            NestThrottlerModule.forRoot({
              throttlers: [
                {
                  name: 'default',
                  ttl: %d * 1000, // Convert to milliseconds
                  limit: %d,
                },
                {
                  name: 'short',
                  ttl: 1000, // 1 second
                  limit: 5,
                },
                {
                  name: 'medium',
                  ttl: 10000, // 10 seconds
                  limit: 20,
                },
                {
                  name: 'long',
                  ttl: 60000, // 1 minute
                  limit: 100,
                },
              ],
            }),
          ],
          exports: [NestThrottlerModule],
        })
        export class ThrottlerModule {}
        """
                .formatted(ttlSeconds, limit);
    }

    private String generateThrottlerGuard() {
        return """
        import { Injectable, ExecutionContext } from '@nestjs/common';
        import { ThrottlerGuard } from '@nestjs/throttler';
        import { Request } from 'express';

        /**
         * Custom throttler guard that handles proxied requests.
         *
         * This guard extracts the real client IP from X-Forwarded-For header
         * when the application is behind a reverse proxy (nginx, load balancer, etc.)
         */
        @Injectable()
        export class ThrottlerBehindProxyGuard extends ThrottlerGuard {
          /**
           * Get the client IP address, handling proxied requests.
           *
           * Priority:
           * 1. X-Forwarded-For header (first IP)
           * 2. X-Real-IP header
           * 3. Request IP (socket remote address)
           */
          protected getTracker(req: Request): Promise<string> {
            const forwardedFor = req.headers['x-forwarded-for'];
            if (forwardedFor) {
              // X-Forwarded-For can contain multiple IPs: client, proxy1, proxy2
              const clientIp = Array.isArray(forwardedFor)
                ? forwardedFor[0]
                : forwardedFor.split(',')[0].trim();
              return Promise.resolve(clientIp);
            }

            const realIp = req.headers['x-real-ip'];
            if (realIp) {
              return Promise.resolve(Array.isArray(realIp) ? realIp[0] : realIp);
            }

            return Promise.resolve(req.ip || 'unknown');
          }

          /**
           * Skip rate limiting for certain routes or conditions.
           */
          protected shouldSkip(_context: ExecutionContext): Promise<boolean> {
            // You can add custom skip logic here
            // For example, skip for health check endpoints:
            // const req = context.switchToHttp().getRequest<Request>();
            // if (req.path === '/health') return Promise.resolve(true);
            return Promise.resolve(false);
          }
        }
        """;
    }

    private String generateThrottleDecorator() {
        return """
        import { applyDecorators, SetMetadata } from '@nestjs/common';
        import { Throttle, SkipThrottle } from '@nestjs/throttler';

        /**
         * Rate limit tiers for different endpoint types.
         */
        export enum RateLimitTier {
          /** Standard API endpoints: 100 requests/minute */
          STANDARD = 'standard',
          /** Heavy endpoints (reports, exports): 10 requests/minute */
          HEAVY = 'heavy',
          /** Authentication endpoints: 5 requests/minute (prevent brute force) */
          AUTH = 'auth',
          /** Public endpoints: 30 requests/minute */
          PUBLIC = 'public',
          /** Admin endpoints: 200 requests/minute */
          ADMIN = 'admin',
          /** Burst-friendly endpoints: 1000 requests/minute */
          BURST = 'burst',
        }

        const RATE_LIMITS: Record<RateLimitTier, { ttl: number; limit: number }> = {
          [RateLimitTier.STANDARD]: { ttl: 60000, limit: 100 },
          [RateLimitTier.HEAVY]: { ttl: 60000, limit: 10 },
          [RateLimitTier.AUTH]: { ttl: 60000, limit: 5 },
          [RateLimitTier.PUBLIC]: { ttl: 60000, limit: 30 },
          [RateLimitTier.ADMIN]: { ttl: 60000, limit: 200 },
          [RateLimitTier.BURST]: { ttl: 60000, limit: 1000 },
        };

        /**
         * Apply rate limiting with a predefined tier.
         *
         * @example
         * ```typescript
         * @RateLimit(RateLimitTier.HEAVY)
         * @Get('export')
         * export() { ... }
         * ```
         */
        export function RateLimit(tier: RateLimitTier) {
          const { ttl, limit } = RATE_LIMITS[tier];
          return applyDecorators(
            SetMetadata('rateLimitTier', tier),
            Throttle({ default: { ttl, limit } }),
          );
        }

        /**
         * Apply custom rate limiting.
         *
         * @example
         * ```typescript
         * @CustomRateLimit(30000, 50) // 50 requests per 30 seconds
         * @Get('items')
         * getItems() { ... }
         * ```
         */
        export function CustomRateLimit(ttl: number, limit: number) {
          return applyDecorators(Throttle({ default: { ttl, limit } }));
        }

        /**
         * Skip rate limiting for this endpoint.
         * Use sparingly - only for health checks and similar endpoints.
         *
         * @example
         * ```typescript
         * @NoRateLimit()
         * @Get('health')
         * health() { ... }
         * ```
         */
        export function NoRateLimit() {
          return applyDecorators(SkipThrottle());
        }
        """;
    }

    private String generateExceptionFilter() {
        return """
        import {
          ExceptionFilter,
          Catch,
          ArgumentsHost,
          HttpStatus,
        } from '@nestjs/common';
        import { ThrottlerException } from '@nestjs/throttler';
        import { Response } from 'express';

        /**
         * Custom exception filter for rate limit exceeded errors.
         *
         * Provides a consistent error response format with retry information.
         */
        @Catch(ThrottlerException)
        export class ThrottlerExceptionFilter implements ExceptionFilter {
          catch(exception: ThrottlerException, host: ArgumentsHost) {
            const ctx = host.switchToHttp();
            const response = ctx.getResponse<Response>();

            // Default retry after 60 seconds
            const retryAfter = 60;

            response
              .status(HttpStatus.TOO_MANY_REQUESTS)
              .header('Retry-After', String(retryAfter))
              .json({
                statusCode: HttpStatus.TOO_MANY_REQUESTS,
                error: 'Too Many Requests',
                message: 'Rate limit exceeded. Please try again later.',
                retryAfter,
                timestamp: new Date().toISOString(),
              });
          }
        }
        """;
    }

    private String generateIndex() {
        return """
        export * from './throttler.module';
        export * from './throttler-behind-proxy.guard';
        export * from './throttle.decorator';
        export * from './throttler-exception.filter';
        """;
    }
}
