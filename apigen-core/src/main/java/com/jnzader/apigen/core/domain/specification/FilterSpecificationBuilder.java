package com.jnzader.apigen.core.domain.specification;

import com.jnzader.apigen.core.domain.entity.Base;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * Dynamic JPA specification builder from filter parameters.
 *
 * <p>Supports a simple query language based on operators:
 *
 * <pre>
 * GET /api/v1/entities?filter=name:like:John,age:gte:25,status:eq:true
 * </pre>
 *
 * <p>Supported operators:
 *
 * <ul>
 *   <li><b>eq</b>: Equal (=)
 *   <li><b>neq</b>: Not equal (!=)
 *   <li><b>like</b>: Contains (LIKE %value%)
 *   <li><b>starts</b>: Starts with (LIKE value%)
 *   <li><b>ends</b>: Ends with (LIKE %value)
 *   <li><b>gt</b>: Greater than (>)
 *   <li><b>gte</b>: Greater than or equal (>=)
 *   <li><b>lt</b>: Less than (<)
 *   <li><b>lte</b>: Less than or equal (<=)
 *   <li><b>in</b>: In list (IN (v1,v2,v3))
 *   <li><b>notin</b>: Not in list (NOT IN)
 *   <li><b>between</b>: Between two values (BETWEEN)
 *   <li><b>null</b>: Is null (IS NULL)
 *   <li><b>notnull</b>: Is not null (IS NOT NULL)
 * </ul>
 *
 * <p>Usage example:
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
     * Builds a JPA Specification from a filter string.
     *
     * @param filterString String with filters in format:
     *     field:operator:value,field2:operator2:value2
     * @param entityClass Entity class for field validation
     * @param <E> Entity type extending Base
     * @return Built specification or empty specification if no filters
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
     * Builds a Specification from a Map of filters. Useful when filters come as individual query
     * params.
     *
     * @param filters Map of field -> value (default operator: eq for simple values, like for
     *     strings)
     * @param <E> Entity type
     * @return Built specification
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
                        .filter(e -> !systemParams.contains(e.getKey().toLowerCase(Locale.ROOT)))
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
            log.warn("Invalid filter (expected format: field:operator:value): {}", filter);
            return null;
        }

        String field = parts[0].trim();
        String operatorStr = parts[1].trim().toLowerCase(Locale.ROOT);
        String value = parts.length > 2 ? parts[2].trim() : null;

        FilterOperator operator;
        try {
            operator = FilterOperator.fromString(operatorStr);
        } catch (IllegalArgumentException _) {
            log.warn("Unknown operator '{}' in filter: {}", operatorStr, filter);
            return null;
        }

        // Operators without value
        if (operator == FilterOperator.NULL || operator == FilterOperator.NOT_NULL) {
            return new FilterCriteria(field, operator, null);
        }

        if (value == null || value.isEmpty()) {
            log.warn("Filter without value: {}", filter);
            return null;
        }

        return new FilterCriteria(field, operator, value);
    }

    private FilterCriteria parseMapEntry(Map.Entry<String, String> entry) {
        String field = entry.getKey();
        String value = entry.getValue();

        // Detect operator in value: field=operator:value
        if (value.contains(OPERATOR_SEPARATOR)) {
            String[] parts = value.split(OPERATOR_SEPARATOR, 2);
            try {
                FilterOperator op =
                        FilterOperator.fromString(parts[0].trim().toLowerCase(Locale.ROOT));
                return new FilterCriteria(field, op, parts.length > 1 ? parts[1].trim() : null);
            } catch (IllegalArgumentException _) {
                // Not an operator, use full value with eq/like
            }
        }

        // Use default operator based on value
        if (value.contains("%") || value.contains("*")) {
            // Contains wildcard, use LIKE
            String likeValue = value.replace("*", "%");
            return new FilterCriteria(field, FilterOperator.LIKE, likeValue);
        }

        // Simple value, use EQ
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
                log.warn("Field not found: {}", c.field());
                return cb.conjunction();
            }

            Object typedValue = convertValue(c.value(), path.getJavaType());

            return switch (c.operator()) {
                case EQ -> cb.equal(path, typedValue);
                case NEQ -> cb.notEqual(path, typedValue);
                case LIKE ->
                        cb.like(
                                cb.lower((Path<String>) path),
                                "%" + c.value().toLowerCase(Locale.ROOT) + "%");
                case STARTS ->
                        cb.like(
                                cb.lower((Path<String>) path),
                                c.value().toLowerCase(Locale.ROOT) + "%");
                case ENDS ->
                        cb.like(
                                cb.lower((Path<String>) path),
                                "%" + c.value().toLowerCase(Locale.ROOT));
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
     * Gets the Path for a field, supporting dot notation for relationships. Example: "role.name" ->
     * root.get("role").get("name")
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

    /** Converts a string value to the appropriate Java type. */
    @SuppressWarnings("unchecked")
    private Object convertValue(String value, Class<?> targetType) {
        if (value == null) return null;

        try {
            return convertToType(value, targetType);
        } catch (Exception e) {
            log.warn(
                    "Error converting value '{}' to type {}: {}",
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
        if (targetType.isEnum())
            return Enum.valueOf((Class<Enum>) targetType, value.toUpperCase(Locale.ROOT));
        return value;
    }

    private LocalDateTime parseLocalDateTime(String value) {
        // Try several common formats
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException _) {
            // If only date, add time 00:00:00
            try {
                return LocalDate.parse(value).atStartOfDay();
            } catch (DateTimeParseException _) {
                throw new IllegalArgumentException("Invalid date format: " + value);
            }
        }
    }

    /** Record representing a parsed filter criterion. */
    private record FilterCriteria(String field, FilterOperator operator, String value) {}

    /** Enum of supported filter operators. */
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
            throw new IllegalArgumentException("Unknown operator: " + text);
        }
    }
}
