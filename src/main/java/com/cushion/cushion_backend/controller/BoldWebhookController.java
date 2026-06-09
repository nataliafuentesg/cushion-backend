package com.cushion.cushion_backend.controller;

import com.cushion.cushion_backend.model.Order;
import com.cushion.cushion_backend.service.BoldService;
import com.cushion.cushion_backend.service.OrderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Recibe los eventos de pago que envía Bold (webhook).
 *
 * URL pública: https://api.cushionjewelry.com/api/webhooks/bold
 * (configurar esta URL en el panel de Bold → Webhooks)
 *
 * Flujo:
 *   1. Bold envía POST con la firma en el header x-bold-signature.
 *   2. Verificamos la firma (HMAC-SHA256) — si no coincide, 401.
 *   3. Si el evento es SALE_APPROVED → confirmamos el pago: se descuenta
 *      inventario, se marca PAGADO y se vacía el carrito.
 *   4. Respondemos 200 de inmediato (Bold exige < 2 s). Las notificaciones
 *      (correo, Meta, Telegram) se disparan de forma asíncrona.
 *
 * Es idempotente: si Bold reintenta, la orden ya estará PAGADO y se ignora.
 */
@RestController
@RequestMapping("/api/webhooks")
public class BoldWebhookController {

    private static final Logger log = LoggerFactory.getLogger(BoldWebhookController.class);

    @Autowired private BoldService boldService;
    @Autowired private OrderService orderService;

    private final ObjectMapper mapper = new ObjectMapper();

    @PostMapping("/bold")
    public ResponseEntity<String> handleBoldWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "x-bold-signature", required = false) String signature) {

        // 1. Verificar autenticidad
        if (!boldService.verifyWebhookSignature(rawBody, signature)) {
            log.warn("[Bold Webhook] Firma inválida — petición rechazada.");
            return ResponseEntity.status(401).body("invalid signature");
        }

        try {
            JsonNode root = mapper.readTree(rawBody);
            String type = root.path("type").asText("");
            JsonNode data = root.path("data");

            // El order-id que enviamos al botón vuelve en metadata.reference
            String reference = data.path("metadata").path("reference").asText(null);
            String paymentId = data.path("payment_id").asText(null);

            log.info("[Bold Webhook] Evento '{}' para referencia '{}' (paymentId={})", type, reference, paymentId);

            if (reference == null) {
                log.warn("[Bold Webhook] Sin referencia de orden — se ignora.");
                return ResponseEntity.ok("no-reference");
            }

            switch (type) {
                case "SALE_APPROVED" -> {
                    // Confirma el pago (idempotente). Devuelve la orden solo si se
                    // confirmó AHORA, para disparar las notificaciones una sola vez.
                    Order confirmed = orderService.confirmPayment(reference, paymentId);
                    if (confirmed != null) {
                        orderService.sendPaidOrderNotifications(confirmed); // asíncrono
                    }
                }
                case "SALE_REJECTED" -> orderService.markPaymentRejected(reference);
                default -> log.info("[Bold Webhook] Evento '{}' sin acción.", type);
            }

            return ResponseEntity.ok("ok");

        } catch (Exception e) {
            // Respondemos 200 para que Bold no reintente por errores de parseo nuestros.
            log.error("[Bold Webhook] Error procesando: {}", e.getMessage());
            return ResponseEntity.ok("error-handled");
        }
    }
}
