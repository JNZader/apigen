package com.jnzader.apigen.core.infrastructure.util;

import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

/**
 * Utilidad para copiar propiedades entre beans de forma segura.
 *
 * <p>Proporciona métodos para copiar solo propiedades no nulas, útil para operaciones PATCH donde
 * solo se envían campos modificados.
 *
 * <p>Características:
 *
 * <ul>
 *   <li>Copia solo propiedades no nulas
 *   <li>Excluye automáticamente propiedades de sistema (id, version, etc.)
 *   <li>Permite especificar propiedades adicionales a ignorar
 *   <li>Thread-safe y sin estado
 * </ul>
 */
public final class BeanCopyUtils {

    private static final Logger log = LoggerFactory.getLogger(BeanCopyUtils.class);

    /**
     * Propiedades que nunca deben copiarse en actualizaciones parciales. Estas son propiedades de
     * infraestructura/auditoría que no deben ser modificadas directamente por el cliente.
     */
    private static final Set<String> ALWAYS_IGNORED_PROPERTIES =
            Set.of(
                    "id",
                    "version",
                    "class",
                    "fechaCreacion",
                    "fechaActualizacion",
                    "fechaEliminacion",
                    "creadoPor",
                    "modificadoPor",
                    "eliminadoPor",
                    "domainEvents");

    private BeanCopyUtils() {
        // Utility class
    }

    /**
     * Copia propiedades no nulas de source a target.
     *
     * <p>Las propiedades de sistema (id, version, fechas de auditoría) son automáticamente
     * excluidas para prevenir modificaciones accidentales.
     *
     * @param source Objeto fuente con los valores a copiar
     * @param target Objeto destino donde se copiarán los valores
     * @param <T> Tipo del objeto
     */
    public static <T> void copyNonNullProperties(T source, T target) {
        copyNonNullProperties(source, target, new String[0]);
    }

    /**
     * Copia propiedades no nulas de source a target, excluyendo propiedades adicionales.
     *
     * @param source Objeto fuente con los valores a copiar
     * @param target Objeto destino donde se copiarán los valores
     * @param additionalIgnored Propiedades adicionales a ignorar (además de las de sistema)
     * @param <T> Tipo del objeto
     */
    public static <T> void copyNonNullProperties(T source, T target, String... additionalIgnored) {
        if (source == null || target == null) {
            return;
        }

        BeanWrapper sourceWrapper = new BeanWrapperImpl(source);
        BeanWrapper targetWrapper = new BeanWrapperImpl(target);

        Set<String> ignoredProperties = new HashSet<>(ALWAYS_IGNORED_PROPERTIES);
        ignoredProperties.addAll(Arrays.asList(additionalIgnored));

        PropertyDescriptor[] pds = sourceWrapper.getPropertyDescriptors();
        int copiedCount = 0;

        for (PropertyDescriptor pd : pds) {
            String propertyName = pd.getName();

            // Verificar si la propiedad debe procesarse
            boolean shouldProcess =
                    !ignoredProperties.contains(propertyName)
                            && sourceWrapper.isReadableProperty(propertyName)
                            && targetWrapper.isWritableProperty(propertyName);

            if (shouldProcess) {
                try {
                    Object value = sourceWrapper.getPropertyValue(propertyName);
                    if (value != null) {
                        targetWrapper.setPropertyValue(propertyName, value);
                        copiedCount++;
                        log.trace("Copied property '{}' with value '{}'", propertyName, value);
                    }
                } catch (Exception _) {
                    log.debug("Could not copy property '{}'", propertyName);
                }
            }
        }

        log.debug(
                "Copied {} non-null properties from {} to {}",
                copiedCount,
                source.getClass().getSimpleName(),
                target.getClass().getSimpleName());
    }

    /**
     * Obtiene los nombres de propiedades con valores nulos en el objeto.
     *
     * @param source El objeto a inspeccionar
     * @return Array de nombres de propiedades con valor null
     */
    public static String[] getNullPropertyNames(Object source) {
        if (source == null) {
            return new String[0];
        }

        BeanWrapper wrapper = new BeanWrapperImpl(source);
        PropertyDescriptor[] pds = wrapper.getPropertyDescriptors();

        Set<String> nullProperties = new HashSet<>();
        for (PropertyDescriptor pd : pds) {
            String propertyName = pd.getName();
            if (wrapper.isReadableProperty(propertyName)) {
                try {
                    Object value = wrapper.getPropertyValue(propertyName);
                    if (value == null) {
                        nullProperties.add(propertyName);
                    }
                } catch (Exception _) {
                    // Ignorar propiedades que no se pueden leer
                }
            }
        }

        return nullProperties.toArray(new String[0]);
    }

    /**
     * Obtiene los nombres de propiedades con valores no nulos en el objeto.
     *
     * @param source El objeto a inspeccionar
     * @return Set de nombres de propiedades con valor no null
     */
    public static Set<String> getNonNullPropertyNames(Object source) {
        if (source == null) {
            return Set.of();
        }

        BeanWrapper wrapper = new BeanWrapperImpl(source);
        PropertyDescriptor[] pds = wrapper.getPropertyDescriptors();

        Set<String> nonNullProperties = new HashSet<>();
        for (PropertyDescriptor pd : pds) {
            String propertyName = pd.getName();
            if (ALWAYS_IGNORED_PROPERTIES.contains(propertyName)) {
                continue;
            }
            if (wrapper.isReadableProperty(propertyName)) {
                try {
                    Object value = wrapper.getPropertyValue(propertyName);
                    if (value != null) {
                        nonNullProperties.add(propertyName);
                    }
                } catch (Exception _) {
                    // Ignorar propiedades que no se pueden leer
                }
            }
        }

        return nonNullProperties;
    }

    /**
     * Verifica si un objeto tiene todas sus propiedades en null (excluyendo propiedades de
     * sistema).
     *
     * @param source El objeto a verificar
     * @return true si todas las propiedades copiables son null
     */
    public static boolean isAllPropertiesNull(Object source) {
        return getNonNullPropertyNames(source).isEmpty();
    }

    /**
     * Cuenta cuántas propiedades no nulas tiene un objeto (excluyendo propiedades de sistema).
     *
     * @param source El objeto a inspeccionar
     * @return Número de propiedades con valor no null
     */
    public static int countNonNullProperties(Object source) {
        return getNonNullPropertyNames(source).size();
    }
}
