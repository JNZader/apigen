package com.jnzader.apigen.core.domain.entity.audit;

import com.jnzader.apigen.core.infrastructure.config.CustomRevisionListener;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

/**
 * Entidad de revisión personalizada para Hibernate Envers, compatible con versiones modernas de
 * Hibernate.
 */
@Entity
@Table(name = "REVISION_INFO")
@RevisionEntity(CustomRevisionListener.class)
@Data
public class Revision implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @RevisionNumber
    private int id;

    @Column(name = "REVISION_DATE")
    @RevisionTimestamp
    private Date date;

    @Column(name = "USERNAME")
    private String username;
}
