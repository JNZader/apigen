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
package com.jnzader.apigen.codegen.generator.rust.security.social;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates OAuth2 social login for Rust/Axum applications.
 *
 * @author APiGen
 * @since 2.13.0
 */
@SuppressWarnings({
    "java:S1192",
    "java:S3400",
    "java:S3457"
}) // S1192: Provider names; S3400: template methods return constants; S3457: Unix line endings
public class RustSocialLoginGenerator {

    /**
     * Generates social login files.
     *
     * @param providers list of OAuth providers (google, github)
     * @return map of file path to content
     */
    public Map<String, String> generate(List<String> providers) {
        Map<String, String> files = new LinkedHashMap<>();

        files.put("src/auth/oauth/mod.rs", generateModRs());
        files.put("src/auth/oauth/config.rs", generateConfig(providers));
        files.put("src/auth/oauth/handler.rs", generateHandler(providers));
        files.put("src/auth/oauth/service.rs", generateService());
        files.put("src/auth/oauth/dto.rs", generateDto());

        return files;
    }

    private String generateModRs() {
        StringBuilder sb = new StringBuilder();
        sb.append(
                """
                //! OAuth2 social login module.

                mod config;
                mod dto;
                mod handler;
                mod service;

                pub use config::*;
                pub use dto::*;
                pub use handler::*;
                pub use service::OAuthService;
                """);
        return sb.toString();
    }

    private String generateConfig(List<String> providers) {
        StringBuilder sb = new StringBuilder();
        sb.append(
                """
                //! OAuth2 provider configuration.

                use serde::Deserialize;

                /// OAuth2 configuration for all providers.
                #[derive(Debug, Clone, Deserialize)]
                pub struct OAuthConfig {
                """);

        for (String provider : providers) {
            sb.append(
                    String.format(
                            "    pub %s: Option<%sConfig>,\n", provider, capitalize(provider)));
        }

        sb.append("}\n\n");

        // Generate provider-specific configs
        for (String provider : providers) {
            sb.append(generateProviderConfig(provider));
        }

        sb.append(
                """

                impl Default for OAuthConfig {
                    fn default() -> Self {
                        Self {
                """);

        for (String provider : providers) {
            sb.append(String.format("            %s: None,\n", provider));
        }

        sb.append(
                """
                        }
                    }
                }
                """);

        return sb.toString();
    }

    private String generateProviderConfig(String provider) {
        String capitalized = capitalize(provider);
        String authUrl = getAuthUrl(provider);
        String tokenUrl = getTokenUrl(provider);
        String userInfoUrl = getUserInfoUrl(provider);

        return String.format(
                """

                /// %s OAuth2 configuration.
                #[derive(Debug, Clone, Deserialize)]
                pub struct %sConfig {
                    pub client_id: String,
                    pub client_secret: String,
                    pub redirect_uri: String,
                    #[serde(default = "%sConfig::default_auth_url")]
                    pub auth_url: String,
                    #[serde(default = "%sConfig::default_token_url")]
                    pub token_url: String,
                    #[serde(default = "%sConfig::default_user_info_url")]
                    pub user_info_url: String,
                }

                impl %sConfig {
                    fn default_auth_url() -> String {
                        "%s".to_string()
                    }

                    fn default_token_url() -> String {
                        "%s".to_string()
                    }

                    fn default_user_info_url() -> String {
                        "%s".to_string()
                    }
                }
                """,
                capitalized,
                capitalized,
                capitalized,
                capitalized,
                capitalized,
                capitalized,
                authUrl,
                tokenUrl,
                userInfoUrl);
    }

    private String generateHandler(List<String> providers) {
        StringBuilder sb = new StringBuilder();
        sb.append(
                """
                //! OAuth2 HTTP handlers.

                use axum::{
                    extract::{Query, State},
                    http::StatusCode,
                    response::{IntoResponse, Redirect},
                    Json,
                };
                use std::sync::Arc;
                use super::{
                    dto::{OAuthCallbackQuery, OAuthLoginResponse},
                    service::OAuthService,
                };
                use crate::error::AppError;

                /// OAuth application state.
                pub struct OAuthState {
                    pub service: OAuthService,
                }

                """);

        // Generate handler for each provider
        for (String provider : providers) {
            sb.append(generateProviderHandler(provider));
        }

        return sb.toString();
    }

    private String generateProviderHandler(String provider) {
        String capitalized = capitalize(provider);

        return String.format(
                """
                /// Redirect to %s OAuth login.
                #[axum::debug_handler]
                pub async fn %s_login(
                    State(state): State<Arc<OAuthState>>,
                ) -> Result<impl IntoResponse, AppError> {
                    let auth_url = state.service.get_authorization_url("%s")?;
                    Ok(Redirect::temporary(&auth_url))
                }

                /// Handle %s OAuth callback.
                #[axum::debug_handler]
                pub async fn %s_callback(
                    State(state): State<Arc<OAuthState>>,
                    Query(query): Query<OAuthCallbackQuery>,
                ) -> Result<Json<OAuthLoginResponse>, AppError> {
                    let response = state.service.handle_callback("%s", &query.code, query.state.as_deref()).await?;
                    Ok(Json(response))
                }

                """,
                capitalized, provider, provider, capitalized, provider, provider);
    }

