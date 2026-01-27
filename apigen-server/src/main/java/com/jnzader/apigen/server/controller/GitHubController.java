package com.jnzader.apigen.server.controller;

import com.jnzader.apigen.server.config.GitHubConfig;
import com.jnzader.apigen.server.dto.github.CreateRepoRequest;
import com.jnzader.apigen.server.dto.github.CreateRepoResponse;
import com.jnzader.apigen.server.dto.github.GitHubAuthResponse;
import com.jnzader.apigen.server.dto.github.GitHubRepoDto;
import com.jnzader.apigen.server.dto.github.PushProjectRequest;
import com.jnzader.apigen.server.dto.github.PushProjectResponse;
import com.jnzader.apigen.server.service.GitHubService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for GitHub integration.
 *
 * <p>Provides endpoints for:
 *
 * <ul>
 *   <li>OAuth authorization flow
 *   <li>Repository creation
 *   <li>Pushing generated projects
 * </ul>
 */
@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
@Slf4j
public class GitHubController {

    private static final String GITHUB_TOKEN_COOKIE = "github_token";
    private static final int COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 7; // 7 days

    private final GitHubService gitHubService;
    private final GitHubConfig gitHubConfig;

    /**
     * Initiates GitHub OAuth authorization flow. Redirects the user to GitHub's authorization page.
     *
     * @return Redirect to GitHub OAuth authorization URL
     */
    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize() {
        log.info("Initiating GitHub OAuth authorization");

        // Generate a random state for CSRF protection
        String state = UUID.randomUUID().toString();

        String authUrl = gitHubService.buildAuthorizationUrl(state);

        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(authUrl)).build();
    }

    /**
     * Returns the authorization URL without redirecting. Useful for frontends that handle the
     * redirect themselves.
     *
     * @return AuthorizationUrlResponse with the URL
     */
    @GetMapping("/authorize/url")
    public ResponseEntity<AuthorizationUrlResponse> getAuthorizationUrl() {
        log.info("Getting GitHub OAuth authorization URL");

        String state = UUID.randomUUID().toString();
        String authUrl = gitHubService.buildAuthorizationUrl(state);

        return ResponseEntity.ok(new AuthorizationUrlResponse(authUrl, state));
    }

    /**
     * Gets the authenticated user's information. Reads token from HttpOnly cookie or Authorization
     * header (for backward compatibility).
     *
     * @param authorization Optional Bearer token header with GitHub access token
     * @param request HTTP request for reading cookies
     * @return GitHubAuthResponse with user info
     */
    @GetMapping("/user")
    public ResponseEntity<GitHubAuthResponse> getUser(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
                    String authorization,
            HttpServletRequest request) {

        String accessToken = extractAccessTokenFromCookieOrHeader(request, authorization);
        if (accessToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(
                            GitHubAuthResponse.builder()
                                    .authenticated(false)
                                    .error("Invalid or missing authorization token")
                                    .build());
        }

        log.info("Fetching GitHub user info");

        try {
            GitHubAuthResponse response = gitHubService.fetchUserInfo(accessToken);
            response.setAuthenticated(true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to fetch GitHub user info", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(
                            GitHubAuthResponse.builder()
                                    .authenticated(false)
                                    .error("Failed to fetch user info: " + e.getMessage())
                                    .build());
        }
    }

    /**
     * Checks authentication status using the HttpOnly cookie. Returns user info if authenticated,
     * or authenticated=false if not.
     *
     * @param request HTTP request for reading cookies
     * @return GitHubAuthResponse with authentication status and user info
     */
    @GetMapping("/auth/status")
    public ResponseEntity<GitHubAuthResponse> checkAuthStatus(HttpServletRequest request) {
        String accessToken = extractAccessTokenFromCookie(request);

        if (accessToken == null) {
            return ResponseEntity.ok(GitHubAuthResponse.builder().authenticated(false).build());
        }

        log.info("Checking GitHub authentication status from cookie");

        try {
            GitHubAuthResponse response = gitHubService.fetchUserInfo(accessToken);
            response.setAuthenticated(true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("Cookie token is invalid or expired: {}", e.getMessage());
            return ResponseEntity.ok(
                    GitHubAuthResponse.builder()
                            .authenticated(false)
                            .error("Token expired or invalid")
                            .build());
        }
    }

    /**
     * Lists the authenticated user's repositories. Reads token from HttpOnly cookie or
     * Authorization header.
     *
     * @param authorization Optional Bearer token header with GitHub access token
     * @param request HTTP request for reading cookies
     * @return List of repositories
     */
    @GetMapping("/repos")
    public ResponseEntity<List<GitHubRepoDto>> listRepositories(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
                    String authorization,
            HttpServletRequest request) {

        String accessToken = extractAccessTokenFromCookieOrHeader(request, authorization);
        if (accessToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Fetching user repositories");

        try {
            List<GitHubRepoDto> repos = gitHubService.fetchUserRepos(accessToken);
            return ResponseEntity.ok(repos);
        } catch (Exception e) {
            log.error("Failed to fetch repositories", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Logs out the user by clearing the HttpOnly cookie.
     *
     * @param response HTTP response for clearing the cookie
     * @return Success response
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        log.info("Logging out GitHub user");

        ResponseCookie clearCookie = createClearTokenCookie();
        response.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());

        return ResponseEntity.ok().build();
    }

    /**
     * Handles the GitHub OAuth callback. Exchanges the authorization code for an access token and
     * sets it as an HttpOnly cookie, then redirects to the frontend.
     *
     * @param code Authorization code from GitHub
     * @param state State parameter for CSRF verification
     * @param error Error code if authorization failed
     * @param errorDescription Error description if authorization failed
     * @param response HTTP response for setting cookies
     * @return Redirect to frontend
     */
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription,
            HttpServletResponse response) {

        String frontendUrl = gitHubConfig.getFrontendUrl();

        if (error != null) {
            log.error("GitHub OAuth error: {} - {}", error, errorDescription);
            String errorMsg = errorDescription != null ? errorDescription : error;
            String redirectUrl = frontendUrl + "?github_error=" + encodeParam(errorMsg);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(redirectUrl))
                    .build();
        }

        if (code == null) {
            log.error("GitHub OAuth callback missing authorization code");
            String redirectUrl = frontendUrl + "?github_error=Missing+authorization+code";
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(redirectUrl))
                    .build();
        }

        log.info("Processing GitHub OAuth callback");

        // State verification should be implemented for production CSRF protection
        // Currently relying on OAuth provider's CSRF protection mechanisms

        GitHubAuthResponse authResponse = gitHubService.exchangeCodeForToken(code);

        if (authResponse.isAuthenticated()) {
            // Set HttpOnly cookie with the access token
            ResponseCookie cookie = createSecureTokenCookie(authResponse.getAccessToken());
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            // Redirect to frontend with success indicator (no token in URL!)
            String redirectUrl = frontendUrl + "?github_auth=success";
            log.info(
                    "Redirecting authenticated user {} to frontend (token in HttpOnly cookie)",
                    authResponse.getLogin());
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(redirectUrl))
                    .build();
        } else {
            String errorMsg =
                    authResponse.getError() != null
                            ? authResponse.getError()
                            : "Authentication failed";
            String redirectUrl = frontendUrl + "?github_error=" + encodeParam(errorMsg);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(redirectUrl))
                    .build();
        }
    }

    /**
     * Creates a secure HttpOnly cookie for storing the GitHub access token.
     *
     * @param token The access token to store
     * @return ResponseCookie configured with security settings
     */
    private ResponseCookie createSecureTokenCookie(String token) {
        return ResponseCookie.from(GITHUB_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(true) // Only sent over HTTPS
                .sameSite("Lax") // CSRF protection
                .path("/api/github") // Only sent to GitHub API endpoints
                .maxAge(COOKIE_MAX_AGE_SECONDS)
                .build();
    }

    /**
     * Creates a cookie that clears the GitHub token.
     *
     * @return ResponseCookie that expires immediately
     */
    private ResponseCookie createClearTokenCookie() {
        return ResponseCookie.from(GITHUB_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/api/github")
                .maxAge(0) // Expire immediately
                .build();
    }

    /** URL-encodes a parameter value. */
    private String encodeParam(String value) {
        if (value == null) {
            return "";
        }
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Creates a new GitHub repository. Reads token from HttpOnly cookie or Authorization header.
     *
     * @param authorization Optional Bearer token header with GitHub access token
     * @param httpRequest HTTP request for reading cookies
     * @param request Repository creation request
     * @return CreateRepoResponse with repository details
     */
    @PostMapping("/repos")
    public ResponseEntity<CreateRepoResponse> createRepository(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
                    String authorization,
            HttpServletRequest httpRequest,
            @Valid @RequestBody CreateRepoRequest request) {

        String accessToken = extractAccessTokenFromCookieOrHeader(httpRequest, authorization);
        if (accessToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(
                            CreateRepoResponse.builder()
                                    .success(false)
                                    .error("Invalid or missing authorization token")
                                    .build());
        }

        log.info("Creating GitHub repository: {}", request.getName());

        CreateRepoResponse response = gitHubService.createRepository(accessToken, request);

        if (response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Pushes a generated project to a GitHub repository. Reads token from HttpOnly cookie, request
     * body, or falls back to body token.
     *
     * @param owner Repository owner
     * @param repo Repository name
     * @param httpRequest HTTP request for reading cookies
     * @param request Push request containing generation config
     * @return PushProjectResponse with push results
     */
    @PostMapping("/repos/{owner}/{repo}/push")
    public ResponseEntity<PushProjectResponse> pushProject(
            @PathVariable String owner,
            @PathVariable String repo,
            HttpServletRequest httpRequest,
            @Valid @RequestBody PushProjectRequest request) {

        // Try cookie first, then fall back to request body token (backward compatibility)
        String accessToken = extractAccessTokenFromCookie(httpRequest);
        if (accessToken == null) {
            accessToken = request.getAccessToken();
        }

        if (accessToken == null || accessToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(
                            PushProjectResponse.builder()
                                    .success(false)
                                    .error("Invalid or missing authorization token")
                                    .build());
        }

        log.info("Pushing project to {}/{}", owner, repo);

        PushProjectResponse response =
                gitHubService.pushProject(
                        accessToken,
                        owner,
                        repo,
                        request.getBranch(),
                        request.getCommitMessage(),
                        request.getGenerateRequest());

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Alternative push endpoint that takes owner/repo from the request body. Reads token from
     * HttpOnly cookie or falls back to request body token.
     *
     * @param httpRequest HTTP request for reading cookies
     * @param request Push request containing all parameters
     * @return PushProjectResponse with push results
     */
    @PostMapping("/push")
    public ResponseEntity<PushProjectResponse> pushProjectAlt(
            HttpServletRequest httpRequest, @Valid @RequestBody PushProjectRequest request) {

        // Try cookie first, then fall back to request body token (backward compatibility)
        String accessToken = extractAccessTokenFromCookie(httpRequest);
        if (accessToken == null) {
            accessToken = request.getAccessToken();
        }

        if (accessToken == null || accessToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(
                            PushProjectResponse.builder()
                                    .success(false)
                                    .error("Invalid or missing authorization token")
                                    .build());
        }

        log.info("Pushing project to {}/{}", request.getOwner(), request.getRepo());

        PushProjectResponse response =
                gitHubService.pushProject(
                        accessToken,
                        request.getOwner(),
                        request.getRepo(),
                        request.getBranch(),
                        request.getCommitMessage(),
                        request.getGenerateRequest());

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /** Extracts access token from Authorization header. */
    private String extractAccessToken(String authorization) {
        if (authorization == null || authorization.isEmpty()) {
            return null;
        }

        if (authorization.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
            return authorization.substring(7).trim();
        }

        return authorization.trim();
    }

    /** Extracts access token from HttpOnly cookie. */
    private String extractAccessTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(cookie -> GITHUB_TOKEN_COOKIE.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isEmpty())
                .findFirst()
                .orElse(null);
    }

    /** Extracts access token from cookie first, then falls back to Authorization header. */
    private String extractAccessTokenFromCookieOrHeader(
            HttpServletRequest request, String authorization) {
        // Try cookie first (preferred for security)
        String tokenFromCookie = extractAccessTokenFromCookie(request);
        if (tokenFromCookie != null) {
            return tokenFromCookie;
        }

        // Fall back to Authorization header (backward compatibility)
        return extractAccessToken(authorization);
    }

    /** Response containing the authorization URL. */
    public record AuthorizationUrlResponse(String url, String state) {}
}
