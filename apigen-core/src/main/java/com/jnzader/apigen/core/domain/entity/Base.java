package com.jnzader.apigen.core.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jnzader.apigen.core.domain.event.DomainEvent;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Abstract base class for all system entities. Provides common auditing fields, soft delete
 * support, and domain events.
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Audited
@EntityListeners(AuditingEntityListener.class)
@SQLRestriction("estado = true")
public abstract class Base implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "base_seq_gen")
    @SequenceGenerator(name = "base_seq_gen", sequenceName = "base_sequence", allocationSize = 50)
    private Long id;

    /** Entity status (true = active, false = inactive/deleted). Used for soft delete. */
    @Builder.Default
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean estado = true;

    /** Creation date and time of the record. */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /** Date and time of the last update. */
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    /** Date and time of logical deletion (soft delete). */
    @Column private LocalDateTime fechaEliminacion;

    /** User who performed the logical deletion. */
    @Column(length = 100)
    private String eliminadoPor;

    /** User who created the record. */
    @CreatedBy
    @Column(length = 100, updatable = false)
    private String creadoPor;

    /** User who performed the last modification. */
    @LastModifiedBy
    @Column(length = 100)
    private String modificadoPor;

    /** Version for optimistic concurrency control. */
    @Builder.Default
    @Version
    @Column(nullable = false)
    private Long version = 0L;

    // ==================== Domain Events ====================

    /**
     * List of domain events pending publication. Uses CopyOnWriteArrayList for thread safety in
     * concurrent environments.
     */
    @Transient @JsonIgnore
    private final List<DomainEvent> domainEvents = new CopyOnWriteArrayList<>();

    /**
     * Registers a domain event to be published. This method is public to allow services to register
     * events.
     *
     * @param event The event to register.
     */
    public void registerEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    /** Returns pending domain events (used by Spring Data). */
    @DomainEvents
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    /** Clears the event list after publication. */
    @AfterDomainEventPublication
    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    // ==================== Soft Delete ====================

    /**
     * Marks the entity as deleted (soft delete).
     *
     * @param usuario The user performing the deletion.
     */
    public void softDelete(String usuario) {
        this.estado = false;
        this.fechaEliminacion = LocalDateTime.now();
        this.eliminadoPor = usuario;
    }

    /** Restores a deleted entity. */
    public void restore() {
        this.estado = true;
        this.fechaEliminacion = null;
        this.eliminadoPor = null;
    }

    /** Checks if the entity is deleted. */
    public boolean isDeleted() {
        return !Boolean.TRUE.equals(estado);
    }

    /** Checks if the entity is active. */
    public boolean isActive() {
        return Boolean.TRUE.equals(estado);
    }

    // ==================== Equals & HashCode ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Base base = (Base) o;
        // Only compare by ID if both have ID (persisted entities)
        if (id != null && base.id != null) {
            return Objects.equals(id, base.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        // Use a constant for new entities (without ID)
        return id != null ? Objects.hash(id) : System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "{"
                + "id="
                + id
                + ", estado="
                + estado
                + ", fechaCreacion="
                + fechaCreacion
                + '}';
    }
}
