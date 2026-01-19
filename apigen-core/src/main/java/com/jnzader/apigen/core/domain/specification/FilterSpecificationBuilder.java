package com.jnzader.apigen.core.domain.specification;

import com.jnzader.apigen.core.domain.entity.Base;
import jakarta.persistence.criteria.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * Constructor de especificaciones JPA dinámicas a partir de parámetros de filtrado.
 *
 * <p>Soporta un lenguaje de consulta simple basado en operadores:
 *
 * <pre>
 * GET /api/v1/entities?filter=nombre:like:Juan,edad:gte:25,estado:eq:true
 * </pre>
 *
 * <p>Operadores soportados:
 *
 * <ul>
 *   <li><b>eq</b>: Igual (=)
 *   <li><b>neq</b>: No igual (!=)
 *   <li><b>like</b>: Contiene (LIKE %value%)
 *   <li><b>starts</b>: Empieza con (LIKE value%)
 *   <li><b>ends</b>: Termina con (LIKE %value)
 *   <li><b>gt</b>: Mayor que (>)
 *   <li><b>gte</b>: Mayor o igual (>=)
 *   <li><b>lt</b>: Menor que (<)
 *   <li><b>lte</b>: Menor o igual (<=)
 *   <li><b>in</b>: En lista (IN (v1,v2,v3))
 *   <li><b>notin</b>: No en lista (NOT IN)
 *   <li><b>between</b>: Entre dos valores (BETWEEN)
 *   <li><b>null</b>: Es nulo (IS NULL)
 *   <li><b>notnull</b>: No es nulo (IS NOT NULL)
 * </ul>
 *
 * <p>Ejemplo de uso:
 *
 * <pre>{@code
 * @GetMapping
 * public ResponseEntity<?> findAll(
 *         @RequestParam(required = false) String filter,
 *         Pageable pageable) {
 *
 *     Specification<MyEntity> spec = filterBuilder.build(filter, MyEntity.class);
 *     return service.findAll(spec, pageable);
 * }
 * }</pre>
 */
@Component
public class FilterSpecificationBuilder {

    private static final Logger log = LoggerFactory.getLogger(FilterSpecificationBuilder.class);

    private static final String FILTER_SEPARATOR = ",";
    private static final String OPERATOR_SEPARATOR = ":";
    private static final String VALUE_LIST_SEPARATOR = ";";

    /**
     * Construye una Specification JPA a partir de un string de filtros.
     *
     * @param filterString String con filtros en formato:
     *     campo:operador:valor,campo2:operador2:valor2
     * @param entityClass Clase de la entidad para validación de campos
     * @param <E> Tipo de entidad que extiende Base
     * @return Specification construida o specification vacía si no hay filtros
     */
    public <E extends Base> Specification<E> build(String filterString, Class<E> entityClass) {
        if (filterString == null || filterString.isBlank()) {
            // Return empty specification that matches all (conjunction = true)
            return (root, query, cb) -> cb.conjunction();
        }

        List<FilterCriteria> criteria = parseFilterString(filterString);
        return buildSpecification(criteria);
    }

    /**
     * Construye una Specification a partir de un Map de filtros. Útil cuando los filtros vienen
     * como query params individuales.
     *
     * @param filters Map de campo -> valor (operador por defecto: eq para valores simples, like
     *     para strings)
     * @param <E> Tipo de entidad
     * @return Specification construida
     */
    public <E extends Base> Specification<E> build(Map<String, String> filters) {
        if (filters == null || filters.isEmpty()) {
            // Return empty specification that matches all (conjunction = true)
            return (root, query, cb) -> cb.conjunction();
        }

        // Filtrar parámetros de sistema (page, size, sort, fields, etc.)
        Set<String> systemParams = Set.of("page", "size", "sort", "fields", "filter");

        List<FilterCriteria> criteria =
                filters.entrySet().stream()
                        .filter(e -> !systemParams.contains(e.getKey().toLowerCase()))
                        .filter(e -> e.getValue() != null && !e.getValue().isBlank())
                        .map(this::parseMapEntry)
                        .filter(Objects::nonNull)
                        .toList();

        return buildSpecification(criteria);
    }