    private String generateService() {
        return """
        //! OAuth2 service implementation.

        use oauth2::{
            basic::BasicClient, AuthUrl, AuthorizationCode, ClientId, ClientSecret,
            CsrfToken, RedirectUrl, Scope, TokenResponse, TokenUrl,
        };
        use reqwest::Client;
        use serde::Deserialize;
        use sqlx::PgPool;
        use tracing::{info, error};
        use super::{config::OAuthConfig, dto::OAuthLoginResponse};
        use crate::error::AppError;

        /// OAuth2 service for social login.
        #[derive(Clone)]
        pub struct OAuthService {
            config: OAuthConfig,
            http_client: Client,
            pool: PgPool,
        }

        #[derive(Debug, Deserialize)]
        struct GoogleUserInfo {
            id: String,
            email: String,
            name: Option<String>,
            picture: Option<String>,
        }

        #[derive(Debug, Deserialize)]
        struct GitHubUserInfo {
            id: i64,
            login: String,
            email: Option<String>,
            name: Option<String>,
            avatar_url: Option<String>,
        }

        impl OAuthService {
            /// Create a new OAuth service.
            pub fn new(config: OAuthConfig, pool: PgPool) -> Self {
                Self {
                    config,
                    http_client: Client::new(),
                    pool,
                }
            }

            /// Get authorization URL for a provider.
            pub fn get_authorization_url(&self, provider: &str) -> Result<String, AppError> {
                let client = self.get_client(provider)?;
                let (auth_url, _csrf_token) = client
                    .authorize_url(CsrfToken::new_random)
                    .add_scope(Scope::new(self.get_scopes(provider)))
                    .url();

                Ok(auth_url.to_string())
            }

            /// Handle OAuth callback and authenticate user.
            pub async fn handle_callback(
                &self,
                provider: &str,
                code: &str,
                _state: Option<&str>,
            ) -> Result<OAuthLoginResponse, AppError> {
                let client = self.get_client(provider)?;

                // Exchange code for token
                let token = client
                    .exchange_code(AuthorizationCode::new(code.to_string()))
                    .request_async(oauth2::reqwest::async_http_client)
                    .await
                    .map_err(|e| AppError::Internal(format!("Token exchange failed: {}", e)))?;

                let access_token = token.access_token().secret();

                // Get user info
                let (provider_id, email, name, avatar) = self.get_user_info(provider, access_token).await?;

                // Find or create user
                let user = self.find_or_create_user(provider, &provider_id, email.as_deref(), name.as_deref(), avatar.as_deref()).await?;

                info!("OAuth login successful for provider: {}, user_id: {}", provider, user.0);

                Ok(OAuthLoginResponse {
                    user_id: user.0,
                    email: user.1,
                    name: user.2,
                    provider: provider.to_string(),
                    is_new_user: user.3,
                    access_token: "generate_jwt_here".to_string(), // TODO: Generate JWT
                })
            }

            fn get_client(&self, provider: &str) -> Result<BasicClient, AppError> {
                match provider {
                    "google" => {
                        let config = self.config.google.as_ref()
                            .ok_or_else(|| AppError::BadRequest("Google OAuth not configured".into()))?;
                        Ok(BasicClient::new(
                            ClientId::new(config.client_id.clone()),
                            Some(ClientSecret::new(config.client_secret.clone())),
                            AuthUrl::new(config.auth_url.clone())
                                .map_err(|e| AppError::Internal(format!("Invalid auth URL: {}", e)))?,
                            Some(TokenUrl::new(config.token_url.clone())
                                .map_err(|e| AppError::Internal(format!("Invalid token URL: {}", e)))?),
                        )
                        .set_redirect_uri(RedirectUrl::new(config.redirect_uri.clone())
                            .map_err(|e| AppError::Internal(format!("Invalid redirect URI: {}", e)))?))
                    }
                    "github" => {
                        let config = self.config.github.as_ref()
                            .ok_or_else(|| AppError::BadRequest("GitHub OAuth not configured".into()))?;
                        Ok(BasicClient::new(
                            ClientId::new(config.client_id.clone()),
                            Some(ClientSecret::new(config.client_secret.clone())),
                            AuthUrl::new(config.auth_url.clone())
                                .map_err(|e| AppError::Internal(format!("Invalid auth URL: {}", e)))?,
                            Some(TokenUrl::new(config.token_url.clone())
                                .map_err(|e| AppError::Internal(format!("Invalid token URL: {}", e)))?),
                        )
                        .set_redirect_uri(RedirectUrl::new(config.redirect_uri.clone())
                            .map_err(|e| AppError::Internal(format!("Invalid redirect URI: {}", e)))?))
                    }
                    _ => Err(AppError::BadRequest(format!("Unknown provider: {}", provider))),
                }
            }

            fn get_scopes(&self, provider: &str) -> String {
                match provider {
                    "google" => "openid email profile".to_string(),
                    "github" => "user:email".to_string(),
                    _ => String::new(),
                }
            }

            async fn get_user_info(
                &self,
                provider: &str,
                access_token: &str,
            ) -> Result<(String, Option<String>, Option<String>, Option<String>), AppError> {
                match provider {
                    "google" => {
                        let config = self.config.google.as_ref().unwrap();
                        let user_info: GoogleUserInfo = self.http_client
                            .get(&config.user_info_url)
                            .bearer_auth(access_token)
                            .send()
                            .await
                            .map_err(|e| AppError::Internal(format!("Failed to get user info: {}", e)))?
                            .json()
                            .await
                            .map_err(|e| AppError::Internal(format!("Failed to parse user info: {}", e)))?;

                        Ok((user_info.id, Some(user_info.email), user_info.name, user_info.picture))
                    }
                    "github" => {
                        let config = self.config.github.as_ref().unwrap();
                        let user_info: GitHubUserInfo = self.http_client
                            .get(&config.user_info_url)
                            .bearer_auth(access_token)
                            .header("User-Agent", "apigen-rust")
                            .send()
                            .await
                            .map_err(|e| AppError::Internal(format!("Failed to get user info: {}", e)))?
                            .json()
                            .await
                            .map_err(|e| AppError::Internal(format!("Failed to parse user info: {}", e)))?;

                        Ok((user_info.id.to_string(), user_info.email, user_info.name, user_info.avatar_url))
                    }
                    _ => Err(AppError::BadRequest(format!("Unknown provider: {}", provider))),
                }
            }

            async fn find_or_create_user(
                &self,
                provider: &str,
                provider_id: &str,
                email: Option<&str>,
                name: Option<&str>,
                avatar: Option<&str>,
            ) -> Result<(uuid::Uuid, Option<String>, Option<String>, bool), AppError> {
                // Try to find existing social account
                let existing = sqlx::query_as::<_, (uuid::Uuid,)>(
                    "SELECT user_id FROM social_accounts WHERE provider = $1 AND provider_id = $2"
                )
                .bind(provider)
                .bind(provider_id)
                .fetch_optional(&self.pool)
                .await?;

                if let Some((user_id,)) = existing {
                    // Get user info
                    let user = sqlx::query_as::<_, (Option<String>, Option<String>)>(
                        "SELECT email, username FROM users WHERE id = $1"
                    )
                    .bind(user_id)
                    .fetch_one(&self.pool)
                    .await?;

                    return Ok((user_id, user.0, user.1, false));
                }

                // Create new user
                let user_id = uuid::Uuid::new_v4();
                sqlx::query(
                    "INSERT INTO users (id, email, username, avatar_url, created_at) VALUES ($1, $2, $3, $4, NOW())"
                )
                .bind(user_id)
                .bind(email)
                .bind(name)
                .bind(avatar)
                .execute(&self.pool)
                .await?;

                // Create social account link
                sqlx::query(
                    "INSERT INTO social_accounts (user_id, provider, provider_id, created_at) VALUES ($1, $2, $3, NOW())"
                )
                .bind(user_id)
                .bind(provider)
                .bind(provider_id)
                .execute(&self.pool)
                .await?;

                Ok((user_id, email.map(String::from), name.map(String::from), true))
            }
        }
        """;
    }

