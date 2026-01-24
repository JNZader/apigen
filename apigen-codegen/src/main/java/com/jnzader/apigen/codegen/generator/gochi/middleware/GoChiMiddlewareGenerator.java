package com.jnzader.apigen.codegen.generator.gochi.middleware;

import com.jnzader.apigen.codegen.generator.gochi.GoChiOptions;

/** Generates middleware for Go/Chi router. */
public class GoChiMiddlewareGenerator {

    private final String moduleName;
    private final GoChiOptions options;

    public GoChiMiddlewareGenerator(String moduleName, GoChiOptions options) {
        this.moduleName = moduleName;
        this.options = options;
    }

    /** Generates a combined middleware file with common middleware. */
    public String generateAll() {
        StringBuilder sb = new StringBuilder();

        sb.append("package middleware\n\n");

        sb.append("import (\n");
        if (options.useMultiTenant()) {
            sb.append("\t\"context\"\n");
        }
        sb.append("\t\"log/slog\"\n");
        sb.append("\t\"net/http\"\n");
        sb.append("\t\"sync\"\n");
        sb.append("\t\"time\"\n\n");
        sb.append("\t\"github.com/go-chi/chi/v5/middleware\"\n");
        sb.append("\t\"golang.org/x/time/rate\"\n");
        sb.append(")\n\n");

        // ContextKey type
        sb.append("// ContextKey is a custom type for context keys.\n");
        sb.append("type ContextKey string\n\n");

        if (options.useMultiTenant()) {
            sb.append("const (\n");
            sb.append("\t// TenantIDKey is the context key for tenant ID.\n");
            sb.append("\tTenantIDKey ContextKey = \"tenantID\"\n");
            sb.append("\t// TenantHeader is the HTTP header for tenant ID.\n");
            sb.append("\tTenantHeader = \"X-Tenant-ID\"\n");
            sb.append(")\n\n");

            sb.append("// TenantAuth extracts tenant ID from header and adds to context.\n");
            sb.append("func TenantAuth(next http.Handler) http.Handler {\n");
            sb.append("\treturn http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {\n");
            sb.append("\t\ttenantID := r.Header.Get(TenantHeader)\n");
            sb.append("\t\tif tenantID == \"\" {\n");
            sb.append("\t\t\thttp.Error(w, \"missing tenant ID\", http.StatusBadRequest)\n");
            sb.append("\t\t\treturn\n");
            sb.append("\t\t}\n\n");
            sb.append("\t\tctx := context.WithValue(r.Context(), TenantIDKey, tenantID)\n");
            sb.append("\t\tnext.ServeHTTP(w, r.WithContext(ctx))\n");
            sb.append("\t})\n");
            sb.append("}\n\n");

            sb.append("// GetTenantID extracts tenant ID from context.\n");
            sb.append("func GetTenantID(ctx context.Context) (string, bool) {\n");
            sb.append("\ttenantID, ok := ctx.Value(TenantIDKey).(string)\n");
            sb.append("\treturn tenantID, ok\n");
            sb.append("}\n\n");
        }

        // Rate Limiter
        sb.append("// IPRateLimiter stores rate limiters per IP address.\n");
        sb.append("type IPRateLimiter struct {\n");
        sb.append("\tlimiters map[string]*rate.Limiter\n");
        sb.append("\tmu       sync.RWMutex\n");
        sb.append("\trate     rate.Limit\n");
        sb.append("\tburst    int\n");
        sb.append("}\n\n");

        sb.append("// NewIPRateLimiter creates a new IP-based rate limiter.\n");
        sb.append("func NewIPRateLimiter(r rate.Limit, burst int) *IPRateLimiter {\n");
        sb.append("\treturn &IPRateLimiter{\n");
        sb.append("\t\tlimiters: make(map[string]*rate.Limiter),\n");
        sb.append("\t\trate:     r,\n");
        sb.append("\t\tburst:    burst,\n");
        sb.append("\t}\n");
        sb.append("}\n\n");

        sb.append("// GetLimiter returns the rate limiter for a given IP.\n");
        sb.append("func (i *IPRateLimiter) GetLimiter(ip string) *rate.Limiter {\n");
        sb.append("\ti.mu.Lock()\n");
        sb.append("\tdefer i.mu.Unlock()\n\n");
        sb.append("\tlimiter, exists := i.limiters[ip]\n");
        sb.append("\tif !exists {\n");
        sb.append("\t\tlimiter = rate.NewLimiter(i.rate, i.burst)\n");
        sb.append("\t\ti.limiters[ip] = limiter\n");
        sb.append("\t}\n\n");
        sb.append("\treturn limiter\n");
        sb.append("}\n\n");

        sb.append("// RateLimit creates a rate limiting middleware.\n");
        sb.append(
                "func RateLimit(requestsPerSecond float64, burst int) func(http.Handler)"
                        + " http.Handler {\n");
        sb.append("\tlimiter := NewIPRateLimiter(rate.Limit(requestsPerSecond), burst)\n\n");
        sb.append("\treturn func(next http.Handler) http.Handler {\n");
        sb.append("\t\treturn http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {\n");
        sb.append("\t\t\tip := r.RemoteAddr\n");
        sb.append("\t\t\tif !limiter.GetLimiter(ip).Allow() {\n");
        sb.append("\t\t\t\tw.Header().Set(\"Retry-After\", \"1\")\n");
        sb.append("\t\t\t\thttp.Error(w, \"rate limit exceeded\", http.StatusTooManyRequests)\n");
        sb.append("\t\t\t\treturn\n");
        sb.append("\t\t\t}\n");
        sb.append("\t\t\tnext.ServeHTTP(w, r)\n");
        sb.append("\t\t})\n");
        sb.append("\t}\n");
        sb.append("}\n\n");

        // Slog Logger
        sb.append("// SlogLogger is a middleware that logs requests using slog.\n");
        sb.append("func SlogLogger(logger *slog.Logger) func(http.Handler) http.Handler {\n");
        sb.append("\treturn func(next http.Handler) http.Handler {\n");
        sb.append("\t\treturn http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {\n");
        sb.append("\t\t\tww := middleware.NewWrapResponseWriter(w, r.ProtoMajor)\n");
        sb.append("\t\t\tstart := time.Now()\n\n");
        sb.append("\t\t\tdefer func() {\n");
        sb.append("\t\t\t\tlogger.Info(\"request completed\",\n");
        sb.append("\t\t\t\t\t\"method\", r.Method,\n");
        sb.append("\t\t\t\t\t\"path\", r.URL.Path,\n");
        sb.append("\t\t\t\t\t\"status\", ww.Status(),\n");
        sb.append("\t\t\t\t\t\"bytes\", ww.BytesWritten(),\n");
        sb.append("\t\t\t\t\t\"duration\", time.Since(start).String(),\n");
        sb.append("\t\t\t\t\t\"request_id\", middleware.GetReqID(r.Context()),\n");
        sb.append("\t\t\t\t)\n");
        sb.append("\t\t\t}()\n\n");
        sb.append("\t\t\tnext.ServeHTTP(ww, r)\n");
        sb.append("\t\t})\n");
        sb.append("\t}\n");
        sb.append("}\n");

        return sb.toString();
    }

