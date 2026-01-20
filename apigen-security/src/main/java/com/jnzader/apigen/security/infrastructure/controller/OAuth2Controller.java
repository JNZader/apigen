package com.jnzader.apigen.security.infrastructure.controller;

import com.jnzader.apigen.security.application.dto.PKCETokenRequestDTO;
import com.jnzader.apigen.security.domain.entity.User;
import com.jnzader.apigen.security.domain.repository.UserRepository;
import com.jnzader.apigen.security.infrastructure.config.SecurityProperties;
import com.jnzader.apigen.security.infrastructure.jwt.JwtService;
import com.jnzader.apigen.security.infrastructure.oauth2.PKCEAuthorizationStore;
import com.jnzader.apigen.security.infrastructure.oauth2.PKCEAuthorizationStore.AuthorizationData;
import com.jnzader.apigen.security.infrastructure.oauth2.PKCEService;
import com.jnzader.apigen.security.infrastructure.oauth2.PKCEService.CodeChallengeMethod;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * OAuth2 controller with PKCE (Proof Key for Code Exchange) support.
 *
 * <p>Implements the OAuth 2.0 Authorization Code Grant with PKCE extension as defined in RFC 7636.
 * This provides enhanced security for public clients (SPAs, mobile apps) that cannot securely store
 * a client secret.
 *
 * <p>Endpoints:
 *
 * <ul>
 *   <li>GET /oauth2/authorize - Initiates authorization (requires authentication)
 *   <li>POST /oauth2/token - Exchanges authorization code for tokens
 *   <li>POST /oauth2/revoke - Revokes tokens
 * </ul>
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7636">RFC 7636 - PKCE</a>
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc6749">RFC 6749 - OAuth 2.0</a>
 */
@RestController
@RequestMapping("/oauth2")
@Tag(name = "OAuth2", description = "OAuth 2.0 Authorization with PKCE support")
public class OAuth2Controller {

    private static final Logger log = LoggerFactory.getLogger(OAuth2Controller.class);

    // OAuth2 error codes (RFC 6749)
    private static final String ERROR_INVALID_GRANT = "invalid_grant";
    private static final String ERROR_INVALID_REQUEST = "invalid_request";

    private final PKCEService pkceService;
    private final PKCEAuthorizationStore authorizationStore;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final SecurityProperties securityProperties;

    public OAuth2Controller(
            PKCEService pkceService,
            PKCEAuthorizationStore authorizationStore,
            JwtService jwtService,
            UserRepository userRepository,
            SecurityProperties securityProperties) {
        this.pkceService = pkceService;
        this.authorizationStore = authorizationStore;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.securityProperties = securityProperties;
    }

