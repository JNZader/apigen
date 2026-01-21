package com.jnzader.apigen.security.infrastructure.network;

import com.jnzader.apigen.security.infrastructure.config.SecurityProperties;
import com.jnzader.apigen.security.infrastructure.config.SecurityProperties.TrustedProxiesProperties;
import com.jnzader.apigen.security.infrastructure.config.SecurityProperties.TrustedProxiesProperties.TrustMode;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Servicio para resolver la IP real del cliente de forma segura.
 *
 * <p>Cuando la aplicación está detrás de un proxy o load balancer, la IP directa del cliente
 * (remoteAddr) será la del proxy, no la del usuario real. Los proxies añaden headers como
 * X-Forwarded-For para transmitir la IP original.
 *
 * <p>PROBLEMA DE SEGURIDAD: Sin validación, un atacante puede falsificar estos headers conectándose
 * directamente. Este servicio implementa validación de proxies de confianza para prevenir esto.
 *
 * <p>Modos de operación:
 *
 * <ul>
 *   <li>TRUST_ALL: Confía en cualquier X-Forwarded-For (solo desarrollo)
 *   <li>TRUST_DIRECT: Ignora headers, usa solo remoteAddr
 *   <li>CONFIGURED: Valida que la request venga de un proxy de confianza
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "apigen.security.enabled", havingValue = "true")
public class ClientIpResolver {

    private static final Logger log = LoggerFactory.getLogger(ClientIpResolver.class);
    private static final String UNKNOWN = "unknown";

    private final TrustedProxiesProperties config;
    private final List<IpRange> trustedRanges;

    public ClientIpResolver(SecurityProperties securityProperties) {
        this.config = securityProperties.getTrustedProxies();
        this.trustedRanges = new ArrayList<>();
    }

    @PostConstruct
    void initialize() {
        if (config.getMode() == TrustMode.CONFIGURED) {
            parseAndValidateTrustedAddresses();
            log.info(
                    "ClientIpResolver initialized with {} trusted proxy ranges",
                    trustedRanges.size());
        } else {
            log.info("ClientIpResolver initialized in {} mode", config.getMode());
            if (config.getMode() == TrustMode.TRUST_ALL) {
                log.warn(
                        "SECURITY WARNING: trusted-proxies.mode=TRUST_ALL is insecure. "
                                + "Configure trusted proxies for production!");
            }
        }
    }

