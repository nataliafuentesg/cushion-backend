package com.cushion.cushion_backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Integración con Bold (pasarela de pagos colombiana).
 *
 * Dos responsabilidades:
 *   1. Generar la firma de integridad del botón de pago
 *      → SHA256(orderId + amount + currency + secretKey) en hexadecimal.
 *   2. Verificar la firma del webhook que envía Bold al confirmar un pago
 *      → HMAC-SHA256( base64(body) ) con la llave secreta, comparado con el
 *        header x-bold-signature.
 *
 * Configuración en application.properties DEL SERVIDOR (nunca local):
 *   bold.api.key=...      (llave de identidad — pública, va en el botón)
 *   bold.secret.key=...   (llave secreta — privada, NUNCA al frontend)
 *
 * En ambiente de pruebas usa tus llaves de prueba. La llave secreta de prueba
 * para verificar el webhook puede ser cadena vacía según la documentación.
 */
@Service
public class BoldService {

    private static final Logger log = LoggerFactory.getLogger(BoldService.class);

    @Value("${bold.api.key:}")
    private String apiKey;

    @Value("${bold.secret.key:}")
    private String secretKey;

    public String getApiKey() {
        return apiKey;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Firma de integridad para el botón de pago.
     * Orden EXACTO de concatenación: {orderId}{amount}{currency}{secretKey}
     *
     * @param orderId   identificador de la orden (data-order-id)
     * @param amount    monto entero en COP sin decimales (data-amount)
     * @param currency  moneda, normalmente "COP" (data-currency)
     */
    public String generateIntegritySignature(String orderId, long amount, String currency) {
        String raw = orderId + amount + currency + (secretKey != null ? secretKey : "");
        return sha256Hex(raw);
    }

    /**
     * Verifica que el webhook venga realmente de Bold.
     * Proceso: HMAC-SHA256( base64(rawBody) ) con la llave secreta → hex,
     * y se compara en tiempo constante con el header x-bold-signature.
     */
    public boolean verifyWebhookSignature(String rawBody, String receivedSignature) {
        if (receivedSignature == null || receivedSignature.isBlank()) {
            log.warn("[Bold] Webhook sin header x-bold-signature");
            return false;
        }
        try {
            String encoded = Base64.getEncoder().encodeToString(rawBody.getBytes(StandardCharsets.UTF_8));
            Mac mac = Mac.getInstance("HmacSHA256");
            byte[] keyBytes = (secretKey != null ? secretKey : "").getBytes(StandardCharsets.UTF_8);
            mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
            byte[] hash = mac.doFinal(encoded.getBytes(StandardCharsets.UTF_8));
            String computed = toHex(hash);
            return constantTimeEquals(computed, receivedSignature.trim());
        } catch (Exception e) {
            log.error("[Bold] Error verificando firma del webhook: {}", e.getMessage());
            return false;
        }
    }

    // ─── Utilidades de hashing ───────────────────────────────────────────────

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error generando SHA-256 para Bold", e);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String h = Integer.toHexString(0xff & b);
            if (h.length() == 1) sb.append('0');
            sb.append(h);
        }
        return sb.toString();
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
