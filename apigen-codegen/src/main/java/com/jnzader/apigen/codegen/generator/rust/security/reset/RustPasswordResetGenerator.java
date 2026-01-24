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
package com.jnzader.apigen.codegen.generator.rust.security.reset;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates password reset flow for Rust/Axum applications.
 *
 * @author APiGen
 * @since 2.13.0
 */
public class RustPasswordResetGenerator {

    /**
     * Generates password reset files.
     *
     * @param tokenExpirationMinutes token expiration in minutes
     * @return map of file path to content
     */
    public Map<String, String> generate(int tokenExpirationMinutes) {
        Map<String, String> files = new LinkedHashMap<>();

        files.put("src/auth/password_reset/mod.rs", generateModRs());
        files.put("src/auth/password_reset/handler.rs", generateHandler());
        files.put("src/auth/password_reset/service.rs", generateService(tokenExpirationMinutes));
        files.put("src/auth/password_reset/dto.rs", generateDto());
        files.put("src/auth/password_reset/model.rs", generateModel());

        return files;
    }

    private String generateModRs() {
        return """
        //! Password reset module.

        mod dto;
        mod handler;
        mod model;
        mod service;

        pub use dto::*;
        pub use handler::*;
        pub use model::PasswordResetToken;
        pub use service::PasswordResetService;
        """;
    }

    private String generateHandler() {
        return """
        //! Password reset HTTP handlers.

        use axum::{
            extract::State,
            http::StatusCode,
            Json,
        };
        use std::sync::Arc;
        use super::{
            dto::{ForgotPasswordRequest, ForgotPasswordResponse, ResetPasswordRequest, ResetPasswordResponse, ValidateTokenRequest, ValidateTokenResponse},
            service::PasswordResetService,
        };
        use crate::error::AppError;

        /// Password reset application state.
        pub struct PasswordResetState {
            pub service: PasswordResetService,
        }

        /// Request password reset email.
        #[axum::debug_handler]
        pub async fn forgot_password(
            State(state): State<Arc<PasswordResetState>>,
            Json(req): Json<ForgotPasswordRequest>,
        ) -> Result<Json<ForgotPasswordResponse>, AppError> {
            // Always return success to prevent email enumeration
            let _ = state.service.request_password_reset(&req.email, "http://localhost:3000").await;

            Ok(Json(ForgotPasswordResponse {
                message: "If the email exists, a password reset link has been sent.".to_string(),
            }))
        }

        /// Validate reset token.
        #[axum::debug_handler]
        pub async fn validate_token(
            State(state): State<Arc<PasswordResetState>>,
            Json(req): Json<ValidateTokenRequest>,
        ) -> Result<Json<ValidateTokenResponse>, AppError> {
            let result = state.service.validate_token(&req.token).await;
            Ok(Json(result))
        }

        /// Reset password with token.
        #[axum::debug_handler]
        pub async fn reset_password(
            State(state): State<Arc<PasswordResetState>>,
            Json(req): Json<ResetPasswordRequest>,
        ) -> Result<Json<ResetPasswordResponse>, AppError> {
            if req.new_password != req.confirm_password {
                return Err(AppError::BadRequest("Passwords do not match".to_string()));
            }

            let result = state.service.reset_password(&req.token, &req.new_password).await?;
            Ok(Json(result))
        }
        """;
    }

