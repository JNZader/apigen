package com.jnzader.apigen.codegen.generator.go.auth;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates Go/Gin JWT authentication functionality.
 *
 * <p>This generator creates:
 *
 * <ul>
 *   <li>JWT service for token generation and validation
 *   <li>Auth middleware for protected routes
 *   <li>Auth handler with login, register, refresh endpoints
 *   <li>User model and repository for authentication
 *   <li>Auth DTOs for requests/responses
 * </ul>
 */
@SuppressWarnings({
    "java:S2479",
    "java:S1192",
    "java:S3400"
}) // S2479: Literal tabs for Go code; S1192: template strings; S3400: template methods return
// constants
public class GoJwtAuthGenerator {

    private static final int DEFAULT_ACCESS_TOKEN_HOURS = 1;
    private static final int DEFAULT_REFRESH_TOKEN_HOURS = 168; // 7 days

    private final String moduleName;

    public GoJwtAuthGenerator(String moduleName) {
        this.moduleName = moduleName;
    }

    /**
     * Generates all JWT authentication files.
     *
     * @param accessTokenHours access token expiration in hours
     * @param refreshTokenHours refresh token expiration in hours
     * @return map of file path to content
     */
    public Map<String, String> generate(int accessTokenHours, int refreshTokenHours) {
        Map<String, String> files = new LinkedHashMap<>();

        // JWT Service
        files.put("internal/auth/jwt_service.go", generateJwtService(accessTokenHours));

        // Auth Middleware
        files.put("internal/auth/middleware.go", generateAuthMiddleware());

        // Auth Handler
        files.put("internal/auth/handler.go", generateAuthHandler());

        // Auth DTOs
        files.put("internal/auth/dto.go", generateAuthDtos());

        // User Model for Auth
        files.put("internal/auth/user_model.go", generateUserModel());

        // User Repository for Auth
        files.put("internal/auth/user_repository.go", generateUserRepository());

        // Password Service
        files.put("internal/auth/password_service.go", generatePasswordService());

        // Auth Routes Helper
        files.put("internal/auth/routes.go", generateAuthRoutes());

        // Integration documentation with module-specific imports
        files.put("internal/auth/INTEGRATION.md", generateIntegrationDoc());

        return files;
    }

    private String generateIntegrationDoc() {
        return """
        # JWT Authentication Integration

        ## Setup in main.go

        Add the auth import to your main.go:

        ```go
        import (
            "%s/internal/auth"
        )
        ```

        ## Initialize and configure routes

        ```go
        func main() {
            // ... database setup ...

            r := gin.Default()

            // Setup auth routes (public: register, login, refresh)
            // Protected routes require AuthMiddleware
            auth.SetupAuthRoutes(r.Group("/api"), db)

            // For custom protected routes, use the middleware:
            jwtService := auth.GetJWTService()
            protected := r.Group("/api/protected")
            protected.Use(auth.AuthMiddleware(jwtService))
            {
                protected.GET("/data", yourHandler)
            }

            r.Run(":8080")
        }
        ```

        ## Environment Variables

        - `JWT_SECRET` - Secret key for signing tokens (required in production)

        ## Endpoints

        | Method | Path | Description | Auth |
        |--------|------|-------------|------|
        | POST | /api/auth/register | Register new user | No |
        | POST | /api/auth/login | Login and get tokens | No |
        | POST | /api/auth/refresh | Refresh access token | No |
        | GET | /api/auth/profile | Get current user | Yes |
        | PUT | /api/auth/password | Change password | Yes |
        """
                .formatted(moduleName);
    }

    /** Generates with default settings. */
    public Map<String, String> generate() {
        return generate(DEFAULT_ACCESS_TOKEN_HOURS, DEFAULT_REFRESH_TOKEN_HOURS);
    }

