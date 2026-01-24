package com.jnzader.apigen.server.dto.github;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request to create a new GitHub repository. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRepoRequest {

    /** Repository name (required). */
    @NotBlank(message = "Repository name is required")
    @Size(min = 1, max = 100, message = "Repository name must be between 1 and 100 characters")
    @Pattern(
            regexp = "^[a-zA-Z0-9._-]+$",
            message =
                    "Repository name can only contain alphanumeric characters, dots, hyphens and"
                            + " underscores")
    private String name;

    /** Repository description (optional). */
    @Size(max = 350, message = "Description must be less than 350 characters")
    private String description;

    /** Whether the repository should be private. Default: false (public). */
    @Builder.Default private boolean isPrivate = false;

    /** Whether to auto-initialize with README. Default: true. */
    @Builder.Default private boolean autoInit = true;

    /** Gitignore template to use (e.g., "Java", "Gradle"). */
    private String gitignoreTemplate;

    /** License template (e.g., "mit", "apache-2.0"). */
    private String license;

    /** Homepage URL for the repository. */
    private String homepage;
}
