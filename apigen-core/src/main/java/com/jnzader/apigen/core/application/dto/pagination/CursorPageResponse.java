package com.jnzader.apigen.core.application.dto.pagination;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Respuesta para paginación basada en cursor.
 * <p>
 * Proporciona cursores para navegación hacia adelante y atrás,
 * permitiendo una paginación eficiente sin los problemas de offset.
 *
 * @param <T> Tipo de los elementos en la página
 * @param content     Lista de elementos
 * @param pageInfo    Información de paginación con cursores
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CursorPageResponse<T>(
        List<T> content,
        PageInfo pageInfo
) {
    /**
     * Información de paginación con cursores.
     *
     * @param size         Tamaño de página solicitado
     * @param hasNext      Si hay más elementos después
     * @param hasPrevious  Si hay elementos antes
     * @param nextCursor   Cursor para la siguiente página (null si no hay más)
     * @param prevCursor   Cursor para la página anterior (null si es la primera)
     * @param totalElements Total de elementos (opcional, solo si se solicita)
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PageInfo(
            int size,
            boolean hasNext,
            boolean hasPrevious,
            String nextCursor,
            String prevCursor,
            Long totalElements
    ) {
        /**
         * Crea PageInfo sin total de elementos.
         */
        public static PageInfo of(int size, boolean hasNext, boolean hasPrevious,
                                  String nextCursor, String prevCursor) {
            return new PageInfo(size, hasNext, hasPrevious, nextCursor, prevCursor, null);
        }

        /**
         * Crea PageInfo con total de elementos.
         */
        public static PageInfo withTotal(int size, boolean hasNext, boolean hasPrevious,
                                         String nextCursor, String prevCursor, long totalElements) {
            return new PageInfo(size, hasNext, hasPrevious, nextCursor, prevCursor, totalElements);
        }
    }

    /**
     * Crea una respuesta de cursor vacía.
     */
    public static <T> CursorPageResponse<T> empty(int size) {
        return new CursorPageResponse<>(
                List.of(),
                PageInfo.of(size, false, false, null, null)
        );
    }

    /**
     * Builder para construir la respuesta.
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static class Builder<T> {
        private List<T> content;
        private int size;
        private boolean hasNext;
        private boolean hasPrevious;
        private String nextCursor;
        private String prevCursor;
        private Long totalElements;

        public Builder<T> content(List<T> content) {
            this.content = content;
            return this;
        }

        public Builder<T> size(int size) {
            this.size = size;
            return this;
        }

        public Builder<T> hasNext(boolean hasNext) {
            this.hasNext = hasNext;
            return this;
        }

        public Builder<T> hasPrevious(boolean hasPrevious) {
            this.hasPrevious = hasPrevious;
            return this;
        }

        public Builder<T> nextCursor(String nextCursor) {
            this.nextCursor = nextCursor;
            return this;
        }

        public Builder<T> prevCursor(String prevCursor) {
            this.prevCursor = prevCursor;
            return this;
        }

        public Builder<T> totalElements(Long totalElements) {
            this.totalElements = totalElements;
            return this;
        }

        public CursorPageResponse<T> build() {
            PageInfo pageInfo = totalElements != null
                    ? PageInfo.withTotal(size, hasNext, hasPrevious, nextCursor, prevCursor, totalElements)
                    : PageInfo.of(size, hasNext, hasPrevious, nextCursor, prevCursor);

            return new CursorPageResponse<>(content, pageInfo);
        }
    }
}
