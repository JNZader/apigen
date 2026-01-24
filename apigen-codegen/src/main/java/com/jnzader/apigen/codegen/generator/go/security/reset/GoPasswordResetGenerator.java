/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jnzader.apigen.codegen.generator.go.security.reset;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates password reset flow for Go/Gin applications.
 *
 * @author APiGen
 * @since 2.13.0
 */
public class GoPasswordResetGenerator {

    private final String moduleName;

    public GoPasswordResetGenerator(String moduleName) {
        this.moduleName = moduleName;
    }

    /**
     * Generates password reset files.
     *
     * @param tokenExpirationMinutes token expiration in minutes
     * @return map of file path to content
     */
    public Map<String, String> generate(int tokenExpirationMinutes) {
        Map<String, String> files = new LinkedHashMap<>();

        files.put("internal/auth/password_reset_handler.go", generateHandler());
        files.put(
                "internal/auth/password_reset_service.go", generateService(tokenExpirationMinutes));
        files.put("internal/models/password_reset_token.go", generateModel());
        files.put("internal/dto/password_reset_dto.go", generateDto());

        return files;
    }

    private String generateHandler() {
        return String.format(
                """
                package auth

                import (
                	"net/http"

                	"%s/internal/dto"

                	"github.com/gin-gonic/gin"
                )

                // PasswordResetHandler handles password reset endpoints.
                type PasswordResetHandler struct {
                	service *PasswordResetService
                }

                // NewPasswordResetHandler creates a new password reset handler.
                func NewPasswordResetHandler(service *PasswordResetService) *PasswordResetHandler {
                	return &PasswordResetHandler{service: service}
                }

                // RegisterRoutes registers password reset routes.
                func (h *PasswordResetHandler) RegisterRoutes(rg *gin.RouterGroup) {
                	auth := rg.Group("/auth/password")
                	{
                		auth.POST("/forgot", h.ForgotPassword)
                		auth.POST("/validate", h.ValidateToken)
                		auth.POST("/reset", h.ResetPassword)
                	}
                }

                // ForgotPassword godoc
                // @Summary Request password reset email
                // @Tags Password Reset
                // @Accept json
                // @Produce json
                // @Param request body dto.ForgotPasswordRequest true "Forgot password request"
                // @Success 200 {object} dto.ForgotPasswordResponse
                // @Router /auth/password/forgot [post]
                func (h *PasswordResetHandler) ForgotPassword(c *gin.Context) {
                	var req dto.ForgotPasswordRequest
                	if err := c.ShouldBindJSON(&req); err != nil {
                		c.JSON(http.StatusBadRequest, dto.ErrorResponse{Error: err.Error()})
                		return
                	}

                	baseURL := c.Request.URL.Scheme + "://" + c.Request.Host
                	if baseURL == "://" {
                		baseURL = "http://" + c.Request.Host
                	}

                	_ = h.service.RequestPasswordReset(req.Email, baseURL)

                	// Always return success to prevent email enumeration
                	c.JSON(http.StatusOK, dto.ForgotPasswordResponse{
                		Message: "If the email exists, a password reset link has been sent.",
                	})
                }

                // ValidateToken godoc
                // @Summary Validate reset token
                // @Tags Password Reset
                // @Accept json
                // @Produce json
                // @Param request body dto.ValidateTokenRequest true "Validate token request"
                // @Success 200 {object} dto.ValidateTokenResponse
                // @Router /auth/password/validate [post]
                func (h *PasswordResetHandler) ValidateToken(c *gin.Context) {
                	var req dto.ValidateTokenRequest
                	if err := c.ShouldBindJSON(&req); err != nil {
                		c.JSON(http.StatusBadRequest, dto.ErrorResponse{Error: err.Error()})
                		return
                	}

                	result := h.service.ValidateToken(req.Token)
                	c.JSON(http.StatusOK, result)
                }

                // ResetPassword godoc
                // @Summary Reset password with token
                // @Tags Password Reset
                // @Accept json
                // @Produce json
                // @Param request body dto.ResetPasswordRequest true "Reset password request"
                // @Success 200 {object} dto.ResetPasswordResponse
                // @Failure 400 {object} dto.ErrorResponse
                // @Router /auth/password/reset [post]
                func (h *PasswordResetHandler) ResetPassword(c *gin.Context) {
                	var req dto.ResetPasswordRequest
                	if err := c.ShouldBindJSON(&req); err != nil {
                		c.JSON(http.StatusBadRequest, dto.ErrorResponse{Error: err.Error()})
                		return
                	}

                	if req.NewPassword != req.ConfirmPassword {
                		c.JSON(http.StatusBadRequest, dto.ErrorResponse{Error: "Passwords do not match"})
                		return
                	}

                	result, err := h.service.ResetPassword(req.Token, req.NewPassword)
                	if err != nil {
                		c.JSON(http.StatusBadRequest, dto.ErrorResponse{Error: err.Error()})
                		return
                	}

                	c.JSON(http.StatusOK, result)
                }
                """,
                moduleName);
    }