    private String generateJwtService(int accessTokenHours) {
        return """
        package auth

        import (
        \t"errors"
        \t"os"
        \t"time"

        \t"github.com/golang-jwt/jwt/v5"
        \t"github.com/google/uuid"
        )

        // JWTClaims represents the JWT claims.
        type JWTClaims struct {
        \tjwt.RegisteredClaims
        \tUserID   uuid.UUID `json:"user_id"`
        \tEmail    string    `json:"email"`
        \tTokenType string   `json:"token_type"`
        }

        // JWTService handles JWT token operations.
        type JWTService struct {
        \tsecretKey            []byte
        \taccessTokenDuration  time.Duration
        \trefreshTokenDuration time.Duration
        }

        // NewJWTService creates a new JWT service.
        func NewJWTService() *JWTService {
        \tsecret := os.Getenv("JWT_SECRET")
        \tif secret == "" {
        \t\tsecret = "your-secret-key-change-in-production"
        \t}

        \treturn &JWTService{
        \t\tsecretKey:            []byte(secret),
        \t\taccessTokenDuration:  time.Duration(%d) * time.Hour,
        \t\trefreshTokenDuration: time.Duration(168) * time.Hour, // 7 days
        \t}
        }

        // GenerateTokenPair creates both access and refresh tokens.
        func (s *JWTService) GenerateTokenPair(user *User) (*TokenPair, error) {
        \taccessToken, err := s.generateToken(user, "access", s.accessTokenDuration)
        \tif err != nil {
        \t\treturn nil, err
        \t}

        \trefreshToken, err := s.generateToken(user, "refresh", s.refreshTokenDuration)
        \tif err != nil {
        \t\treturn nil, err
        \t}

        \treturn &TokenPair{
        \t\tAccessToken:  accessToken,
        \t\tRefreshToken: refreshToken,
        \t\tTokenType:    "Bearer",
        \t\tExpiresIn:    int64(s.accessTokenDuration.Seconds()),
        \t}, nil
        }

        func (s *JWTService) generateToken(user *User, tokenType string, duration time.Duration) (string, error) {
        \tclaims := &JWTClaims{
        \t\tRegisteredClaims: jwt.RegisteredClaims{
        \t\t\tExpiresAt: jwt.NewNumericDate(time.Now().Add(duration)),
        \t\t\tIssuedAt:  jwt.NewNumericDate(time.Now()),
        \t\t\tNotBefore: jwt.NewNumericDate(time.Now()),
        \t\t\tIssuer:    "apigen",
        \t\t\tSubject:   user.ID.String(),
        \t\t},
        \t\tUserID:    user.ID,
        \t\tEmail:     user.Email,
        \t\tTokenType: tokenType,
        \t}

        \ttoken := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
        \treturn token.SignedString(s.secretKey)
        }

        // ValidateToken validates a JWT token and returns the claims.
        func (s *JWTService) ValidateToken(tokenString string) (*JWTClaims, error) {
        \ttoken, err := jwt.ParseWithClaims(tokenString, &JWTClaims{}, func(token *jwt.Token) (interface{}, error) {
        \t\tif _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
        \t\t\treturn nil, errors.New("invalid signing method")
        \t\t}
        \t\treturn s.secretKey, nil
        \t})

        \tif err != nil {
        \t\treturn nil, err
        \t}

        \tif claims, ok := token.Claims.(*JWTClaims); ok && token.Valid {
        \t\treturn claims, nil
        \t}

        \treturn nil, errors.New("invalid token")
        }

        // ValidateRefreshToken validates a refresh token.
        func (s *JWTService) ValidateRefreshToken(tokenString string) (*JWTClaims, error) {
        \tclaims, err := s.ValidateToken(tokenString)
        \tif err != nil {
        \t\treturn nil, err
        \t}

        \tif claims.TokenType != "refresh" {
        \t\treturn nil, errors.New("not a refresh token")
        \t}

        \treturn claims, nil
        }
        """
                .formatted(accessTokenHours);
    }

