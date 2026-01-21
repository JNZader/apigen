package com.jnzader.apigen.core.application.service;

import com.jnzader.apigen.core.application.dto.pagination.CursorPageRequest;
import com.jnzader.apigen.core.application.dto.pagination.CursorPageResponse;
import com.jnzader.apigen.core.application.util.Result;
import com.jnzader.apigen.core.domain.entity.Base;
import com.jnzader.apigen.core.domain.event.EntityCreatedEvent;
import com.jnzader.apigen.core.domain.event.EntityDeletedEvent;
import com.jnzader.apigen.core.domain.event.EntityHardDeletedEvent;
import com.jnzader.apigen.core.domain.event.EntityRestoredEvent;
import com.jnzader.apigen.core.domain.event.EntityUpdatedEvent;
import com.jnzader.apigen.core.domain.exception.ResourceNotFoundException;
import com.jnzader.apigen.core.domain.repository.BaseRepository;
import com.jnzader.apigen.core.domain.specification.BaseSpecification;
import com.jnzader.apigen.core.infrastructure.util.BeanCopyUtils;
import com.jnzader.apigen.core.infrastructure.util.FieldAccessorCache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

/**
 * Base implementation for generic services providing standard CRUD operations.
 *
 * <p>Features: - CRUD operations with Result pattern support - Soft delete and restore - Batch
 * operations with periodic flush - JPA specification-based search - Integrated logging - Optional
 * cache (enabled via annotations)
 *
 * @param <E> The entity type extending {@link Base}.
 * @param <I> The entity identifier type.
 */
