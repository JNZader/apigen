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
package com.jnzader.apigen.codegen.generator.gochi.security.reset;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates password reset flow for Go/Chi applications.
 *
 * @author APiGen
 * @since 2.16.0
 */
public class GoChiPasswordResetGenerator {

    private final String moduleName;

    public GoChiPasswordResetGenerator(String moduleName) {
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
                	"encoding/json"
                	"log/slog"
                	"net/http"

                	"%s/internal/dto"

                	"github.com/go-chi/chi/v5"
                )

                // PasswordResetHandler handles password reset endpoints.
                type PasswordResetHandler struct {
                	service *PasswordResetService
                	logger  *slog.Logger
                }

                // NewPasswordResetHandler creates a new password reset handler.
                func NewPasswordResetHandler(service *PasswordResetService, logger *slog.Logger) *PasswordResetHandler {
                	return &PasswordResetHandler{
                		service: service,
                		logger:  logger.With("handler", "password_reset"),
                	}
                }

                // RegisterRoutes registers password reset routes.
                func (h *PasswordResetHandler) RegisterRoutes(r chi.Router) {
                	r.Route("/auth/password", func(r chi.Router) {
                		r.Post("/forgot", h.ForgotPassword)
                		r.Post("/validate", h.ValidateToken)
                		r.Post("/reset", h.ResetPassword)
                	})
                }

                // ForgotPassword handles password reset request.
                // @Summary Request password reset email
                // @Tags Password Reset
                // @Accept json
                // @Produce json
                // @Param request body dto.ForgotPasswordRequest true "Forgot password request"
                // @Success 200 {object} dto.ForgotPasswordResponse
                // @Router /auth/password/forgot [post]
                func (h *PasswordResetHandler) ForgotPassword(w http.ResponseWriter, r *http.Request) {
                	var req dto.ForgotPasswordRequest
                	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
                		h.writeError(w, http.StatusBadRequest, err.Error())
                		return
                	}

                	baseURL := r.URL.Scheme + "://" + r.Host
                	if baseURL == "://" {
                		baseURL = "http://" + r.Host
                	}

                	_ = h.service.RequestPasswordReset(req.Email, baseURL)

