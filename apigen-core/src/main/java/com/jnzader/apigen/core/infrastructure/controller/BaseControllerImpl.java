package com.jnzader.apigen.core.infrastructure.controller;

import com.jnzader.apigen.core.application.dto.BaseDTO;
import com.jnzader.apigen.core.application.dto.pagination.CursorPageRequest;
import com.jnzader.apigen.core.application.dto.pagination.CursorPageResponse;
import com.jnzader.apigen.core.application.mapper.BaseMapper;
import com.jnzader.apigen.core.application.service.BaseService;
import com.jnzader.apigen.core.domain.entity.Base;
import com.jnzader.apigen.core.domain.exception.IdMismatchException;
import com.jnzader.apigen.core.domain.exception.OperationFailedException;
import com.jnzader.apigen.core.domain.exception.PreconditionFailedException;
import com.jnzader.apigen.core.domain.specification.FilterSpecificationBuilder;
import com.jnzader.apigen.core.infrastructure.hateoas.BaseResourceAssembler;
import com.jnzader.apigen.core.infrastructure.util.ETagGenerator;
import com.jnzader.apigen.core.infrastructure.util.FieldAccessorCache;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.io.Serializable;
import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Implementación base para controladores REST genéricos que gestionan operaciones CRUD.
 *
 * <p>Características RESTful implementadas: - Location header en POST (201 Created) - ETag para
 * cache condicional (If-None-Match → 304) - Concurrencia optimista (If-Match → 412) - Validación de
 * ID en PUT - HATEOAS en respuestas - Filtrado dinámico via query params - Sparse fieldsets
 * (selección de campos)
 *
 * <p>Documentación OpenAPI integrada.
 *
 * @param <E> El tipo de la entidad que extiende {@link Base}.
 * @param <D> El tipo del DTO que extiende {@link BaseDTO}.
 * @param <I> El tipo del identificador de la entidad.
 */