    private String generateService(int tokenExpirationMinutes) {
        return String.format(
                """
                package auth

                import (
                	"crypto/rand"
                	"encoding/hex"
                	"errors"
                	"log"
                	"time"

                	"%s/internal/mail"
                	"%s/internal/models"

                	"golang.org/x/crypto/bcrypt"
                	"gorm.io/gorm"
                )

                const tokenExpirationMinutes = %d

                // PasswordResetService handles password reset operations.
                type PasswordResetService struct {
                	db          *gorm.DB
                	mailService *mail.MailService
                }

                // NewPasswordResetService creates a new password reset service.
                func NewPasswordResetService(db *gorm.DB, mailService *mail.MailService) *PasswordResetService {
                	return &PasswordResetService{
                		db:          db,
                		mailService: mailService,
                	}
                }

                // ValidateTokenResult represents the result of token validation.
                type ValidateTokenResult struct {
                	Valid   bool    `json:"valid"`
                	Message *string `json:"message,omitempty"`
                }

                // ResetPasswordResult represents the result of password reset.
                type ResetPasswordResult struct {
                	Success bool   `json:"success"`
                	Message string `json:"message"`
                }

                // RequestPasswordReset initiates a password reset for the given email.
                func (s *PasswordResetService) RequestPasswordReset(email, baseURL string) error {
                	var user models.User
                	if err := s.db.Where("email = ?", email).First(&user).Error; err != nil {
                		if errors.Is(err, gorm.ErrRecordNotFound) {
                			// Don't reveal if email exists
                			return nil
                		}
                		return err
                	}

                	// Invalidate existing tokens
                	s.db.Where("user_id = ?", user.ID).Delete(&models.PasswordResetToken{})

                	// Generate new token
                	tokenBytes := make([]byte, 32)
                	if _, err := rand.Read(tokenBytes); err != nil {
                		return err
                	}
                	token := hex.EncodeToString(tokenBytes)

                	expiresAt := time.Now().Add(time.Duration(tokenExpirationMinutes) * time.Minute)

                	resetToken := models.PasswordResetToken{
                		UserID:    user.ID,
                		Token:     token,
                		ExpiresAt: expiresAt,
                	}

                	if err := s.db.Create(&resetToken).Error; err != nil {
                		return err
                	}

                	// Send email
                	resetLink := baseURL + "/reset-password?token=" + token
                	userName := user.Email
                	if user.Name != "" {
                		userName = user.Name
                	}

                	if err := s.mailService.SendPasswordResetEmail(email, userName, resetLink, tokenExpirationMinutes); err != nil {
                		log.Printf("Failed to send password reset email: %%v", err)
                	}

                	log.Printf("Password reset email sent to: %%s", email)
                	return nil
                }

                // ValidateToken validates a password reset token.
                func (s *PasswordResetService) ValidateToken(token string) ValidateTokenResult {
                	var resetToken models.PasswordResetToken
                	if err := s.db.Where("token = ? AND used = ?", token, false).First(&resetToken).Error; err != nil {
                		msg := "Invalid or expired token"
                		return ValidateTokenResult{Valid: false, Message: &msg}
                	}

                	if resetToken.ExpiresAt.Before(time.Now()) {
                		msg := "Token has expired"
                		return ValidateTokenResult{Valid: false, Message: &msg}
                	}

                	return ValidateTokenResult{Valid: true}
                }

                // ResetPassword resets the user's password using the token.
                func (s *PasswordResetService) ResetPassword(token, newPassword string) (*ResetPasswordResult, error) {
                	validation := s.ValidateToken(token)
                	if !validation.Valid {
                		return nil, errors.New(*validation.Message)
                	}

                	var resetToken models.PasswordResetToken
                	if err := s.db.Where("token = ?", token).First(&resetToken).Error; err != nil {
                		return nil, errors.New("invalid token")
                	}

                	var user models.User
                	if err := s.db.First(&user, resetToken.UserID).Error; err != nil {
                		return nil, errors.New("user not found")
                	}

                	// Hash new password
                	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(newPassword), bcrypt.DefaultCost)
                	if err != nil {
                		return nil, err
                	}

                	// Update password
                	if err := s.db.Model(&user).Update("password", string(hashedPassword)).Error; err != nil {
                		return nil, err
                	}

                	// Mark token as used
                	s.db.Model(&resetToken).Update("used", true)

                	log.Printf("Password reset for user: %%s", user.Email)
                	return &ResetPasswordResult{
                		Success: true,
                		Message: "Password has been reset successfully",
                	}, nil
                }

                // CleanupExpiredTokens removes expired tokens from the database.
                func (s *PasswordResetService) CleanupExpiredTokens() int64 {
                	result := s.db.Where("expires_at < ?", time.Now()).Delete(&models.PasswordResetToken{})
                	log.Printf("Cleaned up %%d expired password reset tokens", result.RowsAffected)
                	return result.RowsAffected
                }
                """,
                moduleName, moduleName, tokenExpirationMinutes);
    }

