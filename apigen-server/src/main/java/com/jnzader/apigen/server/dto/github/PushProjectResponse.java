package com.jnzader.apigen.server.dto.github;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response after pushing a project to GitHub. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushProjectResponse {

    /** Whether the push was successful. */
    private boolean success;

    /** Commit SHA of the pushed changes. */
    private String commitSha;

    /** Repository HTML URL. */
    private String repositoryUrl;

    /** Branch that was pushed to. */
    private String branch;

    /** Number of files pushed. */
    private int filesCount;

    /** List of files that were pushed. */
    private List<String> files;

    /** Error message if push failed. */
    private String error;
}