                	// Always return success to prevent email enumeration
                	h.writeJSON(w, http.StatusOK, dto.ForgotPasswordResponse{
                		Message: "If the email exists, a password reset link has been sent.",
                	})
                }

                // ValidateToken validates a reset token.
                // @Summary Validate reset token
                // @Tags Password Reset
                // @Accept json
                // @Produce json
                // @Param request body dto.ValidateTokenRequest true "Validate token request"
                // @Success 200 {object} dto.ValidateTokenResponse
                // @Router /auth/password/validate [post]
                func (h *PasswordResetHandler) ValidateToken(w http.ResponseWriter, r *http.Request) {
                	var req dto.ValidateTokenRequest
                	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
                		h.writeError(w, http.StatusBadRequest, err.Error())
                		return
                	}

                	result := h.service.ValidateToken(req.Token)
                	h.writeJSON(w, http.StatusOK, result)
                }

                // ResetPassword resets password with token.
                // @Summary Reset password with token
                // @Tags Password Reset
                // @Accept json
                // @Produce json
                // @Param request body dto.ResetPasswordRequest true "Reset password request"
                // @Success 200 {object} dto.ResetPasswordResponse
                // @Failure 400 {object} dto.ErrorResponse
                // @Router /auth/password/reset [post]
                func (h *PasswordResetHandler) ResetPassword(w http.ResponseWriter, r *http.Request) {
                	var req dto.ResetPasswordRequest
                	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
                		h.writeError(w, http.StatusBadRequest, err.Error())
                		return
                	}

                	if req.NewPassword != req.ConfirmPassword {
                		h.writeError(w, http.StatusBadRequest, "Passwords do not match")
                		return
                	}

                	result, err := h.service.ResetPassword(req.Token, req.NewPassword)
                	if err != nil {
                		h.writeError(w, http.StatusBadRequest, err.Error())
                		return
                	}

                	h.writeJSON(w, http.StatusOK, result)
                }

                func (h *PasswordResetHandler) writeJSON(w http.ResponseWriter, status int, data interface{}) {
                	w.Header().Set("Content-Type", "application/json")
                	w.WriteHeader(status)
                	if err := json.NewEncoder(w).Encode(data); err != nil {
                		h.logger.Error("failed to encode response", "error", err)
                	}
                }

                func (h *PasswordResetHandler) writeError(w http.ResponseWriter, status int, message string) {
                	h.writeJSON(w, status, dto.ErrorResponse{Error: message})
                }
                """,
                moduleName);
    }

    private String generateService(int tokenExpirationMinutes) {
        return String.format(
                """
                package auth

                import (
                	"context"
                	"crypto/rand"
                	"encoding/hex"
                	"errors"
                	"log/slog"
                	"time"

                	"%s/internal/mail"
                	"%s/internal/models"

                	"github.com/jackc/pgx/v5/pgxpool"
                	"golang.org/x/crypto/bcrypt"
                )

                const tokenExpirationMinutes = %d

                // PasswordResetService handles password reset operations.
                type PasswordResetService struct {
                	db          *pgxpool.Pool
                	mailService *mail.MailService
                	logger      *slog.Logger
                }

                // NewPasswordResetService creates a new password reset service.
                func NewPasswordResetService(db *pgxpool.Pool, mailService *mail.MailService, logger *slog.Logger) *PasswordResetService {
                	return &PasswordResetService{
                		db:          db,
                		mailService: mailService,
                		logger:      logger.With("service", "password_reset"),
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
                	ctx := context.Background()

                	// Find user by email
                	var userID string
                	var userName string
                	err := s.db.QueryRow(ctx,
                		"SELECT id, COALESCE(name, email) FROM users WHERE email = $1",
                		email,
                	).Scan(&userID, &userName)

                	if err != nil {
                		// Don't reveal if email exists
                		s.logger.Debug("user not found for password reset", "email", email)
                		return nil
                	}

                	// Invalidate existing tokens
                	_, _ = s.db.Exec(ctx, "DELETE FROM password_reset_tokens WHERE user_id = $1", userID)

                	// Generate new token
                	tokenBytes := make([]byte, 32)
                	if _, err := rand.Read(tokenBytes); err != nil {
                		return err
                	}
                	token := hex.EncodeToString(tokenBytes)

                	expiresAt := time.Now().Add(time.Duration(tokenExpirationMinutes) * time.Minute)

                	// Save token
                	_, err = s.db.Exec(ctx,
                		`INSERT INTO password_reset_tokens (user_id, token, expires_at) VALUES ($1, $2, $3)`,
                		userID, token, expiresAt,
                	)
                	if err != nil {
                		s.logger.Error("failed to create password reset token", "error", err)
                		return err
                	}

                	// Send email
                	resetLink := baseURL + "/reset-password?token=" + token
                	if err := s.mailService.SendPasswordResetEmail(email, userName, resetLink, tokenExpirationMinutes); err != nil {
                		s.logger.Error("failed to send password reset email", "error", err)
                	}

                	s.logger.Info("password reset email sent", "email", email)
                	return nil
                }

                // ValidateToken validates a password reset token.
                func (s *PasswordResetService) ValidateToken(token string) ValidateTokenResult {
                	ctx := context.Background()

                	var expiresAt time.Time
                	var used bool
                	err := s.db.QueryRow(ctx,
                		"SELECT expires_at, used FROM password_reset_tokens WHERE token = $1",
                		token,
                	).Scan(&expiresAt, &used)

                	if err != nil {
                		msg := "Invalid or expired token"
                		return ValidateTokenResult{Valid: false, Message: &msg}
                	}

                	if used {
                		msg := "Token has already been used"
                		return ValidateTokenResult{Valid: false, Message: &msg}
                	}

                	if expiresAt.Before(time.Now()) {
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

                	ctx := context.Background()

                	// Get user ID from token
                	var userID string
                	err := s.db.QueryRow(ctx,
                		"SELECT user_id FROM password_reset_tokens WHERE token = $1",
                		token,
                	).Scan(&userID)
                	if err != nil {
                		return nil, errors.New("invalid token")
                	}

                	// Hash new password
                	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(newPassword), bcrypt.DefaultCost)
                	if err != nil {
                		return nil, err
                	}

                	// Update password
                	_, err = s.db.Exec(ctx,
                		"UPDATE users SET password = $1, updated_at = $2 WHERE id = $3",
                		string(hashedPassword), time.Now(), userID,
                	)
                	if err != nil {
                		return nil, err
                	}

                	// Mark token as used
                	_, _ = s.db.Exec(ctx, "UPDATE password_reset_tokens SET used = true WHERE token = $1", token)

                	s.logger.Info("password reset completed", "user_id", userID)
                	return &ResetPasswordResult{
                		Success: true,
                		Message: "Password has been reset successfully",
                	}, nil
                }

                // CleanupExpiredTokens removes expired tokens from the database.
                func (s *PasswordResetService) CleanupExpiredTokens() int64 {
                	ctx := context.Background()
                	result, err := s.db.Exec(ctx, "DELETE FROM password_reset_tokens WHERE expires_at < $1", time.Now())
                	if err != nil {
                		s.logger.Error("failed to cleanup expired tokens", "error", err)
                		return 0
                	}
                	count := result.RowsAffected()
                	s.logger.Info("cleaned up expired password reset tokens", "count", count)
                	return count
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
        )

        // PasswordResetToken represents a password reset token.
        type PasswordResetToken struct {
        	ID        uuid.UUID `json:"id" db:"id"`
        	UserID    uuid.UUID `json:"user_id" db:"user_id"`
        	Token     string    `json:"token" db:"token"`
        	ExpiresAt time.Time `json:"expires_at" db:"expires_at"`
        	Used      bool      `json:"used" db:"used"`
        	CreatedAt time.Time `json:"created_at" db:"created_at"`
        }

        // TableName returns the table name.
        func (PasswordResetToken) TableName() string {
        	return "password_reset_tokens"
        }

        // IsExpired checks if the token has expired.
        func (t *PasswordResetToken) IsExpired() bool {
        	return t.ExpiresAt.Before(time.Now())
        }

        // IsValid checks if the token is valid (not used and not expired).
        func (t *PasswordResetToken) IsValid() bool {
        	return !t.Used && !t.IsExpired()
        }
        """;
    }

    private String generateDto() {
        return """
        package dto

        // ForgotPasswordRequest represents a forgot password request.
        type ForgotPasswordRequest struct {
        	Email string `json:"email" validate:"required,email"`
        }

        // ForgotPasswordResponse represents a forgot password response.
        type ForgotPasswordResponse struct {
        	Message string `json:"message"`
        }

        // ValidateTokenRequest represents a token validation request.
        type ValidateTokenRequest struct {
        	Token string `json:"token" validate:"required"`
        }

        // ValidateTokenResponse represents a token validation response.
        type ValidateTokenResponse struct {
        	Valid   bool    `json:"valid"`
        	Message *string `json:"message,omitempty"`
        }

        // ResetPasswordRequest represents a password reset request.
        type ResetPasswordRequest struct {
        	Token           string `json:"token" validate:"required"`
        	NewPassword     string `json:"new_password" validate:"required,min=8,max=128"`
        	ConfirmPassword string `json:"confirm_password" validate:"required"`
        }

        // ResetPasswordResponse represents a password reset response.
        type ResetPasswordResponse struct {
        	Success bool   `json:"success"`
        	Message string `json:"message"`
        }
        """;
    }
}
