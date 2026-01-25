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
package com.jnzader.apigen.codegen.generator.gochi.security.social;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Generates social login (OAuth2) code for Go/Chi applications.
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
public class GoChiSocialLoginGenerator {

    private final String moduleName;

    public GoChiSocialLoginGenerator(String moduleName) {
        this.moduleName = moduleName;
    }

    /**
     * Generates social login files.
     *
     * @param providers list of OAuth2 providers (google, github, etc.)
     * @return map of file path to content
     */
    public Map<String, String> generate(List<String> providers) {
        Map<String, String> files = new LinkedHashMap<>();

        files.put("internal/auth/social_auth_handler.go", generateHandler(providers));
        files.put("internal/auth/social_auth_service.go", generateService());
        files.put("internal/auth/oauth_config.go", generateOAuthConfig(providers));
        files.put("internal/dto/social_auth_dto.go", generateDto());

        return files;
    }

    private String generateHandler(List<String> providers) {
        StringBuilder routeRegistration = new StringBuilder();
        StringBuilder handlers = new StringBuilder();

        for (String provider : providers) {
            String capitalProvider =
                    provider.substring(0, 1).toUpperCase(Locale.ROOT) + provider.substring(1);

            routeRegistration.append(
                    String.format(
                            """
                            \t\tr.Get("/%s", h.%sAuth)
                            \t\tr.Get("/%s/callback", h.%sCallback)
                            """,
                            provider, capitalProvider, provider, capitalProvider));

            handlers.append(
                    String.format(
                            """

                            // %sAuth initiates %s OAuth login.
                            // @Summary Initiate %s OAuth login
                            // @Tags Social Authentication
                            // @Success 302 {string} string "Redirect to %s"
                            // @Router /auth/social/%s [get]
                            func (h *SocialAuthHandler) %sAuth(w http.ResponseWriter, r *http.Request) {
                            \turl := h.oauthConfig.GetAuthURL("%s")
                            \thttp.Redirect(w, r, url, http.StatusFound)
                            }

                            // %sCallback handles %s OAuth callback.
                            // @Summary Handle %s OAuth callback
                            // @Tags Social Authentication
                            // @Param code query string true "OAuth code"
                            // @Success 200 {object} dto.OAuthCallbackResult
                            // @Failure 400 {object} dto.ErrorResponse
                            // @Router /auth/social/%s/callback [get]
                            func (h *SocialAuthHandler) %sCallback(w http.ResponseWriter, r *http.Request) {
                            \tcode := r.URL.Query().Get("code")
                            \tif code == "" {
                            \t\th.writeError(w, http.StatusBadRequest, "Missing authorization code")
                            \t\treturn
                            \t}

                            \tresult, err := h.service.HandleCallback("%s", code)
                            \tif err != nil {
                            \t\th.writeError(w, http.StatusBadRequest, err.Error())
                            \t\treturn
                            \t}

                            \th.writeJSON(w, http.StatusOK, result)
                            }
                            """,
                            capitalProvider,
                            capitalProvider,
                            capitalProvider,
                            capitalProvider,
                            provider,
                            capitalProvider,
                            provider,
                            capitalProvider,
                            capitalProvider,
                            capitalProvider,
                            provider,
                            capitalProvider,
                            provider));
        }

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

                // SocialAuthHandler handles social authentication endpoints.
                type SocialAuthHandler struct {
                \tservice     *SocialAuthService
                \toauthConfig *OAuthConfig
                \tlogger      *slog.Logger
                }

                // NewSocialAuthHandler creates a new social auth handler.
                func NewSocialAuthHandler(service *SocialAuthService, oauthConfig *OAuthConfig, logger *slog.Logger) *SocialAuthHandler {
                \treturn &SocialAuthHandler{
                \t\tservice:     service,
                \t\toauthConfig: oauthConfig,
                \t\tlogger:      logger.With("handler", "social_auth"),
                \t}
                }

                // RegisterRoutes registers social auth routes.
                func (h *SocialAuthHandler) RegisterRoutes(r chi.Router) {
                \tr.Route("/auth/social", func(r chi.Router) {
                \t\tr.Get("/providers", h.GetProviders)
                %s\t})
                }

                // GetProviders lists supported OAuth providers.
                // @Summary List supported OAuth providers
                // @Tags Social Authentication
                // @Produce json
                // @Success 200 {object} dto.ProvidersResponse
                // @Router /auth/social/providers [get]
                func (h *SocialAuthHandler) GetProviders(w http.ResponseWriter, r *http.Request) {
                \tproviders := h.oauthConfig.GetSupportedProviders()
                \th.writeJSON(w, http.StatusOK, dto.ProvidersResponse{Providers: providers})
                }

                func (h *SocialAuthHandler) writeJSON(w http.ResponseWriter, status int, data interface{}) {
                \tw.Header().Set("Content-Type", "application/json")
                \tw.WriteHeader(status)
                \tif err := json.NewEncoder(w).Encode(data); err != nil {
                \t\th.logger.Error("failed to encode response", "error", err)
                \t}
                }

                func (h *SocialAuthHandler) writeError(w http.ResponseWriter, status int, message string) {
                \th.writeJSON(w, status, dto.ErrorResponse{Error: message})
                }
                %s
                """,
                moduleName, routeRegistration, handlers);
    }

    private String generateService() {
        return String.format(
                """
                package auth

                import (
                \t"context"
                \t"errors"
                \t"log/slog"
                \t"time"

                \t"%s/internal/dto"

                \t"github.com/golang-jwt/jwt/v5"
                \t"github.com/google/uuid"
                \t"github.com/jackc/pgx/v5/pgxpool"
                \t"github.com/spf13/viper"
                \t"golang.org/x/oauth2"
                )

                // SocialAuthService handles social authentication operations.
                type SocialAuthService struct {
                \tdb          *pgxpool.Pool
                \toauthConfig *OAuthConfig
                \tlogger      *slog.Logger
                }

                // NewSocialAuthService creates a new social auth service.
                func NewSocialAuthService(db *pgxpool.Pool, oauthConfig *OAuthConfig, logger *slog.Logger) *SocialAuthService {
                \treturn &SocialAuthService{
                \t\tdb:          db,
                \t\toauthConfig: oauthConfig,
                \t\tlogger:      logger.With("service", "social_auth"),
                \t}
                }

                // HandleCallback processes the OAuth callback and returns tokens.
                func (s *SocialAuthService) HandleCallback(provider, code string) (*dto.OAuthCallbackResult, error) {
                \tconfig := s.oauthConfig.GetConfig(provider)
                \tif config == nil {
                \t\treturn nil, errors.New("unsupported provider: " + provider)
                \t}

                \t// Exchange code for token
                \ttoken, err := config.Exchange(context.Background(), code)
                \tif err != nil {
                \t\treturn nil, errors.New("failed to exchange code: " + err.Error())
                \t}

                \t// Get user info from provider
                \tsocialUser, err := s.oauthConfig.GetUserInfo(provider, token)
                \tif err != nil {
                \t\treturn nil, err
                \t}

                \t// Find or create user
                \tuserID, isNewUser, err := s.findOrCreateUser(provider, socialUser)
                \tif err != nil {
                \t\treturn nil, err
                \t}

                \t// Generate JWT tokens
                \taccessToken, refreshToken, err := s.generateTokens(userID, socialUser.Email)
                \tif err != nil {
                \t\treturn nil, err
                \t}

                \treturn &dto.OAuthCallbackResult{
                \t\tAccessToken:  accessToken,
                \t\tRefreshToken: refreshToken,
                \t\tTokenType:    "bearer",
                \t\tUserID:       userID,
                \t\tEmail:        socialUser.Email,
                \t\tIsNewUser:    isNewUser,
                \t}, nil
                }

                func (s *SocialAuthService) findOrCreateUser(provider string, socialUser *dto.SocialUser) (string, bool, error) {
                \tctx := context.Background()
                \tvar userID string
                \tisNewUser := false

                \t// Try to find by provider ID first
                \tproviderIDField := provider + "_id"
                \terr := s.db.QueryRow(ctx,
                \t\t"SELECT id FROM users WHERE "+providerIDField+" = $1",
                \t\tsocialUser.ProviderID,
                \t).Scan(&userID)

                \tif err != nil {
                \t\t// Try to find by email
                \t\terr = s.db.QueryRow(ctx,
                \t\t\t"SELECT id FROM users WHERE email = $1",
                \t\t\tsocialUser.Email,
                \t\t).Scan(&userID)
                \t}

                \tif err != nil {
                \t\t// Create new user
                \t\tnewID := uuid.New().String()
                \t\tnow := time.Now()

                \t\tvar insertErr error
                \t\tswitch provider {
                \t\tcase "google":
                \t\t\t_, insertErr = s.db.Exec(ctx,
                \t\t\t\t`INSERT INTO users (id, email, name, google_id, is_active, created_at, updated_at)
                \t\t\t\t VALUES ($1, $2, $3, $4, true, $5, $5)`,
                \t\t\t\tnewID, socialUser.Email, socialUser.Name, socialUser.ProviderID, now,
                \t\t\t)
                \t\tcase "github":
                \t\t\t_, insertErr = s.db.Exec(ctx,
                \t\t\t\t`INSERT INTO users (id, email, name, github_id, is_active, created_at, updated_at)
                \t\t\t\t VALUES ($1, $2, $3, $4, true, $5, $5)`,
                \t\t\t\tnewID, socialUser.Email, socialUser.Name, socialUser.ProviderID, now,
                \t\t\t)
                \t\tdefault:
                \t\t\treturn "", false, errors.New("unsupported provider: " + provider)
                \t\t}

                \t\tif insertErr != nil {
                \t\t\treturn "", false, insertErr
                \t\t}

                \t\tuserID = newID
                \t\tisNewUser = true
                \t\ts.logger.Info("new user created via social login", "provider", provider, "email", socialUser.Email)
                \t}

                \treturn userID, isNewUser, nil
                }

                func (s *SocialAuthService) generateTokens(userID, email string) (string, string, error) {
                \tsecret := viper.GetString("jwt.secret")
                \taccessExpiration := viper.GetDuration("jwt.access_expiration")
                \trefreshExpiration := viper.GetDuration("jwt.refresh_expiration")

                \tif accessExpiration == 0 {
                \t\taccessExpiration = 15 * time.Minute
                \t}
                \tif refreshExpiration == 0 {
                \t\trefreshExpiration = 7 * 24 * time.Hour
                \t}

                \t// Access token
                \taccessClaims := jwt.MapClaims{
                \t\t"sub":   userID,
                \t\t"email": email,
                \t\t"exp":   time.Now().Add(accessExpiration).Unix(),
                \t\t"iat":   time.Now().Unix(),
                \t}
                \taccessToken := jwt.NewWithClaims(jwt.SigningMethodHS256, accessClaims)
                \taccessTokenString, err := accessToken.SignedString([]byte(secret))
                \tif err != nil {
                \t\treturn "", "", err
                \t}

                \t// Refresh token
                \trefreshClaims := jwt.MapClaims{
                \t\t"sub":     userID,
                \t\t"refresh": true,
                \t\t"exp":     time.Now().Add(refreshExpiration).Unix(),
                \t\t"iat":     time.Now().Unix(),
                \t}
                \trefreshToken := jwt.NewWithClaims(jwt.SigningMethodHS256, refreshClaims)
                \trefreshTokenString, err := refreshToken.SignedString([]byte(secret))
                \tif err != nil {
                \t\treturn "", "", err
                \t}

                \treturn accessTokenString, refreshTokenString, nil
                }
                """,
                moduleName);
    }

    private String generateOAuthConfig(List<String> providers) {
        StringBuilder providerConfigs = new StringBuilder();
        StringBuilder userInfoCases = new StringBuilder();

        for (String provider : providers) {
            String upperProvider = provider.toUpperCase(Locale.ROOT);

            if (provider.equals("google")) {
                providerConfigs.append(
                        String.format(
                                """
                                \tif clientID := viper.GetString("%s_client_id"); clientID != "" {
                                \t\tc.configs["%s"] = &oauth2.Config{
                                \t\t\tClientID:     clientID,
                                \t\t\tClientSecret: viper.GetString("%s_client_secret"),
                                \t\t\tRedirectURL:  viper.GetString("%s_redirect_uri"),
                                \t\t\tScopes:       []string{"email", "profile"},
                                \t\t\tEndpoint: oauth2.Endpoint{
                                \t\t\t\tAuthURL:  "https://accounts.google.com/o/oauth2/auth",
                                \t\t\t\tTokenURL: "https://oauth2.googleapis.com/token",
                                \t\t\t},
                                \t\t}
                                \t\tc.providers = append(c.providers, "%s")
                                \t}

                                """,
                                upperProvider, provider, upperProvider, upperProvider, provider));

                userInfoCases.append(
                        """
                        \tcase "google":
                        \t\tresp, err := http.Get("https://www.googleapis.com/oauth2/v2/userinfo?access_token=" + token.AccessToken)
                        \t\tif err != nil {
                        \t\t\treturn nil, err
                        \t\t}
                        \t\tdefer resp.Body.Close()

                        \t\tvar data struct {
                        \t\t\tID      string `json:"id"`
                        \t\t\tEmail   string `json:"email"`
                        \t\t\tName    string `json:"name"`
                        \t\t\tPicture string `json:"picture"`
                        \t\t}
                        \t\tif err := json.NewDecoder(resp.Body).Decode(&data); err != nil {
                        \t\t\treturn nil, err
                        \t\t}

                        \t\treturn &dto.SocialUser{
                        \t\t\tProvider:   "google",
                        \t\t\tProviderID: data.ID,
                        \t\t\tEmail:      data.Email,
                        \t\t\tName:       data.Name,
                        \t\t\tPicture:    data.Picture,
                        \t\t}, nil

                        """);
            } else if (provider.equals("github")) {
                providerConfigs.append(
                        String.format(
                                """
                                \tif clientID := viper.GetString("%s_client_id"); clientID != "" {
                                \t\tc.configs["%s"] = &oauth2.Config{
                                \t\t\tClientID:     clientID,
                                \t\t\tClientSecret: viper.GetString("%s_client_secret"),
                                \t\t\tRedirectURL:  viper.GetString("%s_redirect_uri"),
                                \t\t\tScopes:       []string{"user:email"},
                                \t\t\tEndpoint: oauth2.Endpoint{
                                \t\t\t\tAuthURL:  "https://github.com/login/oauth/authorize",
                                \t\t\t\tTokenURL: "https://github.com/login/oauth/access_token",
                                \t\t\t},
                                \t\t}
                                \t\tc.providers = append(c.providers, "%s")
                                \t}

                                """,
                                upperProvider, provider, upperProvider, upperProvider, provider));

                userInfoCases.append(
                        """
                        \tcase "github":
                        \t\treq, _ := http.NewRequest("GET", "https://api.github.com/user", nil)
                        \t\treq.Header.Set("Authorization", "Bearer "+token.AccessToken)
                        \t\treq.Header.Set("Accept", "application/vnd.github.v3+json")

                        \t\tclient := &http.Client{}
                        \t\tresp, err := client.Do(req)
                        \t\tif err != nil {
                        \t\t\treturn nil, err
                        \t\t}
                        \t\tdefer resp.Body.Close()

                        \t\tvar data struct {
                        \t\t\tID        int64  `json:"id"`
                        \t\t\tEmail     string `json:"email"`
                        \t\t\tName      string `json:"name"`
                        \t\t\tLogin     string `json:"login"`
                        \t\t\tAvatarURL string `json:"avatar_url"`
                        \t\t}
                        \t\tif err := json.NewDecoder(resp.Body).Decode(&data); err != nil {
                        \t\t\treturn nil, err
                        \t\t}

                        \t\temail := data.Email
                        \t\tif email == "" {
                        \t\t\temail = data.Login + "@github.com"
                        \t\t}

                        \t\tname := data.Name
                        \t\tif name == "" {
                        \t\t\tname = data.Login
                        \t\t}

                        \t\treturn &dto.SocialUser{
                        \t\t\tProvider:   "github",
                        \t\t\tProviderID: fmt.Sprintf("%d", data.ID),
                        \t\t\tEmail:      email,
                        \t\t\tName:       name,
                        \t\t\tPicture:    data.AvatarURL,
                        \t\t}, nil

                        """);
            }
        }

        return String.format(
                """
                package auth

                import (
                \t"encoding/json"
                \t"errors"
                \t"fmt"
                \t"net/http"

                \t"%s/internal/dto"

                \t"github.com/spf13/viper"
                \t"golang.org/x/oauth2"
                )

                // OAuthConfig manages OAuth2 provider configurations.
                type OAuthConfig struct {
                \tconfigs   map[string]*oauth2.Config
                \tproviders []string
                }

                // NewOAuthConfig creates a new OAuth configuration from environment.
                func NewOAuthConfig() *OAuthConfig {
                \tc := &OAuthConfig{
                \t\tconfigs:   make(map[string]*oauth2.Config),
                \t\tproviders: []string{},
                \t}

                %s
                \treturn c
                }

                // GetConfig returns the OAuth2 config for a provider.
                func (c *OAuthConfig) GetConfig(provider string) *oauth2.Config {
                \treturn c.configs[provider]
                }

                // GetAuthURL returns the authorization URL for a provider.
                func (c *OAuthConfig) GetAuthURL(provider string) string {
                \tconfig := c.configs[provider]
                \tif config == nil {
                \t\treturn ""
                \t}
                \treturn config.AuthCodeURL("state")
                }

                // GetSupportedProviders returns the list of configured providers.
                func (c *OAuthConfig) GetSupportedProviders() []string {
                \treturn c.providers
                }

                // GetUserInfo fetches user info from the OAuth provider.
                func (c *OAuthConfig) GetUserInfo(provider string, token *oauth2.Token) (*dto.SocialUser, error) {
                \tswitch provider {
                %s
                \tdefault:
                \t\treturn nil, errors.New("unsupported provider: " + provider)
                \t}
                }
                """,
                moduleName, providerConfigs, userInfoCases);
    }

    private String generateDto() {
        return """
        package dto

        // SocialUser represents a user from an OAuth provider.
        type SocialUser struct {
        \tProvider   string `json:"provider"`
        \tProviderID string `json:"provider_id"`
        \tEmail      string `json:"email"`
        \tName       string `json:"name,omitempty"`
        \tPicture    string `json:"picture,omitempty"`
        }

        // OAuthCallbackResult represents the result of an OAuth callback.
        type OAuthCallbackResult struct {
        \tAccessToken  string `json:"access_token"`
        \tRefreshToken string `json:"refresh_token"`
        \tTokenType    string `json:"token_type"`
        \tUserID       string `json:"user_id"`
        \tEmail        string `json:"email"`
        \tIsNewUser    bool   `json:"is_new_user"`
        }

        // ProvidersResponse represents the list of supported OAuth providers.
        type ProvidersResponse struct {
        \tProviders []string `json:"providers"`
        }
        """;
    }
}
