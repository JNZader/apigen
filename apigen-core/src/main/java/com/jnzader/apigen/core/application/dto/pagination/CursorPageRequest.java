package com.jnzader.apigen.core.application.dto.pagination;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Request para paginación basada en cursor.
 *
 * <p>El cursor es una representación codificada en Base64 del último ID visto, lo que permite una
 * paginación eficiente sin los problemas de offset: - Performance constante independiente de la
 * página - Sin duplicados/omisiones al insertar/eliminar datos
 *
 * <p>Formato del cursor: Base64(id:sortField:sortValue:direction)
 *
 * @param cursor Cursor codificado en Base64 (null para primera página)
 * @param size Tamaño de página (default 20, max 100)
 * @param sortField Campo por el cual ordenar (default "id")
 * @param sortDirection Dirección de ordenamiento (ASC o DESC)
 */
public record CursorPageRequest(
        String cursor, int size, String sortField, SortDirection sortDirection) {
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;
    public static final String DEFAULT_SORT_FIELD = "id";

    public enum SortDirection {
        ASC,
        DESC
    }

    /** Constructor con valores por defecto. */
    public CursorPageRequest {
        if (size <= 0) {
            size = DEFAULT_SIZE;
        }
        if (size > MAX_SIZE) {
            size = MAX_SIZE;
        }
        if (sortField == null || sortField.isBlank()) {
            sortField = DEFAULT_SORT_FIELD;
        }
        if (sortDirection == null) {
            sortDirection = SortDirection.DESC;
        }
    }

    /** Crea un request para la primera página. */
    public static CursorPageRequest firstPage(int size, String sortField, SortDirection direction) {
        return new CursorPageRequest(null, size, sortField, direction);
    }

    /** Crea un request desde un cursor existente. */
    public static CursorPageRequest fromCursor(String cursor, int size) {
        if (cursor == null || cursor.isBlank()) {
            return firstPage(size, DEFAULT_SORT_FIELD, SortDirection.DESC);
        }

        DecodedCursor decoded = decodeCursor(cursor);
        return new CursorPageRequest(cursor, size, decoded.sortField(), decoded.sortDirection());
    }

    /** Verifica si es la primera página. */
    public boolean isFirstPage() {
        return cursor == null || cursor.isBlank();
    }

    /** Decodifica el cursor para obtener el ID del último elemento. */
    public DecodedCursor getDecodedCursor() {
        if (isFirstPage()) {
            return null;
        }
        return decodeCursor(cursor);
    }

    /** Crea un cursor codificado a partir de los componentes. */
    public static String encodeCursor(
            Long id, String sortField, String sortValue, SortDirection direction) {
        String raw =
                String.format(
                        "%d:%s:%s:%s",
                        id, sortField, sortValue != null ? sortValue : "", direction.name());
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /** Decodifica un cursor. */
    private static DecodedCursor decodeCursor(String cursor) {
        try {
            String decoded =
                    new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":", 4);

            if (parts.length < 4) {
                throw new IllegalArgumentException("Cursor inválido: formato incorrecto");
            }

            return new DecodedCursor(
                    Long.parseLong(parts[0]),
                    parts[1],
                    parts[2].isEmpty() ? null : parts[2],
                    SortDirection.valueOf(parts[3]));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Cursor inválido: " + e.getMessage(), e);
        }
    }

    /** Representación decodificada del cursor. */
    public record DecodedCursor(
            Long lastId, String sortField, String sortValue, SortDirection sortDirection) {}
}
