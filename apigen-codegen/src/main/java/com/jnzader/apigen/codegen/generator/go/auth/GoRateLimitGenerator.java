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
@SuppressWarnings({
    "java:S2479",
    "java:S1192",
    "java:S3400"
}) // S2479: Literal tabs for Go code; S1192: template strings; S3400: template methods return
// constants
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
        \t"net/http"
        \t"os"
        \t"strconv"
        \t"sync"
        \t"time"

        \t"github.com/gin-gonic/gin"
        )

        // RateLimiter defines the interface for rate limiters.
        type RateLimiter interface {
        \tAllow(key string) bool
        \tAllowN(key string, n int) bool
        \tGetLimit() int
        \tGetRemaining(key string) int
        \tGetResetTime(key string) time.Time
        }

        // RateLimiterMiddleware creates a rate limiting middleware.
        func RateLimiterMiddleware(limiter RateLimiter, keyFunc KeyFunc) gin.HandlerFunc {
        \treturn func(c *gin.Context) {
        \t\tkey := keyFunc(c)

        \t\tif !limiter.Allow(key) {
        \t\t\tremaining := limiter.GetRemaining(key)
        \t\t\tresetTime := limiter.GetResetTime(key)

        \t\t\tc.Header("X-RateLimit-Limit", strconv.Itoa(limiter.GetLimit()))
        \t\t\tc.Header("X-RateLimit-Remaining", strconv.Itoa(remaining))
        \t\t\tc.Header("X-RateLimit-Reset", strconv.FormatInt(resetTime.Unix(), 10))
        \t\t\tc.Header("Retry-After", strconv.FormatInt(time.Until(resetTime).Milliseconds()/1000+1, 10))

        \t\t\tc.AbortWithStatusJSON(http.StatusTooManyRequests, gin.H{
        \t\t\t\t"error":       "too_many_requests",
        \t\t\t\t"message":     "Rate limit exceeded. Please try again later.",
        \t\t\t\t"retry_after": time.Until(resetTime).Seconds(),
        \t\t\t})
        \t\t\treturn
        \t\t}

        \t\tremaining := limiter.GetRemaining(key)
        \t\tresetTime := limiter.GetResetTime(key)

        \t\tc.Header("X-RateLimit-Limit", strconv.Itoa(limiter.GetLimit()))
        \t\tc.Header("X-RateLimit-Remaining", strconv.Itoa(remaining))
        \t\tc.Header("X-RateLimit-Reset", strconv.FormatInt(resetTime.Unix(), 10))

        \t\tc.Next()
        \t}
        }

        // KeyFunc defines the function type for extracting rate limit keys.
        type KeyFunc func(c *gin.Context) string

        // IPKeyFunc returns client IP as the rate limit key.
        func IPKeyFunc(c *gin.Context) string {
        \treturn c.ClientIP()
        }

        // UserKeyFunc returns user ID as the rate limit key (requires auth).
        func UserKeyFunc(c *gin.Context) string {
        \tif userID, exists := c.Get("user_id"); exists {
        \t\treturn "user:" + userID.(string)
        \t}
        \treturn "ip:" + c.ClientIP()
        }

        // CombinedKeyFunc combines IP and path for rate limiting.
        func CombinedKeyFunc(c *gin.Context) string {
        \treturn c.ClientIP() + ":" + c.Request.URL.Path
        }

        // NewDefaultRateLimiter creates a default in-memory rate limiter.
        func NewDefaultRateLimiter() RateLimiter {
        \tlimit := getEnvInt("RATE_LIMIT_REQUESTS_PER_SECOND", 100)
        \tburst := getEnvInt("RATE_LIMIT_BURST", 50)
        \treturn NewTokenBucketLimiter(limit, burst)
        }

        // NewAPIRateLimiter creates a rate limiter for API endpoints.
        func NewAPIRateLimiter() RateLimiter {
        \tlimit := getEnvInt("RATE_LIMIT_API", 60)
        \tburst := getEnvInt("RATE_LIMIT_API_BURST", 30)
        \treturn NewTokenBucketLimiter(limit, burst)
        }

        // NewAuthRateLimiter creates a strict rate limiter for auth endpoints.
        func NewAuthRateLimiter() RateLimiter {
        \tlimit := getEnvInt("RATE_LIMIT_AUTH", 5)
        \tburst := getEnvInt("RATE_LIMIT_AUTH_BURST", 3)
        \treturn NewTokenBucketLimiter(limit, burst)
        }

        // NewHeavyRateLimiter creates a rate limiter for heavy operations.
        func NewHeavyRateLimiter() RateLimiter {
        \tlimit := getEnvInt("RATE_LIMIT_HEAVY", 10)
        \tburst := getEnvInt("RATE_LIMIT_HEAVY_BURST", 5)
        \treturn NewTokenBucketLimiter(limit, burst)
        }

        func getEnvInt(key string, defaultValue int) int {
        \tif value := os.Getenv(key); value != "" {
        \t\tif i, err := strconv.Atoi(value); err == nil {
        \t\t\treturn i
        \t\t}
        \t}
        \treturn defaultValue
        }

        // LimiterStore holds multiple rate limiters for different purposes.
        type LimiterStore struct {
        \tmu       sync.RWMutex
        \tlimiters map[string]RateLimiter
        }

        // NewLimiterStore creates a new limiter store.
        func NewLimiterStore() *LimiterStore {
        \treturn &LimiterStore{
        \t\tlimiters: make(map[string]RateLimiter),
        \t}
        }

        // Get returns a rate limiter by name.
        func (s *LimiterStore) Get(name string) RateLimiter {
        \ts.mu.RLock()
        \tdefer s.mu.RUnlock()
        \treturn s.limiters[name]
        }

        // Set stores a rate limiter by name.
        func (s *LimiterStore) Set(name string, limiter RateLimiter) {
        \ts.mu.Lock()
        \tdefer s.mu.Unlock()
        \ts.limiters[name] = limiter
        }

        // DefaultStore is the global limiter store.
        var DefaultStore = NewLimiterStore()

        func init() {
        \tDefaultStore.Set("default", NewDefaultRateLimiter())
        \tDefaultStore.Set("api", NewAPIRateLimiter())
        \tDefaultStore.Set("auth", NewAuthRateLimiter())
        \tDefaultStore.Set("heavy", NewHeavyRateLimiter())
        }
        """;
    }

    private String generateRateLimitConfig() {
        return """
        package middleware

        import (
        \t"os"
        \t"strconv"
        )

        // RateLimitConfig holds rate limiting configuration.
        type RateLimitConfig struct {
        \t// Global settings
        \tEnabled           bool
        \tRequestsPerSecond int
        \tBurstSize         int

        \t// Tier-specific limits
        \tAPILimit         int
        \tAPIBurst         int
        \tAuthLimit        int
        \tAuthBurst        int
        \tHeavyLimit       int
        \tHeavyBurst       int
        \tAdminLimit       int
        \tAdminBurst       int

        \t// Redis settings (for distributed rate limiting)
        \tUseRedis   bool
        \tRedisAddr  string
        \tRedisDB    int
        \tKeyPrefix  string
        }

        // LoadRateLimitConfig loads rate limit configuration from environment.
        func LoadRateLimitConfig() *RateLimitConfig {
        \treturn &RateLimitConfig{
        \t\tEnabled:           getEnvBool("RATE_LIMIT_ENABLED", true),
        \t\tRequestsPerSecond: getEnvInt("RATE_LIMIT_REQUESTS_PER_SECOND", 100),
        \t\tBurstSize:         getEnvInt("RATE_LIMIT_BURST", 50),

        \t\tAPILimit:   getEnvInt("RATE_LIMIT_API", 60),
        \t\tAPIBurst:   getEnvInt("RATE_LIMIT_API_BURST", 30),
        \t\tAuthLimit:  getEnvInt("RATE_LIMIT_AUTH", 5),
        \t\tAuthBurst:  getEnvInt("RATE_LIMIT_AUTH_BURST", 3),
        \t\tHeavyLimit: getEnvInt("RATE_LIMIT_HEAVY", 10),
        \t\tHeavyBurst: getEnvInt("RATE_LIMIT_HEAVY_BURST", 5),
        \t\tAdminLimit: getEnvInt("RATE_LIMIT_ADMIN", 200),
        \t\tAdminBurst: getEnvInt("RATE_LIMIT_ADMIN_BURST", 100),

        \t\tUseRedis:  getEnvBool("RATE_LIMIT_USE_REDIS", false),
        \t\tRedisAddr: os.Getenv("REDIS_ADDR"),
        \t\tRedisDB:   getEnvInt("REDIS_DB", 0),
        \t\tKeyPrefix: getEnvString("RATE_LIMIT_KEY_PREFIX", "ratelimit:"),
        \t}
        }

        func getEnvString(key, defaultValue string) string {
        \tif value := os.Getenv(key); value != "" {
        \t\treturn value
        \t}
        \treturn defaultValue
        }

        func getEnvBool(key string, defaultValue bool) bool {
        \tif value := os.Getenv(key); value != "" {
        \t\tif b, err := strconv.ParseBool(value); err == nil {
        \t\t\treturn b
        \t\t}
        \t}
        \treturn defaultValue
        }
        """;
    }

    private String generateTokenBucket() {
        return """
        package middleware

        import (
        \t"sync"
        \t"time"
        )

        // TokenBucketLimiter implements the token bucket rate limiting algorithm.
        type TokenBucketLimiter struct {
        \tmu       sync.Mutex
        \tbuckets  map[string]*bucket
        \tlimit    int
        \tburst    int
        \tinterval time.Duration
        }

        type bucket struct {
        \ttokens     float64
        \tlastUpdate time.Time
        \tresetTime  time.Time
        }

        // NewTokenBucketLimiter creates a new token bucket rate limiter.
        func NewTokenBucketLimiter(requestsPerSecond, burstSize int) *TokenBucketLimiter {
        \treturn &TokenBucketLimiter{
        \t\tbuckets:  make(map[string]*bucket),
        \t\tlimit:    requestsPerSecond,
        \t\tburst:    burstSize,
        \t\tinterval: time.Second,
        \t}
        }

        // Allow checks if a request is allowed for the given key.
        func (l *TokenBucketLimiter) Allow(key string) bool {
        \treturn l.AllowN(key, 1)
        }

        // AllowN checks if n requests are allowed for the given key.
        func (l *TokenBucketLimiter) AllowN(key string, n int) bool {
        \tl.mu.Lock()
        \tdefer l.mu.Unlock()

        \tnow := time.Now()
        \tb, exists := l.buckets[key]

        \tif !exists {
        \t\tb = &bucket{
        \t\t\ttokens:     float64(l.burst),
        \t\t\tlastUpdate: now,
        \t\t\tresetTime:  now.Add(l.interval),
        \t\t}
        \t\tl.buckets[key] = b
        \t}

        \t// Calculate tokens to add based on elapsed time
        \telapsed := now.Sub(b.lastUpdate)
        \ttokensToAdd := elapsed.Seconds() * float64(l.limit)
        \tb.tokens = min(float64(l.burst), b.tokens+tokensToAdd)
        \tb.lastUpdate = now

        \t// Update reset time if needed
        \tif now.After(b.resetTime) {
        \t\tb.resetTime = now.Add(l.interval)
        \t}

        \t// Check if we have enough tokens
        \tif b.tokens < float64(n) {
        \t\treturn false
        \t}

        \tb.tokens -= float64(n)
        \treturn true
        }

        // GetLimit returns the rate limit.
        func (l *TokenBucketLimiter) GetLimit() int {
        \treturn l.limit
        }

        // GetRemaining returns the remaining requests for a key.
        func (l *TokenBucketLimiter) GetRemaining(key string) int {
        \tl.mu.Lock()
        \tdefer l.mu.Unlock()

        \tb, exists := l.buckets[key]
        \tif !exists {
        \t\treturn l.burst
        \t}

        \t// Recalculate tokens
        \tnow := time.Now()
        \telapsed := now.Sub(b.lastUpdate)
        \ttokensToAdd := elapsed.Seconds() * float64(l.limit)
        \ttokens := min(float64(l.burst), b.tokens+tokensToAdd)

        \treturn int(tokens)
        }

        // GetResetTime returns the reset time for a key.
        func (l *TokenBucketLimiter) GetResetTime(key string) time.Time {
        \tl.mu.Lock()
        \tdefer l.mu.Unlock()

        \tb, exists := l.buckets[key]
        \tif !exists {
        \t\treturn time.Now().Add(l.interval)
        \t}

        \treturn b.resetTime
        }

        // Cleanup removes expired buckets.
        func (l *TokenBucketLimiter) Cleanup(maxAge time.Duration) {
        \tl.mu.Lock()
        \tdefer l.mu.Unlock()

        \tnow := time.Now()
        \tfor key, b := range l.buckets {
        \t\tif now.Sub(b.lastUpdate) > maxAge {
        \t\t\tdelete(l.buckets, key)
        \t\t}
        \t}
        }

        // StartCleanup starts a background cleanup goroutine.
        func (l *TokenBucketLimiter) StartCleanup(interval, maxAge time.Duration) {
        \tgo func() {
        \t\tticker := time.NewTicker(interval)
        \t\tdefer ticker.Stop()

        \t\tfor range ticker.C {
        \t\t\tl.Cleanup(maxAge)
        \t\t}
        \t}()
        }

        func min(a, b float64) float64 {
        \tif a < b {
        \t\treturn a
        \t}
        \treturn b
        }
        """;
    }

    private String generateRedisRateLimiter() {
        return """
        package middleware

        import (
        \t"context"
        \t"strconv"
        \t"time"

        \t"github.com/redis/go-redis/v9"
        )

        // RedisRateLimiter implements rate limiting using Redis.
        type RedisRateLimiter struct {
        \tclient    *redis.Client
        \tlimit     int
        \twindow    time.Duration
        \tkeyPrefix string
        }

        // NewRedisRateLimiter creates a new Redis-based rate limiter.
        func NewRedisRateLimiter(addr string, db int, limit int, window time.Duration, keyPrefix string) (*RedisRateLimiter, error) {
        \tclient := redis.NewClient(&redis.Options{
        \t\tAddr: addr,
        \t\tDB:   db,
        \t})

        \t// Test connection
        \tctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
        \tdefer cancel()

        \tif err := client.Ping(ctx).Err(); err != nil {
        \t\treturn nil, err
        \t}

        \treturn &RedisRateLimiter{
        \t\tclient:    client,
        \t\tlimit:     limit,
        \t\twindow:    window,
        \t\tkeyPrefix: keyPrefix,
        \t}, nil
        }

        // Allow checks if a request is allowed for the given key.
        func (r *RedisRateLimiter) Allow(key string) bool {
        \treturn r.AllowN(key, 1)
        }

        // AllowN checks if n requests are allowed for the given key.
        func (r *RedisRateLimiter) AllowN(key string, n int) bool {
        \tctx := context.Background()
        \tfullKey := r.keyPrefix + key

        \t// Use a Lua script for atomic increment and check
        \tscript := redis.NewScript(`
        \t\tlocal current = redis.call("INCR", KEYS[1])
        \t\tif current == 1 then
        \t\t\tredis.call("EXPIRE", KEYS[1], ARGV[1])
        \t\tend
        \t\treturn current
        \t`)

        \tresult, err := script.Run(ctx, r.client, []string{fullKey}, int(r.window.Seconds())).Int64()
        \tif err != nil {
        \t\treturn false
        \t}

        \treturn result <= int64(r.limit)
        }

        // GetLimit returns the rate limit.
        func (r *RedisRateLimiter) GetLimit() int {
        \treturn r.limit
        }

        // GetRemaining returns the remaining requests for a key.
        func (r *RedisRateLimiter) GetRemaining(key string) int {
        \tctx := context.Background()
        \tfullKey := r.keyPrefix + key

        \tresult, err := r.client.Get(ctx, fullKey).Int64()
        \tif err != nil {
        \t\treturn r.limit
        \t}

        \tremaining := r.limit - int(result)
        \tif remaining < 0 {
        \t\treturn 0
        \t}
        \treturn remaining
        }

        // GetResetTime returns the reset time for a key.
        func (r *RedisRateLimiter) GetResetTime(key string) time.Time {
        \tctx := context.Background()
        \tfullKey := r.keyPrefix + key

        \tttl, err := r.client.TTL(ctx, fullKey).Result()
        \tif err != nil || ttl < 0 {
        \t\treturn time.Now().Add(r.window)
        \t}

        \treturn time.Now().Add(ttl)
        }

        // Close closes the Redis connection.
        func (r *RedisRateLimiter) Close() error {
        \treturn r.client.Close()
        }

        // SlidingWindowRateLimiter implements sliding window rate limiting using Redis.
        type SlidingWindowRateLimiter struct {
        \tclient    *redis.Client
        \tlimit     int
        \twindow    time.Duration
        \tkeyPrefix string
        }

        // NewSlidingWindowRateLimiter creates a new sliding window rate limiter.
        func NewSlidingWindowRateLimiter(addr string, db int, limit int, window time.Duration, keyPrefix string) (*SlidingWindowRateLimiter, error) {
        \tclient := redis.NewClient(&redis.Options{
        \t\tAddr: addr,
        \t\tDB:   db,
        \t})

        \tctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
        \tdefer cancel()

        \tif err := client.Ping(ctx).Err(); err != nil {
        \t\treturn nil, err
        \t}

        \treturn &SlidingWindowRateLimiter{
        \t\tclient:    client,
        \t\tlimit:     limit,
        \t\twindow:    window,
        \t\tkeyPrefix: keyPrefix,
        \t}, nil
        }

        // Allow checks if a request is allowed using sliding window algorithm.
        func (s *SlidingWindowRateLimiter) Allow(key string) bool {
        \treturn s.AllowN(key, 1)
        }

        // AllowN checks if n requests are allowed.
        func (s *SlidingWindowRateLimiter) AllowN(key string, n int) bool {
        \tctx := context.Background()
        \tfullKey := s.keyPrefix + key
        \tnow := time.Now()
        \twindowStart := now.Add(-s.window)

        \tpipe := s.client.Pipeline()

        \t// Remove old entries
        \tpipe.ZRemRangeByScore(ctx, fullKey, "0", strconv.FormatInt(windowStart.UnixMicro(), 10))

        \t// Count current entries
        \tcountCmd := pipe.ZCard(ctx, fullKey)

        \t// Add new entry
        \tpipe.ZAdd(ctx, fullKey, redis.Z{
        \t\tScore:  float64(now.UnixMicro()),
        \t\tMember: now.UnixMicro(),
        \t})

        \t// Set expiry
        \tpipe.Expire(ctx, fullKey, s.window)

        \t_, err := pipe.Exec(ctx)
        \tif err != nil {
        \t\treturn false
        \t}

        \treturn countCmd.Val() < int64(s.limit)
        }

        // GetLimit returns the rate limit.
        func (s *SlidingWindowRateLimiter) GetLimit() int {
        \treturn s.limit
        }

        // GetRemaining returns the remaining requests.
        func (s *SlidingWindowRateLimiter) GetRemaining(key string) int {
        \tctx := context.Background()
        \tfullKey := s.keyPrefix + key
        \tnow := time.Now()
        \twindowStart := now.Add(-s.window)

        \t// Remove old and count
        \ts.client.ZRemRangeByScore(ctx, fullKey, "0", strconv.FormatInt(windowStart.UnixMicro(), 10))
        \tcount, err := s.client.ZCard(ctx, fullKey).Result()
        \tif err != nil {
        \t\treturn s.limit
        \t}

        \tremaining := s.limit - int(count)
        \tif remaining < 0 {
        \t\treturn 0
        \t}
        \treturn remaining
        }

        // GetResetTime returns the reset time.
        func (s *SlidingWindowRateLimiter) GetResetTime(key string) time.Time {
        \treturn time.Now().Add(s.window)
        }

        // Close closes the Redis connection.
        func (s *SlidingWindowRateLimiter) Close() error {
        \treturn s.client.Close()
        }
        """;
    }

    private String generateRateLimitResponse() {
        return """
        package middleware

        import (
        \t"net/http"

        \t"github.com/gin-gonic/gin"
        )

        // RateLimitErrorResponse represents a rate limit error.
        type RateLimitErrorResponse struct {
        \tError      string  `json:"error"`
        \tMessage    string  `json:"message"`
        \tRetryAfter float64 `json:"retry_after"`
        }

        // RateLimitHandler creates a rate limit handler with custom options.
        type RateLimitHandler struct {
        \tLimiter      RateLimiter
        \tKeyFunc      KeyFunc
        \tErrorHandler func(c *gin.Context, remaining int, resetTime int64)
        }

        // NewRateLimitHandler creates a new rate limit handler.
        func NewRateLimitHandler(limiter RateLimiter) *RateLimitHandler {
        \treturn &RateLimitHandler{
        \t\tLimiter: limiter,
        \t\tKeyFunc: IPKeyFunc,
        \t\tErrorHandler: func(c *gin.Context, remaining int, resetTime int64) {
        \t\t\tc.AbortWithStatusJSON(http.StatusTooManyRequests, RateLimitErrorResponse{
        \t\t\t\tError:   "too_many_requests",
        \t\t\t\tMessage: "Rate limit exceeded. Please try again later.",
        \t\t\t})
        \t\t},
        \t}
        }

        // WithKeyFunc sets the key function.
        func (h *RateLimitHandler) WithKeyFunc(keyFunc KeyFunc) *RateLimitHandler {
        \th.KeyFunc = keyFunc
        \treturn h
        }

        // WithErrorHandler sets the error handler.
        func (h *RateLimitHandler) WithErrorHandler(handler func(c *gin.Context, remaining int, resetTime int64)) *RateLimitHandler {
        \th.ErrorHandler = handler
        \treturn h
        }

        // Middleware returns the gin middleware.
        func (h *RateLimitHandler) Middleware() gin.HandlerFunc {
        \treturn RateLimiterMiddleware(h.Limiter, h.KeyFunc)
        }

        // SkipRateLimit is a middleware that marks the request to skip rate limiting.
        func SkipRateLimit() gin.HandlerFunc {
        \treturn func(c *gin.Context) {
        \t\tc.Set("skip_rate_limit", true)
        \t\tc.Next()
        \t}
        }

        // ConditionalRateLimiter applies rate limiting conditionally.
        func ConditionalRateLimiter(limiter RateLimiter, keyFunc KeyFunc) gin.HandlerFunc {
        \treturn func(c *gin.Context) {
        \t\tif skip, exists := c.Get("skip_rate_limit"); exists && skip.(bool) {
        \t\t\tc.Next()
        \t\t\treturn
        \t\t}

        \t\tRateLimiterMiddleware(limiter, keyFunc)(c)
        \t}
        }
        """;
    }
}
