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
        	"errors"
        	"os"
        	"time"

        	"github.com/golang-jwt/jwt/v5"
        	"github.com/google/uuid"
        )

        // JWTClaims represents the JWT claims.
        type JWTClaims struct {
        	jwt.RegisteredClaims
        	UserID   uuid.UUID `json:"user_id"`
        	Email    string    `json:"email"`
        	TokenType string   `json:"token_type"`
        }

        // JWTService handles JWT token operations.
        type JWTService struct {
        	secretKey            []byte
        	accessTokenDuration  time.Duration
        	refreshTokenDuration time.Duration
        }

        // NewJWTService creates a new JWT service.
        func NewJWTService() *JWTService {
        	secret := os.Getenv("JWT_SECRET")
        	if secret == "" {
        		secret = "your-secret-key-change-in-production"
        	}

        	return &JWTService{
        		secretKey:            []byte(secret),
        		accessTokenDuration:  time.Duration(%d) * time.Hour,
        		refreshTokenDuration: time.Duration(168) * time.Hour, // 7 days
        	}
        }

        // GenerateTokenPair creates both access and refresh tokens.
        func (s *JWTService) GenerateTokenPair(user *User) (*TokenPair, error) {
        	accessToken, err := s.generateToken(user, "access", s.accessTokenDuration)
        	if err != nil {
        		return nil, err
        	}

        	refreshToken, err := s.generateToken(user, "refresh", s.refreshTokenDuration)
        	if err != nil {
        		return nil, err
        	}

        	return &TokenPair{
        		AccessToken:  accessToken,
        		RefreshToken: refreshToken,
        		TokenType:    "Bearer",
        		ExpiresIn:    int64(s.accessTokenDuration.Seconds()),
        	}, nil
        }

        func (s *JWTService) generateToken(user *User, tokenType string, duration time.Duration) (string, error) {
        	claims := &JWTClaims{
        		RegisteredClaims: jwt.RegisteredClaims{
        			ExpiresAt: jwt.NewNumericDate(time.Now().Add(duration)),
        			IssuedAt:  jwt.NewNumericDate(time.Now()),
        			NotBefore: jwt.NewNumericDate(time.Now()),
        			Issuer:    "apigen",
        			Subject:   user.ID.String(),
        		},
        		UserID:    user.ID,
        		Email:     user.Email,
        		TokenType: tokenType,
        	}

        	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
        	return token.SignedString(s.secretKey)
        }

        // ValidateToken validates a JWT token and returns the claims.
        func (s *JWTService) ValidateToken(tokenString string) (*JWTClaims, error) {
        	token, err := jwt.ParseWithClaims(tokenString, &JWTClaims{}, func(token *jwt.Token) (interface{}, error) {
        		if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
        			return nil, errors.New("invalid signing method")
        		}
        		return s.secretKey, nil
        	})

        	if err != nil {
        		return nil, err
        	}

        	if claims, ok := token.Claims.(*JWTClaims); ok && token.Valid {
        		return claims, nil
        	}

        	return nil, errors.New("invalid token")
        }

        // ValidateRefreshToken validates a refresh token.
        func (s *JWTService) ValidateRefreshToken(tokenString string) (*JWTClaims, error) {
        	claims, err := s.ValidateToken(tokenString)
        	if err != nil {
        		return nil, err
        	}

        	if claims.TokenType != "refresh" {
        		return nil, errors.New("not a refresh token")
        	}

        	return claims, nil
        }
        """
                .formatted(accessTokenHours);
    }

    private String generateAuthMiddleware() {
        return """
        package auth

        import (
        	"net/http"
        	"strings"

        	"github.com/gin-gonic/gin"
        )

        // AuthMiddleware creates a middleware that validates JWT tokens.
        func AuthMiddleware(jwtService *JWTService) gin.HandlerFunc {
        	return func(c *gin.Context) {
        		authHeader := c.GetHeader("Authorization")
        		if authHeader == "" {
        			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{
        				"error":   "unauthorized",
        				"message": "Missing authorization header",
        			})
        			return
        		}

        		// Check Bearer prefix
        		parts := strings.SplitN(authHeader, " ", 2)
        		if len(parts) != 2 || !strings.EqualFold(parts[0], "Bearer") {
        			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{
        				"error":   "unauthorized",
        				"message": "Invalid authorization header format",
        			})
        			return
        		}

        		tokenString := parts[1]

        		// Validate token
        		claims, err := jwtService.ValidateToken(tokenString)
        		if err != nil {
        			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{
        				"error":   "unauthorized",
        				"message": "Invalid or expired token",
        			})
        			return
        		}

        		// Check token type
        		if claims.TokenType != "access" {
        			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{
        				"error":   "unauthorized",
        				"message": "Invalid token type",
        			})
        			return
        		}

        		// Set user info in context
        		c.Set("user_id", claims.UserID)
        		c.Set("user_email", claims.Email)
        		c.Set("claims", claims)

        		c.Next()
        	}
        }

        // OptionalAuthMiddleware creates a middleware that optionally validates JWT tokens.
        // If a token is present, it validates it. If not, it continues without authentication.
        func OptionalAuthMiddleware(jwtService *JWTService) gin.HandlerFunc {
        	return func(c *gin.Context) {
        		authHeader := c.GetHeader("Authorization")
        		if authHeader == "" {
        			c.Next()
        			return
        		}

        		parts := strings.SplitN(authHeader, " ", 2)
        		if len(parts) != 2 || !strings.EqualFold(parts[0], "Bearer") {
        			c.Next()
        			return
        		}

        		tokenString := parts[1]
        		claims, err := jwtService.ValidateToken(tokenString)
        		if err != nil {
        			c.Next()
        			return
        		}

        		if claims.TokenType == "access" {
        			c.Set("user_id", claims.UserID)
        			c.Set("user_email", claims.Email)
        			c.Set("claims", claims)
        		}

        		c.Next()
        	}
        }
        """;
    }

    private String generateAuthHandler() {
        return """
        package auth

        import (
        	"net/http"

        	"github.com/gin-gonic/gin"
        )

        // AuthHandler handles authentication endpoints.
        type AuthHandler struct {
        	userRepo        *UserRepository
        	jwtService      *JWTService
        	passwordService *PasswordService
        }

        // NewAuthHandler creates a new auth handler.
        func NewAuthHandler(userRepo *UserRepository, jwtService *JWTService) *AuthHandler {
        	return &AuthHandler{
        		userRepo:        userRepo,
        		jwtService:      jwtService,
        		passwordService: NewPasswordService(),
        	}
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
        	var req RegisterRequest
        	if err := c.ShouldBindJSON(&req); err != nil {
        		c.JSON(http.StatusBadRequest, gin.H{
        			"error":   "validation_error",
        			"message": err.Error(),
        		})
        		return
        	}

        	// Check if user already exists
        	existingUser, _ := h.userRepo.FindByEmail(req.Email)
        	if existingUser != nil {
        		c.JSON(http.StatusConflict, gin.H{
        			"error":   "conflict",
        			"message": "Email already registered",
        		})
        		return
        	}

        	// Hash password
        	hashedPassword, err := h.passwordService.Hash(req.Password)
        	if err != nil {
        		c.JSON(http.StatusInternalServerError, gin.H{
        			"error":   "internal_error",
        			"message": "Failed to process password",
        		})
        		return
        	}

        	// Create user
        	user := &User{
        		Name:     req.Name,
        		Email:    req.Email,
        		Password: hashedPassword,
        	}

        	if err := h.userRepo.Create(user); err != nil {
        		c.JSON(http.StatusInternalServerError, gin.H{
        			"error":   "internal_error",
        			"message": "Failed to create user",
        		})
        		return
        	}

        	// Generate tokens
        	tokens, err := h.jwtService.GenerateTokenPair(user)
        	if err != nil {
        		c.JSON(http.StatusInternalServerError, gin.H{
        			"error":   "internal_error",
        			"message": "Failed to generate tokens",
        		})
        		return
        	}

        	c.JSON(http.StatusCreated, AuthResponse{
        		Message: "User registered successfully",
        		User: UserResponse{
        			ID:    user.ID,
        			Name:  user.Name,
        			Email: user.Email,
        		},
        		Tokens: *tokens,
        	})
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
        	var req LoginRequest
        	if err := c.ShouldBindJSON(&req); err != nil {
        		c.JSON(http.StatusBadRequest, gin.H{
        			"error":   "validation_error",
        			"message": err.Error(),
        		})
        		return
        	}

        	// Find user
        	user, err := h.userRepo.FindByEmail(req.Email)
        	if err != nil {
        		c.JSON(http.StatusUnauthorized, gin.H{
        			"error":   "unauthorized",
        			"message": "Invalid credentials",
        		})
        		return
        	}

        	// Verify password
        	if !h.passwordService.Verify(req.Password, user.Password) {
        		c.JSON(http.StatusUnauthorized, gin.H{
        			"error":   "unauthorized",
        			"message": "Invalid credentials",
        		})
        		return
        	}

        	// Generate tokens
        	tokens, err := h.jwtService.GenerateTokenPair(user)
        	if err != nil {
        		c.JSON(http.StatusInternalServerError, gin.H{
        			"error":   "internal_error",
        			"message": "Failed to generate tokens",
        		})
        		return
        	}

        	c.JSON(http.StatusOK, AuthResponse{
        		Message: "Login successful",
        		User: UserResponse{
        			ID:    user.ID,
        			Name:  user.Name,
        			Email: user.Email,
        		},
        		Tokens: *tokens,
        	})
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
        	var req RefreshRequest
        	if err := c.ShouldBindJSON(&req); err != nil {
        		c.JSON(http.StatusBadRequest, gin.H{
        			"error":   "validation_error",
        			"message": err.Error(),
        		})
        		return
        	}

        	// Validate refresh token
        	claims, err := h.jwtService.ValidateRefreshToken(req.RefreshToken)
        	if err != nil {
        		c.JSON(http.StatusUnauthorized, gin.H{
        			"error":   "unauthorized",
        			"message": "Invalid or expired refresh token",
        		})
        		return
        	}

        	// Find user
        	user, err := h.userRepo.FindByID(claims.UserID)
        	if err != nil {
        		c.JSON(http.StatusUnauthorized, gin.H{
        			"error":   "unauthorized",
        			"message": "User not found",
        		})
        		return
        	}

        	// Generate new tokens
        	tokens, err := h.jwtService.GenerateTokenPair(user)
        	if err != nil {
        		c.JSON(http.StatusInternalServerError, gin.H{
        			"error":   "internal_error",
        			"message": "Failed to generate tokens",
        		})
        		return
        	}

        	c.JSON(http.StatusOK, tokens)
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
        	claims, exists := c.Get("claims")
        	if !exists {
        		c.JSON(http.StatusUnauthorized, gin.H{
        			"error":   "unauthorized",
        			"message": "Not authenticated",
        		})
        		return
        	}

        	jwtClaims := claims.(*JWTClaims)
        	user, err := h.userRepo.FindByID(jwtClaims.UserID)
        	if err != nil {
        		c.JSON(http.StatusNotFound, gin.H{
        			"error":   "not_found",
        			"message": "User not found",
        		})
        		return
        	}

        	c.JSON(http.StatusOK, UserResponse{
        		ID:    user.ID,
        		Name:  user.Name,
        		Email: user.Email,
        	})
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
        	claims, exists := c.Get("claims")
        	if !exists {
        		c.JSON(http.StatusUnauthorized, gin.H{
        			"error":   "unauthorized",
        			"message": "Not authenticated",
        		})
        		return
        	}

        	var req ChangePasswordRequest
        	if err := c.ShouldBindJSON(&req); err != nil {
        		c.JSON(http.StatusBadRequest, gin.H{
        			"error":   "validation_error",
        			"message": err.Error(),
        		})
        		return
        	}

        	jwtClaims := claims.(*JWTClaims)
        	user, err := h.userRepo.FindByID(jwtClaims.UserID)
        	if err != nil {
        		c.JSON(http.StatusNotFound, gin.H{
        			"error":   "not_found",
        			"message": "User not found",
        		})
        		return
        	}

        	// Verify current password
        	if !h.passwordService.Verify(req.CurrentPassword, user.Password) {
        		c.JSON(http.StatusBadRequest, gin.H{
        			"error":   "validation_error",
        			"message": "Current password is incorrect",
        		})
        		return
        	}

        	// Hash new password
        	hashedPassword, err := h.passwordService.Hash(req.NewPassword)
        	if err != nil {
        		c.JSON(http.StatusInternalServerError, gin.H{
        			"error":   "internal_error",
        			"message": "Failed to process password",
        		})
        		return
        	}

        	// Update password
        	if err := h.userRepo.UpdatePassword(user.ID, hashedPassword); err != nil {
        		c.JSON(http.StatusInternalServerError, gin.H{
        			"error":   "internal_error",
        			"message": "Failed to update password",
        		})
        		return
        	}

        	c.JSON(http.StatusOK, gin.H{
        		"message": "Password changed successfully",
        	})
        }
        """;
    }

    private String generateAuthDtos() {
        return """
        package auth

        import "github.com/google/uuid"

        // RegisterRequest represents a registration request.
        type RegisterRequest struct {
        	Name     string `json:"name" binding:"required,min=2,max=100"`
        	Email    string `json:"email" binding:"required,email"`
        	Password string `json:"password" binding:"required,min=8"`
        }

        // LoginRequest represents a login request.
        type LoginRequest struct {
        	Email    string `json:"email" binding:"required,email"`
        	Password string `json:"password" binding:"required"`
        }

        // RefreshRequest represents a token refresh request.
        type RefreshRequest struct {
        	RefreshToken string `json:"refresh_token" binding:"required"`
        }

        // ChangePasswordRequest represents a password change request.
        type ChangePasswordRequest struct {
        	CurrentPassword string `json:"current_password" binding:"required"`
        	NewPassword     string `json:"new_password" binding:"required,min=8"`
        }

        // TokenPair represents access and refresh tokens.
        type TokenPair struct {
        	AccessToken  string `json:"access_token"`
        	RefreshToken string `json:"refresh_token"`
        	TokenType    string `json:"token_type"`
        	ExpiresIn    int64  `json:"expires_in"`
        }

        // UserResponse represents a user in API responses.
        type UserResponse struct {
        	ID    uuid.UUID `json:"id"`
        	Name  string    `json:"name"`
        	Email string    `json:"email"`
        }

        // AuthResponse represents an authentication response.
        type AuthResponse struct {
        	Message string       `json:"message"`
        	User    UserResponse `json:"user"`
        	Tokens  TokenPair    `json:"tokens"`
        }
        """;
    }

    private String generateUserModel() {
        return """
        package auth

        import (
        	"time"

        	"github.com/google/uuid"
        	"gorm.io/gorm"
        )

        // User represents an authenticated user.
        type User struct {
        	ID        uuid.UUID      `gorm:"type:uuid;primary_key;default:gen_random_uuid()" json:"id"`
        	Name      string         `gorm:"type:varchar(100);not null" json:"name"`
        	Email     string         `gorm:"type:varchar(255);uniqueIndex;not null" json:"email"`
        	Password  string         `gorm:"type:varchar(255);not null" json:"-"`
        	CreatedAt time.Time      `gorm:"autoCreateTime" json:"created_at"`
        	UpdatedAt time.Time      `gorm:"autoUpdateTime" json:"updated_at"`
        	DeletedAt gorm.DeletedAt `gorm:"index" json:"-"`
        }

        // TableName returns the table name for the User model.
        func (User) TableName() string {
        	return "users"
        }

        // BeforeCreate is called before creating a new user.
        func (u *User) BeforeCreate(tx *gorm.DB) error {
        	if u.ID == uuid.Nil {
        		u.ID = uuid.New()
        	}
        	return nil
        }
        """;
    }

    private String generateUserRepository() {
        return """
        package auth

        import (
        	"github.com/google/uuid"
        	"gorm.io/gorm"
        )

        // UserRepository handles user database operations.
        type UserRepository struct {
        	db *gorm.DB
        }

        // NewUserRepository creates a new user repository.
        func NewUserRepository(db *gorm.DB) *UserRepository {
        	return &UserRepository{db: db}
        }

        // Create creates a new user.
        func (r *UserRepository) Create(user *User) error {
        	return r.db.Create(user).Error
        }

        // FindByID finds a user by ID.
        func (r *UserRepository) FindByID(id uuid.UUID) (*User, error) {
        	var user User
        	err := r.db.Where("id = ?", id).First(&user).Error
        	if err != nil {
        		return nil, err
        	}
        	return &user, nil
        }

        // FindByEmail finds a user by email.
        func (r *UserRepository) FindByEmail(email string) (*User, error) {
        	var user User
        	err := r.db.Where("email = ?", email).First(&user).Error
        	if err != nil {
        		return nil, err
        	}
        	return &user, nil
        }

        // UpdatePassword updates a user's password.
        func (r *UserRepository) UpdatePassword(id uuid.UUID, hashedPassword string) error {
        	return r.db.Model(&User{}).Where("id = ?", id).Update("password", hashedPassword).Error
        }

        // Delete soft deletes a user.
        func (r *UserRepository) Delete(id uuid.UUID) error {
        	return r.db.Delete(&User{}, "id = ?", id).Error
        }
        """;
    }

    private String generatePasswordService() {
        return """
        package auth

        import "golang.org/x/crypto/bcrypt"

        // PasswordService handles password hashing and verification.
        type PasswordService struct {
        	cost int
        }

        // NewPasswordService creates a new password service.
        func NewPasswordService() *PasswordService {
        	return &PasswordService{cost: bcrypt.DefaultCost}
        }

        // Hash hashes a plain text password.
        func (s *PasswordService) Hash(password string) (string, error) {
        	bytes, err := bcrypt.GenerateFromPassword([]byte(password), s.cost)
        	if err != nil {
        		return "", err
        	}
        	return string(bytes), nil
        }

        // Verify checks if a password matches the hash.
        func (s *PasswordService) Verify(password, hash string) bool {
        	err := bcrypt.CompareHashAndPassword([]byte(hash), []byte(password))
        	return err == nil
        }
        """;
    }

    private String generateAuthRoutes() {
        return """
        package auth

        import (
        	"github.com/gin-gonic/gin"
        	"gorm.io/gorm"
        )

        // SetupAuthRoutes configures authentication routes.
        func SetupAuthRoutes(router *gin.RouterGroup, db *gorm.DB) {
        	userRepo := NewUserRepository(db)
        	jwtService := NewJWTService()
        	handler := NewAuthHandler(userRepo, jwtService)

        	auth := router.Group("/auth")
        	{
        		// Public routes
        		auth.POST("/register", handler.Register)
        		auth.POST("/login", handler.Login)
        		auth.POST("/refresh", handler.Refresh)

        		// Protected routes
        		protected := auth.Group("")
        		protected.Use(AuthMiddleware(jwtService))
        		{
        			protected.GET("/profile", handler.Profile)
        			protected.PUT("/password", handler.ChangePassword)
        		}
        	}
        }

        // GetJWTService returns a new JWT service instance.
        // Use this to get the JWT service for middleware.
        func GetJWTService() *JWTService {
        	return NewJWTService()
        }
        """;
    }
}
