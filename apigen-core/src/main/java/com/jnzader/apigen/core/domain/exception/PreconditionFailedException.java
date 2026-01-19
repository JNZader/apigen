package com.jnzader.apigen.core.domain.exception;

/**
 * Excepción lanzada cuando falla una precondición HTTP.
 *
 * <p>Utilizada principalmente para: - ETag mismatch (If-Match header no coincide) - Conflictos de
 * concurrencia optimista
 *
 * <p>Resulta en HTTP 412 Precondition Failed.
 */
public class PreconditionFailedException extends RuntimeException {

    private final String currentEtag;
    private final String providedEtag;

    public PreconditionFailedException(String message) {
        super(message);
        this.currentEtag = null;
        this.providedEtag = null;
    }

    public PreconditionFailedException(String message, String currentEtag, String providedEtag) {
        super(message);
        this.currentEtag = currentEtag;
        this.providedEtag = providedEtag;
    }

    public static PreconditionFailedException etagMismatch(
            String currentEtag, String providedEtag) {
        return new PreconditionFailedException(
                String.format(
                        "El recurso ha sido modificado. ETag actual: %s, ETag proporcionado: %s",
                        currentEtag, providedEtag),
                currentEtag,
                providedEtag);
    }

    public String getCurrentEtag() {
        return currentEtag;
    }

    public String getProvidedEtag() {
        return providedEtag;
    }
}
