package com.jnzader.apigen.server.controller;

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
     * Handles the GitHub OAuth callback. Exchanges the authorization code for an access token.
     *
     * @param code Authorization code from GitHub
     * @param state State parameter for CSRF verification
     * @param error Error code if authorization failed
     * @param errorDescription Error description if authorization failed
     * @return GitHubAuthResponse with access token and user info
     */
    @GetMapping("/callback")
    public ResponseEntity<GitHubAuthResponse> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription) {

        if (error != null) {
            log.error("GitHub OAuth error: {} - {}", error, errorDescription);
            return ResponseEntity.badRequest()
                    .body(
                            GitHubAuthResponse.builder()
                                    .authenticated(false)
                                    .error(errorDescription != null ? errorDescription : error)
                                    .build());
        }

        if (code == null) {
            log.error("GitHub OAuth callback missing authorization code");
            return ResponseEntity.badRequest()
                    .body(
                            GitHubAuthResponse.builder()
                                    .authenticated(false)
                                    .error("Missing authorization code")
                                    .build());
        }

        log.info("Processing GitHub OAuth callback");

        // TODO: Verify state parameter against stored session state

        GitHubAuthResponse response = gitHubService.exchangeCodeForToken(code);

        if (response.isAuthenticated()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
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
