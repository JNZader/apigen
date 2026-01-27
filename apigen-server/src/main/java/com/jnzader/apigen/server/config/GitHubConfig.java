package com.jnzader.apigen.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

/**
 * Configuration for GitHub OAuth and API integration.
 *
 * <p>Configure via environment variables:
 *
 * <pre>
 * GITHUB_CLIENT_ID=your-client-id
 * GITHUB_CLIENT_SECRET=your-client-secret
 * GITHUB_REDIRECT_URI=http://localhost:8081/api/github/callback
 * </pre>
 */
@Configuration
@ConfigurationProperties(prefix = "github")
@Data
public class GitHubConfig {

    /** GitHub OAuth App Client ID. */
    private String clientId;

    /** GitHub OAuth App Client Secret. */
    private String clientSecret;

    /** OAuth callback redirect URI. */
    private String redirectUri = "http://localhost:8081/api/github/callback";

    /** GitHub OAuth authorization URL. */
    private String authorizeUrl = "https://github.com/login/oauth/authorize";

    /** GitHub OAuth token URL. */
    private String tokenUrl = "https://github.com/login/oauth/access_token";

    /** GitHub API base URL. */
    private String apiUrl = "https://api.github.com";

    /** OAuth scopes for repository access. */
    private String scopes = "repo,user:email";

    /** Frontend URL to redirect after OAuth callback. */
    private String frontendUrl = "http://localhost:5173";

    /**
     * Creates a WebClient bean configured for GitHub API calls.
     *
     * <p>Uses DefaultUriBuilderFactory with TEMPLATE_AND_VALUES encoding mode to ensure proper URI
     * encoding compatible with Netty 4.2.x's strict URI validation.
     *
     * @return Configured WebClient
     */
    @Bean
    public WebClient gitHubWebClient() {
        // Configure UriBuilderFactory with proper encoding for Netty 4.2.x compatibility
        DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory(apiUrl);
        uriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.TEMPLATE_AND_VALUES);

        return WebClient.builder()
                .uriBuilderFactory(uriBuilderFactory)
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }
}