    private String generateModel() {
        return """
        package models

        import (
        	"time"

        	"github.com/google/uuid"
        	"gorm.io/gorm"
        )

        // PasswordResetToken represents a password reset token.
        type PasswordResetToken struct {
        	ID        uuid.UUID `gorm:"type:uuid;primaryKey"`
        	UserID    uuid.UUID `gorm:"type:uuid;not null;index"`
        	Token     string    `gorm:"type:varchar(255);uniqueIndex;not null"`
        	ExpiresAt time.Time `gorm:"not null"`
        	Used      bool      `gorm:"default:false"`
        	CreatedAt time.Time
        	User      User `gorm:"foreignKey:UserID"`
        }

        // BeforeCreate sets the UUID before creating the record.
        func (p *PasswordResetToken) BeforeCreate(tx *gorm.DB) error {
        	if p.ID == uuid.Nil {
        		p.ID = uuid.New()
        	}
        	return nil
        }

        // TableName returns the table name for GORM.
        func (PasswordResetToken) TableName() string {
        	return "password_reset_tokens"
        }
        """;
    }

    private String generateDto() {
        return """
        package dto

        // ForgotPasswordRequest represents a forgot password request.
        type ForgotPasswordRequest struct {
        	Email string `json:"email" binding:"required,email"`
        }

        // ForgotPasswordResponse represents a forgot password response.
        type ForgotPasswordResponse struct {
        	Message string `json:"message"`
        }

        // ValidateTokenRequest represents a token validation request.
        type ValidateTokenRequest struct {
        	Token string `json:"token" binding:"required"`
        }

        // ValidateTokenResponse represents a token validation response.
        type ValidateTokenResponse struct {
        	Valid   bool    `json:"valid"`
        	Message *string `json:"message,omitempty"`
        }

        // ResetPasswordRequest represents a password reset request.
        type ResetPasswordRequest struct {
        	Token           string `json:"token" binding:"required"`
        	NewPassword     string `json:"new_password" binding:"required,min=8,max=128"`
        	ConfirmPassword string `json:"confirm_password" binding:"required"`
        }

        // ResetPasswordResponse represents a password reset response.
        type ResetPasswordResponse struct {
        	Success bool   `json:"success"`
        	Message string `json:"message"`
        }
        """;
    }
}
