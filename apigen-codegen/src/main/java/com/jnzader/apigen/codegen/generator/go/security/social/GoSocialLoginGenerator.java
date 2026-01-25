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
package com.jnzader.apigen.codegen.generator.go.security.social;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates social login (OAuth2) code for Go/Gin applications.
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
public class GoSocialLoginGenerator {

    private final String moduleName;

    public GoSocialLoginGenerator(String moduleName) {
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
            String capitalProvider = provider.substring(0, 1).toUpperCase() + provider.substring(1);

            routeRegistration.append(
                    String.format(
                            """
                            \t\tsocial.GET("/%s", h.%sAuth)
                            \t\tsocial.GET("/%s/callback", h.%sCallback)
                            """,
                            provider, capitalProvider, provider, capitalProvider));

            handlers.append(
                    String.format(
                            """

                            // %sAuth godoc
                            // @Summary Initiate %s OAuth login
                            // @Tags Social Authentication
                            // @Success 302 {string} string "Redirect to %s"
                            // @Router /auth/social/%s [get]
                            func (h *SocialAuthHandler) %sAuth(c *gin.Context) {
                            \turl := h.oauthConfig.GetAuthURL("%s")
                            \tc.Redirect(http.StatusFound, url)
                            }

                            // %sCallback godoc
                            // @Summary Handle %s OAuth callback
                            // @Tags Social Authentication
                            // @Param code query string true "OAuth code"
                            // @Success 200 {object} dto.OAuthCallbackResult
                            // @Failure 400 {object} dto.ErrorResponse
                            // @Router /auth/social/%s/callback [get]
                            func (h *SocialAuthHandler) %sCallback(c *gin.Context) {
                            \tcode := c.Query("code")
                            \tif code == "" {
                            \t\tc.JSON(http.StatusBadRequest, dto.ErrorResponse{Error: "Missing authorization code"})
                            \t\treturn
                            \t}

                            \tresult, err := h.service.HandleCallback("%s", code)
                            \tif err != nil {
                            \t\tc.JSON(http.StatusBadRequest, dto.ErrorResponse{Error: err.Error()})
                            \t\treturn
                            \t}

                            \tc.JSON(http.StatusOK, result)
                            }
                            """,
                            capitalProvider,
                            capitalProvider,
                            capitalProvider,
                            provider,
                            capitalProvider,
                            provider,
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
                \t"net/http"

                \t"%s/internal/dto"

                \t"github.com/gin-gonic/gin"
                )

                // SocialAuthHandler handles social authentication endpoints.
                type SocialAuthHandler struct {
                \tservice     *SocialAuthService
                \toauthConfig *OAuthConfig
                }

                // NewSocialAuthHandler creates a new social auth handler.
                func NewSocialAuthHandler(service *SocialAuthService, oauthConfig *OAuthConfig) *SocialAuthHandler {
                \treturn &SocialAuthHandler{
                \t\tservice:     service,
                \t\toauthConfig: oauthConfig,
                \t}
                }

                // RegisterRoutes registers social auth routes.
                func (h *SocialAuthHandler) RegisterRoutes(rg *gin.RouterGroup) {
                \tsocial := rg.Group("/auth/social")
                \t{
                \t\tsocial.GET("/providers", h.GetProviders)
                %s\t}
                }

                // GetProviders godoc
                // @Summary List supported OAuth providers
                // @Tags Social Authentication
                // @Produce json
                // @Success 200 {object} dto.ProvidersResponse
                // @Router /auth/social/providers [get]
                func (h *SocialAuthHandler) GetProviders(c *gin.Context) {
                \tproviders := h.oauthConfig.GetSupportedProviders()
                \tc.JSON(http.StatusOK, dto.ProvidersResponse{Providers: providers})
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
                \t"log"
                \t"time"

                \t"%s/internal/dto"
                \t"%s/internal/models"

                \t"github.com/golang-jwt/jwt/v5"
                \t"github.com/spf13/viper"
                \t"golang.org/x/oauth2"
                \t"gorm.io/gorm"
                )

                // SocialAuthService handles social authentication operations.
                type SocialAuthService struct {
                \tdb          *gorm.DB
                \toauthConfig *OAuthConfig
                }

                // NewSocialAuthService creates a new social auth service.
                func NewSocialAuthService(db *gorm.DB, oauthConfig *OAuthConfig) *SocialAuthService {
                \treturn &SocialAuthService{
                \t\tdb:          db,
                \t\toauthConfig: oauthConfig,
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
                \tsocialUser, err := s.getUserInfo(provider, token)
                \tif err != nil {
                \t\treturn nil, err
                \t}

                \t// Find or create user
                \tuser, isNewUser, err := s.findOrCreateUser(provider, socialUser)
                \tif err != nil {
                \t\treturn nil, err
                \t}

                \t// Generate JWT tokens
                \taccessToken, refreshToken, err := s.generateTokens(user)
                \tif err != nil {
                \t\treturn nil, err
                \t}

                \treturn &dto.OAuthCallbackResult{
                \t\tAccessToken:  accessToken,
                \t\tRefreshToken: refreshToken,
                \t\tTokenType:    "bearer",
                \t\tUserID:       user.ID.String(),
                \t\tEmail:        user.Email,
                \t\tIsNewUser:    isNewUser,
                \t}, nil
                }

                func (s *SocialAuthService) getUserInfo(provider string, token *oauth2.Token) (*dto.SocialUser, error) {
                \treturn s.oauthConfig.GetUserInfo(provider, token)
                }

                func (s *SocialAuthService) findOrCreateUser(provider string, socialUser *dto.SocialUser) (*models.User, bool, error) {
                \tvar user models.User
                \tisNewUser := false

                \t// Try to find by provider ID first
                \tproviderIDField := provider + "_id"
                \terr := s.db.Where(providerIDField+" = ?", socialUser.ProviderID).First(&user).Error

                \tif errors.Is(err, gorm.ErrRecordNotFound) {
                \t\t// Try to find by email
                \t\terr = s.db.Where("email = ?", socialUser.Email).First(&user).Error
                \t}

                \tif errors.Is(err, gorm.ErrRecordNotFound) {
                \t\t// Create new user
                \t\tuser = models.User{
                \t\t\tEmail:    socialUser.Email,
                \t\t\tName:     socialUser.Name,
                \t\t\tIsActive: true,
                \t\t}

                \t\t// Set provider ID dynamically
                \t\tswitch provider {
                \t\tcase "google":
                \t\t\tuser.GoogleID = &socialUser.ProviderID
                \t\tcase "github":
                \t\t\tuser.GitHubID = &socialUser.ProviderID
                \t\t}

                \t\tif err := s.db.Create(&user).Error; err != nil {
                \t\t\treturn nil, false, err
                \t\t}

                \t\tisNewUser = true
                \t\tlog.Printf("New user created via %%s: %%s", provider, socialUser.Email)
                \t} else if err != nil {
                \t\treturn nil, false, err
                \t}

                \treturn &user, isNewUser, nil
                }

                func (s *SocialAuthService) generateTokens(user *models.User) (string, string, error) {
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
                \t\t"sub":   user.ID.String(),
                \t\t"email": user.Email,
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
                \t\t"sub":     user.ID.String(),
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
                moduleName, moduleName);
    }

    private String generateOAuthConfig(List<String> providers) {
        StringBuilder providerConfigs = new StringBuilder();
        StringBuilder userInfoCases = new StringBuilder();

        for (String provider : providers) {
            String upperProvider = provider.toUpperCase();

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
