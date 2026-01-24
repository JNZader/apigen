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
                            		social.GET("/%s", h.%sAuth)
                            		social.GET("/%s/callback", h.%sCallback)
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
                            	url := h.oauthConfig.GetAuthURL("%s")
                            	c.Redirect(http.StatusFound, url)
                            }

                            // %sCallback godoc
                            // @Summary Handle %s OAuth callback
                            // @Tags Social Authentication
                            // @Param code query string true "OAuth code"
                            // @Success 200 {object} dto.OAuthCallbackResult
                            // @Failure 400 {object} dto.ErrorResponse
                            // @Router /auth/social/%s/callback [get]
                            func (h *SocialAuthHandler) %sCallback(c *gin.Context) {
                            	code := c.Query("code")
                            	if code == "" {
                            		c.JSON(http.StatusBadRequest, dto.ErrorResponse{Error: "Missing authorization code"})
                            		return
                            	}

                            	result, err := h.service.HandleCallback("%s", code)
                            	if err != nil {
                            		c.JSON(http.StatusBadRequest, dto.ErrorResponse{Error: err.Error()})
                            		return
                            	}

                            	c.JSON(http.StatusOK, result)
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
                	"net/http"

                	"%s/internal/dto"

                	"github.com/gin-gonic/gin"
                )

                // SocialAuthHandler handles social authentication endpoints.
                type SocialAuthHandler struct {
                	service     *SocialAuthService
                	oauthConfig *OAuthConfig
                }

                // NewSocialAuthHandler creates a new social auth handler.
                func NewSocialAuthHandler(service *SocialAuthService, oauthConfig *OAuthConfig) *SocialAuthHandler {
                	return &SocialAuthHandler{
                		service:     service,
                		oauthConfig: oauthConfig,
                	}
                }

                // RegisterRoutes registers social auth routes.
                func (h *SocialAuthHandler) RegisterRoutes(rg *gin.RouterGroup) {
                	social := rg.Group("/auth/social")
                	{
                		social.GET("/providers", h.GetProviders)
                %s	}
                }

                // GetProviders godoc
                // @Summary List supported OAuth providers
                // @Tags Social Authentication
                // @Produce json
                // @Success 200 {object} dto.ProvidersResponse
                // @Router /auth/social/providers [get]
                func (h *SocialAuthHandler) GetProviders(c *gin.Context) {
                	providers := h.oauthConfig.GetSupportedProviders()
                	c.JSON(http.StatusOK, dto.ProvidersResponse{Providers: providers})
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
                	"context"
                	"errors"
                	"log"
                	"time"

                	"%s/internal/dto"
                	"%s/internal/models"

                	"github.com/golang-jwt/jwt/v5"
                	"github.com/spf13/viper"
                	"golang.org/x/oauth2"
                	"gorm.io/gorm"
                )

                // SocialAuthService handles social authentication operations.
                type SocialAuthService struct {
                	db          *gorm.DB
                	oauthConfig *OAuthConfig
                }

                // NewSocialAuthService creates a new social auth service.
                func NewSocialAuthService(db *gorm.DB, oauthConfig *OAuthConfig) *SocialAuthService {
                	return &SocialAuthService{
                		db:          db,
                		oauthConfig: oauthConfig,
                	}
                }

                // HandleCallback processes the OAuth callback and returns tokens.
                func (s *SocialAuthService) HandleCallback(provider, code string) (*dto.OAuthCallbackResult, error) {
                	config := s.oauthConfig.GetConfig(provider)
                	if config == nil {
                		return nil, errors.New("unsupported provider: " + provider)
                	}

                	// Exchange code for token
                	token, err := config.Exchange(context.Background(), code)
                	if err != nil {
                		return nil, errors.New("failed to exchange code: " + err.Error())
                	}

                	// Get user info from provider
                	socialUser, err := s.getUserInfo(provider, token)
                	if err != nil {
                		return nil, err
                	}

                	// Find or create user
                	user, isNewUser, err := s.findOrCreateUser(provider, socialUser)
                	if err != nil {
                		return nil, err
                	}

                	// Generate JWT tokens
                	accessToken, refreshToken, err := s.generateTokens(user)
                	if err != nil {
                		return nil, err
                	}

                	return &dto.OAuthCallbackResult{
                		AccessToken:  accessToken,
                		RefreshToken: refreshToken,
                		TokenType:    "bearer",
                		UserID:       user.ID.String(),
                		Email:        user.Email,
                		IsNewUser:    isNewUser,
                	}, nil
                }

                func (s *SocialAuthService) getUserInfo(provider string, token *oauth2.Token) (*dto.SocialUser, error) {
                	return s.oauthConfig.GetUserInfo(provider, token)
                }

                func (s *SocialAuthService) findOrCreateUser(provider string, socialUser *dto.SocialUser) (*models.User, bool, error) {
                	var user models.User
                	isNewUser := false

                	// Try to find by provider ID first
                	providerIDField := provider + "_id"
                	err := s.db.Where(providerIDField+" = ?", socialUser.ProviderID).First(&user).Error

                	if errors.Is(err, gorm.ErrRecordNotFound) {
                		// Try to find by email
                		err = s.db.Where("email = ?", socialUser.Email).First(&user).Error
                	}

                	if errors.Is(err, gorm.ErrRecordNotFound) {
                		// Create new user
                		user = models.User{
                			Email:    socialUser.Email,
                			Name:     socialUser.Name,
                			IsActive: true,
                		}

                		// Set provider ID dynamically
                		switch provider {
                		case "google":
                			user.GoogleID = &socialUser.ProviderID
                		case "github":
                			user.GitHubID = &socialUser.ProviderID
                		}

                		if err := s.db.Create(&user).Error; err != nil {
                			return nil, false, err
                		}

                		isNewUser = true
                		log.Printf("New user created via %%s: %%s", provider, socialUser.Email)
                	} else if err != nil {
                		return nil, false, err
                	}

                	return &user, isNewUser, nil
                }

                func (s *SocialAuthService) generateTokens(user *models.User) (string, string, error) {
                	secret := viper.GetString("jwt.secret")
                	accessExpiration := viper.GetDuration("jwt.access_expiration")
                	refreshExpiration := viper.GetDuration("jwt.refresh_expiration")

                	if accessExpiration == 0 {
                		accessExpiration = 15 * time.Minute
                	}
                	if refreshExpiration == 0 {
                		refreshExpiration = 7 * 24 * time.Hour
                	}

                	// Access token
                	accessClaims := jwt.MapClaims{
                		"sub":   user.ID.String(),
                		"email": user.Email,
                		"exp":   time.Now().Add(accessExpiration).Unix(),
                		"iat":   time.Now().Unix(),
                	}
                	accessToken := jwt.NewWithClaims(jwt.SigningMethodHS256, accessClaims)
                	accessTokenString, err := accessToken.SignedString([]byte(secret))
                	if err != nil {
                		return "", "", err
                	}

                	// Refresh token
                	refreshClaims := jwt.MapClaims{
                		"sub":     user.ID.String(),
                		"refresh": true,
                		"exp":     time.Now().Add(refreshExpiration).Unix(),
                		"iat":     time.Now().Unix(),
                	}
                	refreshToken := jwt.NewWithClaims(jwt.SigningMethodHS256, refreshClaims)
                	refreshTokenString, err := refreshToken.SignedString([]byte(secret))
                	if err != nil {
                		return "", "", err
                	}

                	return accessTokenString, refreshTokenString, nil
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
                                	if clientID := viper.GetString("%s_client_id"); clientID != "" {
                                		c.configs["%s"] = &oauth2.Config{
                                			ClientID:     clientID,
                                			ClientSecret: viper.GetString("%s_client_secret"),
                                			RedirectURL:  viper.GetString("%s_redirect_uri"),
                                			Scopes:       []string{"email", "profile"},
                                			Endpoint: oauth2.Endpoint{
                                				AuthURL:  "https://accounts.google.com/o/oauth2/auth",
                                				TokenURL: "https://oauth2.googleapis.com/token",
                                			},
                                		}
                                		c.providers = append(c.providers, "%s")
                                	}

                                """,
                                upperProvider, provider, upperProvider, upperProvider, provider));

                userInfoCases.append(
                        """
                        	case "google":
                        		resp, err := http.Get("https://www.googleapis.com/oauth2/v2/userinfo?access_token=" + token.AccessToken)
                        		if err != nil {
                        			return nil, err
                        		}
                        		defer resp.Body.Close()

                        		var data struct {
                        			ID      string `json:"id"`
                        			Email   string `json:"email"`
                        			Name    string `json:"name"`
                        			Picture string `json:"picture"`
                        		}
                        		if err := json.NewDecoder(resp.Body).Decode(&data); err != nil {
                        			return nil, err
                        		}

                        		return &dto.SocialUser{
                        			Provider:   "google",
                        			ProviderID: data.ID,
                        			Email:      data.Email,
                        			Name:       data.Name,
                        			Picture:    data.Picture,
                        		}, nil

                        """);
            } else if (provider.equals("github")) {
                providerConfigs.append(
                        String.format(
                                """
                                	if clientID := viper.GetString("%s_client_id"); clientID != "" {
                                		c.configs["%s"] = &oauth2.Config{
                                			ClientID:     clientID,
                                			ClientSecret: viper.GetString("%s_client_secret"),
                                			RedirectURL:  viper.GetString("%s_redirect_uri"),
                                			Scopes:       []string{"user:email"},
                                			Endpoint: oauth2.Endpoint{
                                				AuthURL:  "https://github.com/login/oauth/authorize",
                                				TokenURL: "https://github.com/login/oauth/access_token",
                                			},
                                		}
                                		c.providers = append(c.providers, "%s")
                                	}

                                """,
                                upperProvider, provider, upperProvider, upperProvider, provider));

                userInfoCases.append(
                        """
                        	case "github":
                        		req, _ := http.NewRequest("GET", "https://api.github.com/user", nil)
                        		req.Header.Set("Authorization", "Bearer "+token.AccessToken)
                        		req.Header.Set("Accept", "application/vnd.github.v3+json")

                        		client := &http.Client{}
                        		resp, err := client.Do(req)
                        		if err != nil {
                        			return nil, err
                        		}
                        		defer resp.Body.Close()

                        		var data struct {
                        			ID        int64  `json:"id"`
                        			Email     string `json:"email"`
                        			Name      string `json:"name"`
                        			Login     string `json:"login"`
                        			AvatarURL string `json:"avatar_url"`
                        		}
                        		if err := json.NewDecoder(resp.Body).Decode(&data); err != nil {
                        			return nil, err
                        		}

                        		email := data.Email
                        		if email == "" {
                        			email = data.Login + "@github.com"
                        		}

                        		name := data.Name
                        		if name == "" {
                        			name = data.Login
                        		}

                        		return &dto.SocialUser{
                        			Provider:   "github",
                        			ProviderID: fmt.Sprintf("%d", data.ID),
                        			Email:      email,
                        			Name:       name,
                        			Picture:    data.AvatarURL,
                        		}, nil

                        """);
            }
        }

        return String.format(
                """
                package auth

                import (
                	"encoding/json"
                	"errors"
                	"fmt"
                	"net/http"

                	"%s/internal/dto"

                	"github.com/spf13/viper"
                	"golang.org/x/oauth2"
                )

                // OAuthConfig manages OAuth2 provider configurations.
                type OAuthConfig struct {
                	configs   map[string]*oauth2.Config
                	providers []string
                }

                // NewOAuthConfig creates a new OAuth configuration from environment.
                func NewOAuthConfig() *OAuthConfig {
                	c := &OAuthConfig{
                		configs:   make(map[string]*oauth2.Config),
                		providers: []string{},
                	}

                %s
                	return c
                }

                // GetConfig returns the OAuth2 config for a provider.
                func (c *OAuthConfig) GetConfig(provider string) *oauth2.Config {
                	return c.configs[provider]
                }

                // GetAuthURL returns the authorization URL for a provider.
                func (c *OAuthConfig) GetAuthURL(provider string) string {
                	config := c.configs[provider]
                	if config == nil {
                		return ""
                	}
                	return config.AuthCodeURL("state")
                }

                // GetSupportedProviders returns the list of configured providers.
                func (c *OAuthConfig) GetSupportedProviders() []string {
                	return c.providers
                }

                // GetUserInfo fetches user info from the OAuth provider.
                func (c *OAuthConfig) GetUserInfo(provider string, token *oauth2.Token) (*dto.SocialUser, error) {
                	switch provider {
                %s
                	default:
                		return nil, errors.New("unsupported provider: " + provider)
                	}
                }
                """,
                moduleName, providerConfigs, userInfoCases);
    }

    private String generateDto() {
        return """
        package dto

        // SocialUser represents a user from an OAuth provider.
        type SocialUser struct {
        	Provider   string `json:"provider"`
        	ProviderID string `json:"provider_id"`
        	Email      string `json:"email"`
        	Name       string `json:"name,omitempty"`
        	Picture    string `json:"picture,omitempty"`
        }

        // OAuthCallbackResult represents the result of an OAuth callback.
        type OAuthCallbackResult struct {
        	AccessToken  string `json:"access_token"`
        	RefreshToken string `json:"refresh_token"`
        	TokenType    string `json:"token_type"`
        	UserID       string `json:"user_id"`
        	Email        string `json:"email"`
        	IsNewUser    bool   `json:"is_new_user"`
        }

        // ProvidersResponse represents the list of supported OAuth providers.
        type ProvidersResponse struct {
        	Providers []string `json:"providers"`
        }
        """;
    }
}