public abstract class BaseServiceImpl<E extends Base, I extends Serializable>
        implements BaseService<E, I> {

    private static final Logger log = LoggerFactory.getLogger(BaseServiceImpl.class);
    private static final String ERROR_NOT_FOUND = "Entity not found with ID: ";
    private static final int BATCH_SIZE = 50;
    private static final int MAX_RESULTS_WITHOUT_PAGINATION = 1000;
    private static final int WARN_THRESHOLD = 500;
    private static final int MAX_BATCH_OPERATION_SIZE = 10000;

    protected final BaseRepository<E, I> baseRepository;
    protected final CacheEvictionService cacheEvictionService;
    protected final ApplicationEventPublisher eventPublisher;
    protected final AuditorAware<String> auditorAware;

    @PersistenceContext protected EntityManager entityManager;

    protected BaseServiceImpl(
            BaseRepository<E, I> baseRepository,
            CacheEvictionService cacheEvictionService,
            ApplicationEventPublisher eventPublisher,
            AuditorAware<String> auditorAware) {
        this.baseRepository = baseRepository;
        this.cacheEvictionService = cacheEvictionService;
        this.eventPublisher = eventPublisher;
        this.auditorAware = auditorAware;
    }

    // ==================== Internal methods (avoid self-invocation) ====================

    /**
     * Internal lookup by ID without going through Spring proxy. Used by write methods that need to
     * read before modifying.
     */
    private Result<E, Exception> findByIdInternal(I id) {
        log.debug("Finding {} with ID: {} (internal)", getEntityName(), id);
        return Result.fromOptional(
                baseRepository.findById(id),
                () -> new ResourceNotFoundException(ERROR_NOT_FOUND + id));
    }

    /**
     * Internal save without going through Spring proxy. Used by methods already within a
     * transaction.
     */
    private Result<E, Exception> saveInternal(E entity) {
        return Result.of(
                () -> {
                    boolean isNew = entity.getId() == null;
                    log.debug(
                            "{} entity of type {} (internal)",
                            isNew ? "Creating" : "Updating",
                            getEntityName());

                    E saved = baseRepository.save(entity);

                    if (isNew) {
                        saved.registerEvent(new EntityCreatedEvent<>(saved, saved.getCreadoPor()));
                        log.info("Entity {} created with ID: {}", getEntityName(), saved.getId());
                    } else {
                        saved.registerEvent(
                                new EntityUpdatedEvent<>(saved, saved.getModificadoPor()));
                        log.info("Entity {} updated with ID: {}", getEntityName(), saved.getId());
                    }

                    return saved;
                });
    }

    /**
     * Returns the entity name for log messages and cache. This method is public to allow access
     * from SpEL in cache annotations.
     *
     * <p>By default, derives the name from {@link #getEntityClass()}. Subclasses may override if
     * they need a different name.
     *
     * @return The entity name (derived from getEntityClass()).
     */
    public String getEntityName() {
        return getEntityClass().getSimpleName();
    }

    /**
     * Returns the entity class managed by this service. Subclasses must implement this method to
     * allow operations that require knowing the entity type at runtime.
     *
     * @return The entity class.
     */
    protected abstract Class<E> getEntityClass();

    /**
     * Gets the database table name for the entity. Uses the @Table annotation if present, otherwise
     * uses the class name.
     *
     * @return The table name.
     */
    protected String getTableName() {
        Class<E> entityClass = getEntityClass();
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
            return tableAnnotation.name();
        }
        // Convert CamelCase to snake_case as Hibernate convention
        return entityClass.getSimpleName().replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    // ==================== Basic queries ====================

    @Override
    @Transactional(readOnly = true)
    public Result<List<E>, Exception> findAll() {
        return Result.of(
                () -> {
                    log.debug("Finding all entities of type {}", getEntityName());

                    // COUNT first - more efficient than fetching data to verify
                    long total = baseRepository.count();

                    if (total == 0) {
                        return List.of();
                    }

                    if (total > MAX_RESULTS_WITHOUT_PAGINATION) {
                        log.warn(
                                "findAll() of {} has {} records (limit: {}). "
                                        + "Consider using findAll(Pageable) for large tables.",
                                getEntityName(),
                                total,
                                MAX_RESULTS_WITHOUT_PAGINATION);
                    } else if (total > WARN_THRESHOLD) {
                        log.info(
                                "findAll() of {} has {} records. "
                                        + "Consider using pagination for better performance.",
                                getEntityName(),
                                total);
                    }

                    // Fetch only up to the limit
                    int limit = (int) Math.min(total, MAX_RESULTS_WITHOUT_PAGINATION);
                    return baseRepository.findAll(PageRequest.of(0, limit)).getContent();
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<List<E>, Exception> findAllActive() {
        return Result.of(
                () -> {
                    log.debug("Finding all active entities of type {}", getEntityName());

                    // COUNT first with specification - more efficient
                    long total = baseRepository.count(BaseSpecification.isActive());

                    if (total == 0) {
                        return List.of();
                    }

                    if (total > MAX_RESULTS_WITHOUT_PAGINATION) {
                        log.warn(
                                "findAllActive() of {} has {} records (limit: {}). Consider"
                                        + " using findAllActive(Pageable) for large tables.",
                                getEntityName(),
                                total,
                                MAX_RESULTS_WITHOUT_PAGINATION);
                    } else if (total > WARN_THRESHOLD) {
                        log.info(
                                "findAllActive() of {} has {} records. "
                                        + "Consider using pagination for better performance.",
                                getEntityName(),
                                total);
                    }

                    // Fetch only up to the limit
                    int limit = (int) Math.min(total, MAX_RESULTS_WITHOUT_PAGINATION);
                    return baseRepository
                            .findAll(BaseSpecification.isActive(), PageRequest.of(0, limit))
                            .getContent();
                });
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            value = "lists",
            key =
                    "#root.target.entityName + ':all:' + #pageable.pageNumber + ':' +"
                            + " #pageable.pageSize + ':' + #pageable.sort.toString()",
            unless = "#result.isFailure()")
    public Result<Page<E>, Exception> findAll(Pageable pageable) {
        return Result.of(
                () -> {
                    log.debug(
                            "Finding entities of type {} with pagination: {}",
                            getEntityName(),
                            pageable);
                    return baseRepository.findAll(pageable);
                });
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            value = "lists",
            key =
                    "#root.target.entityName + ':active:' + #pageable.pageNumber + ':' +"
                            + " #pageable.pageSize + ':' + #pageable.sort.toString()",
            unless = "#result.isFailure()")
    public Result<Page<E>, Exception> findAllActive(Pageable pageable) {
        return Result.of(
                () -> {
                    log.debug(
                            "Finding active entities of type {} with pagination: {}",
                            getEntityName(),
                            pageable);
                    return baseRepository.findAll(BaseSpecification.isActive(), pageable);
                });
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            value = "entities",
            key = "#root.target.entityName + ':' + #id",
            unless = "#result.isFailure()")
    public Result<E, Exception> findById(I id) {
        log.debug("Finding {} with ID: {}", getEntityName(), id);
        return Result.fromOptional(
                baseRepository.findById(id),
                () -> new ResourceNotFoundException(ERROR_NOT_FOUND + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Boolean, Exception> existsById(I id) {
        return Result.of(
                () -> {
                    log.debug("Checking existence of {} with ID: {}", getEntityName(), id);
                    return baseRepository.existsById(id);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Long, Exception> count() {
        return Result.of(
                () -> {
                    log.debug("Counting all entities of type {}", getEntityName());
                    return baseRepository.count();
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Long, Exception> countActive() {
        return Result.of(
                () -> {
                    log.debug("Counting active entities of type {}", getEntityName());
                    return baseRepository.count(BaseSpecification.isActive());
                });
    }

    // ==================== Specification-based search ====================

    @Override
    @Transactional(readOnly = true)
    public Result<List<E>, Exception> findAll(Specification<E> spec) {
        return Result.of(
                () -> {
                    log.debug("Finding entities of type {} with specification", getEntityName());
                    return baseRepository.findAll(spec);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<Page<E>, Exception> findAll(Specification<E> spec, Pageable pageable) {
        return Result.of(
                () -> {
                    log.debug(
                            "Finding entities of type {} with specification and pagination",
                            getEntityName());
                    return baseRepository.findAll(spec, pageable);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Result<E, Exception> findOne(Specification<E> spec) {
        log.debug("Finding single entity of type {} with specification", getEntityName());
        return Result.fromOptional(
                baseRepository.findOne(spec),
                () -> new ResourceNotFoundException("No entity found matching the criteria"));
    }

    // ==================== Write operations ====================

    @Override
    @Transactional(noRollbackFor = Exception.class)
    public Result<E, Exception> save(E entity) {
        try {
            boolean isNew = entity.getId() == null;
            log.debug("{} entity of type {}", isNew ? "Creating" : "Updating", getEntityName());

            E saved = baseRepository.save(entity);

            // Register domain event after saving (to have the assigned ID)
            if (isNew) {
                saved.registerEvent(new EntityCreatedEvent<>(saved, saved.getCreadoPor()));
                log.info("Entity {} created with ID: {}", getEntityName(), saved.getId());
            } else {
                saved.registerEvent(new EntityUpdatedEvent<>(saved, saved.getModificadoPor()));
                log.info("Entity {} updated with ID: {}", getEntityName(), saved.getId());
            }

            return Result.success(saved);
        } catch (Exception e) {
            log.error("Error saving entity {}: {}", getEntityName(), e.getMessage());
            return Result.failure(e);
        }
    }

    @Override
    @Transactional(timeout = 30)
    @CacheEvict(value = "entities", key = "#root.target.entityName + ':' + #id")
    public Result<E, Exception> update(I id, E entity) {
        log.debug("Updating {} with ID: {}", getEntityName(), id);
        return findByIdInternal(id)
                .flatMap(
                        existingEntity -> {
                            // Type-safe ID assignment
                            setEntityId(entity, id, existingEntity);
                            entity.setVersion(existingEntity.getVersion());
                            return saveInternal(entity)
                                    .map(
                                            saved -> {
                                                // Selective eviction: only lists of this entity
                                                cacheEvictionService.evictListsByEntityName(
                                                        getEntityName());
                                                cacheEvictionService.evictCounts(getEntityName());
                                                return saved;
                                            });
                        });
    }

    /**
     * Assigns the ID in a type-safe manner to the entity.
     *
     * <p>Supports the following ID types: - Long (most common) - Integer and other numeric types
     * (converted to Long)
     *
     * <p>Subclasses may override if they use different ID types or need custom assignment logic.
     *
     * @param entity The entity to assign the ID to
     * @param id The ID to assign
     * @param existingEntity The existing entity (for reference and fallback)
     * @throws IllegalArgumentException if ID is null
     */
    protected void setEntityId(E entity, I id, E existingEntity) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null for update operation");
        }

        switch (id) {
            case Long longId -> entity.setId(longId);
            case java.math.BigInteger bigInt
                    when bigInt.compareTo(java.math.BigInteger.valueOf(Long.MAX_VALUE)) > 0
                            || bigInt.compareTo(java.math.BigInteger.valueOf(Long.MIN_VALUE))
                                    < 0 -> {
                log.error(
                        "BigInteger value {} exceeds Long range. Using existing entity ID.",
                        bigInt);
                entity.setId(existingEntity.getId());
            }
            case Number numberId -> {
                long longValue = numberId.longValue();
                entity.setId(longValue);
                if (log.isDebugEnabled()) {
                    log.debug(
                            "ID converted from {} to Long: {} -> {}",
                            id.getClass().getSimpleName(),
                            id,
                            longValue);
                }
            }
            default -> {
                entity.setId(existingEntity.getId());
                log.warn(
                        "Unsupported ID type: {}. Using existing entity ID: {}",
                        id.getClass().getName(),
                        existingEntity.getId());
            }
        }
    }

    @Override
    @Transactional(timeout = 30)
    @CacheEvict(value = "entities", key = "#root.target.entityName + ':' + #id")
    public Result<E, Exception> partialUpdate(I id, E partialEntity) {
        log.debug("Partially updating {} with ID: {}", getEntityName(), id);
        return findByIdInternal(id)
                .flatMap(
                        existingEntity -> {
                            // Copy only non-null fields from partialEntity to existingEntity
                            copyNonNullProperties(partialEntity, existingEntity);
                            return saveInternal(existingEntity)
                                    .map(
                                            saved -> {
                                                // Selective eviction: only lists of this entity
                                                cacheEvictionService.evictListsByEntityName(
                                                        getEntityName());
                                                cacheEvictionService.evictCounts(getEntityName());
                                                return saved;
                                            });
                        });
    }

    /**
     * Copies non-null properties from source to target.
     *
     * <p>Uses {@link BeanCopyUtils} to automatically copy all non-null properties, excluding system
     * properties like: id, version, audit dates, etc.
     *
     * <p>Subclasses may override this method if they need custom behavior (e.g., using MapStruct).
     *
     * @param source Source entity with values to copy
     * @param target Target entity where values will be copied
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

    /** Internal soft delete implementation to avoid self-invocation of proxy methods. */
    private Result<Void, Exception> softDeleteInternal(I id, String usuario) {
        log.debug("Soft deleting {} with ID: {} by user: {}", getEntityName(), id, usuario);
        return findByIdInternal(id)
                .flatMap(
                        entity -> {
                            entity.softDelete(usuario);
                            return Result.of(
                                    () -> {
                                        E saved = baseRepository.save(entity);
                                        // Register deletion event
                                        saved.registerEvent(
                                                new EntityDeletedEvent<>(saved, usuario));
                                        // Selective eviction: only lists of this entity
                                        cacheEvictionService.evictListsByEntityName(
                                                getEntityName());
                                        cacheEvictionService.evictCounts(getEntityName());
                                        log.info(
                                                "Entity {} with ID: {} soft deleted",
                                                getEntityName(),
                                                id);
                                        return null;
                                    });
                        });
    }

    @Override
    @Transactional(timeout = 30)
    @CacheEvict(value = "entities", key = "#root.target.entityName + ':' + #id")
    public Result<E, Exception> restore(I id) {
        log.debug("Restoring {} with ID: {}", getEntityName(), id);

        return Result.of(
                () -> {
                    // Use native SQL to bypass @SQLRestriction in Hibernate 6.2+
                    // The @SQLRestriction annotation affects JPQL UPDATE/DELETE queries,
                    // but native SQL is not affected by this restriction
                    String tableName = getTableName();
                    String sql =
                            "UPDATE "
                                    + tableName
                                    + " SET estado = true, fecha_eliminacion = NULL, eliminado_por"
                                    + " = NULL WHERE id = :id";

                    Query query = entityManager.createNativeQuery(sql);
                    query.setParameter("id", id);
                    int updated = query.executeUpdate();

                    if (updated == 0) {
                        throw new ResourceNotFoundException(ERROR_NOT_FOUND + id);
                    }

                    // Selective eviction: only lists of this entity
                    cacheEvictionService.evictListsByEntityName(getEntityName());
                    cacheEvictionService.evictCounts(getEntityName());

                    // Refresh entity manager to recognize the change made by native SQL
                    entityManager.flush();
                    entityManager.clear();

                    // Now the entity is active, we can find it normally
                    E restored =
                            baseRepository
                                    .findById(id)
                                    .orElseThrow(
                                            () ->
                                                    new ResourceNotFoundException(
                                                            ERROR_NOT_FOUND + id));

                    // Register restore event
                    restored.registerEvent(new EntityRestoredEvent<>(restored));
                    log.info("Entity {} with ID: {} restored", getEntityName(), id);
                    return restored;
                });
    }

    // ==================== Hard Delete ====================

    @Override
    @Transactional(timeout = 30)
    @CacheEvict(value = "entities", key = "#root.target.entityName + ':' + #id")
    public Result<Void, Exception> hardDelete(I id) {
        log.warn(
                "Permanently deleting {} with ID: {} - THIS OPERATION IS IRREVERSIBLE!",
                getEntityName(),
                id);

        return Result.of(
                () -> {
                    // hardDeleteById uses direct DELETE which is not affected by @SQLRestriction
                    int deleted = baseRepository.hardDeleteById(id);
                    if (deleted == 0) {
                        throw new ResourceNotFoundException(ERROR_NOT_FOUND + id);
                    }

                    // Publish permanent deletion event with deleted entity information
                    eventPublisher.publishEvent(new EntityHardDeletedEvent<>(id, getEntityName()));

                    // Selective eviction: only lists of this entity
                    cacheEvictionService.evictListsByEntityName(getEntityName());
                    cacheEvictionService.evictCounts(getEntityName());
                    log.info("Entity {} with ID: {} permanently deleted", getEntityName(), id);
                    return null;
                });
    }

    // ==================== Batch Operations ====================

    @Override
    @Transactional(timeout = 300) // 5 minutes for batch operations
    public Result<List<E>, Exception> saveAll(List<E> entities) {
        return Result.of(
                () -> {
                    // Validate batch size
                    validateBatchSize(entities.size(), "saveAll");

                    log.debug(
                            "Saving {} entities of type {} in batch",
                            entities.size(),
                            getEntityName());

                    if (entities.isEmpty()) {
                        return new ArrayList<>();
                    }

                    List<E> allSavedEntities = new ArrayList<>(entities.size());

                    // Process in batches to avoid memory issues with large volumes
                    for (int i = 0; i < entities.size(); i += BATCH_SIZE) {
                        int end = Math.min(i + BATCH_SIZE, entities.size());
                        List<E> batch = entities.subList(i, end);

                        // Use native JPA saveAll for optimized batch insert
                        List<E> savedBatch = baseRepository.saveAll(batch);
                        allSavedEntities.addAll(savedBatch);

                        // Flush and clear to release persistence context memory
                        entityManager.flush();
                        entityManager.clear();

                        log.debug(
                                "Batch {} of {} processed ({} entities)",
                                (i / BATCH_SIZE) + 1,
                                (int) Math.ceil((double) entities.size() / BATCH_SIZE),
                                savedBatch.size());
                    }

                    // Selective eviction: only caches of this entity (doesn't affect others)
                    cacheEvictionService.evictAll(getEntityName(), null);

                    log.info(
                            "Saved {} entities of type {} in batch",
                            allSavedEntities.size(),
                            getEntityName());
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

    /** Internal batch soft delete implementation to avoid self-invocation. */
    private Result<Integer, Exception> softDeleteAllInternal(List<I> ids, String usuario) {
        return Result.of(
                () -> {
                    // Validate batch size
                    validateBatchSize(ids.size(), "softDeleteAll");

                    log.debug("Soft deleting {} entities of type {}", ids.size(), getEntityName());

                    if (ids.isEmpty()) {
                        return 0;
                    }

                    // Use bulk operation to avoid N+1 queries
                    LocalDateTime now = LocalDateTime.now();
                    int count = baseRepository.softDeleteAllByIds(ids, now, usuario);

                    // Selective eviction: only caches of this entity (doesn't affect others)
                    cacheEvictionService.evictAll(getEntityName(), null);

                    log.info(
                            "Soft deleted {} of {} entities of type {}",
                            count,
                            ids.size(),
                            getEntityName());
                    return count;
                });
    }

    @Override
    @Transactional(timeout = 300) // 5 minutes for batch operations
    public Result<Integer, Exception> restoreAll(List<I> ids) {
        return Result.of(
                () -> {
                    // Validate batch size
                    validateBatchSize(ids.size(), "restoreAll");

                    log.debug("Restoring {} entities of type {}", ids.size(), getEntityName());

                    if (ids.isEmpty()) {
                        return 0;
                    }

                    // Use bulk operation to avoid N+1 queries
                    int count = baseRepository.restoreAllByIds(ids);

                    // Selective eviction: only caches of this entity (doesn't affect others)
                    cacheEvictionService.evictAll(getEntityName(), null);

                    log.info(
                            "Restored {} of {} entities of type {}",
                            count,
                            ids.size(),
                            getEntityName());
                    return count;
                });
    }

    @Override
    @Transactional(timeout = 300) // 5 minutes for batch operations
    public Result<Integer, Exception> hardDeleteAll(List<I> ids) {
        return Result.of(
                () -> {
                    // Validate batch size
                    validateBatchSize(ids.size(), "hardDeleteAll");

                    log.warn(
                            "Permanently deleting {} entities of type {} - IRREVERSIBLE OPERATION!",
                            ids.size(),
                            getEntityName());

                    if (ids.isEmpty()) {
                        return 0;
                    }

                    // Use deleteAllByIdInBatch for DELETE WHERE IN (more efficient)
                    baseRepository.deleteAllByIdInBatch(ids);

                    // Selective eviction: only caches of this entity (doesn't affect others)
                    cacheEvictionService.evictAll(getEntityName(), null);

                    log.info(
                            "Permanently deleted {} entities of type {}",
                            ids.size(),
                            getEntityName());
                    return ids.size();
                });
    }

    // ==================== Validation methods ====================

    /**
     * Validates that the batch size doesn't exceed the allowed limit. This prevents denial of
     * service attacks and resource issues.
     *
     * @param size The batch size to validate.
     * @param operationName The operation name (for error messages).
     * @throws IllegalArgumentException If the size exceeds the limit.
     */
    protected void validateBatchSize(int size, String operationName) {
        if (size > MAX_BATCH_OPERATION_SIZE) {
            String message =
                    String.format(
                            "Batch size (%d) exceeds allowed limit (%d) for operation %s on %s",
                            size, MAX_BATCH_OPERATION_SIZE, operationName, getEntityName());
            log.warn(message);
            throw new IllegalArgumentException(message);
        }

        if (size > WARN_THRESHOLD) {
            log.info(
                    "Operation {} on {} with {} elements - consider smaller batch processing",
                    operationName,
                    getEntityName(),
                    size);
        }
    }

    // ==================== Cursor-based Pagination ====================

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("java:S6809")
    // S6809: The call via this is SAFE here because both methods have identical
    // @Transactional(readOnly=true) - no change in transactional behavior
    public Result<CursorPageResponse<E>, Exception> findAllWithCursor(CursorPageRequest request) {
        return findAllWithCursor((Specification<E>) null, request);
    }

    @Override
    @Transactional(readOnly = true)
    public Result<CursorPageResponse<E>, Exception> findAllWithCursor(
            Specification<E> spec, CursorPageRequest request) {
        return Result.of(
                () -> {
                    log.debug(
                            "Finding entities of type {} with cursor pagination: size={},"
                                    + " sortField={}, sortDirection={}",
                            getEntityName(),
                            request.size(),
                            request.sortField(),
                            request.sortDirection());

                    // Build the specification with the cursor
                    Specification<E> cursorSpec = buildCursorSpecification(spec, request);

                    // Determine the order
                    Sort sort = buildSort(request);

                    // Get size + 1 to know if there are more elements
                    Pageable pageable = PageRequest.of(0, request.size() + 1, sort);
                    List<E> results = baseRepository.findAll(cursorSpec, pageable).getContent();

                    // Check if there are more elements
                    boolean hasNext = results.size() > request.size();
                    if (hasNext) {
                        results = results.subList(0, request.size());
                    }

                    // Check if there are previous elements (only if not the first page)
                    boolean hasPrevious = !request.isFirstPage();

                    // Build cursors
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

    /** Builds the JPA specification for the cursor. */
    private Specification<E> buildCursorSpecification(
            Specification<E> baseSpec, CursorPageRequest request) {
        if (request.isFirstPage()) {
            return baseSpec != null ? baseSpec : (root, query, cb) -> cb.conjunction();
        }

        CursorPageRequest.DecodedCursor decoded = request.getDecodedCursor();
        if (decoded == null) {
            return baseSpec != null ? baseSpec : (root, query, cb) -> cb.conjunction();
        }

        // Specification for "after the cursor"
        Specification<E> cursorSpec =
                (root, query, cb) -> {
                    // For DESC ordering: id < lastId
                    // For ASC ordering: id > lastId
                    if (request.sortDirection() == CursorPageRequest.SortDirection.DESC) {
                        return cb.lessThan(root.get("id"), decoded.lastId());
                    } else {
                        return cb.greaterThan(root.get("id"), decoded.lastId());
                    }
                };

        return baseSpec != null ? baseSpec.and(cursorSpec) : cursorSpec;
    }

    /** Builds the Sort for the query. */
    private Sort buildSort(CursorPageRequest request) {
        Sort.Direction direction =
                request.sortDirection() == CursorPageRequest.SortDirection.DESC
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;

        // Always sort by ID as secondary field to guarantee consistency
        if ("id".equals(request.sortField())) {
            return Sort.by(direction, "id");
        }

        return Sort.by(
                Sort.Order.by(request.sortField()).with(direction),
                Sort.Order.by("id").with(direction));
    }

    /** Builds the encoded cursor for an entity. */
    private String buildCursor(E entity, CursorPageRequest request) {
        String sortValue = getSortFieldValue(entity, request.sortField());
        return CursorPageRequest.encodeCursor(
                entity.getId(), request.sortField(), sortValue, request.sortDirection());
    }

    /**
     * Gets the sort field value from an entity.
     *
     * <p>Uses {@link FieldAccessorCache} for optimized access with cached MethodHandles, avoiding
     * direct reflection overhead (~90% less overhead).
     */
    private String getSortFieldValue(E entity, String sortField) {
        Object value = FieldAccessorCache.getFieldValue(entity, sortField);
        return value != null ? value.toString() : null;
    }
}