    private String generateAuthMiddleware() {
        return """
        package auth

        import (
        \t"net/http"
        \t"strings"

        \t"github.com/gin-gonic/gin"
        )

        // AuthMiddleware creates a middleware that validates JWT tokens.
        func AuthMiddleware(jwtService *JWTService) gin.HandlerFunc {
        \treturn func(c *gin.Context) {
        \t\tauthHeader := c.GetHeader("Authorization")
        \t\tif authHeader == "" {
        \t\t\tc.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{
        \t\t\t\t"error":   "unauthorized",
        \t\t\t\t"message": "Missing authorization header",
        \t\t\t})
        \t\t\treturn
        \t\t}

        \t\t// Check Bearer prefix
        \t\tparts := strings.SplitN(authHeader, " ", 2)
        \t\tif len(parts) != 2 || !strings.EqualFold(parts[0], "Bearer") {
        \t\t\tc.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{
        \t\t\t\t"error":   "unauthorized",
        \t\t\t\t"message": "Invalid authorization header format",
        \t\t\t})
        \t\t\treturn
        \t\t}

        \t\ttokenString := parts[1]

        \t\t// Validate token
        \t\tclaims, err := jwtService.ValidateToken(tokenString)
        \t\tif err != nil {
        \t\t\tc.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{
        \t\t\t\t"error":   "unauthorized",
        \t\t\t\t"message": "Invalid or expired token",
        \t\t\t})
        \t\t\treturn
        \t\t}

        \t\t// Check token type
        \t\tif claims.TokenType != "access" {
        \t\t\tc.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{
        \t\t\t\t"error":   "unauthorized",
        \t\t\t\t"message": "Invalid token type",
        \t\t\t})
        \t\t\treturn
        \t\t}

        \t\t// Set user info in context
        \t\tc.Set("user_id", claims.UserID)
        \t\tc.Set("user_email", claims.Email)
        \t\tc.Set("claims", claims)

        \t\tc.Next()
        \t}
        }

        // OptionalAuthMiddleware creates a middleware that optionally validates JWT tokens.
        // If a token is present, it validates it. If not, it continues without authentication.
        func OptionalAuthMiddleware(jwtService *JWTService) gin.HandlerFunc {
        \treturn func(c *gin.Context) {
        \t\tauthHeader := c.GetHeader("Authorization")
        \t\tif authHeader == "" {
        \t\t\tc.Next()
        \t\t\treturn
        \t\t}

        \t\tparts := strings.SplitN(authHeader, " ", 2)
        \t\tif len(parts) != 2 || !strings.EqualFold(parts[0], "Bearer") {
        \t\t\tc.Next()
        \t\t\treturn
        \t\t}

        \t\ttokenString := parts[1]
        \t\tclaims, err := jwtService.ValidateToken(tokenString)
        \t\tif err != nil {
        \t\t\tc.Next()
        \t\t\treturn
        \t\t}

        \t\tif claims.TokenType == "access" {
        \t\t\tc.Set("user_id", claims.UserID)
        \t\t\tc.Set("user_email", claims.Email)
        \t\t\tc.Set("claims", claims)
        \t\t}

        \t\tc.Next()
        \t}
        }
        """;
    }

