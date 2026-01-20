package com.jnzader.apigen.security.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO for PKCE token exchange requests.
 *
 * <p>This represents the token request in the PKCE flow. The client sends this to exchange an
 * authorization code for access and refresh tokens.
 *
 * <p>Example request:
 *
 * <pre>{@code
 * {
 *   "grant_type": "authorization_code",
 *   "code": "SplxlOBeZQQYbYS6WxSbIA",
 *   "redirect_uri": "https://myapp.com/callback",
 *   "client_id": "my-spa-app",
 *   "code_verifier": "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
 * }
 * }</pre>
 *
 * @param grantType must be "authorization_code" for PKCE flow
 * @param code the authorization code received from the authorization endpoint
 * @param redirectUri must match the redirect_uri from the authorization request
 * @param clientId the OAuth2 client identifier
 * @param codeVerifier the PKCE code verifier (plain text, 43-128 characters)
 */
public record PKCETokenRequestDTO(
        @NotBlank(message = "grant_type is required")
                @Pattern(
                        regexp = "authorization_code|refresh_token",
                        message = "grant_type must be 'authorization_code' or 'refresh_token'")
                String grantType,
        String code,
        String redirectUri,
        @NotBlank(message = "client_id is required") String clientId,
        @Size(min = 43, max = 128, message = "code_verifier must be 43-128 characters")
                String codeVerifier,
        String refreshToken) {

    /**
     * Checks if this is an authorization code grant request.
     *
     * @return true if grant_type is "authorization_code"
     */
    public boolean isAuthorizationCodeGrant() {
        return "authorization_code".equals(grantType);
    }

    /**
     * Checks if this is a refresh token grant request.
     *
     * @return true if grant_type is "refresh_token"
     */
    public boolean isRefreshTokenGrant() {
        return "refresh_token".equals(grantType);
    }

    /**
     * Creates a builder for the token request.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for PKCETokenRequestDTO. */
    public static class Builder {
        private String grantType = "authorization_code";
        private String code;
        private String redirectUri;
        private String clientId;
        private String codeVerifier;
        private String refreshToken;

        public Builder grantType(String grantType) {
            this.grantType = grantType;
            return this;
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder redirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
            return this;
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder codeVerifier(String codeVerifier) {
            this.codeVerifier = codeVerifier;
            return this;
        }

        public Builder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public PKCETokenRequestDTO build() {
            return new PKCETokenRequestDTO(
                    grantType, code, redirectUri, clientId, codeVerifier, refreshToken);
        }
    }
}