    /** Generates JWT authentication module for internal/auth/jwt.go. */
    public String generateJwtAuth() {
        if (!options.useJwt()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();

        sb.append("package auth\n\n");

        sb.append("import (\n");
        sb.append("\t\"context\"\n");
        sb.append("\t\"errors\"\n");
        sb.append("\t\"net/http\"\n");
        sb.append("\t\"strings\"\n");
        sb.append("\t\"time\"\n\n");
        sb.append("\t\"github.com/golang-jwt/jwt/v5\"\n");
        sb.append("\t\"").append(moduleName).append("/internal/config\"\n");
        sb.append(")\n\n");

        sb.append("// ContextKey is a custom type for context keys.\n");
        sb.append("type ContextKey string\n\n");

        sb.append("const (\n");
        sb.append("\t// UserIDKey is the context key for user ID.\n");
        sb.append("\tUserIDKey ContextKey = \"userID\"\n");
        sb.append("\t// ClaimsKey is the context key for JWT claims.\n");
        sb.append("\tClaimsKey ContextKey = \"claims\"\n");
        sb.append(")\n\n");

        sb.append("// Claims represents JWT claims.\n");
        sb.append("type Claims struct {\n");
        sb.append("\tjwt.RegisteredClaims\n");
        sb.append("\tUserID int64  `json:\"user_id\"`\n");
        sb.append("\tEmail  string `json:\"email\"`\n");
        sb.append("\tRole   string `json:\"role,omitempty\"`\n");
        sb.append("}\n\n");

        sb.append("// JWTAuth is a middleware that validates JWT tokens.\n");
        sb.append("func JWTAuth(next http.Handler) http.Handler {\n");
        sb.append("\treturn http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {\n");
        sb.append("\t\tauthHeader := r.Header.Get(\"Authorization\")\n");
        sb.append("\t\tif authHeader == \"\" {\n");
        sb.append(
                "\t\t\thttp.Error(w, \"missing authorization header\", http.StatusUnauthorized)\n");
        sb.append("\t\t\treturn\n");
        sb.append("\t\t}\n\n");
        sb.append("\t\tparts := strings.Split(authHeader, \" \")\n");
        sb.append("\t\tif len(parts) != 2 || parts[0] != \"Bearer\" {\n");
        sb.append(
                "\t\t\thttp.Error(w, \"invalid authorization header format\","
                        + " http.StatusUnauthorized)\n");
        sb.append("\t\t\treturn\n");
        sb.append("\t\t}\n\n");
        sb.append("\t\ttokenString := parts[1]\n");
        sb.append("\t\tclaims, err := ValidateToken(tokenString)\n");
        sb.append("\t\tif err != nil {\n");
        sb.append(
                "\t\t\thttp.Error(w, \"invalid token: \"+err.Error(), http.StatusUnauthorized)\n");
        sb.append("\t\t\treturn\n");
        sb.append("\t\t}\n\n");
        sb.append("\t\tctx := context.WithValue(r.Context(), UserIDKey, claims.UserID)\n");
        sb.append("\t\tctx = context.WithValue(ctx, ClaimsKey, claims)\n");
        sb.append("\t\tnext.ServeHTTP(w, r.WithContext(ctx))\n");
        sb.append("\t})\n");
        sb.append("}\n\n");

        sb.append("// ValidateToken validates a JWT token and returns the claims.\n");
        sb.append("func ValidateToken(tokenString string) (*Claims, error) {\n");
        sb.append("\tcfg := config.Get()\n");
        sb.append("\tsecret := []byte(cfg.JWT.Secret)\n\n");
        sb.append(
                "\ttoken, err := jwt.ParseWithClaims(tokenString, &Claims{}, func(token *jwt.Token)"
                        + " (interface{}, error) {\n");
        sb.append("\t\tif _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {\n");
        sb.append("\t\t\treturn nil, errors.New(\"unexpected signing method\")\n");
        sb.append("\t\t}\n");
        sb.append("\t\treturn secret, nil\n");
        sb.append("\t})\n\n");
        sb.append("\tif err != nil {\n");
        sb.append("\t\treturn nil, err\n");
        sb.append("\t}\n\n");
        sb.append("\tclaims, ok := token.Claims.(*Claims)\n");
        sb.append("\tif !ok || !token.Valid {\n");
        sb.append("\t\treturn nil, errors.New(\"invalid token claims\")\n");
        sb.append("\t}\n\n");
        sb.append("\treturn claims, nil\n");
        sb.append("}\n\n");

        sb.append("// GenerateToken generates a new JWT token.\n");
        sb.append("func GenerateToken(userID int64, email, role string) (string, error) {\n");
        sb.append("\tcfg := config.Get()\n");
        sb.append("\tsecret := []byte(cfg.JWT.Secret)\n");
        sb.append("\texpiration := time.Duration(cfg.JWT.ExpirationHours) * time.Hour\n\n");
        sb.append("\tclaims := &Claims{\n");
        sb.append("\t\tRegisteredClaims: jwt.RegisteredClaims{\n");
        sb.append("\t\t\tExpiresAt: jwt.NewNumericDate(time.Now().Add(expiration)),\n");
        sb.append("\t\t\tIssuedAt:  jwt.NewNumericDate(time.Now()),\n");
        sb.append("\t\t\tIssuer:    cfg.JWT.Issuer,\n");
        sb.append("\t\t},\n");
        sb.append("\t\tUserID: userID,\n");
        sb.append("\t\tEmail:  email,\n");
        sb.append("\t\tRole:   role,\n");
        sb.append("\t}\n\n");
        sb.append("\ttoken := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)\n");
        sb.append("\treturn token.SignedString(secret)\n");
        sb.append("}\n\n");

        sb.append("// GetUserID extracts user ID from context.\n");
        sb.append("func GetUserID(ctx context.Context) (int64, bool) {\n");
        sb.append("\tuserID, ok := ctx.Value(UserIDKey).(int64)\n");
        sb.append("\treturn userID, ok\n");
        sb.append("}\n\n");

        sb.append("// GetClaims extracts claims from context.\n");
        sb.append("func GetClaims(ctx context.Context) (*Claims, bool) {\n");
        sb.append("\tclaims, ok := ctx.Value(ClaimsKey).(*Claims)\n");
        sb.append("\treturn claims, ok\n");
        sb.append("}\n");

        return sb.toString();
    }

    /** Generates password hashing utilities for internal/auth/password.go. */
    public String generatePasswordHash() {
        if (!options.useBcrypt()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();

        sb.append("package auth\n\n");

        sb.append("import (\n");
        sb.append("\t\"golang.org/x/crypto/bcrypt\"\n");
        sb.append(")\n\n");

        sb.append("// DefaultCost is the default bcrypt cost.\n");
        sb.append("const DefaultCost = bcrypt.DefaultCost\n\n");

        sb.append("// HashPassword hashes a password using bcrypt.\n");
        sb.append("func HashPassword(password string) (string, error) {\n");
        sb.append("\thashed, err := bcrypt.GenerateFromPassword([]byte(password), DefaultCost)\n");
        sb.append("\tif err != nil {\n");
        sb.append("\t\treturn \"\", err\n");
        sb.append("\t}\n");
        sb.append("\treturn string(hashed), nil\n");
        sb.append("}\n\n");

        sb.append("// CheckPassword compares a password with a hash.\n");
        sb.append("func CheckPassword(password, hash string) bool {\n");
        sb.append("\terr := bcrypt.CompareHashAndPassword([]byte(hash), []byte(password))\n");
        sb.append("\treturn err == nil\n");
        sb.append("}\n");

        return sb.toString();
    }

    public String generateJwtMiddleware() {
        if (!options.useJwt()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();

        sb.append("package middleware\n\n");

        sb.append("import (\n");
        sb.append("\t\"context\"\n");
        sb.append("\t\"errors\"\n");
        sb.append("\t\"net/http\"\n");
        sb.append("\t\"strings\"\n");
        sb.append("\t\"time\"\n\n");
        sb.append("\t\"github.com/golang-jwt/jwt/v5\"\n");
        sb.append("\t\"").append(moduleName).append("/internal/config\"\n");
        sb.append(")\n\n");

        // Context keys (ContextKey type is defined in middleware.go)
        sb.append("const (\n");
        sb.append("\t// UserIDKey is the context key for user ID.\n");
        sb.append("\tUserIDKey ContextKey = \"userID\"\n");
        sb.append("\t// ClaimsKey is the context key for JWT claims.\n");
        sb.append("\tClaimsKey ContextKey = \"claims\"\n");
        sb.append(")\n\n");

        // Custom claims
        sb.append("// Claims represents JWT claims.\n");
        sb.append("type Claims struct {\n");
        sb.append("\tjwt.RegisteredClaims\n");
        sb.append("\tUserID int64  `json:\"user_id\"`\n");
        sb.append("\tEmail  string `json:\"email\"`\n");
        sb.append("\tRole   string `json:\"role,omitempty\"`\n");
        sb.append("}\n\n");

        // JWT Auth middleware
        sb.append("// JWTAuth is a middleware that validates JWT tokens.\n");
        sb.append("func JWTAuth(next http.Handler) http.Handler {\n");
        sb.append("\treturn http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {\n");
        sb.append("\t\tauthHeader := r.Header.Get(\"Authorization\")\n");
        sb.append("\t\tif authHeader == \"\" {\n");
        sb.append(
                "\t\t\thttp.Error(w, \"missing authorization header\", http.StatusUnauthorized)\n");
        sb.append("\t\t\treturn\n");
        sb.append("\t\t}\n\n");
        sb.append("\t\tparts := strings.Split(authHeader, \" \")\n");
        sb.append("\t\tif len(parts) != 2 || parts[0] != \"Bearer\" {\n");
        sb.append(
                "\t\t\thttp.Error(w, \"invalid authorization header format\","
                        + " http.StatusUnauthorized)\n");
        sb.append("\t\t\treturn\n");
        sb.append("\t\t}\n\n");
        sb.append("\t\ttokenString := parts[1]\n");
        sb.append("\t\tclaims, err := ValidateToken(tokenString)\n");
        sb.append("\t\tif err != nil {\n");
        sb.append(
                "\t\t\thttp.Error(w, \"invalid token: \"+err.Error(), http.StatusUnauthorized)\n");
        sb.append("\t\t\treturn\n");
        sb.append("\t\t}\n\n");
        sb.append("\t\tctx := context.WithValue(r.Context(), UserIDKey, claims.UserID)\n");
        sb.append("\t\tctx = context.WithValue(ctx, ClaimsKey, claims)\n");
        sb.append("\t\tnext.ServeHTTP(w, r.WithContext(ctx))\n");
        sb.append("\t})\n");
        sb.append("}\n\n");

        // ValidateToken function
        sb.append("// ValidateToken validates a JWT token and returns the claims.\n");
        sb.append("func ValidateToken(tokenString string) (*Claims, error) {\n");
        sb.append("\tcfg := config.Get()\n");
        sb.append("\tsecret := []byte(cfg.JWT.Secret)\n\n");
        sb.append(
                "\ttoken, err := jwt.ParseWithClaims(tokenString, &Claims{}, func(token *jwt.Token)"
                        + " (interface{}, error) {\n");
        sb.append("\t\tif _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {\n");
        sb.append("\t\t\treturn nil, errors.New(\"unexpected signing method\")\n");
        sb.append("\t\t}\n");
        sb.append("\t\treturn secret, nil\n");
        sb.append("\t})\n\n");
        sb.append("\tif err != nil {\n");
        sb.append("\t\treturn nil, err\n");
        sb.append("\t}\n\n");
        sb.append("\tclaims, ok := token.Claims.(*Claims)\n");
        sb.append("\tif !ok || !token.Valid {\n");
        sb.append("\t\treturn nil, errors.New(\"invalid token claims\")\n");
        sb.append("\t}\n\n");
        sb.append("\treturn claims, nil\n");
        sb.append("}\n\n");

        // GenerateToken function
        sb.append("// GenerateToken generates a new JWT token.\n");
        sb.append("func GenerateToken(userID int64, email, role string) (string, error) {\n");
        sb.append("\tcfg := config.Get()\n");
        sb.append("\tsecret := []byte(cfg.JWT.Secret)\n");
        sb.append("\texpiration := time.Duration(cfg.JWT.ExpirationHours) * time.Hour\n\n");
        sb.append("\tclaims := &Claims{\n");
        sb.append("\t\tRegisteredClaims: jwt.RegisteredClaims{\n");
        sb.append("\t\t\tExpiresAt: jwt.NewNumericDate(time.Now().Add(expiration)),\n");
        sb.append("\t\t\tIssuedAt:  jwt.NewNumericDate(time.Now()),\n");
        sb.append("\t\t\tIssuer:    cfg.JWT.Issuer,\n");
        sb.append("\t\t},\n");
        sb.append("\t\tUserID: userID,\n");
        sb.append("\t\tEmail:  email,\n");
        sb.append("\t\tRole:   role,\n");
        sb.append("\t}\n\n");
        sb.append("\ttoken := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)\n");
        sb.append("\treturn token.SignedString(secret)\n");
        sb.append("}\n\n");

        // Helper functions
        sb.append("// GetUserID extracts user ID from context.\n");
        sb.append("func GetUserID(ctx context.Context) (int64, bool) {\n");
        sb.append("\tuserID, ok := ctx.Value(UserIDKey).(int64)\n");
        sb.append("\treturn userID, ok\n");
        sb.append("}\n\n");

        sb.append("// GetClaims extracts claims from context.\n");
        sb.append("func GetClaims(ctx context.Context) (*Claims, bool) {\n");
        sb.append("\tclaims, ok := ctx.Value(ClaimsKey).(*Claims)\n");
        sb.append("\treturn claims, ok\n");
        sb.append("}\n");

        return sb.toString();
    }

    public String generateRateLimitMiddleware() {
        StringBuilder sb = new StringBuilder();

        sb.append("package middleware\n\n");

        sb.append("import (\n");
        sb.append("\t\"net/http\"\n");
        sb.append("\t\"sync\"\n");
        sb.append("\t\"time\"\n\n");
        sb.append("\t\"golang.org/x/time/rate\"\n");
        sb.append(")\n\n");

        // Rate limiter per IP
        sb.append("// IPRateLimiter stores rate limiters per IP address.\n");
        sb.append("type IPRateLimiter struct {\n");
        sb.append("\tlimiters map[string]*rate.Limiter\n");
        sb.append("\tmu       sync.RWMutex\n");
        sb.append("\trate     rate.Limit\n");
        sb.append("\tburst    int\n");
        sb.append("}\n\n");

        sb.append("// NewIPRateLimiter creates a new IP-based rate limiter.\n");
        sb.append("func NewIPRateLimiter(r rate.Limit, burst int) *IPRateLimiter {\n");
        sb.append("\treturn &IPRateLimiter{\n");
        sb.append("\t\tlimiters: make(map[string]*rate.Limiter),\n");
        sb.append("\t\trate:     r,\n");
        sb.append("\t\tburst:    burst,\n");
        sb.append("\t}\n");
        sb.append("}\n\n");

        sb.append("// GetLimiter returns the rate limiter for a given IP.\n");
        sb.append("func (i *IPRateLimiter) GetLimiter(ip string) *rate.Limiter {\n");
        sb.append("\ti.mu.Lock()\n");
        sb.append("\tdefer i.mu.Unlock()\n\n");
        sb.append("\tlimiter, exists := i.limiters[ip]\n");
        sb.append("\tif !exists {\n");
        sb.append("\t\tlimiter = rate.NewLimiter(i.rate, i.burst)\n");
        sb.append("\t\ti.limiters[ip] = limiter\n");
        sb.append("\t}\n\n");
        sb.append("\treturn limiter\n");
        sb.append("}\n\n");

        // Middleware
        sb.append("// RateLimit creates a rate limiting middleware.\n");
        sb.append(
                "func RateLimit(requestsPerSecond float64, burst int) func(http.Handler)"
                        + " http.Handler {\n");
        sb.append("\tlimiter := NewIPRateLimiter(rate.Limit(requestsPerSecond), burst)\n\n");
        sb.append("\treturn func(next http.Handler) http.Handler {\n");
        sb.append("\t\treturn http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {\n");
        sb.append("\t\t\tip := r.RemoteAddr\n");
        sb.append("\t\t\tif !limiter.GetLimiter(ip).Allow() {\n");
        sb.append("\t\t\t\tw.Header().Set(\"Retry-After\", \"1\")\n");
        sb.append("\t\t\t\thttp.Error(w, \"rate limit exceeded\", http.StatusTooManyRequests)\n");
        sb.append("\t\t\t\treturn\n");
        sb.append("\t\t\t}\n");
        sb.append("\t\t\tnext.ServeHTTP(w, r)\n");
        sb.append("\t\t})\n");
        sb.append("\t}\n");
        sb.append("}\n\n");

        // Cleanup goroutine
        sb.append("// StartCleanup starts a goroutine to clean up old limiters.\n");
        sb.append("func (i *IPRateLimiter) StartCleanup(interval time.Duration) {\n");
        sb.append("\tgo func() {\n");
        sb.append("\t\tfor {\n");
        sb.append("\t\t\ttime.Sleep(interval)\n");
        sb.append("\t\t\ti.mu.Lock()\n");
        sb.append("\t\t\ti.limiters = make(map[string]*rate.Limiter)\n");
        sb.append("\t\t\ti.mu.Unlock()\n");
        sb.append("\t\t}\n");
        sb.append("\t}()\n");
        sb.append("}\n");

        return sb.toString();
    }

    public String generateTenantMiddleware() {
        if (!options.useMultiTenant()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();

        sb.append("package middleware\n\n");

        sb.append("import (\n");
        sb.append("\t\"context\"\n");
        sb.append("\t\"net/http\"\n");
        sb.append(")\n\n");

        sb.append("const (\n");
        sb.append("\t// TenantIDKey is the context key for tenant ID.\n");
        sb.append("\tTenantIDKey ContextKey = \"tenantID\"\n");
        sb.append("\t// TenantHeader is the HTTP header for tenant ID.\n");
        sb.append("\tTenantHeader = \"X-Tenant-ID\"\n");
        sb.append(")\n\n");

        sb.append("// TenantAuth extracts tenant ID from header and adds to context.\n");
        sb.append("func TenantAuth(next http.Handler) http.Handler {\n");
        sb.append("\treturn http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {\n");
        sb.append("\t\ttenantID := r.Header.Get(TenantHeader)\n");
        sb.append("\t\tif tenantID == \"\" {\n");
        sb.append("\t\t\thttp.Error(w, \"missing tenant ID\", http.StatusBadRequest)\n");
        sb.append("\t\t\treturn\n");
        sb.append("\t\t}\n\n");
        sb.append("\t\tctx := context.WithValue(r.Context(), TenantIDKey, tenantID)\n");
        sb.append("\t\tnext.ServeHTTP(w, r.WithContext(ctx))\n");
        sb.append("\t})\n");
        sb.append("}\n\n");

        sb.append("// GetTenantID extracts tenant ID from context.\n");
        sb.append("func GetTenantID(ctx context.Context) (string, bool) {\n");
        sb.append("\ttenantID, ok := ctx.Value(TenantIDKey).(string)\n");
        sb.append("\treturn tenantID, ok\n");
        sb.append("}\n");

        return sb.toString();
    }

    public String generateLoggingMiddleware() {
        StringBuilder sb = new StringBuilder();

        sb.append("package middleware\n\n");

        sb.append("import (\n");
        sb.append("\t\"log/slog\"\n");
        sb.append("\t\"net/http\"\n");
        sb.append("\t\"time\"\n\n");
        sb.append("\t\"github.com/go-chi/chi/v5/middleware\"\n");
        sb.append(")\n\n");

        sb.append("// SlogLogger is a middleware that logs requests using slog.\n");
        sb.append("func SlogLogger(logger *slog.Logger) func(http.Handler) http.Handler {\n");
        sb.append("\treturn func(next http.Handler) http.Handler {\n");
        sb.append("\t\treturn http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {\n");
        sb.append("\t\t\tww := middleware.NewWrapResponseWriter(w, r.ProtoMajor)\n");
        sb.append("\t\t\tstart := time.Now()\n\n");
        sb.append("\t\t\tdefer func() {\n");
        sb.append("\t\t\t\tlogger.Info(\"request completed\",\n");
        sb.append("\t\t\t\t\t\"method\", r.Method,\n");
        sb.append("\t\t\t\t\t\"path\", r.URL.Path,\n");
        sb.append("\t\t\t\t\t\"status\", ww.Status(),\n");
        sb.append("\t\t\t\t\t\"bytes\", ww.BytesWritten(),\n");
        sb.append("\t\t\t\t\t\"duration\", time.Since(start).String(),\n");
        sb.append("\t\t\t\t\t\"request_id\", middleware.GetReqID(r.Context()),\n");
        sb.append("\t\t\t\t)\n");
        sb.append("\t\t\t}()\n\n");
        sb.append("\t\t\tnext.ServeHTTP(ww, r)\n");
        sb.append("\t\t})\n");
        sb.append("\t}\n");
        sb.append("}\n");

        return sb.toString();
    }
}
