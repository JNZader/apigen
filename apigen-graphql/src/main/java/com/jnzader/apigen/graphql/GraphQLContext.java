package com.jnzader.apigen.graphql;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Context object passed to all GraphQL data fetchers.
 *
 * <p>Contains request-scoped information such as the current user, locale, and custom attributes.
 *
 * <p>Example usage in a data fetcher:
 *
 * <pre>{@code
 * @Override
 * public Object get(DataFetchingEnvironment env) {
 *     GraphQLContext context = env.getContext();
 *     String userId = context.getUserId().orElse("anonymous");
 *     Locale locale = context.getLocale();
 *     // ...
 * }
 * }</pre>
 */
public class GraphQLContext {

    private final String userId;
    private final Locale locale;
    private final Map<String, Object> attributes;

    private GraphQLContext(Builder builder) {
        this.userId = builder.userId;
        this.locale = builder.locale != null ? builder.locale : Locale.getDefault();
        this.attributes = new ConcurrentHashMap<>(builder.attributes);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<String> getUserId() {
        return Optional.ofNullable(userId);
    }

    public Locale getLocale() {
        return locale;
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getAttribute(String key) {
        return Optional.ofNullable((T) attributes.get(key));
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }

    public static class Builder {
        private String userId;
        private Locale locale;
        private final Map<String, Object> attributes = new ConcurrentHashMap<>();

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder locale(Locale locale) {
            this.locale = locale;
            return this;
        }

        public Builder attribute(String key, Object value) {
            this.attributes.put(key, value);
            return this;
        }

        public GraphQLContext build() {
            return new GraphQLContext(this);
        }
    }
}
