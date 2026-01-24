package com.jnzader.apigen.server.dto.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response from GitHub OAuth token exchange. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubAuthResponse {

    /** OAuth access token for API calls. */
    @JsonProperty("access_token")
    private String accessToken;

    /** Token type (usually "bearer"). */
    @JsonProperty("token_type")
    private String tokenType;

    /** OAuth scopes granted. */
    private String scope;

    /** GitHub user login name. */
    private String login;

    /** GitHub user display name. */
    private String name;

    /** GitHub user email. */
    private String email;

    /** GitHub user avatar URL. */
    @JsonProperty("avatar_url")
    private String avatarUrl;

    /** Whether authentication was successful. */
    private boolean authenticated;

    /** Error message if authentication failed. */
    private String error;
}
