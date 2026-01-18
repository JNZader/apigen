package com.jnzader.apigen.core.application.service;

import com.jnzader.apigen.core.application.dto.pagination.CursorPageRequest;
import com.jnzader.apigen.core.application.dto.pagination.CursorPageResponse;
import com.jnzader.apigen.core.domain.entity.Base;
import com.jnzader.apigen.core.domain.event.EntityCreatedEvent;
import com.jnzader.apigen.core.domain.event.EntityDeletedEvent;
import com.jnzader.apigen.core.domain.event.EntityHardDeletedEvent;
import com.jnzader.apigen.core.domain.event.EntityRestoredEvent;
import com.jnzader.apigen.core.domain.event.EntityUpdatedEvent;
import com.jnzader.apigen.core.domain.repository.BaseRepository;
import com.jnzader.apigen.core.domain.specification.BaseSpecification;
import com.jnzader.apigen.core.infrastructure.util.BeanCopyUtils;
import com.jnzader.apigen.core.domain.exception.ResourceNotFoundException;
import com.jnzader.apigen.core.application.util.Result;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementación base para servicios genéricos que proporciona las operaciones CRUD estándar.
 * <p>
 * Características:
 * - Operaciones CRUD con soporte para Result pattern
 * - Soft delete y restauración
 * - Operaciones en batch con flush periódico
 * - Búsqueda con especificaciones JPA
 * - Logging integrado
 * - Caché opcional (habilitado por anotaciones)
 *
 * @param <E> El tipo de la entidad que extiende {@link Base}.
 * @param <I> El tipo del identificador de la entidad.
 */
public abstract class BaseServiceImpl<E extends Base, I extends Serializable> implements BaseService<E, I> {

    private static final Logger log = LoggerFactory.getLogger(BaseServiceImpl.class);
    private static final String ERROR_NOT_FOUND = "Entidad no encontrada con ID: ";
    private static final int BATCH_SIZE = 50;
    private static final int MAX_RESULTS_WITHOUT_PAGINATION = 1000;
    private static final int WARN_THRESHOLD = 500;
    private static final int MAX_BATCH_OPERATION_SIZE = 10000;

    protected final BaseRepository<E, I> baseRepository;
    protected final CacheEvictionService cacheEvictionService;
    protected final ApplicationEventPublisher eventPublisher;
    protected final AuditorAware<String> auditorAware;

    @PersistenceContext
    protected EntityManager entityManager;

    protected BaseServiceImpl(BaseRepository<E, I> baseRepository,
                              CacheEvictionService cacheEvictionService,
                              ApplicationEventPublisher eventPublisher,
                              AuditorAware<String> auditorAware) {
        this.baseRepository = baseRepository;
        this.cacheEvictionService = cacheEvictionService;
        this.eventPublisher = eventPublisher;
        this.auditorAware = auditorAware;
    }

    // ==================== Métodos internos (evitan self-invocation) ====================

    /**
     * Búsqueda interna por ID sin pasar por el proxy de Spring.
     * Usado por métodos de escritura que necesitan leer antes de modificar.
     */
    private Result<E, Exception> findByIdInternal(I id) {
        log.debug("Buscando {} con ID: {} (interno)", getEntityName(), id);
        return Result.fromOptional(
                baseRepository.findById(id),
                () -> new ResourceNotFoundException(ERROR_NOT_FOUND + id)
        );
    }

    /**
     * Guardado interno sin pasar por el proxy de Spring.
     * Usado por métodos que ya están dentro de una transacción.
     */
    private Result<E, Exception> saveInternal(E entity) {
        return Result.of(() -> {
            boolean isNew = entity.getId() == null;
            log.debug("{} entidad de tipo {} (interno)", isNew ? "Creando" : "Actualizando", getEntityName());

            E saved = baseRepository.save(entity);

            if (isNew) {
                saved.registerEvent(new EntityCreatedEvent<>(saved, saved.getCreadoPor()));
                log.info("Entidad {} creada con ID: {}", getEntityName(), saved.getId());
            } else {
                saved.registerEvent(new EntityUpdatedEvent<>(saved, saved.getModificadoPor()));
                log.info("Entidad {} actualizada con ID: {}", getEntityName(), saved.getId());
            }

            return saved;
        });
    }