    /**
     * Resuelve la IP real del cliente según la configuración de proxies de confianza.
     *
     * @param request HTTP request
     * @return IP del cliente real o la IP directa si no hay proxy de confianza
     */
    public String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        switch (config.getMode()) {
            case TRUST_DIRECT:
                // Ignorar headers, usar solo IP directa
                return remoteAddr;

            case TRUST_ALL:
                // Comportamiento legacy: confiar en cualquier header
                return extractFromHeadersUnsafe(request, remoteAddr);

            case CONFIGURED:
                // Validar que la request venga de un proxy de confianza
                return extractFromHeadersWithValidation(request, remoteAddr);

            default:
                return remoteAddr;
        }
    }

    /**
     * Extrae IP sin validación (modo TRUST_ALL - inseguro).
     *
     * <p>Solo para desarrollo o cuando se confía completamente en la red.
     */
    private String extractFromHeadersUnsafe(HttpServletRequest request, String fallback) {
        String forwardedFor = request.getHeader(config.getForwardedForHeader());

        if (forwardedFor != null
                && !forwardedFor.isBlank()
                && !UNKNOWN.equalsIgnoreCase(forwardedFor)) {
            String[] ips = forwardedFor.split(",");
            if (config.isUseFirstInChain()) {
                return ips[0].trim();
            } else {
                return ips[ips.length - 1].trim();
            }
        }

        // Fallback a otros headers comunes
        String[] fallbackHeaders = {"X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String header : fallbackHeaders) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !UNKNOWN.equalsIgnoreCase(ip)) {
                return ip.trim();
            }
        }

        return fallback;
    }

    /**
     * Extrae IP con validación de proxy de confianza (modo CONFIGURED).
     *
     * <p>Solo usa X-Forwarded-For si la request viene de un proxy en la lista de confianza.
     */
    private String extractFromHeadersWithValidation(HttpServletRequest request, String remoteAddr) {
        // Si la conexión directa no es de un proxy de confianza, no usar headers
        if (!isTrustedProxy(remoteAddr)) {
            log.debug(
                    "Request from {} - not a trusted proxy, ignoring forwarded headers",
                    remoteAddr);
            return remoteAddr;
        }

        String forwardedFor = request.getHeader(config.getForwardedForHeader());
        if (forwardedFor == null
                || forwardedFor.isBlank()
                || UNKNOWN.equalsIgnoreCase(forwardedFor)) {
            return remoteAddr;
        }

        // Procesar la cadena de IPs
        String[] ipChain = forwardedFor.split(",");

        if (config.isUseFirstInChain()) {
            // Tomar la primera IP (cliente original)
            String clientIp = ipChain[0].trim();
            log.debug(
                    "Resolved client IP: {} (from X-Forwarded-For via trusted proxy {})",
                    clientIp,
                    remoteAddr);
            return clientIp;
        } else {
            // Tomar la última IP antes del primer proxy de confianza
            // Recorrer la cadena desde el final hacia el principio
            for (int i = ipChain.length - 1; i >= 0; i--) {
                String ip = ipChain[i].trim();
                if (!isTrustedProxy(ip)) {
                    log.debug("Resolved client IP: {} (last non-trusted in chain)", ip);
                    return ip;
                }
            }
            // Todos son proxies de confianza, usar la primera
            return ipChain[0].trim();
        }
    }

    /**
     * Verifica si una IP está en la lista de proxies de confianza.
     *
     * @param ip Dirección IP a verificar
     * @return true si es un proxy de confianza
     */
    public boolean isTrustedProxy(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }

        try {
            InetAddress address = InetAddress.getByName(ip.trim());
            for (IpRange range : trustedRanges) {
                if (range.contains(address)) {
                    return true;
                }
            }
        } catch (UnknownHostException _) {
            log.debug("Could not parse IP address: {}", ip);
        }

        return false;
    }

    /** Parsea y valida las direcciones de proxies de confianza configuradas. */
    private void parseAndValidateTrustedAddresses() {
        for (String address : config.getAddresses()) {
            try {
                IpRange range = IpRange.parse(address.trim());
                trustedRanges.add(range);
                log.debug("Added trusted proxy range: {}", address);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid trusted proxy address '{}': {}", address, e.getMessage());
            }
        }

        if (trustedRanges.isEmpty()) {
            log.warn(
                    "No valid trusted proxy addresses configured! "
                            + "All X-Forwarded-For headers will be ignored.");
        }
    }

    /**
     * Representa un rango de IPs (CIDR) o una IP individual.
     *
     * <p>Soporta tanto IPv4 como IPv6.
     */
    static class IpRange {
        private final byte[] networkAddress;
        private final int prefixLength;

        private IpRange(byte[] networkAddress, int prefixLength) {
            this.networkAddress = networkAddress;
            this.prefixLength = prefixLength;
        }

        /**
         * Parsea una dirección IP o rango CIDR.
         *
         * @param addressOrCidr Dirección IP (ej: "192.168.1.1") o CIDR (ej: "10.0.0.0/8")
         * @return IpRange que representa la dirección o rango
         * @throws IllegalArgumentException si el formato es inválido
         */
        static IpRange parse(String addressOrCidr) {
            String address;
            int prefix;

            int slashIndex = addressOrCidr.indexOf('/');
            if (slashIndex > 0) {
                address = addressOrCidr.substring(0, slashIndex);
                prefix = Integer.parseInt(addressOrCidr.substring(slashIndex + 1));
            } else {
                address = addressOrCidr;
                prefix = -1; // Se determinará por el tipo de IP
            }

            try {
                InetAddress inetAddress = InetAddress.getByName(address);
                byte[] addressBytes = inetAddress.getAddress();

                // Si no se especificó prefijo, usar el máximo (IP exacta)
                if (prefix == -1) {
                    prefix = addressBytes.length * 8;
                }

                // Validar prefijo
                int maxPrefix = addressBytes.length * 8;
                if (prefix < 0 || prefix > maxPrefix) {
                    throw new IllegalArgumentException(
                            "Invalid prefix length: " + prefix + " (max: " + maxPrefix + ")");
                }

                return new IpRange(addressBytes, prefix);
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException("Invalid IP address: " + address, e);
            }
        }

        /**
         * Verifica si una dirección IP está contenida en este rango.
         *
         * @param address Dirección a verificar
         * @return true si la dirección está en el rango
         */
        boolean contains(InetAddress address) {
            byte[] addressBytes = address.getAddress();

            // Deben ser del mismo tipo (IPv4 o IPv6)
            if (addressBytes.length != networkAddress.length) {
                return false;
            }

            // Comparar bit a bit según el prefijo
            int bytesToCompare = prefixLength / 8;
            int bitsRemaining = prefixLength % 8;

            // Comparar bytes completos
            for (int i = 0; i < bytesToCompare; i++) {
                if (addressBytes[i] != networkAddress[i]) {
                    return false;
                }
            }

            // Comparar bits parciales del último byte si es necesario
            if (bitsRemaining > 0 && bytesToCompare < addressBytes.length) {
                int mask = 0xFF << (8 - bitsRemaining);
                if ((addressBytes[bytesToCompare] & mask)
                        != (networkAddress[bytesToCompare] & mask)) {
                    return false;
                }
            }

            return true;
        }
    }
}
