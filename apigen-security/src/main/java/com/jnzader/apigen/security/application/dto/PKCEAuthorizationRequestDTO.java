package com.jnzader.apigen.security.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO for PKCE authorization requests.
 *
 * <p>This represents the initial authorization request in the PKCE flow. The client sends this to
 * initiate the OAuth2 authorization code flow with PKCE protection.
 *
 * <p>Example request:
 *
 * <pre>{@code
 * {
 *   "client_id": "my-spa-app",
 *   "redirect_uri": "https://myapp.com/callback",
 *   "response_type": "code",
 *   "scope": "openid profile email",
 *   "state": "abc123",
 *   "code_challenge": "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
 *   "code_challenge_method": "S256"
 * }
 * }</pre>
 *
 * @param clientId the OAuth2 client identifier
 * @param redirectUri the URI to redirect after authorization
 * @param responseType must be "code" for authorization code flow
 * @param scope space-separated list of requested scopes
 * @param state optional state parameter to prevent CSRF
 * @param codeChallenge the PKCE code challenge (BASE64URL encoded)
 * @param codeChallengeMethod the method used to create the challenge (S256 or plain)
 */
public record PKCEAuthorizationRequestDTO(
        @NotBlank(message = "client_id is required") String clientId,
        @NotBlank(message = "redirect_uri is required") String redirectUri,
        @NotBlank(message = "response_type is required")
                @Pattern(regexp = "code", message = "response_type must be 'code'")
                String responseType,
        String scope,
        String state,
        @NotBlank(message = "code_challenge is required")
                @Size(min = 43, max = 128, message = "code_challenge must be 43-128 characters")
                String codeChallenge,
        @NotBlank(message = "code_challenge_method is required")
                @Pattern(
                        regexp = "S256|plain",
                        message = "code_challenge_method must be 'S256' or 'plain'")
                String codeChallengeMethod) {

    /**
     * Creates a builder for the authorization request.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for PKCEAuthorizationRequestDTO. */
    public static class Builder {
        private String clientId;
        private String redirectUri;
        private String responseType = "code";
        private String scope;
        private String state;
        private String codeChallenge;
        private String codeChallengeMethod = "S256";

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder redirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
            return this;
        }

        public Builder responseType(String responseType) {
            this.responseType = responseType;
            return this;
        }

        public Builder scope(String scope) {
            this.scope = scope;
            return this;
        }

        public Builder state(String state) {
            this.state = state;
            return this;
        }

        public Builder codeChallenge(String codeChallenge) {
            this.codeChallenge = codeChallenge;
            return this;
        }

        public Builder codeChallengeMethod(String codeChallengeMethod) {
            this.codeChallengeMethod = codeChallengeMethod;
            return this;
        }

        public PKCEAuthorizationRequestDTO build() {
            return new PKCEAuthorizationRequestDTO(
                    clientId,
                    redirectUri,
                    responseType,
                    scope,
                    state,
                    codeChallenge,
                    codeChallengeMethod);
        }
    }
}