    private List<FilterCriteria> parseFilterString(String filterString) {
        List<FilterCriteria> criteria = new ArrayList<>();

        String[] filters = filterString.split(FILTER_SEPARATOR);
        for (String filter : filters) {
            String trimmed = filter.trim();
            if (trimmed.isEmpty()) continue;

            FilterCriteria parsed = parseFilter(trimmed);
            if (parsed != null) {
                criteria.add(parsed);
            }
        }

        return criteria;
    }

    private FilterCriteria parseFilter(String filter) {
        String[] parts = filter.split(OPERATOR_SEPARATOR, 3);

        if (parts.length < 2) {
            log.warn("Filtro inválido (formato esperado: campo:operador:valor): {}", filter);
            return null;
        }

        String field = parts[0].trim();
        String operatorStr = parts[1].trim().toLowerCase();
        String value = parts.length > 2 ? parts[2].trim() : null;

        FilterOperator operator;
        try {
            operator = FilterOperator.fromString(operatorStr);
        } catch (IllegalArgumentException _) {
            log.warn("Operador desconocido '{}' en filtro: {}", operatorStr, filter);
            return null;
        }

        // Operadores sin valor
        if (operator == FilterOperator.NULL || operator == FilterOperator.NOT_NULL) {
            return new FilterCriteria(field, operator, null);
        }

        if (value == null || value.isEmpty()) {
            log.warn("Filtro sin valor: {}", filter);
            return null;
        }

        return new FilterCriteria(field, operator, value);
    }

    private FilterCriteria parseMapEntry(Map.Entry<String, String> entry) {
        String field = entry.getKey();
        String value = entry.getValue();

        // Detectar operador en el valor: campo=operador:valor
        if (value.contains(OPERATOR_SEPARATOR)) {
            String[] parts = value.split(OPERATOR_SEPARATOR, 2);
            try {
                FilterOperator op = FilterOperator.fromString(parts[0].trim().toLowerCase());
                return new FilterCriteria(field, op, parts.length > 1 ? parts[1].trim() : null);
            } catch (IllegalArgumentException _) {
                // No es un operador, usar valor completo con eq/like
            }
        }

        // Usar operador por defecto basado en el valor
        if (value.contains("%") || value.contains("*")) {
            // Contiene wildcard, usar LIKE
            String likeValue = value.replace("*", "%");
            return new FilterCriteria(field, FilterOperator.LIKE, likeValue);
        }

        // Valor simple, usar EQ
        return new FilterCriteria(field, FilterOperator.EQ, value);
    }