    /**
     * Retorna el nombre de la entidad para mensajes de log y caché.
     * Este método es público para permitir acceso desde SpEL en anotaciones de caché.
     * <p>
     * Por defecto, deriva el nombre de {@link #getEntityClass()}.
     * Las subclases pueden sobrescribir si necesitan un nombre diferente.
     *
     * @return El nombre de la entidad (derivado de getEntityClass()).
     */
    public String getEntityName() {
        return getEntityClass().getSimpleName();
    }

    /**
     * Retorna la clase de la entidad manejada por este servicio.
     * Las subclases deben implementar este método para permitir operaciones
     * que requieren conocer el tipo de entidad en tiempo de ejecución.
     *
     * @return La clase de la entidad.
     */
    protected abstract Class<E> getEntityClass();

    /**
     * Obtiene el nombre de la tabla de base de datos para la entidad.
     * Usa la anotación @Table si está presente, de lo contrario usa el nombre de la clase.
     *
     * @return El nombre de la tabla.
     */
    protected String getTableName() {
        Class<E> entityClass = getEntityClass();
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
            return tableAnnotation.name();
        }
        // Convertir CamelCase a snake_case como convención de Hibernate
        return entityClass.getSimpleName()
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toLowerCase();
    }

    // ==================== Consultas básicas ====================

    @Override
    @Transactional(readOnly = true)
    public Result<List<E>, Exception> findAll() {
        return Result.of(() -> {
            log.debug("Buscando todas las entidades de tipo {}", getEntityName());

            // COUNT primero - más eficiente que traer datos para verificar
            long total = baseRepository.count();

            if (total == 0) {
                return List.of();
            }

            if (total > MAX_RESULTS_WITHOUT_PAGINATION) {
                log.warn("findAll() de {} tiene {} registros (limite: {}). " +
                                "Considere usar findAll(Pageable) para tablas grandes.",
                        getEntityName(), total, MAX_RESULTS_WITHOUT_PAGINATION);
            } else if (total > WARN_THRESHOLD) {
                log.info("findAll() de {} tiene {} registros. " +
                                "Considere usar paginacion para mejor rendimiento.",
                        getEntityName(), total);
            }

            // Traer solo hasta el límite
            int limit = (int) Math.min(total, MAX_RESULTS_WITHOUT_PAGINATION);
            return baseRepository.findAll(PageRequest.of(0, limit)).getContent();
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<List<E>, Exception> findAllActive() {
        return Result.of(() -> {
            log.debug("Buscando todas las entidades activas de tipo {}", getEntityName());

            // COUNT primero con especificación - más eficiente
            long total = baseRepository.count(BaseSpecification.isActive());

            if (total == 0) {
                return List.of();
            }

            if (total > MAX_RESULTS_WITHOUT_PAGINATION) {
                log.warn("findAllActive() de {} tiene {} registros (limite: {}). " +
                                "Considere usar findAllActive(Pageable) para tablas grandes.",
                        getEntityName(), total, MAX_RESULTS_WITHOUT_PAGINATION);
            } else if (total > WARN_THRESHOLD) {
                log.info("findAllActive() de {} tiene {} registros. " +
                                "Considere usar paginacion para mejor rendimiento.",
                        getEntityName(), total);
            }

            // Traer solo hasta el límite
            int limit = (int) Math.min(total, MAX_RESULTS_WITHOUT_PAGINATION);
            return baseRepository.findAll(BaseSpecification.isActive(),
                    PageRequest.of(0, limit)).getContent();
        });
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "lists",
            key = "#root.target.entityName + ':all:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort.toString()",
            unless = "#result.isFailure()")
    public Result<Page<E>, Exception> findAll(Pageable pageable) {
        return Result.of(() -> {
            log.debug("Buscando entidades de tipo {} con paginación: {}", getEntityName(), pageable);
            return baseRepository.findAll(pageable);
        });
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "lists",
            key = "#root.target.entityName + ':active:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort.toString()",
            unless = "#result.isFailure()")
    public Result<Page<E>, Exception> findAllActive(Pageable pageable) {
        return Result.of(() -> {
            log.debug("Buscando entidades activas de tipo {} con paginación: {}", getEntityName(), pageable);
            return baseRepository.findAll(BaseSpecification.isActive(), pageable);
        });
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "entities", key = "#root.target.entityName + ':' + #id", unless = "#result.isFailure()")
    public Result<E, Exception> findById(I id) {
        log.debug("Buscando {} con ID: {}", getEntityName(), id);
        return Result.fromOptional(
                baseRepository.findById(id),
                () -> new ResourceNotFoundException(ERROR_NOT_FOUND + id)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Boolean, Exception> existsById(I id) {
        return Result.of(() -> {
            log.debug("Verificando existencia de {} con ID: {}", getEntityName(), id);
            return baseRepository.existsById(id);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Long, Exception> count() {
        return Result.of(() -> {
            log.debug("Contando todas las entidades de tipo {}", getEntityName());
            return baseRepository.count();
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Long, Exception> countActive() {
        return Result.of(() -> {
            log.debug("Contando entidades activas de tipo {}", getEntityName());
            return baseRepository.count(BaseSpecification.isActive());
        });
    }

    // ==================== Búsqueda con especificaciones ====================

    @Override
    @Transactional(readOnly = true)
    public Result<List<E>, Exception> findAll(Specification<E> spec) {
        return Result.of(() -> {
            log.debug("Buscando entidades de tipo {} con especificación", getEntityName());
            return baseRepository.findAll(spec);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Page<E>, Exception> findAll(Specification<E> spec, Pageable pageable) {
        return Result.of(() -> {
            log.debug("Buscando entidades de tipo {} con especificación y paginación", getEntityName());
            return baseRepository.findAll(spec, pageable);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<E, Exception> findOne(Specification<E> spec) {
        log.debug("Buscando una entidad de tipo {} con especificación", getEntityName());
        return Result.fromOptional(
                baseRepository.findOne(spec),
                () -> new ResourceNotFoundException("No se encontró ninguna entidad que cumpla los criterios")
        );
    }

    // ==================== Operaciones de escritura ====================

    @Override
    @Transactional(noRollbackFor = Exception.class)
    public Result<E, Exception> save(E entity) {
        try {
            boolean isNew = entity.getId() == null;
            log.debug("{} entidad de tipo {}", isNew ? "Creando" : "Actualizando", getEntityName());

            E saved = baseRepository.save(entity);

            // Registrar evento de dominio después de guardar (para tener el ID asignado)
            if (isNew) {
                saved.registerEvent(new EntityCreatedEvent<>(saved, saved.getCreadoPor()));
                log.info("Entidad {} creada con ID: {}", getEntityName(), saved.getId());
            } else {
                saved.registerEvent(new EntityUpdatedEvent<>(saved, saved.getModificadoPor()));
                log.info("Entidad {} actualizada con ID: {}", getEntityName(), saved.getId());
            }

            return Result.success(saved);
        } catch (Exception e) {
            log.error("Error al guardar entidad {}: {}", getEntityName(), e.getMessage());
            return Result.failure(e);
        }
    }

    @Override
    @Transactional(timeout = 30)
    @CacheEvict(value = "entities", key = "#root.target.entityName + ':' + #id")
    public Result<E, Exception> update(I id, E entity) {
        log.debug("Actualizando {} con ID: {}", getEntityName(), id);
        return findByIdInternal(id).flatMap(existingEntity -> {
            // Type-safe ID assignment
            setEntityId(entity, id, existingEntity);
            entity.setVersion(existingEntity.getVersion());
            return saveInternal(entity).map(saved -> {
                // Eviction selectivo: solo listas de esta entidad
                cacheEvictionService.evictListsByEntityName(getEntityName());
                cacheEvictionService.evictCounts(getEntityName());
                return saved;
            });
        });
    }

    /**
     * Asigna el ID de forma type-safe a la entidad.
     * <p>
     * Soporta los siguientes tipos de ID:
     * - Long (más común)
     * - Integer y otros tipos numéricos (convertidos a Long)
     * <p>
     * Las subclases pueden sobrescribir si usan tipos de ID diferentes
     * o necesitan lógica de asignación personalizada.
     *
     * @param entity         La entidad a la que asignar el ID
     * @param id             El ID a asignar
     * @param existingEntity La entidad existente (para referencia y fallback)
     * @throws IllegalArgumentException si el ID es null
     */
    protected void setEntityId(E entity, I id, E existingEntity) {
        if (id == null) {
            throw new IllegalArgumentException("El ID no puede ser null para la operación de actualización");
        }

        switch (id) {
            case Long longId -> entity.setId(longId);
            case java.math.BigInteger bigInt
                    when bigInt.compareTo(java.math.BigInteger.valueOf(Long.MAX_VALUE)) > 0
                      || bigInt.compareTo(java.math.BigInteger.valueOf(Long.MIN_VALUE)) < 0 -> {
                log.error("Valor de BigInteger {} excede el rango de Long. Usando ID de entidad existente.", bigInt);
                entity.setId(existingEntity.getId());
            }
            case Number numberId -> {
                long longValue = numberId.longValue();
                entity.setId(longValue);
                if (log.isDebugEnabled()) {
                    log.debug("ID convertido de {} a Long: {} -> {}", id.getClass().getSimpleName(), id, longValue);
                }
            }
            default -> {
                entity.setId(existingEntity.getId());
                log.warn("Tipo de ID no soportado: {}. Usando ID de entidad existente: {}",
                        id.getClass().getName(), existingEntity.getId());
            }
        }
    }

    @Override
    @Transactional(timeout = 30)
    @CacheEvict(value = "entities", key = "#root.target.entityName + ':' + #id")
    public Result<E, Exception> partialUpdate(I id, E partialEntity) {
        log.debug("Actualizando parcialmente {} con ID: {}", getEntityName(), id);
        return findByIdInternal(id).flatMap(existingEntity -> {
            // Copiar solo campos no nulas del partialEntity al existingEntity
            copyNonNullProperties(partialEntity, existingEntity);
            return saveInternal(existingEntity).map(saved -> {
                // Eviction selectivo: solo listas de esta entidad
                cacheEvictionService.evictListsByEntityName(getEntityName());
                cacheEvictionService.evictCounts(getEntityName());
                return saved;
            });
        });
    }

    /**
     * Copia propiedades no nulas de la fuente al destino.
     * <p>
     * Utiliza {@link BeanCopyUtils} para copiar automáticamente todas las
     * propiedades no nulas, excluyendo propiedades de sistema como:
     * id, version, fechas de auditoría, etc.
     * <p>
     * Las subclases pueden sobrescribir este método si necesitan
     * un comportamiento personalizado (por ejemplo, usar MapStruct).
     *
     * @param source Entidad fuente con los valores a copiar
     * @param target Entidad destino donde se copiarán los valores
     */
    protected void copyNonNullProperties(E source, E target) {
        BeanCopyUtils.copyNonNullProperties(source, target);
    }

    // ==================== Soft Delete ====================

    @Override
    @Transactional(timeout = 30)
    @CacheEvict(value = "entities", key = "#root.target.entityName + ':' + #id")
    public Result<Void, Exception> softDelete(I id) {
        String usuario = auditorAware.getCurrentAuditor().orElse("system");
        return softDeleteInternal(id, usuario);
    }

    @Override
    @Transactional(timeout = 30)
    @CacheEvict(value = "entities", key = "#root.target.entityName + ':' + #id")
    public Result<Void, Exception> softDelete(I id, String usuario) {
        return softDeleteInternal(id, usuario);
    }

    /**
     * Implementación interna de soft delete para evitar self-invocation de métodos proxy.
     */
    private Result<Void, Exception> softDeleteInternal(I id, String usuario) {
        log.debug("Eliminando lógicamente {} con ID: {} por usuario: {}", getEntityName(), id, usuario);
        return findByIdInternal(id).flatMap(entity -> {
            entity.softDelete(usuario);
            return Result.of(() -> {
                E saved = baseRepository.save(entity);
                // Registrar evento de eliminación
                saved.registerEvent(new EntityDeletedEvent<>(saved, usuario));
                // Eviction selectivo: solo listas de esta entidad
                cacheEvictionService.evictListsByEntityName(getEntityName());
                cacheEvictionService.evictCounts(getEntityName());
                log.info("Entidad {} con ID: {} eliminada lógicamente", getEntityName(), id);
                return null;
            });
        });
    }

    @Override
    @Transactional(timeout = 30)
    @CacheEvict(value = "entities", key = "#root.target.entityName + ':' + #id")
    public Result<E, Exception> restore(I id) {
        log.debug("Restaurando {} con ID: {}", getEntityName(), id);

        return Result.of(() -> {
            // Usar native SQL para bypass de @SQLRestriction en Hibernate 6.2+
            // La anotación @SQLRestriction afecta a JPQL UPDATE/DELETE queries,
            // pero native SQL no se ve afectado por esta restricción
            String tableName = getTableName();
            String sql = "UPDATE " + tableName + " SET estado = true, fecha_eliminacion = NULL, eliminado_por = NULL WHERE id = :id";

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("id", id);
            int updated = query.executeUpdate();

            if (updated == 0) {
                throw new ResourceNotFoundException(ERROR_NOT_FOUND + id);
            }

            // Eviction selectivo: solo listas de esta entidad
            cacheEvictionService.evictListsByEntityName(getEntityName());
            cacheEvictionService.evictCounts(getEntityName());

            // Refresh del entity manager para que reconozca el cambio hecho por native SQL
            entityManager.flush();
            entityManager.clear();

            // Ahora la entidad está activa, podemos buscarla normalmente
            E restored = baseRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(ERROR_NOT_FOUND + id));

            // Registrar evento de restauración
            restored.registerEvent(new EntityRestoredEvent<>(restored));
            log.info("Entidad {} con ID: {} restaurada", getEntityName(), id);
            return restored;
        });
    }

    // ==================== Hard Delete ====================

    @Override
    @Transactional(timeout = 30)
    @CacheEvict(value = "entities", key = "#root.target.entityName + ':' + #id")
    public Result<Void, Exception> hardDelete(I id) {
        log.warn("Eliminando permanentemente {} con ID: {} - ¡ESTA OPERACIÓN ES IRREVERSIBLE!", getEntityName(), id);

        return Result.of(() -> {
            // hardDeleteById usa DELETE directo que no se ve afectado por @SQLRestriction
            int deleted = baseRepository.hardDeleteById(id);
            if (deleted == 0) {
                throw new ResourceNotFoundException(ERROR_NOT_FOUND + id);
            }

            // Publicar evento de eliminación permanente con información de la entidad eliminada
            eventPublisher.publishEvent(new EntityHardDeletedEvent<>(id, getEntityName()));

            // Eviction selectivo: solo listas de esta entidad
            cacheEvictionService.evictListsByEntityName(getEntityName());
            cacheEvictionService.evictCounts(getEntityName());
            log.info("Entidad {} con ID: {} eliminada permanentemente", getEntityName(), id);
            return null;
        });
    }

    // ==================== Operaciones en Batch ====================

    @Override
    @Transactional(timeout = 300) // 5 minutes for batch operations
    public Result<List<E>, Exception> saveAll(List<E> entities) {
        return Result.of(() -> {
            // Validar tamaño del batch
            validateBatchSize(entities.size(), "saveAll");

            log.debug("Guardando {} entidades de tipo {} en batch", entities.size(), getEntityName());

            if (entities.isEmpty()) {
                return new ArrayList<>();
            }

            List<E> allSavedEntities = new ArrayList<>(entities.size());

            // Procesar en lotes para evitar problemas de memoria con grandes volúmenes
            for (int i = 0; i < entities.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, entities.size());
                List<E> batch = entities.subList(i, end);

                // Usar saveAll nativo de JPA para batch insert optimizado
                List<E> savedBatch = baseRepository.saveAll(batch);
                allSavedEntities.addAll(savedBatch);

                // Flush y clear para liberar memoria del contexto de persistencia
                entityManager.flush();
                entityManager.clear();

                log.debug("Batch {} de {} procesado ({} entidades)",
                        (i / BATCH_SIZE) + 1,
                        (int) Math.ceil((double) entities.size() / BATCH_SIZE),
                        savedBatch.size());
            }

            // Eviction selectivo: solo caches de esta entidad (no afecta otras)
            cacheEvictionService.evictAll(getEntityName(), null);

            log.info("Guardadas {} entidades de tipo {} en batch", allSavedEntities.size(), getEntityName());
            return allSavedEntities;
        });
    }

    @Override
    @Transactional(timeout = 300) // 5 minutes for batch operations
    public Result<Integer, Exception> softDeleteAll(List<I> ids) {
        String usuario = auditorAware.getCurrentAuditor().orElse("system");
        return softDeleteAllInternal(ids, usuario);
    }

    @Override
    @Transactional(timeout = 300) // 5 minutes for batch operations
    public Result<Integer, Exception> softDeleteAll(List<I> ids, String usuario) {
        return softDeleteAllInternal(ids, usuario);
    }

    /**
     * Implementación interna de soft delete en batch para evitar self-invocation.
     */
    private Result<Integer, Exception> softDeleteAllInternal(List<I> ids, String usuario) {
        return Result.of(() -> {
            // Validar tamaño del batch
            validateBatchSize(ids.size(), "softDeleteAll");

            log.debug("Eliminando lógicamente {} entidades de tipo {}", ids.size(), getEntityName());

            if (ids.isEmpty()) {
                return 0;
            }

            // Usar operación bulk para evitar N+1 queries
            LocalDateTime now = LocalDateTime.now();
            int count = baseRepository.softDeleteAllByIds(ids, now, usuario);

            // Eviction selectivo: solo caches de esta entidad (no afecta otras)
            cacheEvictionService.evictAll(getEntityName(), null);

            log.info("Eliminadas lógicamente {} de {} entidades de tipo {}", count, ids.size(), getEntityName());
            return count;
        });
    }

    @Override
    @Transactional(timeout = 300) // 5 minutes for batch operations
    public Result<Integer, Exception> restoreAll(List<I> ids) {
        return Result.of(() -> {
            // Validar tamaño del batch
            validateBatchSize(ids.size(), "restoreAll");

            log.debug("Restaurando {} entidades de tipo {}", ids.size(), getEntityName());

            if (ids.isEmpty()) {
                return 0;
            }

            // Usar operación bulk para evitar N+1 queries
            int count = baseRepository.restoreAllByIds(ids);

            // Eviction selectivo: solo caches de esta entidad (no afecta otras)
            cacheEvictionService.evictAll(getEntityName(), null);

            log.info("Restauradas {} de {} entidades de tipo {}", count, ids.size(), getEntityName());
            return count;
        });
    }

    @Override
    @Transactional(timeout = 300) // 5 minutes for batch operations
    public Result<Integer, Exception> hardDeleteAll(List<I> ids) {
        return Result.of(() -> {
            // Validar tamaño del batch
            validateBatchSize(ids.size(), "hardDeleteAll");

            log.warn("Eliminando permanentemente {} entidades de tipo {} - ¡OPERACIÓN IRREVERSIBLE!",
                    ids.size(), getEntityName());

            if (ids.isEmpty()) {
                return 0;
            }

            // Usar deleteAllByIdInBatch para DELETE WHERE IN (más eficiente)
            baseRepository.deleteAllByIdInBatch(ids);

            // Eviction selectivo: solo caches de esta entidad (no afecta otras)
            cacheEvictionService.evictAll(getEntityName(), null);

            log.info("Eliminadas permanentemente {} entidades de tipo {}", ids.size(), getEntityName());
            return ids.size();
        });
    }

    // ==================== Métodos de validación ====================

    /**
     * Valida que el tamaño del batch no exceda el límite permitido.
     * Esto previene ataques de denegación de servicio y problemas de recursos.
     *
     * @param size          El tamaño del batch a validar.
     * @param operationName El nombre de la operación (para mensajes de error).
     * @throws IllegalArgumentException Si el tamaño excede el límite.
     */
    protected void validateBatchSize(int size, String operationName) {
        if (size > MAX_BATCH_OPERATION_SIZE) {
            String message = String.format(
                    "El tamaño del batch (%d) excede el límite permitido (%d) para la operación %s en %s",
                    size, MAX_BATCH_OPERATION_SIZE, operationName, getEntityName());
            log.warn(message);
            throw new IllegalArgumentException(message);
        }

        if (size > WARN_THRESHOLD) {
            log.info("Operación {} de {} con {} elementos - considere procesamiento por lotes más pequeños",
                    operationName, getEntityName(), size);
        }
    }

    // ==================== Cursor-based Pagination ====================

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("java:S6809")
    // S6809: La llamada via this es SEGURA aqui porque ambos metodos tienen
    // @Transactional(readOnly=true) identico - no hay cambio de comportamiento transaccional
    public Result<CursorPageResponse<E>, Exception> findAllWithCursor(CursorPageRequest request) {
        return findAllWithCursor((Specification<E>) null, request);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<CursorPageResponse<E>, Exception> findAllWithCursor(Specification<E> spec, CursorPageRequest request) {
        return Result.of(() -> {
            log.debug("Buscando entidades de tipo {} con cursor pagination: size={}, sortField={}, sortDirection={}",
                    getEntityName(), request.size(), request.sortField(), request.sortDirection());

            // Construir la especificación con el cursor
            Specification<E> cursorSpec = buildCursorSpecification(spec, request);

            // Determinar el orden
            Sort sort = buildSort(request);

            // Obtener size + 1 para saber si hay más elementos
            Pageable pageable = PageRequest.of(0, request.size() + 1, sort);
            List<E> results = baseRepository.findAll(cursorSpec, pageable).getContent();

            // Verificar si hay más elementos
            boolean hasNext = results.size() > request.size();
            if (hasNext) {
                results = results.subList(0, request.size());
            }

            // Verificar si hay elementos anteriores (solo si no es la primera página)
            boolean hasPrevious = !request.isFirstPage();

            // Construir cursores
            String nextCursor = null;
            String prevCursor = null;

            if (!results.isEmpty()) {
                E lastEntity = results.get(results.size() - 1);
                if (hasNext) {
                    nextCursor = buildCursor(lastEntity, request);
                }

                E firstEntity = results.get(0);
                if (hasPrevious) {
                    prevCursor = buildCursor(firstEntity, request);
                }
            }

            return CursorPageResponse.<E>builder()
                    .content(results)
                    .size(request.size())
                    .hasNext(hasNext)
                    .hasPrevious(hasPrevious)
                    .nextCursor(nextCursor)
                    .prevCursor(prevCursor)
                    .build();
        });
    }

    /**
     * Construye la especificación JPA para el cursor.
     */
    private Specification<E> buildCursorSpecification(Specification<E> baseSpec, CursorPageRequest request) {
        if (request.isFirstPage()) {
            return baseSpec != null ? baseSpec : (root, query, cb) -> cb.conjunction();
        }

        CursorPageRequest.DecodedCursor decoded = request.getDecodedCursor();
        if (decoded == null) {
            return baseSpec != null ? baseSpec : (root, query, cb) -> cb.conjunction();
        }

        // Especificación para "después del cursor"
        Specification<E> cursorSpec = (root, query, cb) -> {
            // Para ordenamiento DESC: id < lastId
            // Para ordenamiento ASC: id > lastId
            if (request.sortDirection() == CursorPageRequest.SortDirection.DESC) {
                return cb.lessThan(root.get("id"), decoded.lastId());
            } else {
                return cb.greaterThan(root.get("id"), decoded.lastId());
            }
        };

        return baseSpec != null ? baseSpec.and(cursorSpec) : cursorSpec;
    }

    /**
     * Construye el Sort para la consulta.
     */
    private Sort buildSort(CursorPageRequest request) {
        Sort.Direction direction = request.sortDirection() == CursorPageRequest.SortDirection.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        // Siempre ordenar por ID como campo secundario para garantizar consistencia
        if ("id".equals(request.sortField())) {
            return Sort.by(direction, "id");
        }

        return Sort.by(
                Sort.Order.by(request.sortField()).with(direction),
                Sort.Order.by("id").with(direction)
        );
    }

    /**
     * Construye el cursor codificado para una entidad.
     */
    private String buildCursor(E entity, CursorPageRequest request) {
        String sortValue = getSortFieldValue(entity, request.sortField());
        return CursorPageRequest.encodeCursor(
                entity.getId(),
                request.sortField(),
                sortValue,
                request.sortDirection()
        );
    }

    /**
     * Obtiene el valor del campo de ordenamiento de una entidad.
     */
    @SuppressWarnings("java:S3011")
    // S3011: setAccessible() es SEGURO aqui porque:
    // 1. Solo accede a campos de entidades propias del dominio (no codigo externo)
    // 2. Es necesario para cursor pagination con campos dinamicos
    // 3. El campo se obtiene de un sortField validado internamente
    private String getSortFieldValue(E entity, String sortField) {
        if ("id".equals(sortField)) {
            return entity.getId() != null ? entity.getId().toString() : null;
        }

        // Para otros campos, usar reflexión
        try {
            Field field = entity.getClass().getDeclaredField(sortField);
            field.setAccessible(true);
            Object value = field.get(entity);
            return value != null ? value.toString() : null;
        } catch (NoSuchFieldException | IllegalAccessException _) {
            log.warn("No se pudo obtener el valor del campo {} para cursor", sortField);
            return null;
        }
    }
}
