package com.jnzader.apigen.server.exception;

/** Exception thrown when GitHub API operations fail. */
public class GitHubException extends RuntimeException {

    public GitHubException(String message) {
        super(message);
    }

    public GitHubException(String message, Throwable cause) {
        super(message, cause);
    }
}
