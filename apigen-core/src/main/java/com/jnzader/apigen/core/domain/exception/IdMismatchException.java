package com.jnzader.apigen.core.domain.exception;

/**
 * Excepción lanzada cuando el ID en el path no coincide con el ID en el body.
 *
 * <p>Utilizada en operaciones PUT donde se requiere consistencia entre el ID de la URL y el ID del
 * recurso en el cuerpo de la solicitud.
 */
public class IdMismatchException extends RuntimeException {

    private final transient Object pathId;
    private final transient Object bodyId;

    public IdMismatchException(Object pathId, Object bodyId) {
        super(
                String.format(
                        "El ID del path (%s) no coincide con el ID del body (%s)", pathId, bodyId));
        this.pathId = pathId;
        this.bodyId = bodyId;
    }

    public IdMismatchException(String message, Object pathId, Object bodyId) {
        super(message);
        this.pathId = pathId;
        this.bodyId = bodyId;
    }

    public Object getPathId() {
        return pathId;
    }

    public Object getBodyId() {
        return bodyId;
    }
}
