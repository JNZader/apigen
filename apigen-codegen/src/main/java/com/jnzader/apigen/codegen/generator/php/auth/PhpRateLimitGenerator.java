package com.jnzader.apigen.codegen.generator.php.auth;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates PHP/Laravel rate limiting functionality.
 *
 * <p>This generator creates:
 *
 * <ul>
 *   <li>Rate limit service provider configuration
 *   <li>Custom rate limiters for different endpoint types
 *   <li>Rate limit middleware
 *   <li>Rate limit configuration file
 * </ul>
 */
@SuppressWarnings({
    "java:S1192",
    "java:S3400"
}) // S1192: template strings; S3400: template methods return constants
public class PhpRateLimitGenerator {

    private static final int DEFAULT_LIMIT = 60; // per minute
    private static final boolean DEFAULT_USE_REDIS = false;

    /**
     * Generates all rate limiting files.
     *
     * @param defaultLimit default requests per minute
     * @param useRedis whether to use Redis for rate limiting
     * @return map of file path to content
     */
    public Map<String, String> generate(int defaultLimit, boolean useRedis) {
        Map<String, String> files = new LinkedHashMap<>();

        // Rate limit service provider
        files.put(
                "app/Providers/RateLimitServiceProvider.php",
                generateRateLimitServiceProvider(defaultLimit));

        // Rate limit config
        files.put("config/rate_limits.php", generateRateLimitConfig(defaultLimit, useRedis));

        // Custom rate limit middleware
        files.put(
                "app/Http/Middleware/CustomRateLimiter.php", generateCustomRateLimiterMiddleware());

        return files;
    }

    /** Generates with default settings. */
    public Map<String, String> generate() {
        return generate(DEFAULT_LIMIT, DEFAULT_USE_REDIS);
    }

    private String generateRateLimitServiceProvider(int defaultLimit) {
        return """
        <?php

        namespace App\\Providers;

        use Illuminate\\Cache\\RateLimiting\\Limit;
        use Illuminate\\Http\\Request;
        use Illuminate\\Support\\Facades\\RateLimiter;
        use Illuminate\\Support\\ServiceProvider;

        /**
         * Rate limiting service provider.
         *
         * Configures various rate limiters for different endpoint types.
         */
        class RateLimitServiceProvider extends ServiceProvider
        {
            /**
             * Register any application services.
             */
            public function register(): void
            {
                //
            }

            /**
             * Bootstrap any application services.
             */
            public function boot(): void
            {
                $this->configureRateLimiting();
            }

            /**
             * Configure the rate limiters for the application.
             */
            protected function configureRateLimiting(): void
            {
                // Default API rate limiter
                RateLimiter::for('api', function (Request $request) {
                    return Limit::perMinute(config('rate_limits.api', %d))
                        ->by($this->resolveRequestKey($request))
                        ->response(function (Request $request, array $headers) {
                            return response()->json([
                                'message' => 'Too many requests. Please try again later.',
                                'retry_after' => $headers['Retry-After'] ?? 60,
                            ], 429)->withHeaders($headers);
                        });
                });

                // Authentication rate limiter (stricter for login attempts)
                RateLimiter::for('auth', function (Request $request) {
                    return Limit::perMinute(config('rate_limits.auth', 5))
                        ->by($request->ip())
                        ->response(function (Request $request, array $headers) {
                            return response()->json([
                                'message' => 'Too many authentication attempts. Please try again later.',
                                'retry_after' => $headers['Retry-After'] ?? 60,
                            ], 429)->withHeaders($headers);
                        });
                });

                // Heavy operations rate limiter (exports, reports, etc.)
                RateLimiter::for('heavy', function (Request $request) {
                    return Limit::perMinute(config('rate_limits.heavy', 10))
                        ->by($this->resolveRequestKey($request))
                        ->response(function (Request $request, array $headers) {
                            return response()->json([
                                'message' => 'Too many requests for heavy operations. Please try again later.',
                                'retry_after' => $headers['Retry-After'] ?? 60,
                            ], 429)->withHeaders($headers);
                        });
                });

                // Uploads rate limiter
                RateLimiter::for('uploads', function (Request $request) {
                    return Limit::perMinute(config('rate_limits.uploads', 20))
                        ->by($this->resolveRequestKey($request))
                        ->response(function (Request $request, array $headers) {
                            return response()->json([
                                'message' => 'Too many upload requests. Please try again later.',
                                'retry_after' => $headers['Retry-After'] ?? 60,
                            ], 429)->withHeaders($headers);
                        });
                });

                // Admin rate limiter (more permissive)
                RateLimiter::for('admin', function (Request $request) {
                    return Limit::perMinute(config('rate_limits.admin', 200))
                        ->by($this->resolveRequestKey($request));
                });

                // Public endpoints (more restrictive)
                RateLimiter::for('public', function (Request $request) {
                    return Limit::perMinute(config('rate_limits.public', 30))
                        ->by($request->ip())
                        ->response(function (Request $request, array $headers) {
                            return response()->json([
                                'message' => 'Too many requests. Please try again later.',
                                'retry_after' => $headers['Retry-After'] ?? 60,
                            ], 429)->withHeaders($headers);
                        });
                });
            }

            /**
             * Resolve the request key for rate limiting.
             *
             * Uses authenticated user ID if available, otherwise falls back to IP.
             */
            protected function resolveRequestKey(Request $request): string
            {
                if ($user = $request->user()) {
                    return 'user:' . $user->id;
                }

                return 'ip:' . $request->ip();
            }
        }
        """
                .formatted(defaultLimit);
    }