    private String generateAuthHandler() {
        return """
        package auth

        import (
        \t"net/http"

        \t"github.com/gin-gonic/gin"
        )

        // AuthHandler handles authentication endpoints.
        type AuthHandler struct {
        \tuserRepo        *UserRepository
        \tjwtService      *JWTService
        \tpasswordService *PasswordService
        }

        // NewAuthHandler creates a new auth handler.
        func NewAuthHandler(userRepo *UserRepository, jwtService *JWTService) *AuthHandler {
        \treturn &AuthHandler{
        \t\tuserRepo:        userRepo,
        \t\tjwtService:      jwtService,
        \t\tpasswordService: NewPasswordService(),
        \t}
        }

        // Register handles user registration.
        // @Summary Register a new user
        // @Description Register a new user with email and password
        // @Tags auth
        // @Accept json
        // @Produce json
        // @Param request body RegisterRequest true "Registration data"
        // @Success 201 {object} AuthResponse
        // @Failure 400 {object} map[string]interface{}
        // @Failure 409 {object} map[string]interface{}
        // @Router /auth/register [post]
        func (h *AuthHandler) Register(c *gin.Context) {
        \tvar req RegisterRequest
        \tif err := c.ShouldBindJSON(&req); err != nil {
        \t\tc.JSON(http.StatusBadRequest, gin.H{
        \t\t\t"error":   "validation_error",
        \t\t\t"message": err.Error(),
        \t\t})
        \t\treturn
        \t}

        \t// Check if user already exists
        \texistingUser, _ := h.userRepo.FindByEmail(req.Email)
        \tif existingUser != nil {
        \t\tc.JSON(http.StatusConflict, gin.H{
        \t\t\t"error":   "conflict",
        \t\t\t"message": "Email already registered",
        \t\t})
        \t\treturn
        \t}

        \t// Hash password
        \thashedPassword, err := h.passwordService.Hash(req.Password)
        \tif err != nil {
        \t\tc.JSON(http.StatusInternalServerError, gin.H{
        \t\t\t"error":   "internal_error",
        \t\t\t"message": "Failed to process password",
        \t\t})
        \t\treturn
        \t}

        \t// Create user
        \tuser := &User{
        \t\tName:     req.Name,
        \t\tEmail:    req.Email,
        \t\tPassword: hashedPassword,
        \t}

        \tif err := h.userRepo.Create(user); err != nil {
        \t\tc.JSON(http.StatusInternalServerError, gin.H{
        \t\t\t"error":   "internal_error",
        \t\t\t"message": "Failed to create user",
        \t\t})
        \t\treturn
        \t}

        \t// Generate tokens
        \ttokens, err := h.jwtService.GenerateTokenPair(user)
        \tif err != nil {
        \t\tc.JSON(http.StatusInternalServerError, gin.H{
        \t\t\t"error":   "internal_error",
        \t\t\t"message": "Failed to generate tokens",
        \t\t})
        \t\treturn
        \t}

        \tc.JSON(http.StatusCreated, AuthResponse{
        \t\tMessage: "User registered successfully",
        \t\tUser: UserResponse{
        \t\t\tID:    user.ID,
        \t\t\tName:  user.Name,
        \t\t\tEmail: user.Email,
        \t\t},
        \t\tTokens: *tokens,
        \t})
        }

        // Login handles user login.
        // @Summary Login user
        // @Description Login with email and password to get JWT tokens
        // @Tags auth
        // @Accept json
        // @Produce json
        // @Param request body LoginRequest true "Login credentials"
        // @Success 200 {object} AuthResponse
        // @Failure 400 {object} map[string]interface{}
        // @Failure 401 {object} map[string]interface{}
        // @Router /auth/login [post]
        func (h *AuthHandler) Login(c *gin.Context) {
        \tvar req LoginRequest
        \tif err := c.ShouldBindJSON(&req); err != nil {
        \t\tc.JSON(http.StatusBadRequest, gin.H{
        \t\t\t"error":   "validation_error",
        \t\t\t"message": err.Error(),
        \t\t})
        \t\treturn
        \t}

        \t// Find user
        \tuser, err := h.userRepo.FindByEmail(req.Email)
        \tif err != nil {
        \t\tc.JSON(http.StatusUnauthorized, gin.H{
        \t\t\t"error":   "unauthorized",
        \t\t\t"message": "Invalid credentials",
        \t\t})
        \t\treturn
        \t}

        \t// Verify password
        \tif !h.passwordService.Verify(req.Password, user.Password) {
        \t\tc.JSON(http.StatusUnauthorized, gin.H{
        \t\t\t"error":   "unauthorized",
        \t\t\t"message": "Invalid credentials",
        \t\t})
        \t\treturn
        \t}

        \t// Generate tokens
        \ttokens, err := h.jwtService.GenerateTokenPair(user)
        \tif err != nil {
        \t\tc.JSON(http.StatusInternalServerError, gin.H{
        \t\t\t"error":   "internal_error",
        \t\t\t"message": "Failed to generate tokens",
        \t\t})
        \t\treturn
        \t}

        \tc.JSON(http.StatusOK, AuthResponse{
        \t\tMessage: "Login successful",
        \t\tUser: UserResponse{
        \t\t\tID:    user.ID,
        \t\t\tName:  user.Name,
        \t\t\tEmail: user.Email,
        \t\t},
        \t\tTokens: *tokens,
        \t})
        }

        // Refresh handles token refresh.
        // @Summary Refresh tokens
        // @Description Get new access token using refresh token
        // @Tags auth
        // @Accept json
        // @Produce json
        // @Param request body RefreshRequest true "Refresh token"
        // @Success 200 {object} TokenPair
        // @Failure 400 {object} map[string]interface{}
        // @Failure 401 {object} map[string]interface{}
        // @Router /auth/refresh [post]
        func (h *AuthHandler) Refresh(c *gin.Context) {
        \tvar req RefreshRequest
        \tif err := c.ShouldBindJSON(&req); err != nil {
        \t\tc.JSON(http.StatusBadRequest, gin.H{
        \t\t\t"error":   "validation_error",
        \t\t\t"message": err.Error(),
        \t\t})
        \t\treturn
        \t}

        \t// Validate refresh token
        \tclaims, err := h.jwtService.ValidateRefreshToken(req.RefreshToken)
        \tif err != nil {
        \t\tc.JSON(http.StatusUnauthorized, gin.H{
        \t\t\t"error":   "unauthorized",
        \t\t\t"message": "Invalid or expired refresh token",
        \t\t})
        \t\treturn
        \t}

        \t// Find user
        \tuser, err := h.userRepo.FindByID(claims.UserID)
        \tif err != nil {
        \t\tc.JSON(http.StatusUnauthorized, gin.H{
        \t\t\t"error":   "unauthorized",
        \t\t\t"message": "User not found",
        \t\t})
        \t\treturn
        \t}

        \t// Generate new tokens
        \ttokens, err := h.jwtService.GenerateTokenPair(user)
        \tif err != nil {
        \t\tc.JSON(http.StatusInternalServerError, gin.H{
        \t\t\t"error":   "internal_error",
        \t\t\t"message": "Failed to generate tokens",
        \t\t})
        \t\treturn
        \t}

        \tc.JSON(http.StatusOK, tokens)
        }

        // Profile returns the current user's profile.
        // @Summary Get current user profile
        // @Description Get the authenticated user's profile
        // @Tags auth
        // @Produce json
        // @Security BearerAuth
        // @Success 200 {object} UserResponse
        // @Failure 401 {object} map[string]interface{}
        // @Router /auth/profile [get]
        func (h *AuthHandler) Profile(c *gin.Context) {
        \tclaims, exists := c.Get("claims")
        \tif !exists {
        \t\tc.JSON(http.StatusUnauthorized, gin.H{
        \t\t\t"error":   "unauthorized",
        \t\t\t"message": "Not authenticated",
        \t\t})
        \t\treturn
        \t}

        \tjwtClaims := claims.(*JWTClaims)
        \tuser, err := h.userRepo.FindByID(jwtClaims.UserID)
        \tif err != nil {
        \t\tc.JSON(http.StatusNotFound, gin.H{
        \t\t\t"error":   "not_found",
        \t\t\t"message": "User not found",
        \t\t})
        \t\treturn
        \t}

        \tc.JSON(http.StatusOK, UserResponse{
        \t\tID:    user.ID,
        \t\tName:  user.Name,
        \t\tEmail: user.Email,
        \t})
        }

        // ChangePassword handles password change.
        // @Summary Change password
        // @Description Change the authenticated user's password
        // @Tags auth
        // @Accept json
        // @Produce json
        // @Security BearerAuth
        // @Param request body ChangePasswordRequest true "Password change data"
        // @Success 200 {object} map[string]interface{}
        // @Failure 400 {object} map[string]interface{}
        // @Failure 401 {object} map[string]interface{}
        // @Router /auth/password [put]
        func (h *AuthHandler) ChangePassword(c *gin.Context) {
        \tclaims, exists := c.Get("claims")
        \tif !exists {
        \t\tc.JSON(http.StatusUnauthorized, gin.H{
        \t\t\t"error":   "unauthorized",
        \t\t\t"message": "Not authenticated",
        \t\t})
        \t\treturn
        \t}

        \tvar req ChangePasswordRequest
        \tif err := c.ShouldBindJSON(&req); err != nil {
        \t\tc.JSON(http.StatusBadRequest, gin.H{
        \t\t\t"error":   "validation_error",
        \t\t\t"message": err.Error(),
        \t\t})
        \t\treturn
        \t}

        \tjwtClaims := claims.(*JWTClaims)
        \tuser, err := h.userRepo.FindByID(jwtClaims.UserID)
        \tif err != nil {
        \t\tc.JSON(http.StatusNotFound, gin.H{
        \t\t\t"error":   "not_found",
        \t\t\t"message": "User not found",
        \t\t})
        \t\treturn
        \t}

        \t// Verify current password
        \tif !h.passwordService.Verify(req.CurrentPassword, user.Password) {
        \t\tc.JSON(http.StatusBadRequest, gin.H{
        \t\t\t"error":   "validation_error",
        \t\t\t"message": "Current password is incorrect",
        \t\t})
        \t\treturn
        \t}

        \t// Hash new password
        \thashedPassword, err := h.passwordService.Hash(req.NewPassword)
        \tif err != nil {
        \t\tc.JSON(http.StatusInternalServerError, gin.H{
        \t\t\t"error":   "internal_error",
        \t\t\t"message": "Failed to process password",
        \t\t})
        \t\treturn
        \t}

        \t// Update password
        \tif err := h.userRepo.UpdatePassword(user.ID, hashedPassword); err != nil {
        \t\tc.JSON(http.StatusInternalServerError, gin.H{
        \t\t\t"error":   "internal_error",
        \t\t\t"message": "Failed to update password",
        \t\t})
        \t\treturn
        \t}

        \tc.JSON(http.StatusOK, gin.H{
        \t\t"message": "Password changed successfully",
        \t})
        }
        """;
    }

