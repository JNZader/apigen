package com.jnzader.apigen.server.dto.github;

import com.jnzader.apigen.server.dto.GenerateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request to push a generated project to a GitHub repository. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushProjectRequest {

    /** GitHub OAuth access token. Optional when using HttpOnly cookie authentication. */
    private String accessToken;

    /** Repository owner (username or organization). */
    @NotBlank(message = "Repository owner is required")
    private String owner;

    /** Repository name. */
    @NotBlank(message = "Repository name is required")
    private String repo;

    /** Branch to push to. Default: "main". */
    private String branch;

    /** Commit message. Default: "Initial commit from APiGen Studio". */
    private String commitMessage;

    /** Whether to force push (overwrite existing content). */
    private Boolean forcePush;

    /** Returns the branch, defaulting to "main" if not specified. */
    public String getBranch() {
        return branch != null && !branch.isEmpty() ? branch : "main";
    }

    /** Returns the commit message, defaulting if not specified. */
    public String getCommitMessage() {
        return commitMessage != null && !commitMessage.isEmpty()
                ? commitMessage
                : "Initial commit from APiGen Studio";
    }

    /** Returns whether to force push, defaulting to false if not specified. */
    public boolean isForcePush() {
        return forcePush != null && forcePush;
    }

    /** The generation request containing SQL and project configuration. */
    @NotNull(message = "Generation request is required")
    @Valid
    private GenerateRequest generateRequest;
}
