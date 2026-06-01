package com.cushion.cushion_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;


@Service
public class MetaConversionsService {

    private static final String CAPI_URL =
            "https://graph.facebook.com/v19.0/%s/events?access_token=%s";

    @Value("${meta.pixel.id:}")
    private String pixelId;

    @Value("${meta.access.token:}")
    private String accessToken;

    @Value("${app.base.url:https://cushionjewelry.com}")
    private String baseUrl;

    // Código de prueba de Meta — SOLO para testing en "Probar eventos".
    // Déjalo vacío en producción. Cuando lo pongas (ej. TEST25072), los eventos
    // del servidor aparecen en el panel "Probar eventos" de Events Manager.
    @Value("${meta.test.event.code:}")
    private String testEventCode;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    // ─── Eventos públicos ─────────────────────────────────────────────────────

    /** Orden completada — el evento más valioso */
    public void sendPurchase(String orderNumber, double value,
                             String email, String ip, String userAgent) {
        Map<String, Object> customData = map(
                "currency", "COP",
                "value", value,
                "order_id", orderNumber
        );
        send("Purchase", orderNumber, baseUrl + "/finalizar-compra",
             buildUserData(email, ip, userAgent), customData);
    }

    /** Formulario de esmeralda enviado */
    public void sendLead(String eventId, String email,
                         String ip, String userAgent) {
        send("Lead", eventId, baseUrl + "/esmeraldas",
             buildUserData(email, ip, userAgent), Collections.emptyMap());
    }

    /** Clic en WhatsApp desde página de producto */
    public void sendContact(String eventId, String productSlug,
                            String email, String ip, String userAgent) {
        send("Contact", eventId, baseUrl + "/producto/" + productSlug,
             buildUserData(email, ip, userAgent), Collections.emptyMap());
    }

    // ─── Core ─────────────────────────────────────────────────────────────────

    private void send(String eventName, String eventId, String sourceUrl,
                      Map<String, Object> userData, Map<String, Object> customData) {
        if (pixelId.isBlank() || accessToken.isBlank()) return; // no configurado aún

        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("event_name", eventName);
            event.put("event_time", System.currentTimeMillis() / 1000L);
            event.put("event_id", eventId);
            event.put("action_source", "website");
            event.put("event_source_url", sourceUrl);
            event.put("user_data", userData);
            if (!customData.isEmpty()) event.put("custom_data", customData);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("data", List.of(event));
            // Solo en modo prueba: hace que el evento aparezca en "Probar eventos"
            if (testEventCode != null && !testEventCode.isBlank()) {
                payload.put("test_event_code", testEventCode);
            }

            String body = mapper.writeValueAsString(payload);
            String url  = String.format(CAPI_URL, pixelId, accessToken);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            // Asíncrono — no bloquea la transacción
            httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(res -> {
                        if (res.statusCode() != 200) {
                            System.err.println("[Meta CAPI] " + eventName +
                                    " → HTTP " + res.statusCode() + " | " + res.body());
                        }
                    });
        } catch (Exception e) {
            System.err.println("[Meta CAPI] Error enviando " + eventName + ": " + e.getMessage());
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Map<String, Object> buildUserData(String email, String ip, String userAgent) {
        Map<String, Object> ud = new LinkedHashMap<>();
        if (email != null && !email.isBlank()) {
            try { ud.put("em", List.of(sha256(email.toLowerCase().trim()))); }
            catch (Exception ignored) {}
        }
        if (ip != null && !ip.isBlank()) ud.put("client_ip_address", ip);
        if (userAgent != null && !userAgent.isBlank()) ud.put("client_user_agent", userAgent);
        return ud;
    }

    private String sha256(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            String h = Integer.toHexString(0xff & b);
            if (h.length() == 1) hex.append('0');
            hex.append(h);
        }
        return hex.toString();
    }

    @SafeVarargs
    private static <K, V> Map<K, V> map(Object... pairs) {
        Map<K, V> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            //noinspection unchecked
            m.put((K) pairs[i], (V) pairs[i + 1]);
        }
        return m;
    }
}
