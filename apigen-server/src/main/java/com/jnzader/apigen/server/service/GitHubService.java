package com.jnzader.apigen.server.service;

import com.jnzader.apigen.server.config.GitHubConfig;
import com.jnzader.apigen.server.dto.GenerateRequest;
import com.jnzader.apigen.server.dto.github.CreateRepoRequest;
import com.jnzader.apigen.server.dto.github.CreateRepoResponse;
import com.jnzader.apigen.server.dto.github.GitHubAuthResponse;
import com.jnzader.apigen.server.dto.github.GitHubRepoDto;
import com.jnzader.apigen.server.dto.github.PushProjectResponse;
import com.jnzader.apigen.server.exception.GitHubException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Service for GitHub OAuth authentication and API operations.
 *
 * <p>Handles:
 *
 * <ul>
 *   <li>OAuth authorization flow
 *   <li>Access token exchange
 *   <li>Repository creation
 *   <li>Pushing generated projects to repositories
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({
    "java:S1192", // API path strings intentional for readability
    "java:S135" // Multiple continues in pushProjectToRepo for directory/empty path skipping
})
public class GitHubService {

    private final GitHubConfig gitHubConfig;
    private final WebClient gitHubWebClient;
    private final GeneratorService generatorService;

    /**
     * Builds the GitHub OAuth authorization URL.
     *
     * @param state Random state parameter for CSRF protection
     * @return Authorization URL to redirect the user to
     */
    public String buildAuthorizationUrl(String state) {
        return UriComponentsBuilder.fromUriString(gitHubConfig.getAuthorizeUrl())
                .queryParam("client_id", gitHubConfig.getClientId())
                .queryParam("redirect_uri", gitHubConfig.getRedirectUri())
                .queryParam("scope", gitHubConfig.getScopes())
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    /**
     * Exchanges the OAuth authorization code for an access token.
     *
     * @param code Authorization code from GitHub callback
     * @return GitHubAuthResponse with access token and user info
     */
    @SuppressWarnings("unchecked")
    public GitHubAuthResponse exchangeCodeForToken(String code) {
        log.info("Exchanging authorization code for access token");

        try {
            // Exchange code for access token
            Map<String, Object> tokenJson =
                    WebClient.create()
                            .post()
                            .uri(gitHubConfig.getTokenUrl())
                            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .body(
                                    BodyInserters.fromFormData(
                                                    "client_id", gitHubConfig.getClientId())
                                            .with("client_secret", gitHubConfig.getClientSecret())
                                            .with("code", code)
                                            .with("redirect_uri", gitHubConfig.getRedirectUri()))
                            .retrieve()
                            .bodyToMono(Map.class)
                            .block();

            if (tokenJson == null) {
                log.error("GitHub OAuth: empty response when exchanging code for token");
                return GitHubAuthResponse.builder()
                        .authenticated(false)
                        .error("Failed to authenticate: empty response from GitHub")
                        .build();
            }

            if (tokenJson.containsKey("error")) {
                String error = (String) tokenJson.get("error");
                String errorDesc =
                        tokenJson.containsKey("error_description")
                                ? (String) tokenJson.get("error_description")
                                : "Unknown error";
                log.error("GitHub OAuth error: {} - {}", error, errorDesc);
                return GitHubAuthResponse.builder().authenticated(false).error(errorDesc).build();
            }

            String accessToken = (String) tokenJson.get("access_token");
            String tokenType =
                    tokenJson.containsKey("token_type")
                            ? (String) tokenJson.get("token_type")
                            : "bearer";
            String scope = tokenJson.containsKey("scope") ? (String) tokenJson.get("scope") : "";

            // Fetch user info
            GitHubAuthResponse userInfo = fetchUserInfo(accessToken);
            userInfo.setAccessToken(accessToken);
            userInfo.setTokenType(tokenType);
            userInfo.setScope(scope);
            userInfo.setAuthenticated(true);

            log.info("Successfully authenticated user: {}", userInfo.getLogin());
            return userInfo;

        } catch (Exception e) {
            log.error("Failed to exchange code for token", e);
            return GitHubAuthResponse.builder()
                    .authenticated(false)
                    .error("Failed to authenticate: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Fetches the authenticated user's information.
     *
     * @param accessToken OAuth access token
     * @return GitHubAuthResponse with user info
     */
    @SuppressWarnings("unchecked")
    public GitHubAuthResponse fetchUserInfo(String accessToken) {
        Map<String, Object> userJson =
                gitHubWebClient
                        .get()
                        .uri("/user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

        if (userJson == null) {
            throw new GitHubException("Failed to fetch user info: empty response from GitHub API");
        }

        return GitHubAuthResponse.builder()
                .login((String) userJson.get("login"))
                .name((String) userJson.get("name"))
                .email((String) userJson.get("email"))
                .avatarUrl((String) userJson.get("avatar_url"))
                .build();
    }

    /**
     * Fetches the authenticated user's repositories.
     *
     * @param accessToken OAuth access token
     * @return List of repositories
     */
    @SuppressWarnings("unchecked")
    public List<GitHubRepoDto> fetchUserRepos(String accessToken) {
        log.info("Fetching user repositories");

        List<Map<String, Object>> reposJson =
                gitHubWebClient
                        .get()
                        .uri(
                                uriBuilder ->
                                        uriBuilder
                                                .path("/user/repos")
                                                .queryParam("sort", "updated")
                                                .queryParam("per_page", "100")
                                                .queryParam("affiliation", "owner")
                                                .build())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .retrieve()
                        .bodyToMono(List.class)
                        .block();

        if (reposJson == null) {
            throw new GitHubException(
                    "Failed to fetch repositories: empty response from GitHub API");
        }

        return reposJson.stream()
                .map(
                        repo ->
                                GitHubRepoDto.builder()
                                        .name((String) repo.get("name"))
                                        .fullName((String) repo.get("full_name"))
                                        .isPrivate((Boolean) repo.get("private"))
                                        .htmlUrl((String) repo.get("html_url"))
                                        .description((String) repo.get("description"))
                                        .defaultBranch((String) repo.get("default_branch"))
                                        .build())
                .toList();
    }

    /**
     * Creates a new GitHub repository.
     *
     * @param accessToken OAuth access token
     * @param request Repository creation request
     * @return CreateRepoResponse with repository details
     */
    @SuppressWarnings("unchecked")
    public CreateRepoResponse createRepository(String accessToken, CreateRepoRequest request) {
        log.info("Creating GitHub repository: {}", request.getName());

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("name", request.getName());
            body.put("private", request.isPrivate());
            body.put("auto_init", request.isAutoInit());

            if (request.getDescription() != null) {
                body.put("description", request.getDescription());
            }
            if (request.getGitignoreTemplate() != null) {
                body.put("gitignore_template", request.getGitignoreTemplate());
            }
            if (request.getLicense() != null) {
                body.put("license_template", request.getLicense());
            }
            if (request.getHomepage() != null) {
                body.put("homepage", request.getHomepage());
            }

            Map<String, Object> response =
                    gitHubWebClient
                            .post()
                            .uri("/user/repos")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(body)
                            .retrieve()
                            .bodyToMono(Map.class)
                            .block();

            if (response == null) {
                throw new GitHubException(
                        "Failed to create repository: empty response from GitHub API");
            }

            CreateRepoResponse repoResponse =
                    CreateRepoResponse.builder()
                            .success(true)
                            .id(((Number) response.get("id")).longValue())
                            .name((String) response.get("name"))
                            .fullName((String) response.get("full_name"))
                            .description((String) response.get("description"))
                            .isPrivate((Boolean) response.get("private"))
                            .htmlUrl((String) response.get("html_url"))
                            .cloneUrl((String) response.get("clone_url"))
                            .sshUrl((String) response.get("ssh_url"))
                            .defaultBranch(
                                    response.containsKey("default_branch")
                                            ? (String) response.get("default_branch")
                                            : "main")
                            .build();

            log.info("Successfully created repository: {}", repoResponse.getFullName());
            return repoResponse;

        } catch (Exception e) {
            log.error("Failed to create repository: {}", request.getName(), e);
            return CreateRepoResponse.builder()
                    .success(false)
                    .error("Failed to create repository: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Pushes a generated project to a GitHub repository.
     *
     * @param accessToken OAuth access token
     * @param owner Repository owner
     * @param repo Repository name
     * @param branch Target branch
     * @param commitMessage Commit message
     * @param generateRequest The generation request
     * @return PushProjectResponse with push results
     */
    public PushProjectResponse pushProject(
            String accessToken,
            String owner,
            String repo,
            String branch,
            String commitMessage,
            GenerateRequest generateRequest) {
        // Sanitize path segments to prevent URI validation issues with Netty 4.2.x
        String cleanOwner = sanitizePathSegment(owner);
        String cleanRepo = sanitizePathSegment(repo);
        String cleanBranch = sanitizePathSegment(branch);

        log.info("Pushing generated project to {}/{}", cleanOwner, cleanRepo);

        try {
            // Generate the project
            byte[] zipBytes = generatorService.generateProject(generateRequest);

            // Extract files from ZIP
            Map<String, byte[]> files = extractZipContents(zipBytes);
            log.info("Extracted {} files from generated project", files.size());

            // Get the current commit SHA for the branch (if exists)
            String baseSha = getLatestCommitSha(accessToken, cleanOwner, cleanRepo, cleanBranch);

            // Get the base tree SHA
            String baseTreeSha = null;
            if (baseSha != null) {
                baseTreeSha = getTreeSha(accessToken, cleanOwner, cleanRepo, baseSha);
            }

            // Create blobs for each file
            List<Map<String, String>> treeItems = new ArrayList<>();
            List<String> fileNames = new ArrayList<>();

            for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                String path = entry.getKey();
                byte[] content = entry.getValue();

                // Skip directory entries and empty paths after removing root folder
                if (path.endsWith("/")) {
                    continue;
                }
                String cleanPath = removeRootFolder(path);
                if (cleanPath.isEmpty()) {
                    continue;
                }

                String blobSha = createBlob(accessToken, cleanOwner, cleanRepo, content);

                Map<String, String> treeItem = new HashMap<>();
                treeItem.put("path", cleanPath);
                treeItem.put("mode", "100644"); // Regular file
                treeItem.put("type", "blob");
                treeItem.put("sha", blobSha);
                treeItems.add(treeItem);
                fileNames.add(cleanPath);
            }

            // Create a tree
            String treeSha = createTree(accessToken, cleanOwner, cleanRepo, treeItems, baseTreeSha);

            // Create a commit
            String commitSha =
                    createCommit(
                            accessToken, cleanOwner, cleanRepo, commitMessage, treeSha, baseSha);

            // Update the reference
            updateRef(accessToken, cleanOwner, cleanRepo, cleanBranch, commitSha, baseSha == null);

            log.info(
                    "Successfully pushed {} files to {}/{}",
                    fileNames.size(),
                    cleanOwner,
                    cleanRepo);

            return PushProjectResponse.builder()
                    .success(true)
                    .commitSha(commitSha)
                    .repositoryUrl(String.format("https://github.com/%s/%s", cleanOwner, cleanRepo))
                    .branch(cleanBranch)
                    .filesCount(fileNames.size())
                    .files(fileNames)
                    .build();

        } catch (Exception e) {
            log.error("Failed to push project to {}/{}", cleanOwner, cleanRepo, e);
            return PushProjectResponse.builder()
                    .success(false)
                    .error("Failed to push project: " + e.getMessage())
                    .build();
        }
    }

    /** Extracts files from a ZIP archive. */
    private Map<String, byte[]> extractZipContents(byte[] zipBytes) throws IOException {
        Map<String, byte[]> files = new HashMap<>();

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
                files.put(entry.getName(), baos.toByteArray());
                zis.closeEntry();
            }
        }

        return files;
    }

    /** Removes the root folder from a path. */
    private String removeRootFolder(String path) {
        int slashIndex = path.indexOf('/');
        if (slashIndex > 0 && slashIndex < path.length() - 1) {
            return path.substring(slashIndex + 1);
        }
        return path;
    }

    /** Gets the latest commit SHA for a branch. */
    @SuppressWarnings("unchecked")
    private String getLatestCommitSha(
            String accessToken, String owner, String repo, String branch) {
        try {
            Map<String, Object> response =
                    gitHubWebClient
                            .get()
                            .uri(
                                    "/repos/{owner}/{repo}/git/ref/heads/{branch}",
                                    owner,
                                    repo,
                                    branch)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .retrieve()
                            .bodyToMono(Map.class)
                            .block();

            if (response == null || !response.containsKey("object")) {
                log.debug("Branch {} does not exist or empty response", branch);
                return null;
            }
            Map<String, Object> objectNode = (Map<String, Object>) response.get("object");
            if (objectNode == null || !objectNode.containsKey("sha")) {
                log.debug("No SHA found for branch {}", branch);
                return null;
            }
            return (String) objectNode.get("sha");
        } catch (Exception _) {
            log.debug("Branch {} does not exist yet", branch);
            return null;
        }
    }

    /** Gets the tree SHA for a commit. */
    @SuppressWarnings("unchecked")
    private String getTreeSha(String accessToken, String owner, String repo, String commitSha) {
        Map<String, Object> response =
                gitHubWebClient
                        .get()
                        .uri("/repos/{owner}/{repo}/git/commits/{sha}", owner, repo, commitSha)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

        if (response == null || !response.containsKey("tree")) {
            throw new GitHubException("Failed to get commit info from GitHub: missing tree data");
        }
        Map<String, Object> treeNode = (Map<String, Object>) response.get("tree");
        if (treeNode == null || !treeNode.containsKey("sha")) {
            throw new GitHubException("Failed to get tree SHA from GitHub commit");
        }
        return (String) treeNode.get("sha");
    }

    /** Creates a blob for file content. */
    @SuppressWarnings("unchecked")
    private String createBlob(String accessToken, String owner, String repo, byte[] content) {
        Map<String, String> body = new HashMap<>();
        body.put("content", Base64.getEncoder().encodeToString(content));
        body.put("encoding", "base64");

        Map<String, Object> response =
                gitHubWebClient
                        .post()
                        .uri("/repos/{owner}/{repo}/git/blobs", owner, repo)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

        if (response == null || !response.containsKey("sha")) {
            throw new GitHubException("Failed to create blob on GitHub: missing SHA in response");
        }
        return (String) response.get("sha");
    }

    /** Creates a tree from blobs. */
    @SuppressWarnings("unchecked")
    private String createTree(
            String accessToken,
            String owner,
            String repo,
            List<Map<String, String>> treeItems,
            String baseTreeSha) {
        Map<String, Object> body = new HashMap<>();
        body.put("tree", treeItems);
        if (baseTreeSha != null) {
            body.put("base_tree", baseTreeSha);
        }

        Map<String, Object> response =
                gitHubWebClient
                        .post()
                        .uri("/repos/{owner}/{repo}/git/trees", owner, repo)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

        if (response == null || !response.containsKey("sha")) {
            throw new GitHubException("Failed to create tree on GitHub: missing SHA in response");
        }
        return (String) response.get("sha");
    }

    /** Creates a commit. */
    @SuppressWarnings("unchecked")
    private String createCommit(
            String accessToken,
            String owner,
            String repo,
            String message,
            String treeSha,
            String parentSha) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        body.put("tree", treeSha);
        if (parentSha != null) {
            body.put("parents", List.of(parentSha));
        } else {
            body.put("parents", List.of());
        }

        Map<String, Object> response =
                gitHubWebClient
                        .post()
                        .uri("/repos/{owner}/{repo}/git/commits", owner, repo)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

        if (response == null || !response.containsKey("sha")) {
            throw new GitHubException("Failed to create commit on GitHub: missing SHA in response");
        }
        return (String) response.get("sha");
    }

    /**
     * Sanitizes a path segment by removing any characters that could cause URI validation issues.
     * This ensures compatibility with Netty 4.2.x's strict URI validation.
     */
    private String sanitizePathSegment(String segment) {
        if (segment == null) {
            return "";
        }
        // Convert to ASCII, trim whitespace, and remove any non-printable characters
        return new String(segment.getBytes(StandardCharsets.US_ASCII), StandardCharsets.US_ASCII)
                .trim()
                .replaceAll("[\\p{Cntrl}\\s]", "");
    }

    /** Updates or creates a branch reference. */
    @SuppressWarnings("unchecked")
    private void updateRef(
            String accessToken,
            String owner,
            String repo,
            String branch,
            String sha,
            boolean create) {
        Map<String, Object> body = new HashMap<>();
        body.put("sha", sha);
        body.put("force", true);

        if (create) {
            body.put("ref", "refs/heads/" + branch);
            gitHubWebClient
                    .post()
                    .uri("/repos/{owner}/{repo}/git/refs", owner, repo)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } else {
            gitHubWebClient
                    .patch()
                    .uri("/repos/{owner}/{repo}/git/refs/heads/{branch}", owner, repo, branch)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        }
    }
}
