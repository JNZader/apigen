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
@SuppressWarnings({
    "java:S2479",
    "java:S1192",
    "java:S3400"
}) // S2479: Literal tabs for Go code; S1192: template strings; S3400: template methods return
// constants
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
                \t"encoding/json"
                \t"log/slog"
                \t"net/http"

                \t"%s/internal/dto"

                \t"github.com/go-chi/chi/v5"
                )

                // PasswordResetHandler handles password reset endpoints.
                type PasswordResetHandler struct {
                \tservice *PasswordResetService
                \tlogger  *slog.Logger
                }

                // NewPasswordResetHandler creates a new password reset handler.
                func NewPasswordResetHandler(service *PasswordResetService, logger *slog.Logger) *PasswordResetHandler {
                \treturn &PasswordResetHandler{
                \t\tservice: service,
                \t\tlogger:  logger.With("handler", "password_reset"),
                \t}
                }

                // RegisterRoutes registers password reset routes.
                func (h *PasswordResetHandler) RegisterRoutes(r chi.Router) {
                \tr.Route("/auth/password", func(r chi.Router) {
                \t\tr.Post("/forgot", h.ForgotPassword)
                \t\tr.Post("/validate", h.ValidateToken)
                \t\tr.Post("/reset", h.ResetPassword)
                \t})
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
                \tvar req dto.ForgotPasswordRequest
                \tif err := json.NewDecoder(r.Body).Decode(&req); err != nil {
                \t\th.writeError(w, http.StatusBadRequest, err.Error())
                \t\treturn
                \t}

                \tbaseURL := r.URL.Scheme + "://" + r.Host
                \tif baseURL == "://" {
                \t\tbaseURL = "http://" + r.Host
                \t}

                \t_ = h.service.RequestPasswordReset(req.Email, baseURL)

                \t// Always return success to prevent email enumeration
                \th.writeJSON(w, http.StatusOK, dto.ForgotPasswordResponse{
                \t\tMessage: "If the email exists, a password reset link has been sent.",
                \t})
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
                \tvar req dto.ValidateTokenRequest
                \tif err := json.NewDecoder(r.Body).Decode(&req); err != nil {
                \t\th.writeError(w, http.StatusBadRequest, err.Error())
                \t\treturn
                \t}

                \tresult := h.service.ValidateToken(req.Token)
                \th.writeJSON(w, http.StatusOK, result)
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
                \tvar req dto.ResetPasswordRequest
                \tif err := json.NewDecoder(r.Body).Decode(&req); err != nil {
                \t\th.writeError(w, http.StatusBadRequest, err.Error())
                \t\treturn
                \t}

                \tif req.NewPassword != req.ConfirmPassword {
                \t\th.writeError(w, http.StatusBadRequest, "Passwords do not match")
                \t\treturn
                \t}

                \tresult, err := h.service.ResetPassword(req.Token, req.NewPassword)
                \tif err != nil {
                \t\th.writeError(w, http.StatusBadRequest, err.Error())
                \t\treturn
                \t}

                \th.writeJSON(w, http.StatusOK, result)
                }

                func (h *PasswordResetHandler) writeJSON(w http.ResponseWriter, status int, data interface{}) {
                \tw.Header().Set("Content-Type", "application/json")
                \tw.WriteHeader(status)
                \tif err := json.NewEncoder(w).Encode(data); err != nil {
                \t\th.logger.Error("failed to encode response", "error", err)
                \t}
                }

                func (h *PasswordResetHandler) writeError(w http.ResponseWriter, status int, message string) {
                \th.writeJSON(w, status, dto.ErrorResponse{Error: message})
                }
                """,
                moduleName);
    }

    private String generateService(int tokenExpirationMinutes) {
        return String.format(
                """
                package auth

                import (
                \t"context"
                \t"crypto/rand"
                \t"encoding/hex"
                \t"errors"
                \t"log/slog"
                \t"time"

                \t"%s/internal/mail"
                \t"%s/internal/models"

                \t"github.com/jackc/pgx/v5/pgxpool"
                \t"golang.org/x/crypto/bcrypt"
                )

                const tokenExpirationMinutes = %d

                // PasswordResetService handles password reset operations.
                type PasswordResetService struct {
                \tdb          *pgxpool.Pool
                \tmailService *mail.MailService
                \tlogger      *slog.Logger
                }

                // NewPasswordResetService creates a new password reset service.
                func NewPasswordResetService(db *pgxpool.Pool, mailService *mail.MailService, logger *slog.Logger) *PasswordResetService {
                \treturn &PasswordResetService{
                \t\tdb:          db,
                \t\tmailService: mailService,
                \t\tlogger:      logger.With("service", "password_reset"),
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
                \tctx := context.Background()

                \t// Find user by email
                \tvar userID string
                \tvar userName string
                \terr := s.db.QueryRow(ctx,
                \t\t"SELECT id, COALESCE(name, email) FROM users WHERE email = $1",
                \t\temail,
                \t).Scan(&userID, &userName)

                \tif err != nil {
                \t\t// Don't reveal if email exists
                \t\ts.logger.Debug("user not found for password reset", "email", email)
                \t\treturn nil
                \t}

                \t// Invalidate existing tokens
                \t_, _ = s.db.Exec(ctx, "DELETE FROM password_reset_tokens WHERE user_id = $1", userID)

                \t// Generate new token
                \ttokenBytes := make([]byte, 32)
                \tif _, err := rand.Read(tokenBytes); err != nil {
                \t\treturn err
                \t}
                \ttoken := hex.EncodeToString(tokenBytes)

                \texpiresAt := time.Now().Add(time.Duration(tokenExpirationMinutes) * time.Minute)

                \t// Save token
                \t_, err = s.db.Exec(ctx,
                \t\t`INSERT INTO password_reset_tokens (user_id, token, expires_at) VALUES ($1, $2, $3)`,
                \t\tuserID, token, expiresAt,
                \t)
                \tif err != nil {
                \t\ts.logger.Error("failed to create password reset token", "error", err)
                \t\treturn err
                \t}

                \t// Send email
                \tresetLink := baseURL + "/reset-password?token=" + token
                \tif err := s.mailService.SendPasswordResetEmail(email, userName, resetLink, tokenExpirationMinutes); err != nil {
                \t\ts.logger.Error("failed to send password reset email", "error", err)
                \t}

                \ts.logger.Info("password reset email sent", "email", email)
                \treturn nil
                }

                // ValidateToken validates a password reset token.
                func (s *PasswordResetService) ValidateToken(token string) ValidateTokenResult {
                \tctx := context.Background()

                \tvar expiresAt time.Time
                \tvar used bool
                \terr := s.db.QueryRow(ctx,
                \t\t"SELECT expires_at, used FROM password_reset_tokens WHERE token = $1",
                \t\ttoken,
                \t).Scan(&expiresAt, &used)

                \tif err != nil {
                \t\tmsg := "Invalid or expired token"
                \t\treturn ValidateTokenResult{Valid: false, Message: &msg}
                \t}

                \tif used {
                \t\tmsg := "Token has already been used"
                \t\treturn ValidateTokenResult{Valid: false, Message: &msg}
                \t}

                \tif expiresAt.Before(time.Now()) {
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

                \tctx := context.Background()

                \t// Get user ID from token
                \tvar userID string
                \terr := s.db.QueryRow(ctx,
                \t\t"SELECT user_id FROM password_reset_tokens WHERE token = $1",
                \t\ttoken,
                \t).Scan(&userID)
                \tif err != nil {
                \t\treturn nil, errors.New("invalid token")
                \t}

                \t// Hash new password
                \thashedPassword, err := bcrypt.GenerateFromPassword([]byte(newPassword), bcrypt.DefaultCost)
                \tif err != nil {
                \t\treturn nil, err
                \t}

                \t// Update password
                \t_, err = s.db.Exec(ctx,
                \t\t"UPDATE users SET password = $1, updated_at = $2 WHERE id = $3",
                \t\tstring(hashedPassword), time.Now(), userID,
                \t)
                \tif err != nil {
                \t\treturn nil, err
                \t}

                \t// Mark token as used
                \t_, _ = s.db.Exec(ctx, "UPDATE password_reset_tokens SET used = true WHERE token = $1", token)

                \ts.logger.Info("password reset completed", "user_id", userID)
                \treturn &ResetPasswordResult{
                \t\tSuccess: true,
                \t\tMessage: "Password has been reset successfully",
                \t}, nil
                }

                // CleanupExpiredTokens removes expired tokens from the database.
                func (s *PasswordResetService) CleanupExpiredTokens() int64 {
                \tctx := context.Background()
                \tresult, err := s.db.Exec(ctx, "DELETE FROM password_reset_tokens WHERE expires_at < $1", time.Now())
                \tif err != nil {
                \t\ts.logger.Error("failed to cleanup expired tokens", "error", err)
                \t\treturn 0
                \t}
                \tcount := result.RowsAffected()
                \ts.logger.Info("cleaned up expired password reset tokens", "count", count)
                \treturn count
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
        )

        // PasswordResetToken represents a password reset token.
        type PasswordResetToken struct {
        \tID        uuid.UUID `json:"id" db:"id"`
        \tUserID    uuid.UUID `json:"user_id" db:"user_id"`
        \tToken     string    `json:"token" db:"token"`
        \tExpiresAt time.Time `json:"expires_at" db:"expires_at"`
        \tUsed      bool      `json:"used" db:"used"`
        \tCreatedAt time.Time `json:"created_at" db:"created_at"`
        }

        // TableName returns the table name.
        func (PasswordResetToken) TableName() string {
        \treturn "password_reset_tokens"
        }

        // IsExpired checks if the token has expired.
        func (t *PasswordResetToken) IsExpired() bool {
        \treturn t.ExpiresAt.Before(time.Now())
        }

        // IsValid checks if the token is valid (not used and not expired).
        func (t *PasswordResetToken) IsValid() bool {
        \treturn !t.Used && !t.IsExpired()
        }
        """;
    }

    private String generateDto() {
        return """
        package dto

        // ForgotPasswordRequest represents a forgot password request.
        type ForgotPasswordRequest struct {
        \tEmail string `json:"email" validate:"required,email"`
        }

        // ForgotPasswordResponse represents a forgot password response.
        type ForgotPasswordResponse struct {
        \tMessage string `json:"message"`
        }

        // ValidateTokenRequest represents a token validation request.
        type ValidateTokenRequest struct {
        \tToken string `json:"token" validate:"required"`
        }

        // ValidateTokenResponse represents a token validation response.
        type ValidateTokenResponse struct {
        \tValid   bool    `json:"valid"`
        \tMessage *string `json:"message,omitempty"`
        }

        // ResetPasswordRequest represents a password reset request.
        type ResetPasswordRequest struct {
        \tToken           string `json:"token" validate:"required"`
        \tNewPassword     string `json:"new_password" validate:"required,min=8,max=128"`
        \tConfirmPassword string `json:"confirm_password" validate:"required"`
        }

        // ResetPasswordResponse represents a password reset response.
        type ResetPasswordResponse struct {
        \tSuccess bool   `json:"success"`
        \tMessage string `json:"message"`
        }
        """;
    }
}
