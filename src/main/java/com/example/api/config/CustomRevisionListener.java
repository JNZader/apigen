package com.example.api.config;

import com.example.api.entities.audit.Revision;
import org.hibernate.envers.RevisionListener;

/**
 * CustomRevisionListener es una implementación de la interfaz {@link RevisionListener}.
 * Este listener se invoca cada vez que se crea una nueva revisión en el sistema de auditoría.
 */
public class CustomRevisionListener implements RevisionListener {

   /**
    * Se llama cuando se crea una nueva revisión.
    *
    * @param revisionEntity la entidad de revisión que representa la nueva revisión.
    *                      Se espera que sea una instancia de {@link Revision}.
    */
   @Override
   public void newRevision(Object revisionEntity) {
      final Revision revision = (Revision) revisionEntity;
      // Aquí se puede agregar lógica adicional para manejar nuevas revisiones
   }
}
