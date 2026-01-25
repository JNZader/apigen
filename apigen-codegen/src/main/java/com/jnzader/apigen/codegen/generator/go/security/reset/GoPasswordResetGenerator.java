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
@SuppressWarnings({
    "java:S2479",
    "java:S1192",
    "java:S3400"
}) // S2479: Literal tabs for Go code; S1192: template strings; S3400: template methods return
// constants
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
                \t"net/http"

                \t"%s/internal/dto"

                \t"github.com/gin-gonic/gin"
                )

                // PasswordResetHandler handles password reset endpoints.
                type PasswordResetHandler struct {
                \tservice *PasswordResetService
                }

                // NewPasswordResetHandler creates a new password reset handler.
                func NewPasswordResetHandler(service *PasswordResetService) *PasswordResetHandler {
                \treturn &PasswordResetHandler{service: service}
                }

                // RegisterRoutes registers password reset routes.
                func (h *PasswordResetHandler) RegisterRoutes(rg *gin.RouterGroup) {
                \tauth := rg.Group("/auth/password")
                \t{
                \t\tauth.POST("/forgot", h.ForgotPassword)
                \t\tauth.POST("/validate", h.ValidateToken)
                \t\tauth.POST("/reset", h.ResetPassword)
                \t}
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
                \tvar req dto.ForgotPasswordRequest
                \tif err := c.ShouldBindJSON(&req); err != nil {
                \t\tc.JSON(http.StatusBadRequest, dto.ErrorResponse{Error: err.Error()})
                \t\treturn
                \t}

                \tbaseURL := c.Request.URL.Scheme + "://" + c.Request.Host
                \tif baseURL == "://" {
                \t\tbaseURL = "http://" + c.Request.Host
                \t}

                \t_ = h.service.RequestPasswordReset(req.Email, baseURL)

                \t// Always return success to prevent email enumeration
                \tc.JSON(http.StatusOK, dto.ForgotPasswordResponse{
                \t\tMessage: "If the email exists, a password reset link has been sent.",
                \t})
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
                \tvar req dto.ValidateTokenRequest
                \tif err := c.ShouldBindJSON(&req); err != nil {
                \t\tc.JSON(http.StatusBadRequest, dto.ErrorResponse{Error: err.Error()})
                \t\treturn
                \t}

                \tresult := h.service.ValidateToken(req.Token)
                \tc.JSON(http.StatusOK, result)
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
                \tvar req dto.ResetPasswordRequest
                \tif err := c.ShouldBindJSON(&req); err != nil {
                \t\tc.JSON(http.StatusBadRequest, dto.ErrorResponse{Error: err.Error()})
                \t\treturn
                \t}

                \tif req.NewPassword != req.ConfirmPassword {
                \t\tc.JSON(http.StatusBadRequest, dto.ErrorResponse{Error: "Passwords do not match"})
                \t\treturn
                \t}

                \tresult, err := h.service.ResetPassword(req.Token, req.NewPassword)
                \tif err != nil {
                \t\tc.JSON(http.StatusBadRequest, dto.ErrorResponse{Error: err.Error()})
                \t\treturn
                \t}

                \tc.JSON(http.StatusOK, result)
                }
                """,
                moduleName);
    }

    private String generateService(int tokenExpirationMinutes) {
        return String.format(
                """
                package auth

                import (
                \t"crypto/rand"
                \t"encoding/hex"
                \t"errors"
                \t"log"
                \t"time"

                \t"%s/internal/mail"
                \t"%s/internal/models"

                \t"golang.org/x/crypto/bcrypt"
                \t"gorm.io/gorm"
                )

                const tokenExpirationMinutes = %d

                // PasswordResetService handles password reset operations.
                type PasswordResetService struct {
                \tdb          *gorm.DB
                \tmailService *mail.MailService
                }

                // NewPasswordResetService creates a new password reset service.
                func NewPasswordResetService(db *gorm.DB, mailService *mail.MailService) *PasswordResetService {
                \treturn &PasswordResetService{
                \t\tdb:          db,
                \t\tmailService: mailService,
                \t}
                }

                // ValidateTokenResult represents the result of token validation.
                type ValidateTokenResult struct {
                \tValid   bool    `json:"valid"`
                \tMessage *string `json:"message,omitempty"`
                }

                // ResetPasswordResult represents the result of password reset.
                type ResetPasswordResult struct {
                \tSuccess bool   `json:"success"`
                \tMessage string `json:"message"`
                }

                // RequestPasswordReset initiates a password reset for the given email.
                func (s *PasswordResetService) RequestPasswordReset(email, baseURL string) error {
                \tvar user models.User
                \tif err := s.db.Where("email = ?", email).First(&user).Error; err != nil {
                \t\tif errors.Is(err, gorm.ErrRecordNotFound) {
                \t\t\t// Don't reveal if email exists
                \t\t\treturn nil
                \t\t}
                \t\treturn err
                \t}

                \t// Invalidate existing tokens
                \ts.db.Where("user_id = ?", user.ID).Delete(&models.PasswordResetToken{})

                \t// Generate new token
                \ttokenBytes := make([]byte, 32)
                \tif _, err := rand.Read(tokenBytes); err != nil {
                \t\treturn err
                \t}
                \ttoken := hex.EncodeToString(tokenBytes)

                \texpiresAt := time.Now().Add(time.Duration(tokenExpirationMinutes) * time.Minute)

                \tresetToken := models.PasswordResetToken{
                \t\tUserID:    user.ID,
                \t\tToken:     token,
                \t\tExpiresAt: expiresAt,
                \t}

                \tif err := s.db.Create(&resetToken).Error; err != nil {
                \t\treturn err
                \t}

                \t// Send email
                \tresetLink := baseURL + "/reset-password?token=" + token
                \tuserName := user.Email
                \tif user.Name != "" {
                \t\tuserName = user.Name
                \t}

                \tif err := s.mailService.SendPasswordResetEmail(email, userName, resetLink, tokenExpirationMinutes); err != nil {
                \t\tlog.Printf("Failed to send password reset email: %%v", err)
                \t}

                \tlog.Printf("Password reset email sent to: %%s", email)
                \treturn nil
                }

                // ValidateToken validates a password reset token.
                func (s *PasswordResetService) ValidateToken(token string) ValidateTokenResult {
                \tvar resetToken models.PasswordResetToken
                \tif err := s.db.Where("token = ? AND used = ?", token, false).First(&resetToken).Error; err != nil {
                \t\tmsg := "Invalid or expired token"
                \t\treturn ValidateTokenResult{Valid: false, Message: &msg}
                \t}

                \tif resetToken.ExpiresAt.Before(time.Now()) {
                \t\tmsg := "Token has expired"
                \t\treturn ValidateTokenResult{Valid: false, Message: &msg}
                \t}

                \treturn ValidateTokenResult{Valid: true}
                }

                // ResetPassword resets the user's password using the token.
                func (s *PasswordResetService) ResetPassword(token, newPassword string) (*ResetPasswordResult, error) {
                \tvalidation := s.ValidateToken(token)
                \tif !validation.Valid {
                \t\treturn nil, errors.New(*validation.Message)
                \t}

                \tvar resetToken models.PasswordResetToken
                \tif err := s.db.Where("token = ?", token).First(&resetToken).Error; err != nil {
                \t\treturn nil, errors.New("invalid token")
                \t}

                \tvar user models.User
                \tif err := s.db.First(&user, resetToken.UserID).Error; err != nil {
                \t\treturn nil, errors.New("user not found")
                \t}

                \t// Hash new password
                \thashedPassword, err := bcrypt.GenerateFromPassword([]byte(newPassword), bcrypt.DefaultCost)
                \tif err != nil {
                \t\treturn nil, err
                \t}

                \t// Update password
                \tif err := s.db.Model(&user).Update("password", string(hashedPassword)).Error; err != nil {
                \t\treturn nil, err
                \t}

                \t// Mark token as used
                \ts.db.Model(&resetToken).Update("used", true)

                \tlog.Printf("Password reset for user: %%s", user.Email)
                \treturn &ResetPasswordResult{
                \t\tSuccess: true,
                \t\tMessage: "Password has been reset successfully",
                \t}, nil
                }

                // CleanupExpiredTokens removes expired tokens from the database.
                func (s *PasswordResetService) CleanupExpiredTokens() int64 {
                \tresult := s.db.Where("expires_at < ?", time.Now()).Delete(&models.PasswordResetToken{})
                \tlog.Printf("Cleaned up %%d expired password reset tokens", result.RowsAffected)
                \treturn result.RowsAffected
                }
                """,
                moduleName, moduleName, tokenExpirationMinutes);
    }

    private String generateModel() {
        return """
        package models

        import (
        \t"time"

        \t"github.com/google/uuid"
        \t"gorm.io/gorm"
        )

        // PasswordResetToken represents a password reset token.
        type PasswordResetToken struct {
        \tID        uuid.UUID `gorm:"type:uuid;primaryKey"`
        \tUserID    uuid.UUID `gorm:"type:uuid;not null;index"`
        \tToken     string    `gorm:"type:varchar(255);uniqueIndex;not null"`
        \tExpiresAt time.Time `gorm:"not null"`
        \tUsed      bool      `gorm:"default:false"`
        \tCreatedAt time.Time
        \tUser      User `gorm:"foreignKey:UserID"`
        }

        // BeforeCreate sets the UUID before creating the record.
        func (p *PasswordResetToken) BeforeCreate(tx *gorm.DB) error {
        \tif p.ID == uuid.Nil {
        \t\tp.ID = uuid.New()
        \t}
        \treturn nil
        }

        // TableName returns the table name for GORM.
        func (PasswordResetToken) TableName() string {
        \treturn "password_reset_tokens"
        }
        """;
    }

    private String generateDto() {
        return """
        package dto

        // ForgotPasswordRequest represents a forgot password request.
        type ForgotPasswordRequest struct {
        \tEmail string `json:"email" binding:"required,email"`
        }

        // ForgotPasswordResponse represents a forgot password response.
        type ForgotPasswordResponse struct {
        \tMessage string `json:"message"`
        }

        // ValidateTokenRequest represents a token validation request.
        type ValidateTokenRequest struct {
        \tToken string `json:"token" binding:"required"`
        }

        // ValidateTokenResponse represents a token validation response.
        type ValidateTokenResponse struct {
        \tValid   bool    `json:"valid"`
        \tMessage *string `json:"message,omitempty"`
        }

        // ResetPasswordRequest represents a password reset request.
        type ResetPasswordRequest struct {
        \tToken           string `json:"token" binding:"required"`
        \tNewPassword     string `json:"new_password" binding:"required,min=8,max=128"`
        \tConfirmPassword string `json:"confirm_password" binding:"required"`
        }

        // ResetPasswordResponse represents a password reset response.
        type ResetPasswordResponse struct {
        \tSuccess bool   `json:"success"`
        \tMessage string `json:"message"`
        }
        """;
    }
}
