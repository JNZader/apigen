package com.jnzader.apigen.core.fixtures;

import com.jnzader.apigen.core.domain.entity.Base;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Test entity for unit tests.
 */
@Entity
@Table(name = "test_entities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestEntity extends Base {

    private String name;

    private String description;

    @Column(name = "entity_value")
    private Integer value;
}
