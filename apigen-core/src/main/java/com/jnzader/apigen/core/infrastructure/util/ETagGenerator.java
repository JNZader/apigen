package com.jnzader.apigen.core.infrastructure.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnzader.apigen.core.domain.entity.Base;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilidad para generar ETags para recursos REST.
 *
 * <p>Los ETags se utilizan para: - Caché condicional (If-None-Match → 304 Not Modified) - Control
 * de concurrencia optimista (If-Match → 412 Precondition Failed)
 *
 * <p>El ETag se genera como un hash MD5 del contenido JSON del objeto.
 */
public final class ETagGenerator {

    private static final Logger log = LoggerFactory.getLogger(ETagGenerator.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private ETagGenerator() {
        // Utility class
    }

    /**
     * Genera un ETag para el objeto dado.
     *
     * <p>El ETag es un hash MD5 del JSON serializado del objeto, envuelto en comillas dobles según
     * RFC 7232.
     *
     * @param object El objeto para el que generar el ETag.
     * @return El ETag generado, o null si no se pudo generar.
     */
    @SuppressWarnings("java:S4790")
    // S4790: MD5 es SEGURO aquí porque:
    //   - NO es contexto criptográfico/seguridad
    //   - Solo genera fingerprint para cache HTTP (304 Not Modified)
    //   - No protege datos sensibles, solo detecta cambios en recursos
    //   - MD5 es rápido y suficiente para unicidad de cache
    public static String generate(Object object) {
        if (object == null) {
            return null;
        }

        try {
            String json = objectMapper.writeValueAsString(object);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(json.getBytes(StandardCharsets.UTF_8));
            String hash = HexFormat.of().formatHex(digest);
            return "\"" + hash + "\"";
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            log.warn("No se pudo generar ETag para el objeto: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Genera un ETag basado en versión para una entidad Base.
     *
     * <p>Este método ofrece complejidad O(1) vs O(n) del método {@link #generate(Object)} que
     * serializa a JSON y calcula MD5. Usa el formato {@code id:version} que es único por entidad y
     * cambio.
     *
     * <p>Ejemplo de ETag generado: "123:5" para entidad con id=123, version=5
     *
     * @param entity La entidad Base para la que generar el ETag.
     * @return El ETag generado, o null si la entidad o sus campos son null.
     */
    public static String generateFromVersion(Base entity) {
        if (entity == null || entity.getId() == null || entity.getVersion() == null) {
            return null;
        }
        return "\"" + entity.getId() + ":" + entity.getVersion() + "\"";
    }

    /**
     * Genera un ETag basado en id y versión.
     *
     * <p>Método de conveniencia para generar ETags O(1) sin necesidad de acceder a la entidad
     * completa.
     *
     * @param id El ID de la entidad.
     * @param version La versión de la entidad.
     * @return El ETag generado, o null si algún parámetro es null.
     */
    public static String generateFromVersion(Long id, Long version) {
        if (id == null || version == null) {
            return null;
        }
        return "\"" + id + ":" + version + "\"";
    }

    /**
     * Genera un ETag débil para el objeto dado.
     *
     * <p>Los ETags débiles (W/"...") indican equivalencia semántica, no igualdad byte-a-byte.
     *
     * @param object El objeto para el que generar el ETag débil.
     * @return El ETag débil generado, o null si no se pudo generar.
     */
    public static String generateWeak(Object object) {
        String strong = generate(object);
        return strong != null ? "W/" + strong : null;
    }

    /**
     * Genera un ETag débil basado en versión para una entidad Base.
     *
     * <p>Combina la eficiencia O(1) de {@link #generateFromVersion(Base)} con semántica de ETag
     * débil.
     *
     * @param entity La entidad Base para la que generar el ETag débil.
     * @return El ETag débil generado, o null si la entidad o sus campos son null.
     */
    public static String generateWeakFromVersion(Base entity) {
        String strong = generateFromVersion(entity);
        return strong != null ? "W/" + strong : null;
    }

    /**
     * Verifica si dos ETags coinciden.
     *
     * <p>Maneja correctamente ETags fuertes y débiles.
     *
     * @param etag1 Primer ETag.
     * @param etag2 Segundo ETag.
     * @return true si los ETags coinciden.
     */
    public static boolean matches(String etag1, String etag2) {
        if (etag1 == null || etag2 == null) {
            return false;
        }

        // Normalizar: quitar prefijo W/ para comparación
        String normalized1 = normalizeEtag(etag1);
        String normalized2 = normalizeEtag(etag2);

        return normalized1.equals(normalized2);
    }

    /**
     * Verifica si el ETag proporcionado coincide con alguno de los ETags en el header
     * If-None-Match.
     *
     * <p>El header If-None-Match puede contener múltiples ETags separados por comas, o el valor
     * especial "*" que coincide con cualquier ETag.
     *
     * @param currentEtag El ETag actual del recurso.
     * @param ifNoneMatch El valor del header If-None-Match.
     * @return true si hay coincidencia (y se debería retornar 304).
     */
    public static boolean matchesIfNoneMatch(String currentEtag, String ifNoneMatch) {
        if (currentEtag == null || ifNoneMatch == null || ifNoneMatch.isBlank()) {
            return false;
        }

        // "*" coincide con cualquier ETag
        if ("*".equals(ifNoneMatch.trim())) {
            return true;
        }

        // Puede haber múltiples ETags separados por coma
        String[] etags = ifNoneMatch.split(",");
        for (String etag : etags) {
            if (matches(currentEtag, etag.trim())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Verifica si el ETag proporcionado coincide con el header If-Match.
     *
     * <p>El header If-Match se usa para control de concurrencia optimista. Si no coincide, se debe
     * retornar 412 Precondition Failed.
     *
     * @param currentEtag El ETag actual del recurso.
     * @param ifMatch El valor del header If-Match.
     * @return true si hay coincidencia (y se puede proceder con la operación).
     */
    public static boolean matchesIfMatch(String currentEtag, String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            // Si no hay If-Match, se permite la operación
            return true;
        }

        if (currentEtag == null) {
            // Si el recurso no tiene ETag pero se requiere If-Match, falla
            return false;
        }

        // "*" coincide con cualquier ETag existente
        if ("*".equals(ifMatch.trim())) {
            return true;
        }

        // Puede haber múltiples ETags separados por coma
        String[] etags = ifMatch.split(",");
        for (String etag : etags) {
            if (matches(currentEtag, etag.trim())) {
                return true;
            }
        }

        return false;
    }

    /** Normaliza un ETag removiendo el prefijo W/ si existe. */
    private static String normalizeEtag(String etag) {
        if (etag == null) {
            return null;
        }
        String trimmed = etag.trim();
        if (trimmed.startsWith("W/")) {
            return trimmed.substring(2);
        }
        return trimmed;
    }
}