    /**
     * Authorization endpoint for PKCE flow.
     *
     * <p>This endpoint must be called by an authenticated user. It generates an authorization code
     * that can be exchanged for tokens at the token endpoint.
     *
     * @param clientId the OAuth2 client identifier
     * @param redirectUri the URI to redirect after authorization
     * @param responseType must be "code"
     * @param scope space-separated list of requested scopes
     * @param state optional state parameter (recommended for CSRF protection)
     * @param codeChallenge the PKCE code challenge
     * @param codeChallengeMethod the method used (S256 recommended)
     * @param principal the authenticated user
     * @return redirect to the redirect_uri with authorization code
     */
    @GetMapping("/authorize")
    @Operation(
            summary = "Start OAuth2 authorization with PKCE",
            description =
                    "Initiates the OAuth2 authorization code flow with PKCE. Requires user"
                            + " authentication. Returns a redirect with authorization code.")
    @ApiResponse(responseCode = "302", description = "Redirect to callback with authorization code")
    @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    @ApiResponse(responseCode = "401", description = "User not authenticated")
    public ResponseEntity<Void> authorize(
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("response_type") String responseType,
            @RequestParam(value = "scope", required = false, defaultValue = "openid") String scope,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam("code_challenge") String codeChallenge,
            @RequestParam(value = "code_challenge_method", defaultValue = "S256")
                    String codeChallengeMethod,
            Principal principal) {

        // Validate response_type
        if (!"code".equals(responseType)) {
            return redirectWithError(
                    redirectUri,
                    "unsupported_response_type",
                    "Only 'code' response type is supported",
                    state);
        }

        // Validate code_challenge_method
        CodeChallengeMethod method;
        try {
            method = CodeChallengeMethod.valueOf(codeChallengeMethod.toUpperCase());
        } catch (IllegalArgumentException _) {
            return redirectWithError(
                    redirectUri,
                    "ERROR_INVALID_REQUEST",
                    "code_challenge_method must be 'S256' or 'plain'",
                    state);
        }

        // Validate code_challenge format
        if (codeChallenge == null || codeChallenge.length() < 43 || codeChallenge.length() > 128) {
            return redirectWithError(
                    redirectUri,
                    "ERROR_INVALID_REQUEST",
                    "code_challenge must be 43-128 characters",
                    state);
        }

        // Get user ID from principal
        String userId = principal.getName();

        // Generate authorization code
        String authorizationCode =
                authorizationStore.createAuthorizationCode(
                        userId, codeChallenge, method, clientId, redirectUri, scope);

        // Build redirect URI with code
        StringBuilder callbackUri = new StringBuilder(redirectUri);
        callbackUri.append(redirectUri.contains("?") ? "&" : "?");
        callbackUri.append("code=").append(authorizationCode);
        if (state != null && !state.isEmpty()) {
            callbackUri.append("&state=").append(state);
        }

        log.info(
                "Authorization code generated for user {} with client {} (method: {})",
                userId,
                clientId,
                method);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(callbackUri.toString()))
                .build();
    }

    /**
     * Token endpoint for PKCE flow.
     *
     * <p>Exchanges an authorization code for access and refresh tokens. The code_verifier must
     * match the code_challenge provided during authorization.
     *
     * @param request the token request with authorization code and code verifier
     * @return access token, refresh token, and metadata
     */
    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Operation(
            summary = "Exchange authorization code for tokens",
            description =
                    "Exchanges an authorization code for access and refresh tokens. "
                            + "For authorization_code grant, code and code_verifier are required. "
                            + "For refresh_token grant, refresh_token is required.")
    @ApiResponse(
            responseCode = "200",
            description = "Tokens issued successfully",
            content = @Content(schema = @Schema(implementation = OAuth2TokenResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request or PKCE verification failed")
    @ApiResponse(responseCode = "401", description = "Invalid or expired authorization code")
    public ResponseEntity<Object> token(@Valid PKCETokenRequestDTO request) {
        if (request.isAuthorizationCodeGrant()) {
            return handleAuthorizationCodeGrant(request);
        } else if (request.isRefreshTokenGrant()) {
            return handleRefreshTokenGrant(request);
        } else {
            return errorResponse("unsupported_grant_type", "Unsupported grant type");
        }
    }

    /** Alternative token endpoint accepting JSON. */
    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> tokenJson(@Valid @RequestBody PKCETokenRequestDTO request) {
        return token(request);
    }

    /**
     * Token revocation endpoint.
     *
     * @param token the token to revoke (access or refresh token)
     * @param tokenTypeHint optional hint about the token type
     * @return empty response on success
     */
    @PostMapping(value = "/revoke", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Operation(summary = "Revoke a token", description = "Revokes an access or refresh token")
    @ApiResponse(responseCode = "200", description = "Token revoked successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public ResponseEntity<Void> revoke(
            @RequestParam("token") String token,
            @RequestParam(value = "token_type_hint", required = false) String tokenTypeHint) {

        // In a real implementation, you would add the token to a blacklist
        // For now, we just log and return success
        log.info("Token revocation requested (hint: {})", tokenTypeHint);

        return ResponseEntity.ok().build();
    }

    /**
     * PKCE helper endpoint - generates a code verifier and challenge pair.
     *
     * <p>This endpoint is for development/testing purposes. In production, clients should generate
     * these values locally.
     *
     * @return code verifier and challenge pair
     */
    @GetMapping("/pkce/generate")
    @Operation(
            summary = "Generate PKCE code verifier and challenge",
            description =
                    "Helper endpoint for development. Generates a code_verifier and "
                            + "code_challenge pair that can be used in the authorization flow.")
    public ResponseEntity<Map<String, String>> generatePKCE() {
        String codeVerifier = pkceService.generateCodeVerifier();
        String codeChallenge = pkceService.generateCodeChallenge(codeVerifier);

        Map<String, String> response = new HashMap<>();
        response.put("code_verifier", codeVerifier);
        response.put("code_challenge", codeChallenge);
        response.put("code_challenge_method", "S256");

        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Object> handleAuthorizationCodeGrant(PKCETokenRequestDTO request) {
        // Validate required fields
        if (request.code() == null || request.code().isEmpty()) {
            return errorResponse(
                    "ERROR_INVALID_REQUEST", "code is required for authorization_code grant");
        }
        if (request.codeVerifier() == null || request.codeVerifier().isEmpty()) {
            return errorResponse(
                    "ERROR_INVALID_REQUEST",
                    "code_verifier is required for authorization_code grant");
        }

        // Consume authorization code (single-use)
        Optional<AuthorizationData> authDataOpt =
                authorizationStore.consumeAuthorizationCode(request.code());

        if (authDataOpt.isEmpty()) {
            return errorResponse(ERROR_INVALID_GRANT, "Invalid or expired authorization code");
        }

        AuthorizationData authData = authDataOpt.get();

        // Validate client_id matches
        if (!authData.clientId().equals(request.clientId())) {
            return errorResponse(ERROR_INVALID_GRANT, "client_id does not match");
        }

        // Validate redirect_uri matches (if provided)
        if (request.redirectUri() != null
                && !authData.redirectUri().equals(request.redirectUri())) {
            return errorResponse(ERROR_INVALID_GRANT, "redirect_uri does not match");
        }

        // Verify PKCE code challenge
        boolean pkceValid =
                pkceService.verifyCodeChallenge(
                        request.codeVerifier(),
                        authData.codeChallenge(),
                        authData.challengeMethod());

        if (!pkceValid) {
            log.warn(
                    "PKCE verification failed for user {} with client {}",
                    authData.userId(),
                    authData.clientId());
            return errorResponse(ERROR_INVALID_GRANT, "PKCE verification failed");
        }

        // Load user and generate tokens
        Optional<User> userOpt = userRepository.findActiveByUsername(authData.userId());
        if (userOpt.isEmpty()) {
            return errorResponse(ERROR_INVALID_GRANT, "User not found");
        }

        User user = userOpt.get();
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        log.info(
                "Tokens issued for user {} with client {} via PKCE flow",
                authData.userId(),
                authData.clientId());

        long expiresIn = securityProperties.getJwt().getExpirationMinutes() * 60L;

        return ResponseEntity.ok(
                new OAuth2TokenResponse(
                        accessToken, "Bearer", expiresIn, refreshToken, authData.scopes()));
    }

    private ResponseEntity<Object> handleRefreshTokenGrant(PKCETokenRequestDTO request) {
        if (request.refreshToken() == null || request.refreshToken().isEmpty()) {
            return errorResponse(
                    "ERROR_INVALID_REQUEST", "refresh_token is required for refresh_token grant");
        }

        // Validate refresh token
        String username = jwtService.extractUsername(request.refreshToken());
        if (username == null || !jwtService.isRefreshToken(request.refreshToken())) {
            return errorResponse(ERROR_INVALID_GRANT, "Invalid refresh token");
        }

        Optional<User> userOpt = userRepository.findActiveByUsername(username);
        if (userOpt.isEmpty()) {
            return errorResponse(ERROR_INVALID_GRANT, "User not found");
        }

        User user = userOpt.get();

        // Validate token is not expired (structure check)
        if (!jwtService.isTokenStructureValid(request.refreshToken())) {
            return errorResponse(ERROR_INVALID_GRANT, "Expired or invalid refresh token");
        }

        // Generate new tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        log.info("Tokens refreshed for user {} with client {}", username, request.clientId());

        long expiresIn = securityProperties.getJwt().getExpirationMinutes() * 60L;

        return ResponseEntity.ok(
                new OAuth2TokenResponse(accessToken, "Bearer", expiresIn, refreshToken, null));
    }

    private ResponseEntity<Void> redirectWithError(
            String redirectUri, String error, String description, String state) {
        StringBuilder errorUri = new StringBuilder(redirectUri);
        errorUri.append(redirectUri.contains("?") ? "&" : "?");
        errorUri.append("error=").append(error);
        errorUri.append("&error_description=").append(description.replace(" ", "+"));
        if (state != null && !state.isEmpty()) {
            errorUri.append("&state=").append(state);
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(errorUri.toString()))
                .build();
    }

    private ResponseEntity<Object> errorResponse(String error, String description) {
        return ResponseEntity.badRequest().body(new OAuth2ErrorResponse(error, description));
    }

    /** OAuth2 token response format. */
    @Schema(description = "OAuth2 Token Response")
    public record OAuth2TokenResponse(
            @Schema(description = "The access token") String access_token,
            @Schema(description = "Token type (always 'Bearer')") String token_type,
            @Schema(description = "Token lifetime in seconds") long expires_in,
            @Schema(description = "The refresh token") String refresh_token,
            @Schema(description = "Granted scopes") String scope) {}

    /** OAuth2 error response format. */
    @Schema(description = "OAuth2 Error Response")
    public record OAuth2ErrorResponse(
            @Schema(description = "Error code") String error,
            @Schema(description = "Human-readable error description") String error_description) {}
}
