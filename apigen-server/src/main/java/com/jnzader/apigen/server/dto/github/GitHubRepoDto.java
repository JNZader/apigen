package com.jnzader.apigen.server.dto.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO representing a GitHub repository in list responses. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubRepoDto {

    /** Repository name. */
    private String name;

    /** Full repository name (owner/repo). */
    @JsonProperty("fullName")
    private String fullName;

    /** Whether the repository is private. */
    @JsonProperty("private")
    private boolean isPrivate;

    /** Repository HTML URL. */
    @JsonProperty("htmlUrl")
    private String htmlUrl;

    /** Repository description. */
    private String description;

    /** Default branch name. */
    @JsonProperty("defaultBranch")
    private String defaultBranch;
}
