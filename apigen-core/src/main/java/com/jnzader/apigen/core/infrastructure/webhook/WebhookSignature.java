package com.jnzader.apigen.core.infrastructure.webhook;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utility class for creating and verifying webhook signatures.
 *
 * <p>Uses HMAC-SHA256 for signature generation. The signature format is:
 *
 * <pre>
 * X-Webhook-Signature: t=timestamp,v1=signature
 * </pre>
 *
 * <p>The signed payload format is:
 *
 * <pre>
 * timestamp.payload
 * </pre>
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * // Generate signature
 * String signature = WebhookSignature.sign(payload, secret, timestamp);
 *
 * // Verify signature
 * boolean valid = WebhookSignature.verify(payload, secret, signatureHeader, tolerance);
 * }</pre>
 */
public final class WebhookSignature {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGNATURE_VERSION = "v1";
    private static final long DEFAULT_TOLERANCE_SECONDS = 300; // 5 minutes

    private WebhookSignature() {
        // Utility class
    }

    /**
     * Generates an HMAC-SHA256 signature for a webhook payload.
     *
     * @param payload the JSON payload to sign
     * @param secret the shared secret
     * @param timestamp the timestamp to include in the signature
     * @return the signature header value in format "t=timestamp,v1=signature"
     */
    public static String sign(String payload, String secret, Instant timestamp) {
        long timestampSeconds = timestamp.getEpochSecond();
        String signedPayload = timestampSeconds + "." + payload;
        String signature = computeHmacSha256(signedPayload, secret);
        return String.format("t=%d,%s=%s", timestampSeconds, SIGNATURE_VERSION, signature);
    }

    /**
     * Generates a signature using the current timestamp.
     *
     * @param payload the JSON payload to sign
     * @param secret the shared secret
     * @return the signature header value
     */
    public static String sign(String payload, String secret) {
        return sign(payload, secret, Instant.now());
    }

    /**
     * Verifies a webhook signature.
     *
     * @param payload the received payload
     * @param secret the shared secret
     * @param signatureHeader the X-Webhook-Signature header value
     * @param toleranceSeconds maximum age of the signature in seconds
     * @return true if the signature is valid
     */
    public static boolean verify(
            String payload, String secret, String signatureHeader, long toleranceSeconds) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }

        ParsedSignature parsed = parseSignature(signatureHeader);
        if (parsed == null) {
            return false;
        }

        // Check timestamp tolerance
        long now = Instant.now().getEpochSecond();
        if (Math.abs(now - parsed.timestamp) > toleranceSeconds) {
            return false;
        }

        // Compute expected signature
        String signedPayload = parsed.timestamp + "." + payload;
        String expectedSignature = computeHmacSha256(signedPayload, secret);

        // Constant-time comparison to prevent timing attacks
        return constantTimeEquals(parsed.signature, expectedSignature);
    }

    /**
     * Verifies a webhook signature with default tolerance (5 minutes).
     *
     * @param payload the received payload
     * @param secret the shared secret
     * @param signatureHeader the X-Webhook-Signature header value
     * @return true if the signature is valid
     */
    public static boolean verify(String payload, String secret, String signatureHeader) {
        return verify(payload, secret, signatureHeader, DEFAULT_TOLERANCE_SECONDS);
    }

    /**
     * Extracts the timestamp from a signature header.
     *
     * @param signatureHeader the signature header value
     * @return the timestamp as Instant, or null if parsing fails
     */
    public static Instant extractTimestamp(String signatureHeader) {
        ParsedSignature parsed = parseSignature(signatureHeader);
        return parsed != null ? Instant.ofEpochSecond(parsed.timestamp) : null;
    }

    private static String computeHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec =
                    new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new WebhookSignatureException("Failed to compute HMAC-SHA256 signature", e);
        }
    }

    private static ParsedSignature parseSignature(String header) {
        if (header == null) {
            return null;
        }
        try {
            String[] parts = header.split(",");
            Long timestamp = null;
            String signature = null;

            for (String part : parts) {
                String[] keyValue = part.split("=", 2);
                if (keyValue.length != 2) {
                    continue;
                }
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();

                if ("t".equals(key)) {
                    timestamp = Long.parseLong(value);
                } else if (SIGNATURE_VERSION.equals(key)) {
                    signature = value;
                }
            }

            if (timestamp == null || signature == null) {
                return null;
            }

            return new ParsedSignature(timestamp, signature);
        } catch (NumberFormatException _) {
            return null;
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    private record ParsedSignature(long timestamp, String signature) {}

    /** Exception thrown when signature computation fails. */
    public static class WebhookSignatureException extends RuntimeException {
        public WebhookSignatureException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