public abstract class BaseControllerImpl<E extends Base, D extends BaseDTO, I extends Serializable>
        implements BaseController<D, I> {

    private static final Logger log = LoggerFactory.getLogger(BaseControllerImpl.class);

    protected final BaseService<E, I> baseService;
    protected final BaseMapper<E, D> baseMapper;
    protected final BaseResourceAssembler<D, I> resourceAssembler;
    protected final FilterSpecificationBuilder filterBuilder;

    protected BaseControllerImpl(
            BaseService<E, I> baseService,
            BaseMapper<E, D> baseMapper,
            BaseResourceAssembler<D, I> resourceAssembler,
            FilterSpecificationBuilder filterBuilder) {
        this.baseService = baseService;
        this.baseMapper = baseMapper;
        this.resourceAssembler = resourceAssembler;
        this.filterBuilder = filterBuilder;
    }

    /** Constructor para compatibilidad con código existente (sin HATEOAS ni filtros). */
    protected BaseControllerImpl(BaseService<E, I> baseService, BaseMapper<E, D> baseMapper) {
        this.baseService = baseService;
        this.baseMapper = baseMapper;
        this.resourceAssembler = null;
        this.filterBuilder = new FilterSpecificationBuilder();
    }

    /** Constructor con HATEOAS pero sin filtros inyectados. */
    protected BaseControllerImpl(
            BaseService<E, I> baseService,
            BaseMapper<E, D> baseMapper,
            BaseResourceAssembler<D, I> resourceAssembler) {
        this.baseService = baseService;
        this.baseMapper = baseMapper;
        this.resourceAssembler = resourceAssembler;
        this.filterBuilder = new FilterSpecificationBuilder();
    }

    /** Retorna el nombre del recurso para mensajes de log. */
    protected String getResourceName() {
        return "Resource";
    }

    // ==================== GET / - Listar ====================

    /**
     * Retorna la clase de la entidad para el filtrado dinámico. Las subclases deben sobrescribir
     * este método si quieren usar filtrado avanzado.
     */
    @SuppressWarnings("unchecked")
    protected Class<E> getEntityClass() {
        return (Class<E>) Base.class;
    }

    @Override
    @GetMapping("")
    @Operation(
            summary = "Listar recursos",
            description =
                    """
                    Lista recursos con paginación, filtrado dinámico y selección de campos.

                    **Filtrado dinámico:** Use el parámetro `filter` con formato: `campo:operador:valor,campo2:op2:valor2`

                    **Operadores soportados:**
                    - `eq`: Igual (=)
                    - `neq`: No igual (!=)
                    - `like`: Contiene (LIKE %value%)
                    - `starts`: Empieza con
                    - `ends`: Termina con
                    - `gt/gte`: Mayor / Mayor o igual
                    - `lt/lte`: Menor / Menor o igual
                    - `in`: En lista (valores separados por ;)
                    - `between`: Entre dos valores (v1;v2)
                    - `null/notnull`: Es nulo / No es nulo

                    **Ejemplos:**
                    - `?filter=nombre:like:Juan`
                    - `?filter=edad:gte:18,estado:eq:true`
                    - `?filter=fechaCreacion:between:2024-01-01;2024-12-31`
                    """)
    @ApiResponse(
            responseCode = "200",
            description = "Lista obtenida exitosamente",
            headers = @Header(name = "X-Total-Count", description = "Total de elementos"))
    @ApiResponse(responseCode = "400", description = "Parámetros inválidos")
    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    public ResponseEntity<?> findAll(
            @Parameter(description = "Filtros dinámicos en formato: campo:operador:valor")
                    @RequestParam(required = false)
                    String filter,
            @Parameter(description = "Filtros simples como query params (campo=valor)")
                    @RequestParam(required = false)
                    Map<String, String> filters,
            @Parameter(description = "Campos a incluir (sparse fieldsets)")
                    @RequestParam(required = false)
                    Set<String> fields,
            @Parameter(description = "Configuración de paginación") Pageable pageable) {

        log.debug(
                "GET {} - findAll filter={}, filters={}, fields={}, pageable={}",
                getResourceName(),
                filter,
                filters,
                fields,
                pageable);

        validatePaginationParams(filters);
        Specification<E> spec = buildSpecification(filter, filters);

        return baseService
                .findAll(spec, pageable)
                .fold(entitiesPage -> buildPageResponse(entitiesPage, fields), this::handleFailure);
    }

    private void validatePaginationParams(Map<String, String> filters) {
        if (filters == null) return;

        validatePageParam(filters.get("page"));
        validateSizeParam(filters.get("size"));
    }

    private void validatePageParam(String pageParam) {
        if (pageParam == null) return;
        try {
            int page = Integer.parseInt(pageParam);
            if (page < 0) {
                throw new IllegalArgumentException("El número de página no puede ser negativo");
            }
        } catch (NumberFormatException _) {
            throw new IllegalArgumentException("El número de página debe ser un entero válido");
        }
    }

    private void validateSizeParam(String sizeParam) {
        if (sizeParam == null) return;
        try {
            int size = Integer.parseInt(sizeParam);
            if (size < 1) {
                throw new IllegalArgumentException("El tamaño de página debe ser al menos 1");
            }
        } catch (NumberFormatException _) {
            throw new IllegalArgumentException("El tamaño de página debe ser un entero válido");
        }
    }

    private Specification<E> buildSpecification(String filter, Map<String, String> filters) {
        Specification<E> spec = Specification.where((root, query, cb) -> cb.conjunction());

        if (filter != null && !filter.isBlank()) {
            spec = spec.and(filterBuilder.build(filter, getEntityClass()));
        }
        if (filters != null && !filters.isEmpty()) {
            spec = spec.and(filterBuilder.build(filters));
        }
        return spec;
    }

    private ResponseEntity<?> buildPageResponse(Page<E> entitiesPage, Set<String> fields) {
        Page<D> dtoPage = entitiesPage.map(baseMapper::toDTO);
        HttpHeaders headers = buildPaginationHeaders(entitiesPage);

        if (resourceAssembler != null) {
            PagedModel<EntityModel<D>> pagedModel = resourceAssembler.toPagedModel(dtoPage);
            return ResponseEntity.ok().headers(headers).body(pagedModel);
        }

        if (fields != null && !fields.isEmpty()) {
            return buildSparseFieldsetResponse(dtoPage, fields, headers);
        }

        return ResponseEntity.ok().headers(headers).body(dtoPage);
    }

    private HttpHeaders buildPaginationHeaders(Page<?> page) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(page.getTotalElements()));
        headers.add("X-Page-Number", String.valueOf(page.getNumber()));
        headers.add("X-Page-Size", String.valueOf(page.getSize()));
        headers.add("X-Total-Pages", String.valueOf(page.getTotalPages()));
        return headers;
    }

    private ResponseEntity<?> buildSparseFieldsetResponse(
            Page<D> dtoPage, Set<String> fields, HttpHeaders headers) {
        List<Map<String, Object>> filtered =
                dtoPage.getContent().stream().map(dto -> filterFields(dto, fields)).toList();
        return ResponseEntity.ok()
                .headers(headers)
                .body(
                        Map.of(
                                "content",
                                filtered,
                                "page",
                                Map.of(
                                        "number", dtoPage.getNumber(),
                                        "size", dtoPage.getSize(),
                                        "totalElements", dtoPage.getTotalElements(),
                                        "totalPages", dtoPage.getTotalPages())));
    }

    // ==================== HEAD / - Conteo ====================

    @Override
    @RequestMapping(method = RequestMethod.HEAD, value = "")
    @Operation(
            summary = "Obtener conteo",
            description = "Retorna el conteo total en el header X-Total-Count")
    @ApiResponse(
            responseCode = "200",
            description = "Conteo obtenido",
            headers = @Header(name = "X-Total-Count", description = "Total de elementos"))
    public ResponseEntity<Void> count(
            @Parameter(description = "Filtros para el conteo") @RequestParam(required = false)
                    Map<String, String> filters) {

        log.debug("HEAD {} - count", getResourceName());

        return baseService
                .count()
                .fold(
                        count ->
                                ResponseEntity.ok()
                                        .header("X-Total-Count", String.valueOf(count))
                                        .build(),
                        error -> {
                            handleFailure(error);
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                        });
    }

    // ==================== GET /{id} - Obtener por ID ====================

    @Override
    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener por ID",
            description = "Obtiene un recurso por su ID. Soporta ETag para cache condicional.")
    @ApiResponse(
            responseCode = "200",
            description = "Recurso encontrado",
            headers = @Header(name = "ETag", description = "Tag de versión para cache"))
    @ApiResponse(responseCode = "304", description = "No modificado (cache válido)")
    @ApiResponse(responseCode = "404", description = "Recurso no encontrado")
    public ResponseEntity<?> findById(
            @Parameter(description = "ID del recurso") @PathVariable I id,
            @Parameter(description = "Campos a incluir") @RequestParam(required = false)
                    Set<String> fields,
            @Parameter(description = "ETag para validación de cache")
                    @RequestHeader(value = "If-None-Match", required = false)
                    String ifNoneMatch) {

        log.debug("GET {} - findById: {}", getResourceName(), id);

        return baseService
                .findById(id)
                .fold(
                        entity -> {
                            D dto = baseMapper.toDTO(entity);
                            String etag = ETagGenerator.generate(dto);

                            // Verificar cache condicional
                            if (etag != null
                                    && ETagGenerator.matchesIfNoneMatch(etag, ifNoneMatch)) {
                                log.debug(
                                        "ETag match for {} {}, returning 304",
                                        getResourceName(),
                                        id);
                                return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                                        .eTag(etag)
                                        .build();
                            }

                            // Construir respuesta
                            ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok();
                            if (etag != null) {
                                responseBuilder.eTag(etag);
                            }

                            // Agregar Last-Modified si la entidad tiene fecha de actualización
                            if (entity.getFechaActualizacion() != null) {
                                ZonedDateTime lastModified =
                                        entity.getFechaActualizacion().atZone(ZoneOffset.UTC);
                                responseBuilder.lastModified(lastModified);
                            }

                            // Aplicar HATEOAS si está disponible
                            if (resourceAssembler != null) {
                                return responseBuilder.body(resourceAssembler.toModel(dto));
                            }

                            // Aplicar sparse fieldsets
                            if (fields != null && !fields.isEmpty()) {
                                return responseBuilder.body(filterFields(dto, fields));
                            }

                            return responseBuilder.body(dto);
                        },
                        this::handleFailure);
    }

    // ==================== HEAD /{id} - Verificar existencia ====================

    @Override
    @RequestMapping(method = RequestMethod.HEAD, value = "/{id}")
    @Operation(
            summary = "Verificar existencia",
            description = "Verifica si existe un recurso con el ID dado")
    @ApiResponse(responseCode = "200", description = "El recurso existe")
    @ApiResponse(responseCode = "404", description = "El recurso no existe")
    public ResponseEntity<Void> existsById(
            @Parameter(description = "ID del recurso") @PathVariable I id) {

        log.debug("HEAD {} - existsById: {}", getResourceName(), id);

        return baseService
                .existsById(id)
                .fold(
                        exists ->
                                Boolean.TRUE.equals(exists)
                                        ? ResponseEntity.ok().build()
                                        : ResponseEntity.notFound().build(),
                        error -> {
                            handleFailure(error);
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                        });
    }

    // ==================== POST / - Crear ====================

    @Override
    @PostMapping("")
    @Operation(
            summary = "Crear recurso",
            description =
                    "Crea un nuevo recurso. Retorna Location header con URI del recurso creado.")
    @ApiResponse(
            responseCode = "201",
            description = "Recurso creado exitosamente",
            headers = {
                @Header(name = "Location", description = "URI del recurso creado"),
                @Header(name = "ETag", description = "Tag de versión")
            })
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "409", description = "Conflicto (recurso duplicado)")
    public ResponseEntity<?> save(
            @Parameter(description = "Datos del nuevo recurso") @Valid @RequestBody D dto) {

        log.debug("POST {} - save", getResourceName());

        E entity = baseMapper.toEntity(dto);
        return baseService
                .save(entity)
                .fold(
                        savedEntity -> {
                            D savedDto = baseMapper.toDTO(savedEntity);

                            // Construir Location URI
                            URI location =
                                    ServletUriComponentsBuilder.fromCurrentRequest()
                                            .path("/{id}")
                                            .buildAndExpand(savedEntity.getId())
                                            .toUri();

                            // Generar ETag
                            String etag = ETagGenerator.generate(savedDto);

                            log.info(
                                    "Creado {} con ID: {} en {}",
                                    getResourceName(),
                                    savedEntity.getId(),
                                    location);

                            ResponseEntity.BodyBuilder responseBuilder =
                                    ResponseEntity.created(location);
                            if (etag != null) {
                                responseBuilder.eTag(etag);
                            }

                            // Aplicar HATEOAS
                            if (resourceAssembler != null) {
                                return responseBuilder.body(resourceAssembler.toModel(savedDto));
                            }

                            return responseBuilder.body(savedDto);
                        },
                        this::handleFailure);
    }

    // ==================== PUT /{id} - Actualizar completo ====================

    @Override
    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar recurso",
            description =
                    "Actualiza completamente un recurso. Valida ID y soporta concurrencia optimista"
                            + " con If-Match.")
    @ApiResponse(
            responseCode = "200",
            description = "Recurso actualizado",
            headers = @Header(name = "ETag", description = "Nuevo tag de versión"))
    @ApiResponse(responseCode = "400", description = "ID no coincide o datos inválidos")
    @ApiResponse(responseCode = "404", description = "Recurso no encontrado")
    @ApiResponse(responseCode = "412", description = "Precondición fallida (ETag no coincide)")
    public ResponseEntity<?> update(
            @Parameter(description = "ID del recurso") @PathVariable I id,
            @Parameter(description = "Datos actualizados") @Valid @RequestBody D dto,
            @Parameter(description = "ETag para concurrencia optimista")
                    @RequestHeader(value = "If-Match", required = false)
                    String ifMatch) {

        log.debug("PUT {} - update: {}", getResourceName(), id);

        // Validar que el ID del path coincide con el del DTO
        if (dto.id() != null && !dto.id().equals(extractIdAsLong(id))) {
            throw new IdMismatchException(id, dto.id());
        }

        // Verificar concurrencia optimista si se proporciona If-Match
        if (ifMatch != null && !ifMatch.isBlank()) {
            return baseService
                    .findById(id)
                    .fold(
                            existingEntity -> {
                                D existingDto = baseMapper.toDTO(existingEntity);
                                String currentEtag = ETagGenerator.generate(existingDto);

                                if (!ETagGenerator.matchesIfMatch(currentEtag, ifMatch)) {
                                    throw PreconditionFailedException.etagMismatch(
                                            currentEtag, ifMatch);
                                }

                                return performUpdate(id, dto);
                            },
                            this::handleFailure);
        }

        return performUpdate(id, dto);
    }

    private ResponseEntity<?> performUpdate(I id, D dto) {
        E entity = baseMapper.toEntity(dto);
        return baseService
                .update(id, entity)
                .fold(
                        updatedEntity -> {
                            D updatedDto = baseMapper.toDTO(updatedEntity);
                            String etag = ETagGenerator.generate(updatedDto);

                            log.info("Actualizado {} con ID: {}", getResourceName(), id);

                            ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok();
                            if (etag != null) {
                                responseBuilder.eTag(etag);
                            }

                            if (resourceAssembler != null) {
                                return responseBuilder.body(resourceAssembler.toModel(updatedDto));
                            }

                            return responseBuilder.body(updatedDto);
                        },
                        this::handleFailure);
    }

    // ==================== PATCH /{id} - Actualizar parcial ====================

    @Override
    @PatchMapping("/{id}")
    @Operation(
            summary = "Actualización parcial",
            description = "Actualiza parcialmente un recurso (solo campos proporcionados)")
    @ApiResponse(responseCode = "200", description = "Recurso actualizado")
    @ApiResponse(responseCode = "404", description = "Recurso no encontrado")
    @ApiResponse(responseCode = "412", description = "Precondición fallida")
    public ResponseEntity<?> partialUpdate(
            @Parameter(description = "ID del recurso") @PathVariable I id,
            @Parameter(description = "Campos a actualizar") @RequestBody D dto,
            @Parameter(description = "ETag para concurrencia optimista")
                    @RequestHeader(value = "If-Match", required = false)
                    String ifMatch) {

        log.debug("PATCH {} - partialUpdate: {}", getResourceName(), id);

        return baseService
                .findById(id)
                .fold(
                        existingEntity -> {
                            // Verificar concurrencia optimista
                            if (ifMatch != null && !ifMatch.isBlank()) {
                                D existingDto = baseMapper.toDTO(existingEntity);
                                String currentEtag = ETagGenerator.generate(existingDto);

                                if (!ETagGenerator.matchesIfMatch(currentEtag, ifMatch)) {
                                    throw PreconditionFailedException.etagMismatch(
                                            currentEtag, ifMatch);
                                }
                            }

                            // Actualizar solo campos no nulos
                            baseMapper.updateEntityFromDTO(dto, existingEntity);

                            return baseService
                                    .save(existingEntity)
                                    .fold(
                                            updatedEntity -> {
                                                D updatedDto = baseMapper.toDTO(updatedEntity);
                                                String etag = ETagGenerator.generate(updatedDto);

                                                log.info(
                                                        "Actualizado parcialmente {} con ID: {}",
                                                        getResourceName(),
                                                        id);

                                                ResponseEntity.BodyBuilder responseBuilder =
                                                        ResponseEntity.ok();
                                                if (etag != null) {
                                                    responseBuilder.eTag(etag);
                                                }

                                                if (resourceAssembler != null) {
                                                    return responseBuilder.body(
                                                            resourceAssembler.toModel(updatedDto));
                                                }

                                                return responseBuilder.body(updatedDto);
                                            },
                                            this::handleFailure);
                        },
                        this::handleFailure);
    }

    // ==================== DELETE /{id} - Eliminar ====================

    @Override
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Eliminar recurso",
            description =
                    "Elimina un recurso. Por defecto soft delete, con ?permanent=true hard delete.")
    @ApiResponse(responseCode = "204", description = "Recurso eliminado")
    @ApiResponse(responseCode = "404", description = "Recurso no encontrado")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID del recurso") @PathVariable I id,
            @Parameter(description = "Eliminar permanentemente")
                    @RequestParam(required = false, defaultValue = "false")
                    boolean permanent) {

        if (permanent) {
            log.warn("DELETE {} - hardDelete: {} - OPERACIÓN IRREVERSIBLE", getResourceName(), id);
            return baseService
                    .hardDelete(id)
                    .<ResponseEntity<Void>>fold(
                            success -> {
                                log.info(
                                        "Eliminado permanentemente {} con ID: {}",
                                        getResourceName(),
                                        id);
                                return ResponseEntity.noContent().build();
                            },
                            error -> {
                                handleFailure(error);
                                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .build();
                            });
        }

        log.debug("DELETE {} - softDelete: {}", getResourceName(), id);
        return baseService
                .softDelete(id)
                .<ResponseEntity<Void>>fold(
                        success -> {
                            log.info("Eliminado lógicamente {} con ID: {}", getResourceName(), id);
                            return ResponseEntity.noContent().build();
                        },
                        error -> {
                            handleFailure(error);
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                        });
    }

    @Override
    @PostMapping("/{id}/restore")
    @Operation(
            summary = "Restaurar recurso",
            description = "Restaura un recurso previamente eliminado con soft delete.")
    @ApiResponse(responseCode = "200", description = "Recurso restaurado")
    @ApiResponse(responseCode = "404", description = "Recurso no encontrado")
    public ResponseEntity<?> restore(
            @Parameter(description = "ID del recurso") @PathVariable I id) {

        log.debug("POST {} - restore: {}", getResourceName(), id);
        return baseService
                .restore(id)
                .fold(
                        entity -> {
                            D dto = baseMapper.toDTO(entity);
                            log.info("Restaurado {} con ID: {}", getResourceName(), id);
                            if (resourceAssembler != null) {
                                return ResponseEntity.ok(resourceAssembler.toModel(dto));
                            }
                            return ResponseEntity.ok(dto);
                        },
                        error -> {
                            handleFailure(error);
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                        });
    }

    // ==================== GET /cursor - Paginación por Cursor ====================

    @Override
    @GetMapping("/cursor")
    @Operation(
            summary = "Listar recursos con paginación por cursor",
            description =
                    """
                    Lista recursos usando paginación basada en cursor (keyset pagination).

                    **Ventajas sobre offset:**
                    - Performance constante O(1) vs O(n) de offset
                    - Sin duplicados/omisiones al insertar/eliminar datos
                    - Ideal para scroll infinito y feeds

                    **Uso:**
                    1. Primera página: `GET /cursor?size=20`
                    2. Siguiente página: `GET /cursor?cursor={nextCursor}&size=20`

                    **Respuesta incluye:**
                    - `content`: Lista de elementos
                    - `pageInfo.nextCursor`: Cursor para siguiente página (null si no hay más)
                    - `pageInfo.prevCursor`: Cursor para página anterior
                    - `pageInfo.hasNext`: Si hay más elementos
                    """)
    @ApiResponse(responseCode = "200", description = "Lista obtenida exitosamente")
    @ApiResponse(responseCode = "400", description = "Cursor inválido o parámetros incorrectos")
    public ResponseEntity<?> findAllWithCursor(
            @Parameter(description = "Cursor de la página anterior (Base64)")
                    @RequestParam(required = false)
                    String cursor,
            @Parameter(description = "Tamaño de página (1-100, default 20)")
                    @RequestParam(required = false, defaultValue = "20")
                    int size,
            @Parameter(description = "Campo de ordenamiento")
                    @RequestParam(required = false, defaultValue = "id")
                    String sort,
            @Parameter(description = "Dirección de ordenamiento")
                    @RequestParam(required = false, defaultValue = "DESC")
                    CursorPageRequest.SortDirection direction,
            @Parameter(description = "Filtros dinámicos en formato campo:operador:valor")
                    @RequestParam(required = false)
                    String filter) {

        log.debug(
                "GET {} - findAllWithCursor cursor={}, size={}, sort={}, direction={}",
                getResourceName(),
                cursor,
                size,
                sort,
                direction);

        CursorPageRequest request = new CursorPageRequest(cursor, size, sort, direction);

        // Construir especificación de filtrado si se proporciona
        Specification<E> spec = null;
        if (filter != null && !filter.isBlank()) {
            spec = filterBuilder.build(filter, getEntityClass());
        }

        return baseService
                .findAllWithCursor(spec, request)
                .fold(
                        cursorResponse -> {
                            // Convertir entidades a DTOs
                            CursorPageResponse<D> dtoResponse =
                                    CursorPageResponse.<D>builder()
                                            .content(
                                                    cursorResponse.content().stream()
                                                            .map(baseMapper::toDTO)
                                                            .toList())
                                            .size(cursorResponse.pageInfo().size())
                                            .hasNext(cursorResponse.pageInfo().hasNext())
                                            .hasPrevious(cursorResponse.pageInfo().hasPrevious())
                                            .nextCursor(cursorResponse.pageInfo().nextCursor())
                                            .prevCursor(cursorResponse.pageInfo().prevCursor())
                                            .build();

                            return ResponseEntity.ok(dtoResponse);
                        },
                        this::handleFailure);
    }

    // ==================== Métodos auxiliares ====================

    /** Maneja errores delegando al GlobalExceptionHandler. */
    @SuppressWarnings("java:S1452") // Wildcard necesario - método auxiliar genérico
    protected ResponseEntity<?> handleFailure(Exception error) {
        log.debug(
                "Delegando error de {} a GlobalExceptionHandler: {}",
                getResourceName(),
                error.getMessage());

        if (error instanceof RuntimeException runtimeEx) {
            throw runtimeEx;
        }
        throw new OperationFailedException(error.getMessage(), error);
    }

    /**
     * Filtra campos del DTO para sparse fieldsets.
     *
     * <p>Utiliza {@link FieldAccessorCache} para optimizar el acceso a campos mediante
     * MethodHandles cacheados, evitando la sobrecarga de reflexión en cada llamada.
     *
     * @param dto El objeto DTO del cual extraer campos
     * @param fields Set de nombres de campos a incluir
     * @return Mapa ordenado con los valores de los campos solicitados
     */
    protected Map<String, Object> filterFields(Object dto, Set<String> fields) {
        return FieldAccessorCache.getFieldValues(dto, fields);
    }

    /** Extrae el ID como Long para comparación. */
    protected Long extractIdAsLong(I id) {
        if (id instanceof Long longId) {
            return longId;
        }
        if (id instanceof Number number) {
            return number.longValue();
        }
        return null;
    }
}
