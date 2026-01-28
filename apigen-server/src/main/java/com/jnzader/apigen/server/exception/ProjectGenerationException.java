package com.jnzader.apigen.server.exception;

import com.jnzader.apigen.core.domain.exception.OperationFailedException;

/**
 * Exception thrown when project generation fails.
 *
 * <p>Contains information about the generation context and the underlying cause.
 */
public class ProjectGenerationException extends OperationFailedException {

    private final String projectName;
    private final String generatorType;

    public ProjectGenerationException(String message) {
        super(message);
        this.projectName = null;
        this.generatorType = null;
    }

    public ProjectGenerationException(String message, Throwable cause) {
        super(message, cause);
        this.projectName = null;
        this.generatorType = null;
    }

    public ProjectGenerationException(
            String message, Throwable cause, String projectName, String generatorType) {
        super(message, cause);
        this.projectName = projectName;
        this.generatorType = generatorType;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getGeneratorType() {
        return generatorType;
    }
}
