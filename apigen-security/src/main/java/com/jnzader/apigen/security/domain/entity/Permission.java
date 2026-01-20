package com.jnzader.apigen.security.domain.entity;

import com.jnzader.apigen.core.domain.entity.Base;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Entidad que representa un permiso en el sistema.
 *
 * <p>Los permisos definen acciones específicas que pueden realizarse. Se agrupan en roles para
 * facilitar la gestión.
 *
 * <p>Convención de nombres: VERBO_RECURSO Ejemplos: CREATE_USER, READ_USERS, UPDATE_PRODUCT,
 * DELETE_ORDER
 */
@Entity
@Table(name = "permissions")
@SuppressWarnings("java:S2160") // equals/hashCode heredados de Base (basado en ID)
public class Permission extends Base {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    /** Categoría del permiso para agrupación en UI. Ejemplos: USERS, PRODUCTS, ORDERS, SYSTEM */
    @Column(length = 50)
    private String category;

    public Permission() {}

    public Permission(String name) {
        this.name = name;
    }

    public Permission(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Permission(String name, String description, String category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
