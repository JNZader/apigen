package com.jnzader.apigen.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnzader.apigen.server.config.GitHubConfig;
import com.jnzader.apigen.server.dto.GenerateRequest;
import com.jnzader.apigen.server.dto.github.CreateRepoRequest;
import com.jnzader.apigen.server.dto.github.CreateRepoResponse;
import com.jnzader.apigen.server.dto.github.GitHubAuthResponse;
import com.jnzader.apigen.server.dto.github.PushProjectResponse;
import com.jnzader.apigen.server.exception.GitHubException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
public class GitHubService {

    private final GitHubConfig gitHubConfig;
    private final WebClient gitHubWebClient;
    private final GeneratorService generatorService;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
    public GitHubAuthResponse exchangeCodeForToken(String code) {
        log.info("Exchanging authorization code for access token");

        try {
            // Exchange code for access token
            String tokenResponse =
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
                            .bodyToMono(String.class)
                            .block();

            if (tokenResponse == null) {
                log.error("GitHub OAuth: empty response when exchanging code for token");
                return GitHubAuthResponse.builder()
                        .authenticated(false)
                        .error("Failed to authenticate: empty response from GitHub")
                        .build();
            }

            JsonNode tokenJson = objectMapper.readTree(tokenResponse);

            if (tokenJson.has("error")) {
                String error = tokenJson.get("error").asText();
                String errorDesc =
                        tokenJson.has("error_description")
                                ? tokenJson.get("error_description").asText()
                                : "Unknown error";
                log.error("GitHub OAuth error: {} - {}", error, errorDesc);
                return GitHubAuthResponse.builder().authenticated(false).error(errorDesc).build();
            }

            String accessToken = tokenJson.get("access_token").asText();
            String tokenType =
                    tokenJson.has("token_type") ? tokenJson.get("token_type").asText() : "bearer";
            String scope = tokenJson.has("scope") ? tokenJson.get("scope").asText() : "";

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
    private GitHubAuthResponse fetchUserInfo(String accessToken) {
        JsonNode userJson =
                gitHubWebClient
                        .get()
                        .uri("/user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block();

        if (userJson == null) {
            throw new GitHubException("Failed to fetch user info: empty response from GitHub API");
        }

        return GitHubAuthResponse.builder()
                .login(userJson.has("login") ? userJson.get("login").asText() : null)
                .name(userJson.has("name") ? userJson.get("name").asText() : null)
                .email(userJson.has("email") ? userJson.get("email").asText() : null)
                .avatarUrl(userJson.has("avatar_url") ? userJson.get("avatar_url").asText() : null)
                .build();
    }

    /**
     * Creates a new GitHub repository.
     *
     * @param accessToken OAuth access token
     * @param request Repository creation request
     * @return CreateRepoResponse with repository details
     */
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

            JsonNode response =
                    gitHubWebClient
                            .post()
                            .uri("/user/repos")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(body)
                            .retrieve()
                            .bodyToMono(JsonNode.class)
                            .block();

            if (response == null) {
                throw new GitHubException(
                        "Failed to create repository: empty response from GitHub API");
            }

            CreateRepoResponse repoResponse =
                    CreateRepoResponse.builder()
                            .success(true)
                            .id(response.get("id").asLong())
                            .name(response.get("name").asText())
                            .fullName(response.get("full_name").asText())
                            .description(
                                    response.has("description")
                                                    && !response.get("description").isNull()
                                            ? response.get("description").asText()
                                            : null)
                            .isPrivate(response.get("private").asBoolean())
                            .htmlUrl(response.get("html_url").asText())
                            .cloneUrl(response.get("clone_url").asText())
                            .sshUrl(response.get("ssh_url").asText())
                            .defaultBranch(
                                    response.has("default_branch")
                                            ? response.get("default_branch").asText()
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
        log.info("Pushing generated project to {}/{}", owner, repo);

        try {
            // Generate the project
            byte[] zipBytes = generatorService.generateProject(generateRequest);

            // Extract files from ZIP
            Map<String, byte[]> files = extractZipContents(zipBytes);
            log.info("Extracted {} files from generated project", files.size());

            // Get the current commit SHA for the branch (if exists)
            String baseSha = getLatestCommitSha(accessToken, owner, repo, branch);

            // Get the base tree SHA
            String baseTreeSha = null;
            if (baseSha != null) {
                baseTreeSha = getTreeSha(accessToken, owner, repo, baseSha);
            }

            // Create blobs for each file
            List<Map<String, String>> treeItems = new ArrayList<>();
            List<String> fileNames = new ArrayList<>();

            for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                String path = entry.getKey();
                byte[] content = entry.getValue();

                // Skip directory entries
                if (path.endsWith("/")) {
                    continue;
                }

                // Remove the root folder from path (e.g., "my-project/" prefix)
                String cleanPath = removeRootFolder(path);
                if (cleanPath.isEmpty()) {
                    continue;
                }

                String blobSha = createBlob(accessToken, owner, repo, content);

                Map<String, String> treeItem = new HashMap<>();
                treeItem.put("path", cleanPath);
                treeItem.put("mode", "100644"); // Regular file
                treeItem.put("type", "blob");
                treeItem.put("sha", blobSha);
                treeItems.add(treeItem);
                fileNames.add(cleanPath);
            }

            // Create a tree
            String treeSha = createTree(accessToken, owner, repo, treeItems, baseTreeSha);

            // Create a commit
            String commitSha =
                    createCommit(accessToken, owner, repo, commitMessage, treeSha, baseSha);

            // Update the reference
            updateRef(accessToken, owner, repo, branch, commitSha, baseSha == null);

            log.info("Successfully pushed {} files to {}/{}", fileNames.size(), owner, repo);

            return PushProjectResponse.builder()
                    .success(true)
                    .commitSha(commitSha)
                    .repositoryUrl(String.format("https://github.com/%s/%s", owner, repo))
                    .branch(branch)
                    .filesCount(fileNames.size())
                    .files(fileNames)
                    .build();

        } catch (Exception e) {
            log.error("Failed to push project to {}/{}", owner, repo, e);
            return PushProjectResponse.builder()
                    .success(false)
                    .error("Failed to push project: " + e.getMessage())
                    .build();
        }
    }

    /** Extracts files from a ZIP archive. */
    private Map<String, byte[]> extractZipContents(byte[] zipBytes) throws Exception {
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
    private String getLatestCommitSha(
            String accessToken, String owner, String repo, String branch) {
        try {
            JsonNode response =
                    gitHubWebClient
                            .get()
                            .uri(
                                    "/repos/{owner}/{repo}/git/ref/heads/{branch}",
                                    owner,
                                    repo,
                                    branch)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .retrieve()
                            .bodyToMono(JsonNode.class)
                            .block();

            if (response == null || !response.has("object")) {
                log.debug("Branch {} does not exist or empty response", branch);
                return null;
            }
            JsonNode objectNode = response.get("object");
            if (objectNode == null || !objectNode.has("sha")) {
                log.debug("No SHA found for branch {}", branch);
                return null;
            }
            return objectNode.get("sha").asText();
        } catch (Exception e) {
            log.debug("Branch {} does not exist yet", branch);
            return null;
        }
    }

    /** Gets the tree SHA for a commit. */
    private String getTreeSha(String accessToken, String owner, String repo, String commitSha) {
        JsonNode response =
                gitHubWebClient
                        .get()
                        .uri("/repos/{owner}/{repo}/git/commits/{sha}", owner, repo, commitSha)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block();

        if (response == null || !response.has("tree")) {
            throw new GitHubException("Failed to get commit info from GitHub: missing tree data");
        }
        JsonNode treeNode = response.get("tree");
        if (treeNode == null || !treeNode.has("sha")) {
            throw new GitHubException("Failed to get tree SHA from GitHub commit");
        }
        return treeNode.get("sha").asText();
    }

    /** Creates a blob for file content. */
    private String createBlob(String accessToken, String owner, String repo, byte[] content) {
        Map<String, String> body = new HashMap<>();
        body.put("content", Base64.getEncoder().encodeToString(content));
        body.put("encoding", "base64");

        JsonNode response =
                gitHubWebClient
                        .post()
                        .uri("/repos/{owner}/{repo}/git/blobs", owner, repo)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block();

        if (response == null || !response.has("sha")) {
            throw new GitHubException("Failed to create blob on GitHub: missing SHA in response");
        }
        return response.get("sha").asText();
    }

    /** Creates a tree from blobs. */
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

        JsonNode response =
                gitHubWebClient
                        .post()
                        .uri("/repos/{owner}/{repo}/git/trees", owner, repo)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block();

        if (response == null || !response.has("sha")) {
            throw new GitHubException("Failed to create tree on GitHub: missing SHA in response");
        }
        return response.get("sha").asText();
    }

    /** Creates a commit. */
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

        JsonNode response =
                gitHubWebClient
                        .post()
                        .uri("/repos/{owner}/{repo}/git/commits", owner, repo)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block();

        if (response == null || !response.has("sha")) {
            throw new GitHubException("Failed to create commit on GitHub: missing SHA in response");
        }
        return response.get("sha").asText();
    }

    /** Updates or creates a branch reference. */
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
                    .bodyToMono(JsonNode.class)
                    .block();
        } else {
            gitHubWebClient
                    .patch()
                    .uri("/repos/{owner}/{repo}/git/refs/heads/{branch}", owner, repo, branch)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        }
    }
}