    private String generateService(int tokenExpirationMinutes) {
        return String.format(
                """
                //! Password reset service.

                use chrono::{Duration, Utc};
                use rand::Rng;
                use sqlx::PgPool;
                use tracing::{info, error};
                use argon2::{
                    password_hash::{PasswordHasher, SaltString, rand_core::OsRng},
                    Argon2,
                };
                use super::{
                    dto::{ValidateTokenResponse, ResetPasswordResponse},
                    model::PasswordResetToken,
                };
                use crate::error::AppError;
                use crate::mail::MailService;

                const TOKEN_EXPIRATION_MINUTES: i64 = %d;

                /// Password reset service.
                #[derive(Clone)]
                pub struct PasswordResetService {
                    pool: PgPool,
                    mail_service: MailService,
                }

                impl PasswordResetService {
                    /// Create a new password reset service.
                    pub fn new(pool: PgPool, mail_service: MailService) -> Self {
                        Self { pool, mail_service }
                    }

                    /// Request a password reset.
                    pub async fn request_password_reset(&self, email: &str, base_url: &str) -> Result<(), AppError> {
                        // Find user
                        let user = sqlx::query_as::<_, (uuid::Uuid, String, Option<String>)>(
                            "SELECT id, email, username FROM users WHERE email = $1"
                        )
                        .bind(email)
                        .fetch_optional(&self.pool)
                        .await?;

                        let Some((user_id, user_email, user_name)) = user else {
                            // Don't reveal if email exists
                            return Ok(());
                        };

                        // Invalidate existing tokens
                        sqlx::query("DELETE FROM password_reset_tokens WHERE user_id = $1")
                            .bind(user_id)
                            .execute(&self.pool)
                            .await?;

                        // Generate token
                        let token: String = rand::rng()
                            .sample_iter(&rand::distr::Alphanumeric)
                            .take(64)
                            .map(char::from)
                            .collect();

                        let expires_at = Utc::now() + Duration::minutes(TOKEN_EXPIRATION_MINUTES);

                        // Save token
                        sqlx::query(
                            "INSERT INTO password_reset_tokens (user_id, token, expires_at) VALUES ($1, $2, $3)"
                        )
                        .bind(user_id)
                        .bind(&token)
                        .bind(expires_at)
                        .execute(&self.pool)
                        .await?;

                        // Send email
                        let reset_link = format!("{}/reset-password?token={}", base_url, token);
                        let name = user_name.unwrap_or_else(|| user_email.clone());

                        if let Err(e) = self.mail_service.send_password_reset(&user_email, &name, &reset_link, TOKEN_EXPIRATION_MINUTES as i32).await {
                            error!("Failed to send password reset email: {}", e);
                        }

                        info!("Password reset email sent to: {}", email);
                        Ok(())
                    }

                    /// Validate a reset token.
                    pub async fn validate_token(&self, token: &str) -> ValidateTokenResponse {
                        let result = sqlx::query_as::<_, PasswordResetToken>(
                            "SELECT id, user_id, token, expires_at, used, created_at FROM password_reset_tokens WHERE token = $1 AND used = false"
                        )
                        .bind(token)
                        .fetch_optional(&self.pool)
                        .await;

                        match result {
                            Ok(Some(reset_token)) => {
                                if reset_token.expires_at < Utc::now() {
                                    ValidateTokenResponse {
                                        valid: false,
                                        message: Some("Token has expired".to_string()),
                                    }
                                } else {
                                    ValidateTokenResponse {
                                        valid: true,
                                        message: None,
                                    }
                                }
                            }
                            _ => ValidateTokenResponse {
                                valid: false,
                                message: Some("Invalid or expired token".to_string()),
                            },
                        }
                    }

                    /// Reset password with token.
                    pub async fn reset_password(&self, token: &str, new_password: &str) -> Result<ResetPasswordResponse, AppError> {
                        let validation = self.validate_token(token).await;
                        if !validation.valid {
                            return Err(AppError::BadRequest(validation.message.unwrap_or_default()));
                        }

                        let reset_token = sqlx::query_as::<_, PasswordResetToken>(
                            "SELECT id, user_id, token, expires_at, used, created_at FROM password_reset_tokens WHERE token = $1"
                        )
                        .bind(token)
                        .fetch_one(&self.pool)
                        .await?;

                        // Hash new password
                        let salt = SaltString::generate(&mut OsRng);
                        let argon2 = Argon2::default();
                        let password_hash = argon2
                            .hash_password(new_password.as_bytes(), &salt)
                            .map_err(|e| AppError::Internal(format!("Password hashing failed: {}", e)))?
                            .to_string();

                        // Update password
                        sqlx::query("UPDATE users SET password = $1 WHERE id = $2")
                            .bind(&password_hash)
                            .bind(reset_token.user_id)
                            .execute(&self.pool)
                            .await?;

                        // Mark token as used
                        sqlx::query("UPDATE password_reset_tokens SET used = true WHERE id = $1")
                            .bind(reset_token.id)
                            .execute(&self.pool)
                            .await?;

                        info!("Password reset for user_id: {}", reset_token.user_id);
                        Ok(ResetPasswordResponse {
                            success: true,
                            message: "Password has been reset successfully".to_string(),
                        })
                    }
                }
                """,
                tokenExpirationMinutes);
    }

    private String generateDto() {
        return """
        //! Password reset DTOs.

        use serde::{Deserialize, Serialize};
        use validator::Validate;

        /// Forgot password request.
        #[derive(Debug, Deserialize, Validate)]
        pub struct ForgotPasswordRequest {
            #[validate(email)]
            pub email: String,
        }

        /// Forgot password response.
        #[derive(Debug, Serialize)]
        pub struct ForgotPasswordResponse {
            pub message: String,
        }

        /// Validate token request.
        #[derive(Debug, Deserialize)]
        pub struct ValidateTokenRequest {
            pub token: String,
        }

        /// Validate token response.
        #[derive(Debug, Serialize)]
        pub struct ValidateTokenResponse {
            pub valid: bool,
            #[serde(skip_serializing_if = "Option::is_none")]
            pub message: Option<String>,
        }

        /// Reset password request.
        #[derive(Debug, Deserialize, Validate)]
        pub struct ResetPasswordRequest {
            pub token: String,
            #[validate(length(min = 8, max = 128))]
            pub new_password: String,
            pub confirm_password: String,
        }

        /// Reset password response.
        #[derive(Debug, Serialize)]
        pub struct ResetPasswordResponse {
            pub success: bool,
            pub message: String,
        }
        """;
    }

    private String generateModel() {
        return """
        //! Password reset token model.

        use chrono::{DateTime, Utc};
        use sqlx::FromRow;
        use uuid::Uuid;

        /// Password reset token entity.
        #[derive(Debug, Clone, FromRow)]
        pub struct PasswordResetToken {
            pub id: Uuid,
            pub user_id: Uuid,
            pub token: String,
            pub expires_at: DateTime<Utc>,
            pub used: bool,
            pub created_at: DateTime<Utc>,
        }
        """;
    }
}
