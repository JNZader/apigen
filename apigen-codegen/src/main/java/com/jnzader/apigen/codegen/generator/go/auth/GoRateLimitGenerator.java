package com.jnzader.apigen.codegen.generator.go.auth;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates Go/Gin rate limiting functionality.
 *
 * <p>This generator creates:
 *
 * <ul>
 *   <li>Rate limiter middleware using token bucket algorithm
 *   <li>Rate limit configuration
 *   <li>Redis-based rate limiter (optional)
 *   <li>Per-endpoint rate limit decorators
 * </ul>
 */
public class GoRateLimitGenerator {

    private static final int DEFAULT_REQUESTS_PER_SECOND = 100;
    private static final int DEFAULT_BURST_SIZE = 50;
    private static final boolean DEFAULT_USE_REDIS = false;

    private final String moduleName;

    public GoRateLimitGenerator(String moduleName) {
        this.moduleName = moduleName;
    }

    /**
     * Generates all rate limiting files.
     *
     * @param requestsPerSecond maximum requests per second
     * @param burstSize maximum burst size
     * @param useRedis whether to use Redis for distributed rate limiting
     * @return map of file path to content
     */
    public Map<String, String> generate(int requestsPerSecond, int burstSize, boolean useRedis) {
        Map<String, String> files = new LinkedHashMap<>();

        // Rate limiter middleware
        files.put("internal/middleware/rate_limiter.go", generateRateLimiterMiddleware());

        // Rate limit config
        files.put("internal/middleware/rate_limit_config.go", generateRateLimitConfig());

        // Token bucket rate limiter (in-memory)
        files.put("internal/middleware/token_bucket.go", generateTokenBucket());

        if (useRedis) {
            // Redis rate limiter
            files.put("internal/middleware/redis_rate_limiter.go", generateRedisRateLimiter());
        }

        // Rate limit response helpers
        files.put("internal/middleware/rate_limit_response.go", generateRateLimitResponse());

        // Integration documentation with module-specific imports
        files.put("internal/middleware/RATE_LIMIT.md", generateRateLimitDoc(useRedis));

        return files;
    }

    private String generateRateLimitDoc(boolean useRedis) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Rate Limiting Integration\n\n");
        sb.append("## Setup in main.go\n\n");
        sb.append("Add the middleware import to your main.go:\n\n");
        sb.append("```go\n");
        sb.append("import (\n");
        sb.append("    \"").append(moduleName).append("/internal/middleware\"\n");
        sb.append(")\n");
        sb.append("```\n\n");
        sb.append("## Basic Usage\n\n");
        sb.append("```go\n");
        sb.append("func main() {\n");
        sb.append("    r := gin.Default()\n\n");
        sb.append("    // Apply global rate limiting\n");
        sb.append("    limiter := middleware.NewDefaultRateLimiter()\n");
        sb.append("    r.Use(middleware.RateLimiterMiddleware(limiter, middleware.IPKeyFunc))\n\n");
        sb.append("    // Or apply to specific routes\n");
        sb.append("    api := r.Group(\"/api\")\n");
        sb.append("    api.Use(middleware.RateLimiterMiddleware(\n");
        sb.append("        middleware.NewAPIRateLimiter(),\n");
        sb.append("        middleware.IPKeyFunc,\n");
        sb.append("    ))\n\n");
        sb.append("    // Stricter limits for auth endpoints\n");
        sb.append("    auth := r.Group(\"/auth\")\n");
        sb.append("    auth.Use(middleware.RateLimiterMiddleware(\n");
        sb.append("        middleware.NewAuthRateLimiter(),\n");
        sb.append("        middleware.IPKeyFunc,\n");
        sb.append("    ))\n");
        sb.append("}\n");
        sb.append("```\n\n");
        sb.append("## Available Rate Limiters\n\n");
        sb.append("| Function | Limit | Use Case |\n");
        sb.append("|----------|-------|----------|\n");
        sb.append("| `NewDefaultRateLimiter()` | 100/s | General endpoints |\n");
        sb.append("| `NewAPIRateLimiter()` | 60/s | API endpoints |\n");
        sb.append("| `NewAuthRateLimiter()` | 5/s | Login/Register |\n");
        sb.append("| `NewHeavyRateLimiter()` | 10/s | Resource-intensive ops |\n\n");
        sb.append("## Key Functions\n\n");
        sb.append("- `IPKeyFunc` - Rate limit by client IP\n");
        sb.append("- `UserKeyFunc` - Rate limit by user ID (requires auth)\n");
        sb.append("- `CombinedKeyFunc` - Rate limit by IP + path\n\n");
        sb.append("## Environment Variables\n\n");
        sb.append("| Variable | Default | Description |\n");
        sb.append("|----------|---------|-------------|\n");
        sb.append("| `RATE_LIMIT_REQUESTS_PER_SECOND` | 100 | Default limit |\n");
        sb.append("| `RATE_LIMIT_BURST` | 50 | Burst size |\n");
        sb.append("| `RATE_LIMIT_API` | 60 | API endpoint limit |\n");
        sb.append("| `RATE_LIMIT_AUTH` | 5 | Auth endpoint limit |\n");

