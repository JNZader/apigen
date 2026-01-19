package com.jnzader.apigen.codegen.model;

/** JPA relationship types. */
public enum RelationType {
    ONE_TO_ONE("@OneToOne"),
    ONE_TO_MANY("@OneToMany"),
    MANY_TO_ONE("@ManyToOne"),
    MANY_TO_MANY("@ManyToMany");

    private final String annotation;

    RelationType(String annotation) {
        this.annotation = annotation;
    }

    public String getAnnotation() {
        return annotation;
    }

    /** Returns the inverse relationship type. */
    public RelationType inverse() {
        return switch (this) {
            case ONE_TO_ONE -> ONE_TO_ONE;
            case ONE_TO_MANY -> MANY_TO_ONE;
            case MANY_TO_ONE -> ONE_TO_MANY;
            case MANY_TO_MANY -> MANY_TO_MANY;
        };
    }

    /** Whether this relationship requires a collection type. */
    public boolean isCollection() {
        return this == ONE_TO_MANY || this == MANY_TO_MANY;
    }
}