    private String generateAuthDtos() {
        return """
        package auth

        import "github.com/google/uuid"

        // RegisterRequest represents a registration request.
        type RegisterRequest struct {
        \tName     string `json:"name" binding:"required,min=2,max=100"`
        \tEmail    string `json:"email" binding:"required,email"`
        \tPassword string `json:"password" binding:"required,min=8"`
        }

        // LoginRequest represents a login request.
        type LoginRequest struct {
        \tEmail    string `json:"email" binding:"required,email"`
        \tPassword string `json:"password" binding:"required"`
        }

        // RefreshRequest represents a token refresh request.
        type RefreshRequest struct {
        \tRefreshToken string `json:"refresh_token" binding:"required"`
        }

        // ChangePasswordRequest represents a password change request.
        type ChangePasswordRequest struct {
        \tCurrentPassword string `json:"current_password" binding:"required"`
        \tNewPassword     string `json:"new_password" binding:"required,min=8"`
        }

        // TokenPair represents access and refresh tokens.
        type TokenPair struct {
        \tAccessToken  string `json:"access_token"`
        \tRefreshToken string `json:"refresh_token"`
        \tTokenType    string `json:"token_type"`
        \tExpiresIn    int64  `json:"expires_in"`
        }

        // UserResponse represents a user in API responses.
        type UserResponse struct {
        \tID    uuid.UUID `json:"id"`
        \tName  string    `json:"name"`
        \tEmail string    `json:"email"`
        }

        // AuthResponse represents an authentication response.
        type AuthResponse struct {
        \tMessage string       `json:"message"`
        \tUser    UserResponse `json:"user"`
        \tTokens  TokenPair    `json:"tokens"`
        }
        """;
    }