        if (useRedis) {
            sb.append("\n## Redis Configuration\n\n");
            sb.append("For distributed rate limiting across multiple instances:\n\n");
            sb.append("| Variable | Default | Description |\n");
            sb.append("|----------|---------|-------------|\n");
            sb.append("| `RATE_LIMIT_USE_REDIS` | false | Enable Redis storage |\n");
            sb.append("| `REDIS_ADDR` | localhost:6379 | Redis address |\n");
            sb.append("| `REDIS_DB` | 0 | Redis database |\n");
        }

        return sb.toString();
    }

    /** Generates with default settings. */
    public Map<String, String> generate() {
        return generate(DEFAULT_REQUESTS_PER_SECOND, DEFAULT_BURST_SIZE, DEFAULT_USE_REDIS);
    }

    private String generateRateLimiterMiddleware() {
        return """
        package middleware

        import (
        	"net/http"
        	"os"
        	"strconv"
        	"sync"
        	"time"

        	"github.com/gin-gonic/gin"
        )

        // RateLimiter defines the interface for rate limiters.
        type RateLimiter interface {
        	Allow(key string) bool
        	AllowN(key string, n int) bool
        	GetLimit() int
        	GetRemaining(key string) int
        	GetResetTime(key string) time.Time
        }

        // RateLimiterMiddleware creates a rate limiting middleware.
        func RateLimiterMiddleware(limiter RateLimiter, keyFunc KeyFunc) gin.HandlerFunc {
        	return func(c *gin.Context) {
        		key := keyFunc(c)

        		if !limiter.Allow(key) {
        			remaining := limiter.GetRemaining(key)
        			resetTime := limiter.GetResetTime(key)

        			c.Header("X-RateLimit-Limit", strconv.Itoa(limiter.GetLimit()))
        			c.Header("X-RateLimit-Remaining", strconv.Itoa(remaining))
        			c.Header("X-RateLimit-Reset", strconv.FormatInt(resetTime.Unix(), 10))
        			c.Header("Retry-After", strconv.FormatInt(time.Until(resetTime).Milliseconds()/1000+1, 10))

        			c.AbortWithStatusJSON(http.StatusTooManyRequests, gin.H{
        				"error":       "too_many_requests",
        				"message":     "Rate limit exceeded. Please try again later.",
        				"retry_after": time.Until(resetTime).Seconds(),
        			})
        			return
        		}

        		remaining := limiter.GetRemaining(key)
        		resetTime := limiter.GetResetTime(key)

        		c.Header("X-RateLimit-Limit", strconv.Itoa(limiter.GetLimit()))
        		c.Header("X-RateLimit-Remaining", strconv.Itoa(remaining))
        		c.Header("X-RateLimit-Reset", strconv.FormatInt(resetTime.Unix(), 10))

        		c.Next()
        	}
        }

        // KeyFunc defines the function type for extracting rate limit keys.
        type KeyFunc func(c *gin.Context) string

        // IPKeyFunc returns client IP as the rate limit key.
        func IPKeyFunc(c *gin.Context) string {
        	return c.ClientIP()
        }

        // UserKeyFunc returns user ID as the rate limit key (requires auth).
        func UserKeyFunc(c *gin.Context) string {
        	if userID, exists := c.Get("user_id"); exists {
        		return "user:" + userID.(string)
        	}
        	return "ip:" + c.ClientIP()
        }

        // CombinedKeyFunc combines IP and path for rate limiting.
        func CombinedKeyFunc(c *gin.Context) string {
        	return c.ClientIP() + ":" + c.Request.URL.Path
        }

        // NewDefaultRateLimiter creates a default in-memory rate limiter.
        func NewDefaultRateLimiter() RateLimiter {
        	limit := getEnvInt("RATE_LIMIT_REQUESTS_PER_SECOND", 100)
        	burst := getEnvInt("RATE_LIMIT_BURST", 50)
        	return NewTokenBucketLimiter(limit, burst)
        }

        // NewAPIRateLimiter creates a rate limiter for API endpoints.
        func NewAPIRateLimiter() RateLimiter {
        	limit := getEnvInt("RATE_LIMIT_API", 60)
        	burst := getEnvInt("RATE_LIMIT_API_BURST", 30)
        	return NewTokenBucketLimiter(limit, burst)
        }

        // NewAuthRateLimiter creates a strict rate limiter for auth endpoints.
        func NewAuthRateLimiter() RateLimiter {
        	limit := getEnvInt("RATE_LIMIT_AUTH", 5)
        	burst := getEnvInt("RATE_LIMIT_AUTH_BURST", 3)
        	return NewTokenBucketLimiter(limit, burst)
        }

        // NewHeavyRateLimiter creates a rate limiter for heavy operations.
        func NewHeavyRateLimiter() RateLimiter {
        	limit := getEnvInt("RATE_LIMIT_HEAVY", 10)
        	burst := getEnvInt("RATE_LIMIT_HEAVY_BURST", 5)
        	return NewTokenBucketLimiter(limit, burst)
        }

        func getEnvInt(key string, defaultValue int) int {
        	if value := os.Getenv(key); value != "" {
        		if i, err := strconv.Atoi(value); err == nil {
        			return i
        		}
        	}
        	return defaultValue
        }

        // LimiterStore holds multiple rate limiters for different purposes.
        type LimiterStore struct {
        	mu       sync.RWMutex
        	limiters map[string]RateLimiter
        }

        // NewLimiterStore creates a new limiter store.
        func NewLimiterStore() *LimiterStore {
        	return &LimiterStore{
        		limiters: make(map[string]RateLimiter),
        	}
        }

        // Get returns a rate limiter by name.
        func (s *LimiterStore) Get(name string) RateLimiter {
        	s.mu.RLock()
        	defer s.mu.RUnlock()
        	return s.limiters[name]
        }

        // Set stores a rate limiter by name.
        func (s *LimiterStore) Set(name string, limiter RateLimiter) {
        	s.mu.Lock()
        	defer s.mu.Unlock()
        	s.limiters[name] = limiter
        }

        // DefaultStore is the global limiter store.
        var DefaultStore = NewLimiterStore()

        func init() {
        	DefaultStore.Set("default", NewDefaultRateLimiter())
        	DefaultStore.Set("api", NewAPIRateLimiter())
        	DefaultStore.Set("auth", NewAuthRateLimiter())
        	DefaultStore.Set("heavy", NewHeavyRateLimiter())
        }
        """;
    }

    private String generateRateLimitConfig() {
        return """
        package middleware

        import (
        	"os"
        	"strconv"
        )

        // RateLimitConfig holds rate limiting configuration.
        type RateLimitConfig struct {
        	// Global settings
        	Enabled           bool
        	RequestsPerSecond int
        	BurstSize         int

        	// Tier-specific limits
        	APILimit         int
        	APIBurst         int
        	AuthLimit        int
        	AuthBurst        int
        	HeavyLimit       int
        	HeavyBurst       int
        	AdminLimit       int
        	AdminBurst       int

        	// Redis settings (for distributed rate limiting)
        	UseRedis   bool
        	RedisAddr  string
        	RedisDB    int
        	KeyPrefix  string
        }

        // LoadRateLimitConfig loads rate limit configuration from environment.
        func LoadRateLimitConfig() *RateLimitConfig {
        	return &RateLimitConfig{
        		Enabled:           getEnvBool("RATE_LIMIT_ENABLED", true),
        		RequestsPerSecond: getEnvInt("RATE_LIMIT_REQUESTS_PER_SECOND", 100),
        		BurstSize:         getEnvInt("RATE_LIMIT_BURST", 50),

        		APILimit:   getEnvInt("RATE_LIMIT_API", 60),
        		APIBurst:   getEnvInt("RATE_LIMIT_API_BURST", 30),
        		AuthLimit:  getEnvInt("RATE_LIMIT_AUTH", 5),
        		AuthBurst:  getEnvInt("RATE_LIMIT_AUTH_BURST", 3),
        		HeavyLimit: getEnvInt("RATE_LIMIT_HEAVY", 10),
        		HeavyBurst: getEnvInt("RATE_LIMIT_HEAVY_BURST", 5),
        		AdminLimit: getEnvInt("RATE_LIMIT_ADMIN", 200),
        		AdminBurst: getEnvInt("RATE_LIMIT_ADMIN_BURST", 100),

        		UseRedis:  getEnvBool("RATE_LIMIT_USE_REDIS", false),
        		RedisAddr: os.Getenv("REDIS_ADDR"),
        		RedisDB:   getEnvInt("REDIS_DB", 0),
        		KeyPrefix: getEnvString("RATE_LIMIT_KEY_PREFIX", "ratelimit:"),
        	}
        }

        func getEnvString(key, defaultValue string) string {
        	if value := os.Getenv(key); value != "" {
        		return value
        	}
        	return defaultValue
        }

        func getEnvBool(key string, defaultValue bool) bool {
        	if value := os.Getenv(key); value != "" {
        		if b, err := strconv.ParseBool(value); err == nil {
        			return b
        		}
        	}
        	return defaultValue
        }
        """;
    }

    private String generateTokenBucket() {
        return """
        package middleware

        import (
        	"sync"
        	"time"
        )

        // TokenBucketLimiter implements the token bucket rate limiting algorithm.
        type TokenBucketLimiter struct {
        	mu       sync.Mutex
        	buckets  map[string]*bucket
        	limit    int
        	burst    int
        	interval time.Duration
        }

        type bucket struct {
        	tokens     float64
        	lastUpdate time.Time
        	resetTime  time.Time
        }

        // NewTokenBucketLimiter creates a new token bucket rate limiter.
        func NewTokenBucketLimiter(requestsPerSecond, burstSize int) *TokenBucketLimiter {
        	return &TokenBucketLimiter{
        		buckets:  make(map[string]*bucket),
        		limit:    requestsPerSecond,
        		burst:    burstSize,
        		interval: time.Second,
        	}
        }

        // Allow checks if a request is allowed for the given key.
        func (l *TokenBucketLimiter) Allow(key string) bool {
        	return l.AllowN(key, 1)
        }

        // AllowN checks if n requests are allowed for the given key.
        func (l *TokenBucketLimiter) AllowN(key string, n int) bool {
        	l.mu.Lock()
        	defer l.mu.Unlock()

        	now := time.Now()
        	b, exists := l.buckets[key]

        	if !exists {
        		b = &bucket{
        			tokens:     float64(l.burst),
        			lastUpdate: now,
        			resetTime:  now.Add(l.interval),
        		}
        		l.buckets[key] = b
        	}

        	// Calculate tokens to add based on elapsed time
        	elapsed := now.Sub(b.lastUpdate)
        	tokensToAdd := elapsed.Seconds() * float64(l.limit)
        	b.tokens = min(float64(l.burst), b.tokens+tokensToAdd)
        	b.lastUpdate = now

        	// Update reset time if needed
        	if now.After(b.resetTime) {
        		b.resetTime = now.Add(l.interval)
        	}

        	// Check if we have enough tokens
        	if b.tokens < float64(n) {
        		return false
        	}

        	b.tokens -= float64(n)
        	return true
        }

        // GetLimit returns the rate limit.
        func (l *TokenBucketLimiter) GetLimit() int {
        	return l.limit
        }

        // GetRemaining returns the remaining requests for a key.
        func (l *TokenBucketLimiter) GetRemaining(key string) int {
        	l.mu.Lock()
        	defer l.mu.Unlock()

        	b, exists := l.buckets[key]
        	if !exists {
        		return l.burst
        	}

        	// Recalculate tokens
        	now := time.Now()
        	elapsed := now.Sub(b.lastUpdate)
        	tokensToAdd := elapsed.Seconds() * float64(l.limit)
        	tokens := min(float64(l.burst), b.tokens+tokensToAdd)

        	return int(tokens)
        }

        // GetResetTime returns the reset time for a key.
        func (l *TokenBucketLimiter) GetResetTime(key string) time.Time {
        	l.mu.Lock()
        	defer l.mu.Unlock()

        	b, exists := l.buckets[key]
        	if !exists {
        		return time.Now().Add(l.interval)
        	}

        	return b.resetTime
        }

        // Cleanup removes expired buckets.
        func (l *TokenBucketLimiter) Cleanup(maxAge time.Duration) {
        	l.mu.Lock()
        	defer l.mu.Unlock()

        	now := time.Now()
        	for key, b := range l.buckets {
        		if now.Sub(b.lastUpdate) > maxAge {
        			delete(l.buckets, key)
        		}
        	}
        }

        // StartCleanup starts a background cleanup goroutine.
        func (l *TokenBucketLimiter) StartCleanup(interval, maxAge time.Duration) {
        	go func() {
        		ticker := time.NewTicker(interval)
        		defer ticker.Stop()

        		for range ticker.C {
        			l.Cleanup(maxAge)
        		}
        	}()
        }

        func min(a, b float64) float64 {
        	if a < b {
        		return a
        	}
        	return b
        }
        """;
    }

    private String generateRedisRateLimiter() {
        return """
        package middleware

        import (
        	"context"
        	"strconv"
        	"time"

        	"github.com/redis/go-redis/v9"
        )

        // RedisRateLimiter implements rate limiting using Redis.
        type RedisRateLimiter struct {
        	client    *redis.Client
        	limit     int
        	window    time.Duration
        	keyPrefix string
        }

        // NewRedisRateLimiter creates a new Redis-based rate limiter.
        func NewRedisRateLimiter(addr string, db int, limit int, window time.Duration, keyPrefix string) (*RedisRateLimiter, error) {
        	client := redis.NewClient(&redis.Options{
        		Addr: addr,
        		DB:   db,
        	})

        	// Test connection
        	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
        	defer cancel()

        	if err := client.Ping(ctx).Err(); err != nil {
        		return nil, err
        	}

        	return &RedisRateLimiter{
        		client:    client,
        		limit:     limit,
        		window:    window,
        		keyPrefix: keyPrefix,
        	}, nil
        }

        // Allow checks if a request is allowed for the given key.
        func (r *RedisRateLimiter) Allow(key string) bool {
        	return r.AllowN(key, 1)
        }

        // AllowN checks if n requests are allowed for the given key.
        func (r *RedisRateLimiter) AllowN(key string, n int) bool {
        	ctx := context.Background()
        	fullKey := r.keyPrefix + key

        	// Use a Lua script for atomic increment and check
        	script := redis.NewScript(`
        		local current = redis.call("INCR", KEYS[1])
        		if current == 1 then
        			redis.call("EXPIRE", KEYS[1], ARGV[1])
        		end
        		return current
        	`)

        	result, err := script.Run(ctx, r.client, []string{fullKey}, int(r.window.Seconds())).Int64()
        	if err != nil {
        		return false
        	}

        	return result <= int64(r.limit)
        }

        // GetLimit returns the rate limit.
        func (r *RedisRateLimiter) GetLimit() int {
        	return r.limit
        }

        // GetRemaining returns the remaining requests for a key.
        func (r *RedisRateLimiter) GetRemaining(key string) int {
        	ctx := context.Background()
        	fullKey := r.keyPrefix + key

        	result, err := r.client.Get(ctx, fullKey).Int64()
        	if err != nil {
        		return r.limit
        	}

        	remaining := r.limit - int(result)
        	if remaining < 0 {
        		return 0
        	}
        	return remaining
        }

        // GetResetTime returns the reset time for a key.
        func (r *RedisRateLimiter) GetResetTime(key string) time.Time {
        	ctx := context.Background()
        	fullKey := r.keyPrefix + key

        	ttl, err := r.client.TTL(ctx, fullKey).Result()
        	if err != nil || ttl < 0 {
        		return time.Now().Add(r.window)
        	}

        	return time.Now().Add(ttl)
        }

        // Close closes the Redis connection.
        func (r *RedisRateLimiter) Close() error {
        	return r.client.Close()
        }

        // SlidingWindowRateLimiter implements sliding window rate limiting using Redis.
        type SlidingWindowRateLimiter struct {
        	client    *redis.Client
        	limit     int
        	window    time.Duration
        	keyPrefix string
        }

        // NewSlidingWindowRateLimiter creates a new sliding window rate limiter.
        func NewSlidingWindowRateLimiter(addr string, db int, limit int, window time.Duration, keyPrefix string) (*SlidingWindowRateLimiter, error) {
        	client := redis.NewClient(&redis.Options{
        		Addr: addr,
        		DB:   db,
        	})

        	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
        	defer cancel()

        	if err := client.Ping(ctx).Err(); err != nil {
        		return nil, err
        	}

        	return &SlidingWindowRateLimiter{
        		client:    client,
        		limit:     limit,
        		window:    window,
        		keyPrefix: keyPrefix,
        	}, nil
        }

        // Allow checks if a request is allowed using sliding window algorithm.
        func (s *SlidingWindowRateLimiter) Allow(key string) bool {
        	return s.AllowN(key, 1)
        }

        // AllowN checks if n requests are allowed.
        func (s *SlidingWindowRateLimiter) AllowN(key string, n int) bool {
        	ctx := context.Background()
        	fullKey := s.keyPrefix + key
        	now := time.Now()
        	windowStart := now.Add(-s.window)

        	pipe := s.client.Pipeline()

        	// Remove old entries
        	pipe.ZRemRangeByScore(ctx, fullKey, "0", strconv.FormatInt(windowStart.UnixMicro(), 10))

        	// Count current entries
        	countCmd := pipe.ZCard(ctx, fullKey)

        	// Add new entry
        	pipe.ZAdd(ctx, fullKey, redis.Z{
        		Score:  float64(now.UnixMicro()),
        		Member: now.UnixMicro(),
        	})

        	// Set expiry
        	pipe.Expire(ctx, fullKey, s.window)

        	_, err := pipe.Exec(ctx)
        	if err != nil {
        		return false
        	}

        	return countCmd.Val() < int64(s.limit)
        }

        // GetLimit returns the rate limit.
        func (s *SlidingWindowRateLimiter) GetLimit() int {
        	return s.limit
        }

        // GetRemaining returns the remaining requests.
        func (s *SlidingWindowRateLimiter) GetRemaining(key string) int {
        	ctx := context.Background()
        	fullKey := s.keyPrefix + key
        	now := time.Now()
        	windowStart := now.Add(-s.window)

        	// Remove old and count
        	s.client.ZRemRangeByScore(ctx, fullKey, "0", strconv.FormatInt(windowStart.UnixMicro(), 10))
        	count, err := s.client.ZCard(ctx, fullKey).Result()
        	if err != nil {
        		return s.limit
        	}

        	remaining := s.limit - int(count)
        	if remaining < 0 {
        		return 0
        	}
        	return remaining
        }

        // GetResetTime returns the reset time.
        func (s *SlidingWindowRateLimiter) GetResetTime(key string) time.Time {
        	return time.Now().Add(s.window)
        }

        // Close closes the Redis connection.
        func (s *SlidingWindowRateLimiter) Close() error {
        	return s.client.Close()
        }
        """;
    }

    private String generateRateLimitResponse() {
        return """
        package middleware

        import (
        	"net/http"

        	"github.com/gin-gonic/gin"
        )

        // RateLimitErrorResponse represents a rate limit error.
        type RateLimitErrorResponse struct {
        	Error      string  `json:"error"`
        	Message    string  `json:"message"`
        	RetryAfter float64 `json:"retry_after"`
        }

        // RateLimitHandler creates a rate limit handler with custom options.
        type RateLimitHandler struct {
        	Limiter      RateLimiter
        	KeyFunc      KeyFunc
        	ErrorHandler func(c *gin.Context, remaining int, resetTime int64)
        }

        // NewRateLimitHandler creates a new rate limit handler.
        func NewRateLimitHandler(limiter RateLimiter) *RateLimitHandler {
        	return &RateLimitHandler{
        		Limiter: limiter,
        		KeyFunc: IPKeyFunc,
        		ErrorHandler: func(c *gin.Context, remaining int, resetTime int64) {
        			c.AbortWithStatusJSON(http.StatusTooManyRequests, RateLimitErrorResponse{
        				Error:   "too_many_requests",
        				Message: "Rate limit exceeded. Please try again later.",
        			})
        		},
        	}
        }

        // WithKeyFunc sets the key function.
        func (h *RateLimitHandler) WithKeyFunc(keyFunc KeyFunc) *RateLimitHandler {
        	h.KeyFunc = keyFunc
        	return h
        }

        // WithErrorHandler sets the error handler.
        func (h *RateLimitHandler) WithErrorHandler(handler func(c *gin.Context, remaining int, resetTime int64)) *RateLimitHandler {
        	h.ErrorHandler = handler
        	return h
        }

        // Middleware returns the gin middleware.
        func (h *RateLimitHandler) Middleware() gin.HandlerFunc {
        	return RateLimiterMiddleware(h.Limiter, h.KeyFunc)
        }

        // SkipRateLimit is a middleware that marks the request to skip rate limiting.
        func SkipRateLimit() gin.HandlerFunc {
        	return func(c *gin.Context) {
        		c.Set("skip_rate_limit", true)
        		c.Next()
        	}
        }

        // ConditionalRateLimiter applies rate limiting conditionally.
        func ConditionalRateLimiter(limiter RateLimiter, keyFunc KeyFunc) gin.HandlerFunc {
        	return func(c *gin.Context) {
        		if skip, exists := c.Get("skip_rate_limit"); exists && skip.(bool) {
        			c.Next()
        			return
        		}

        		RateLimiterMiddleware(limiter, keyFunc)(c)
        	}
        }
        """;
    }
}