    private String generateRateLimitConfig(int defaultLimit, boolean useRedis) {
        String cacheDriver = useRedis ? "redis" : "file";
        return """
        <?php

        return [
            /*
            |--------------------------------------------------------------------------
            | Rate Limit Configuration
            |--------------------------------------------------------------------------
            |
            | Configure rate limits for different endpoint types.
            | All values are requests per minute unless otherwise specified.
            |
            */

            // Default API rate limit
            'api' => env('RATE_LIMIT_API', %d),

            // Authentication endpoints (login, register)
            'auth' => env('RATE_LIMIT_AUTH', 5),

            // Heavy operations (exports, reports, bulk operations)
            'heavy' => env('RATE_LIMIT_HEAVY', 10),

            // File upload endpoints
            'uploads' => env('RATE_LIMIT_UPLOADS', 20),

            // Admin endpoints (more permissive)
            'admin' => env('RATE_LIMIT_ADMIN', 200),

            // Public endpoints (more restrictive)
            'public' => env('RATE_LIMIT_PUBLIC', 30),

            /*
            |--------------------------------------------------------------------------
            | Rate Limit Storage
            |--------------------------------------------------------------------------
            |
            | Configure the cache driver used for rate limiting.
            | Options: file, redis, memcached, database
            |
            */

            'cache_driver' => env('RATE_LIMIT_CACHE_DRIVER', '%s'),

            /*
            |--------------------------------------------------------------------------
            | Rate Limit Headers
            |--------------------------------------------------------------------------
            |
            | Configure which headers to include in rate limit responses.
            |
            */

            'headers' => [
                'limit' => 'X-RateLimit-Limit',
                'remaining' => 'X-RateLimit-Remaining',
                'reset' => 'X-RateLimit-Reset',
                'retry_after' => 'Retry-After',
            ],
        ];
        """
                .formatted(defaultLimit, cacheDriver);
    }

    private String generateCustomRateLimiterMiddleware() {
        return """
        <?php

        namespace App\\Http\\Middleware;

        use Closure;
        use Illuminate\\Http\\Request;
        use Illuminate\\Support\\Facades\\RateLimiter;
        use Symfony\\Component\\HttpFoundation\\Response;

        /**
         * Custom rate limiter middleware with enhanced headers.
         *
         * Adds rate limit information to response headers for client consumption.
         */
        class CustomRateLimiter
        {
            /**
             * Handle an incoming request.
             *
             * @param  \\Illuminate\\Http\\Request  $request
             * @param  \\Closure  $next
             * @param  string  $limiterName
             * @return \\Symfony\\Component\\HttpFoundation\\Response
             */
            public function handle(Request $request, Closure $next, string $limiterName = 'api'): Response
            {
                $key = $this->resolveRequestKey($request, $limiterName);

                if (RateLimiter::tooManyAttempts($key, $this->maxAttempts($limiterName))) {
                    return $this->buildTooManyAttemptsResponse($key);
                }

                RateLimiter::hit($key);

                $response = $next($request);

                return $this->addRateLimitHeaders($response, $key, $limiterName);
            }

            /**
             * Resolve the request key for rate limiting.
             */
            protected function resolveRequestKey(Request $request, string $limiterName): string
            {
                $identifier = $request->user()?->id ?? $request->ip();
                return sprintf('%s:%s', $limiterName, $identifier);
            }

            /**
             * Get max attempts for the given limiter.
             */
            protected function maxAttempts(string $limiterName): int
            {
                return config("rate_limits.{$limiterName}", config('rate_limits.api', 60));
            }

            /**
             * Build the too many attempts response.
             */
            protected function buildTooManyAttemptsResponse(string $key): Response
            {
                $retryAfter = RateLimiter::availableIn($key);

                return response()->json([
                    'message' => 'Too many requests. Please try again later.',
                    'retry_after' => $retryAfter,
                ], 429)->withHeaders([
                    config('rate_limits.headers.retry_after', 'Retry-After') => $retryAfter,
                    config('rate_limits.headers.limit', 'X-RateLimit-Limit') => $this->maxAttempts('api'),
                    config('rate_limits.headers.remaining', 'X-RateLimit-Remaining') => 0,
                ]);
            }

            /**
             * Add rate limit headers to the response.
             */
            protected function addRateLimitHeaders(Response $response, string $key, string $limiterName): Response
            {
                $maxAttempts = $this->maxAttempts($limiterName);
                $remainingAttempts = RateLimiter::remaining($key, $maxAttempts);
                $resetTime = RateLimiter::availableIn($key);

                $response->headers->set(
                    config('rate_limits.headers.limit', 'X-RateLimit-Limit'),
                    $maxAttempts
                );
                $response->headers->set(
                    config('rate_limits.headers.remaining', 'X-RateLimit-Remaining'),
                    $remainingAttempts
                );
                $response->headers->set(
                    config('rate_limits.headers.reset', 'X-RateLimit-Reset'),
                    time() + $resetTime
                );

                return $response;
            }
        }
        """;
    }
}