    private <E extends Base> Specification<E> buildSpecification(List<FilterCriteria> criteria) {
        // Start with a specification that matches all (conjunction = true)
        Specification<E> spec = (root, query, cb) -> cb.conjunction();

        for (FilterCriteria c : criteria) {
            Specification<E> criteriaSpec = buildCriteriaSpecification(c);
            if (criteriaSpec != null) {
                spec = spec.and(criteriaSpec);
            }
        }

        return spec;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <E extends Base> Specification<E> buildCriteriaSpecification(FilterCriteria c) {
        return (root, query, cb) -> {
            Path<?> path;
            try {
                path = getPath(root, c.field());
            } catch (IllegalArgumentException _) {
                log.warn("Campo no encontrado: {}", c.field());
                return cb.conjunction();
            }

            Object typedValue = convertValue(c.value(), path.getJavaType());

            return switch (c.operator()) {
                case EQ -> cb.equal(path, typedValue);
                case NEQ -> cb.notEqual(path, typedValue);
                case LIKE ->
                        cb.like(cb.lower((Path<String>) path), "%" + c.value().toLowerCase() + "%");
                case STARTS ->
                        cb.like(cb.lower((Path<String>) path), c.value().toLowerCase() + "%");
                case ENDS -> cb.like(cb.lower((Path<String>) path), "%" + c.value().toLowerCase());
                case GT -> cb.greaterThan((Path<Comparable>) path, (Comparable) typedValue);
                case GTE ->
                        cb.greaterThanOrEqualTo((Path<Comparable>) path, (Comparable) typedValue);
                case LT -> cb.lessThan((Path<Comparable>) path, (Comparable) typedValue);
                case LTE -> cb.lessThanOrEqualTo((Path<Comparable>) path, (Comparable) typedValue);
                case IN -> buildInPredicate(path, c.value());
                case NOT_IN -> cb.not(buildInPredicate(path, c.value()));
                case BETWEEN -> buildBetweenPredicate(cb, (Path<Comparable>) path, c.value());
                case NULL -> cb.isNull(path);
                case NOT_NULL -> cb.isNotNull(path);
            };
        };
    }

    /**
     * Obtiene el Path para un campo, soportando notación con puntos para relaciones. Ejemplo:
     * "role.name" -> root.get("role").get("name")
     */
    private Path<?> getPath(Root<?> root, String field) {
        String[] parts = field.split("\\.");
        Path<?> path = root;
        for (String part : parts) {
            path = path.get(part);
        }
        return path;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate buildInPredicate(Path<?> path, String value) {
        String[] values = value.split(VALUE_LIST_SEPARATOR);
        List<Object> typedValues = new ArrayList<>();
        Class<?> javaType = path.getJavaType();

        for (String v : values) {
            typedValues.add(convertValue(v.trim(), javaType));
        }

        return path.in(typedValues);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate buildBetweenPredicate(
            CriteriaBuilder cb, Path<? extends Comparable> path, String value) {
        String[] values = value.split(VALUE_LIST_SEPARATOR);
        if (values.length != 2) {
            log.warn(
                    "BETWEEN requiere exactamente 2 valores separados por '{}': {}",
                    VALUE_LIST_SEPARATOR,
                    value);
            return cb.conjunction();
        }

        Class<?> javaType = path.getJavaType();
        Comparable lower = (Comparable) convertValue(values[0].trim(), javaType);
        Comparable upper = (Comparable) convertValue(values[1].trim(), javaType);

        return cb.between(path, lower, upper);
    }

    /** Convierte un valor string al tipo Java apropiado. */
    @SuppressWarnings("unchecked")
    private Object convertValue(String value, Class<?> targetType) {
        if (value == null) return null;

        try {
            return convertToType(value, targetType);
        } catch (Exception e) {
            log.warn(
                    "Error convirtiendo valor '{}' a tipo {}: {}",
                    value,
                    targetType.getSimpleName(),
                    e.getMessage());
            return value;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object convertToType(String value, Class<?> targetType) {
        if (targetType == String.class) return value;
        if (targetType == Long.class || targetType == long.class) return Long.parseLong(value);
        if (targetType == Integer.class || targetType == int.class) return Integer.parseInt(value);
        if (targetType == Double.class || targetType == double.class)
            return Double.parseDouble(value);
        if (targetType == Float.class || targetType == float.class) return Float.parseFloat(value);
        if (targetType == BigDecimal.class) return new BigDecimal(value);
        if (targetType == Boolean.class || targetType == boolean.class)
            return Boolean.parseBoolean(value);
        if (targetType == LocalDateTime.class) return parseLocalDateTime(value);
        if (targetType == LocalDate.class) return LocalDate.parse(value);
        if (targetType.isEnum()) return Enum.valueOf((Class<Enum>) targetType, value.toUpperCase());
        return value;
    }

    private LocalDateTime parseLocalDateTime(String value) {
        // Intentar varios formatos comunes
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException _) {
            // Si solo tiene fecha, agregar tiempo 00:00:00
            try {
                return LocalDate.parse(value).atStartOfDay();
            } catch (DateTimeParseException _) {
                throw new IllegalArgumentException("Formato de fecha inválido: " + value);
            }
        }
    }

    /** Registro para representar un criterio de filtro parseado. */
    private record FilterCriteria(String field, FilterOperator operator, String value) {}

    /** Enum de operadores de filtro soportados. */
    public enum FilterOperator {
        EQ("eq"),
        NEQ("neq"),
        LIKE("like"),
        STARTS("starts"),
        ENDS("ends"),
        GT("gt"),
        GTE("gte"),
        LT("lt"),
        LTE("lte"),
        IN("in"),
        NOT_IN("notin"),
        BETWEEN("between"),
        NULL("null"),
        NOT_NULL("notnull");

        private final String value;

        FilterOperator(String value) {
            this.value = value;
        }

        public static FilterOperator fromString(String text) {
            for (FilterOperator op : FilterOperator.values()) {
                if (op.value.equalsIgnoreCase(text)) {
                    return op;
                }
            }
            throw new IllegalArgumentException("Operador desconocido: " + text);
        }
    }
}
