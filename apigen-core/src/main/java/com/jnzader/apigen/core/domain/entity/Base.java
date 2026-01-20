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
import lombok.Getter;
import lombok.Setter;
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
 * Clase base abstracta para todas las entidades del sistema. Proporciona campos comunes de
 * auditoría, soporte para soft delete y domain events.
 */
@MappedSuperclass
@Getter
@Setter
@Audited
@EntityListeners(AuditingEntityListener.class)
@SQLRestriction("estado = true")
public abstract class Base implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "base_seq_gen")
    @SequenceGenerator(name = "base_seq_gen", sequenceName = "base_sequence", allocationSize = 50)
    private Long id;

    /**
     * Estado de la entidad (true = activo, false = inactivo/eliminado). Se usa para soft delete.
     */
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean estado = true;

    /** Fecha y hora de creación del registro. */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /** Fecha y hora de la última actualización. */
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    /** Fecha y hora de eliminación lógica (soft delete). */
    @Column private LocalDateTime fechaEliminacion;

    /** Usuario que realizó la eliminación lógica. */
    @Column(length = 100)
    private String eliminadoPor;

    /** Usuario que creó el registro. */
    @CreatedBy
    @Column(length = 100, updatable = false)
    private String creadoPor;

    /** Usuario que realizó la última modificación. */
    @LastModifiedBy
    @Column(length = 100)
    private String modificadoPor;

    /** Versión para control de concurrencia optimista. */
    @Version
    @Column(nullable = false)
    private Long version = 0L;

    // ==================== Domain Events ====================

    /**
     * Lista de eventos de dominio pendientes de publicación. Usa CopyOnWriteArrayList para thread
     * safety en entornos concurrentes.
     */
    @Transient @JsonIgnore
    private final List<DomainEvent> domainEvents = new CopyOnWriteArrayList<>();

    /**
     * Registra un evento de dominio para ser publicado. Este método es público para permitir que
     * los servicios registren eventos.
     *
     * @param event El evento a registrar.
     */
    public void registerEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    /** Retorna los eventos de dominio pendientes (usado por Spring Data). */
    @DomainEvents
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    /** Limpia la lista de eventos después de su publicación. */
    @AfterDomainEventPublication
    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    // ==================== Soft Delete ====================

    /**
     * Marca la entidad como eliminada (soft delete).
     *
     * @param usuario El usuario que realiza la eliminación.
     */
    public void softDelete(String usuario) {
        this.estado = false;
        this.fechaEliminacion = LocalDateTime.now();
        this.eliminadoPor = usuario;
    }

    /** Restaura una entidad eliminada. */
    public void restore() {
        this.estado = true;
        this.fechaEliminacion = null;
        this.eliminadoPor = null;
    }

    /** Verifica si la entidad está eliminada. */
    public boolean isDeleted() {
        return !Boolean.TRUE.equals(estado);
    }

    /** Verifica si la entidad está activa. */
    public boolean isActive() {
        return Boolean.TRUE.equals(estado);
    }

    // ==================== Equals & HashCode ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Base base = (Base) o;
        // Solo comparar por ID si ambos tienen ID (entidades persistidas)
        if (id != null && base.id != null) {
            return Objects.equals(id, base.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        // Usar una constante para entidades nuevas (sin ID)
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
