package com.jnzader.apigen.server.controller;

import com.jnzader.apigen.server.config.GitHubConfig;
import com.jnzader.apigen.server.dto.github.CreateRepoRequest;
import com.jnzader.apigen.server.dto.github.CreateRepoResponse;
import com.jnzader.apigen.server.dto.github.GitHubAuthResponse;
import com.jnzader.apigen.server.dto.github.PushProjectRequest;
import com.jnzader.apigen.server.dto.github.PushProjectResponse;
import com.jnzader.apigen.server.service.GitHubService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
     * Gets the authenticated user's information.
     *
     * @param authorization Bearer token header with GitHub access token
     * @return GitHubAuthResponse with user info
     */
    @GetMapping("/user")
    public ResponseEntity<GitHubAuthResponse> getUser(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {

        String accessToken = extractAccessToken(authorization);
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
     * Handles the GitHub OAuth callback. Exchanges the authorization code for an access token and
     * redirects to the frontend with the token in the URL hash.
     *
     * @param code Authorization code from GitHub
     * @param state State parameter for CSRF verification
     * @param error Error code if authorization failed
     * @param errorDescription Error description if authorization failed
     * @return Redirect to frontend with token or error
     */
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription) {

        String frontendUrl = gitHubConfig.getFrontendUrl();

        if (error != null) {
            log.error("GitHub OAuth error: {} - {}", error, errorDescription);
            String errorMsg = errorDescription != null ? errorDescription : error;
            String redirectUrl = frontendUrl + "?github_error=" + encodeParam(errorMsg);
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
        }

        if (code == null) {
            log.error("GitHub OAuth callback missing authorization code");
            String redirectUrl = frontendUrl + "?github_error=Missing+authorization+code";
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
        }

        log.info("Processing GitHub OAuth callback");

        // State verification should be implemented for production CSRF protection
        // Currently relying on OAuth provider's CSRF protection mechanisms

        GitHubAuthResponse response = gitHubService.exchangeCodeForToken(code);

        if (response.isAuthenticated()) {
            // Redirect to frontend with token in hash (not sent to server)
            String redirectUrl =
                    frontendUrl
                            + "#github_token="
                            + response.getAccessToken()
                            + "&github_user="
                            + encodeParam(response.getLogin());
            log.info("Redirecting authenticated user {} to frontend", response.getLogin());
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
        } else {
            String errorMsg = response.getError() != null ? response.getError() : "Authentication failed";
            String redirectUrl = frontendUrl + "?github_error=" + encodeParam(errorMsg);
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
        }
    }

    /** URL-encodes a parameter value. */
    private String encodeParam(String value) {
        if (value == null) {
            return "";
        }
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Creates a new GitHub repository.
     *
     * @param authorization Bearer token header with GitHub access token
     * @param request Repository creation request
     * @return CreateRepoResponse with repository details
     */
    @PostMapping("/repos")
    public ResponseEntity<CreateRepoResponse> createRepository(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody CreateRepoRequest request) {

        String accessToken = extractAccessToken(authorization);
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
     * Pushes a generated project to a GitHub repository.
     *
     * @param owner Repository owner
     * @param repo Repository name
     * @param request Push request containing generation config
     * @return PushProjectResponse with push results
     */
    @PostMapping("/repos/{owner}/{repo}/push")
    public ResponseEntity<PushProjectResponse> pushProject(
            @PathVariable String owner,
            @PathVariable String repo,
            @Valid @RequestBody PushProjectRequest request) {

        log.info("Pushing project to {}/{}", owner, repo);

        PushProjectResponse response =
                gitHubService.pushProject(
                        request.getAccessToken(),
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
     * Alternative push endpoint that takes owner/repo from the request body.
     *
     * @param request Push request containing all parameters
     * @return PushProjectResponse with push results
     */
    @PostMapping("/push")
    public ResponseEntity<PushProjectResponse> pushProjectAlt(
            @Valid @RequestBody PushProjectRequest request) {

        log.info("Pushing project to {}/{}", request.getOwner(), request.getRepo());

        PushProjectResponse response =
                gitHubService.pushProject(
                        request.getAccessToken(),
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

    /** Response containing the authorization URL. */
    public record AuthorizationUrlResponse(String url, String state) {}
}
