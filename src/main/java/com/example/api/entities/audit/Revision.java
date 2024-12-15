package com.example.api.entities.audit;

import com.example.api.config.CustomRevisionListener;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import java.io.Serializable;
import java.util.Date;

/**
 * La clase Revision representa una entidad que almacena información sobre
 * las revisiones de las entidades auditadas en la aplicación. Utiliza
 * Hibernate Envers para manejar la auditoría de los cambios.
 */
@Entity
@Table(name = "REVISION_INFO")
@RevisionEntity(CustomRevisionListener.class)
@Data
public class Revision implements Serializable {

   private static final long serialVersionUID = 1L;

   /**
    * El identificador único de la revisión. Se genera automáticamente
    * utilizando una secuencia.
    */
   @Id
   @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "revision_seq")
   @SequenceGenerator(
         name = "revision_seq",
         sequenceName = "rbac.seq_revision_id"
   )
   @RevisionNumber
   private int id;

   /**
    * La fecha y hora en que se realizó la revisión.
    */
   @Column(name = "REVISION_DATE")
   @Temporal(TemporalType.TIMESTAMP)
   @RevisionTimestamp
   private Date date;
}
