package com.jnzader.apigen.server.dto.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response after creating a GitHub repository. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRepoResponse {

    /** Repository ID. */
    private Long id;

    /** Repository name. */
    private String name;

    /** Full repository name (owner/repo). */
    @JsonProperty("full_name")
    private String fullName;

    /** Repository description. */
    private String description;

    /** Whether the repository is private. */
    @JsonProperty("private")
    private boolean isPrivate;

    /** Repository HTML URL. */
    @JsonProperty("html_url")
    private String htmlUrl;

    /** Clone URL (HTTPS). */
    @JsonProperty("clone_url")
    private String cloneUrl;

    /** SSH clone URL. */
    @JsonProperty("ssh_url")
    private String sshUrl;

    /** Default branch name. */
    @JsonProperty("default_branch")
    private String defaultBranch;

    /** Whether the operation was successful. */
    private boolean success;

    /** Error message if creation failed. */
    private String error;
}
