package com.jnzader.apigen.core.support;

import com.jnzader.apigen.core.fixtures.TestEntity;

/**
 * Builder for creating TestEntity instances in tests. Uses the fluent builder pattern for easy test
 * data creation.
 */
public class TestEntityBuilder {

    private static final java.util.concurrent.atomic.AtomicLong idCounter =
            new java.util.concurrent.atomic.AtomicLong(1);

    private Long id;
    private String name = "Test Entity";
    private String description = "Test Description";
    private Integer value = 100;
    private String creadoPor = "test-user";
    private Boolean estado = true;

    private TestEntityBuilder() {}

    public static TestEntityBuilder aTestEntity() {
        return new TestEntityBuilder();
    }

    public static TestEntityBuilder aTestEntityWithId() {
        return new TestEntityBuilder().withId(idCounter.getAndIncrement());
    }

    public static void resetIdCounter() {
        idCounter.set(1);
    }

    public TestEntityBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public TestEntityBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public TestEntityBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public TestEntityBuilder withValue(Integer value) {
        this.value = value;
        return this;
    }

    public TestEntityBuilder withCreadoPor(String creadoPor) {
        this.creadoPor = creadoPor;
        return this;
    }

    public TestEntityBuilder withEstado(Boolean estado) {
        this.estado = estado;
        return this;
    }

    public TestEntity build() {
        TestEntity entity = new TestEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setDescription(description);
        entity.setValue(value);
        entity.setCreadoPor(creadoPor);
        entity.setEstado(estado);
        return entity;
    }
}