    private String generateDto() {
        return """
        //! OAuth2 DTOs.

        use serde::{Deserialize, Serialize};

        /// OAuth callback query parameters.
        #[derive(Debug, Deserialize)]
        pub struct OAuthCallbackQuery {
            pub code: String,
            pub state: Option<String>,
        }

        /// OAuth login response.
        #[derive(Debug, Serialize)]
        pub struct OAuthLoginResponse {
            pub user_id: uuid::Uuid,
            #[serde(skip_serializing_if = "Option::is_none")]
            pub email: Option<String>,
            #[serde(skip_serializing_if = "Option::is_none")]
            pub name: Option<String>,
            pub provider: String,
            pub is_new_user: bool,
            pub access_token: String,
        }
        """;
    }

    private String getAuthUrl(String provider) {
        return switch (provider) {
            case "google" -> "https://accounts.google.com/o/oauth2/v2/auth";
            case "github" -> "https://github.com/login/oauth/authorize";
            default -> "";
        };
    }

    private String getTokenUrl(String provider) {
        return switch (provider) {
            case "google" -> "https://oauth2.googleapis.com/token";
            case "github" -> "https://github.com/login/oauth/access_token";
            default -> "";
        };
    }

    private String getUserInfoUrl(String provider) {
        return switch (provider) {
            case "google" -> "https://www.googleapis.com/oauth2/v2/userinfo";
            case "github" -> "https://api.github.com/user";
            default -> "";
        };
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