    private String generateUserModel() {
        return """
        package auth

        import (
        \t"time"

        \t"github.com/google/uuid"
        \t"gorm.io/gorm"
        )

        // User represents an authenticated user.
        type User struct {
        \tID        uuid.UUID      `gorm:"type:uuid;primary_key;default:gen_random_uuid()" json:"id"`
        \tName      string         `gorm:"type:varchar(100);not null" json:"name"`
        \tEmail     string         `gorm:"type:varchar(255);uniqueIndex;not null" json:"email"`
        \tPassword  string         `gorm:"type:varchar(255);not null" json:"-"`
        \tCreatedAt time.Time      `gorm:"autoCreateTime" json:"created_at"`
        \tUpdatedAt time.Time      `gorm:"autoUpdateTime" json:"updated_at"`
        \tDeletedAt gorm.DeletedAt `gorm:"index" json:"-"`
        }

        // TableName returns the table name for the User model.
        func (User) TableName() string {
        \treturn "users"
        }

        // BeforeCreate is called before creating a new user.
        func (u *User) BeforeCreate(tx *gorm.DB) error {
        \tif u.ID == uuid.Nil {
        \t\tu.ID = uuid.New()
        \t}
        \treturn nil
        }
        """;
    }

    private String generateUserRepository() {
        return """
        package auth

        import (
        \t"github.com/google/uuid"
        \t"gorm.io/gorm"
        )

        // UserRepository handles user database operations.
        type UserRepository struct {
        \tdb *gorm.DB
        }

        // NewUserRepository creates a new user repository.
        func NewUserRepository(db *gorm.DB) *UserRepository {
        \treturn &UserRepository{db: db}
        }

        // Create creates a new user.
        func (r *UserRepository) Create(user *User) error {
        \treturn r.db.Create(user).Error
        }

        // FindByID finds a user by ID.
        func (r *UserRepository) FindByID(id uuid.UUID) (*User, error) {
        \tvar user User
        \terr := r.db.Where("id = ?", id).First(&user).Error
        \tif err != nil {
        \t\treturn nil, err
        \t}
        \treturn &user, nil
        }

        // FindByEmail finds a user by email.
        func (r *UserRepository) FindByEmail(email string) (*User, error) {
        \tvar user User
        \terr := r.db.Where("email = ?", email).First(&user).Error
        \tif err != nil {
        \t\treturn nil, err
        \t}
        \treturn &user, nil
        }

        // UpdatePassword updates a user's password.
        func (r *UserRepository) UpdatePassword(id uuid.UUID, hashedPassword string) error {
        \treturn r.db.Model(&User{}).Where("id = ?", id).Update("password", hashedPassword).Error
        }

        // Delete soft deletes a user.
        func (r *UserRepository) Delete(id uuid.UUID) error {
        \treturn r.db.Delete(&User{}, "id = ?", id).Error
        }
        """;
    }

    private String generatePasswordService() {
        return """
        package auth

        import "golang.org/x/crypto/bcrypt"

        // PasswordService handles password hashing and verification.
        type PasswordService struct {
        \tcost int
        }

        // NewPasswordService creates a new password service.
        func NewPasswordService() *PasswordService {
        \treturn &PasswordService{cost: bcrypt.DefaultCost}
        }

        // Hash hashes a plain text password.
        func (s *PasswordService) Hash(password string) (string, error) {
        \tbytes, err := bcrypt.GenerateFromPassword([]byte(password), s.cost)
        \tif err != nil {
        \t\treturn "", err
        \t}
        \treturn string(bytes), nil
        }

        // Verify checks if a password matches the hash.
        func (s *PasswordService) Verify(password, hash string) bool {
        \terr := bcrypt.CompareHashAndPassword([]byte(hash), []byte(password))
        \treturn err == nil
        }
        """;
    }

    private String generateAuthRoutes() {
        return """
        package auth

        import (
        \t"github.com/gin-gonic/gin"
        \t"gorm.io/gorm"
        )

        // SetupAuthRoutes configures authentication routes.
        func SetupAuthRoutes(router *gin.RouterGroup, db *gorm.DB) {
        \tuserRepo := NewUserRepository(db)
        \tjwtService := NewJWTService()
        \thandler := NewAuthHandler(userRepo, jwtService)

        \tauth := router.Group("/auth")
        \t{
        \t\t// Public routes
        \t\tauth.POST("/register", handler.Register)
        \t\tauth.POST("/login", handler.Login)
        \t\tauth.POST("/refresh", handler.Refresh)

        \t\t// Protected routes
        \t\tprotected := auth.Group("")
        \t\tprotected.Use(AuthMiddleware(jwtService))
        \t\t{
        \t\t\tprotected.GET("/profile", handler.Profile)
        \t\t\tprotected.PUT("/password", handler.ChangePassword)
        \t\t}
        \t}
        }

        // GetJWTService returns a new JWT service instance.
        // Use this to get the JWT service for middleware.
        func GetJWTService() *JWTService {
        \treturn NewJWTService()
        }
        """;
    }
}
