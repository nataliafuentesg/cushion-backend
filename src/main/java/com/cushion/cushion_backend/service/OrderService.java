package com.cushion.cushion_backend.service;

import com.cushion.cushion_backend.dto.OrderRequestDTO;
import com.cushion.cushion_backend.model.*;
import com.cushion.cushion_backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    @Autowired private OrderRepository orderRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private TelegramService telegramService;
    @Autowired private EmailService emailService;
    @Autowired private MetaConversionsService metaConversions;

    private static final double SHIPPING_NACIONAL = 25000.0;
    private static final double SHIPPING_INTERNACIONAL = 150000.0;

    // ───────────────────────────────────────────────────────────────────────
    // PASO 1 — Crear la orden en estado PENDIENTE_PAGO.
    // Valida que haya stock disponible pero NO lo descuenta todavía.
    // NO vacía el carrito. NO envía Purchase ni correos. Eso ocurre solo
    // cuando el pago se confirma vía webhook de Bold.
    // ───────────────────────────────────────────────────────────────────────
    @Transactional
    public Order createPendingOrder(OrderRequestDTO dto, String sessionId) {
        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("No se encontró un carrito para la sesión: " + sessionId));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("El carrito está vacío. No se puede generar la orden.");
        }

        Order order = new Order();
        order.setOrderNumber("CUSH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setCustomerName(dto.getCustomerName());
        order.setCustomerEmail(dto.getCustomerEmail());
        order.setShippingAddress(dto.getShippingAddress());
        order.setPhoneNumber(dto.getPhoneNumber());
        order.setNotes(dto.getNotes());
        order.setSessionId(sessionId);
        order.setStatus("PENDIENTE_PAGO");

        double subtotal = 0;
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            // Validamos disponibilidad, pero NO descontamos aún.
            if (product.getStock() < cartItem.getQuantity()) {
                throw new RuntimeException("Stock insuficiente para: " + product.getName() +
                        " (Disponible: " + product.getStock() + ")");
            }
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPriceAtPurchase(product.getPrice());
            orderItem.setOrder(order);
            order.getItems().add(orderItem);
            subtotal += product.getPrice() * cartItem.getQuantity();
        }

        double shippingFee = SHIPPING_NACIONAL;
        if (dto.getShippingAddress() != null && !dto.getShippingAddress().toUpperCase().startsWith("[COLOMBIA]")) {
            shippingFee = SHIPPING_INTERNACIONAL;
        }
        order.setTotalAmount(subtotal + shippingFee);

        if (dto.getClientId() != null) {
            clientRepository.findById(dto.getClientId()).ifPresent(order::setClient);
        }
        if (order.getClient() == null && cart.getClient() != null) {
            order.setClient(cart.getClient());
        }

        Order savedOrder = orderRepository.save(order);
        log.info("[Orden] Creada PENDIENTE_PAGO #{} por ${}", savedOrder.getOrderNumber(), savedOrder.getTotalAmount());
        return savedOrder;
    }

    // ───────────────────────────────────────────────────────────────────────
    // PASO 2 — Confirmar el pago (lo llama el webhook de Bold tras SALE_APPROVED).
    // Aquí SÍ se descuenta el inventario, se marca PAGADO y se vacía el carrito.
    // Es idempotente: si la orden ya está pagada, no hace nada (protege contra
    // los reintentos del webhook de Bold).
    //
    // Devuelve la orden si se confirmó AHORA (para disparar notificaciones), o
    // null si no aplica (ya pagada, no encontrada, etc.).
    // ───────────────────────────────────────────────────────────────────────
    @Transactional
    public Order confirmPayment(String orderNumber, String paymentId) {
        Order order = orderRepository.findByOrderNumber(orderNumber).orElse(null);
        if (order == null) {
            log.warn("[Pago] Webhook para orden inexistente: {}", orderNumber);
            return null;
        }
        // Idempotencia: si ya está pagada, ignoramos (reintento de Bold).
        if ("PAGADO".equals(order.getStatus())) {
            log.info("[Pago] Orden {} ya estaba pagada — ignorando reintento.", orderNumber);
            return null;
        }

        // Descontar inventario ahora (revalidando por seguridad).
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            int restante = (product.getStock() != null ? product.getStock() : 0) - item.getQuantity();
            product.setStock(Math.max(restante, 0));
            productRepository.save(product);
        }

        order.setStatus("PAGADO");
        order.setPaymentId(paymentId);
        orderRepository.save(order);

        // Vaciar el carrito de esa sesión (ya se concretó la compra).
        if (order.getSessionId() != null) {
            cartRepository.findBySessionId(order.getSessionId()).ifPresent(cart -> {
                cart.getItems().clear();
                cartRepository.save(cart);
            });
        }

        log.info("[Pago] ✅ Orden {} CONFIRMADA y pagada (paymentId={})", orderNumber, paymentId);
        return order;
    }

    // ───────────────────────────────────────────────────────────────────────
    // Marcar una orden como rechazada (webhook SALE_REJECTED). No toca inventario.
    // ───────────────────────────────────────────────────────────────────────
    @Transactional
    public void markPaymentRejected(String orderNumber) {
        orderRepository.findByOrderNumber(orderNumber).ifPresent(order -> {
            if (!"PAGADO".equals(order.getStatus())) {
                order.setStatus("PAGO_RECHAZADO");
                orderRepository.save(order);
                log.info("[Pago] Orden {} marcada como rechazada.", orderNumber);
            }
        });
    }

    // ───────────────────────────────────────────────────────────────────────
    // Notificaciones tras pago confirmado — ASÍNCRONAS para que el webhook
    // responda en < 2 segundos (Meta CAPI, correos y Telegram pueden tardar).
    // Se invoca desde el controller (invocación externa) para que @Async aplique.
    // ───────────────────────────────────────────────────────────────────────
    @Async
    public void sendPaidOrderNotifications(Order order) {
        // ── Meta CAPI — Purchase (deduplica contra el Pixel por orderNumber) ──
        try {
            metaConversions.sendPurchase(
                    order.getOrderNumber(),
                    order.getTotalAmount(),
                    order.getCustomerEmail(),
                    null, null // IP/UA no disponibles desde el webhook; el Pixel del navegador aporta esa señal
            );
        } catch (Exception e) {
            log.error("[Meta CAPI] Purchase error: {}", e.getMessage());
        }

        // ── Telegram ──
        try {
            String msg = "🚨 <b>¡NUEVA VENTA CUSHION — PAGO CONFIRMADO!</b> 🚨\n\n" +
                    "<b>Pedido:</b> #" + order.getOrderNumber() + "\n" +
                    "<b>Cliente:</b> " + order.getCustomerName() + "\n" +
                    "<b>Valor:</b> $" + String.format("%,.0f", order.getTotalAmount()) + "\n\n" +
                    "Revisa el panel de administración para coordinar el envío.";
            telegramService.sendNotification(msg);
        } catch (Exception e) {
            log.error("Telegram error: {}", e.getMessage());
        }

        // ── Email al cliente ──
        try {
            String clienteBody = """
                <h2 style="color:#B89B6A; font-family:Georgia,serif;">¡Pago confirmado!</h2>
                <p>Hola <b>%s</b>, recibimos tu pago y confirmamos tu pedido <b>#%s</b>.</p>
                <p>Estamos preparando tu pieza con el mayor cuidado. Muy pronto coordinaremos el envío
                a la dirección indicada y te compartiremos el número de guía.</p>
                <p><b>Total pagado:</b> $%s COP</p>
                <p style="color:#B89B6A; font-style:italic;">Equipo Cushion — Alta Joyería</p>
                """.formatted(
                    order.getCustomerName(),
                    order.getOrderNumber(),
                    String.format("%,.0f", order.getTotalAmount())
            );
            emailService.sendHtmlEmail(
                    order.getCustomerEmail(),
                    "Pago confirmado — Cushion #" + order.getOrderNumber(),
                    clienteBody
            );
        } catch (Exception e) {
            log.error("Email cliente error: {}", e.getMessage());
        }

        // ── Email interno ──
        try {
            String internoBody = """
                <h2 style="color:#2e7d32;">✅ VENTA PAGADA</h2>
                <table style="border-collapse:collapse; font-family:Arial,sans-serif; font-size:14px;">
                    <tr><td style="padding:6px 12px; border:1px solid #ddd;"><b>Orden</b></td><td style="padding:6px 12px; border:1px solid #ddd;">#%s</td></tr>
                    <tr><td style="padding:6px 12px; border:1px solid #ddd;"><b>Cliente</b></td><td style="padding:6px 12px; border:1px solid #ddd;">%s</td></tr>
                    <tr><td style="padding:6px 12px; border:1px solid #ddd;"><b>Email</b></td><td style="padding:6px 12px; border:1px solid #ddd;">%s</td></tr>
                    <tr><td style="padding:6px 12px; border:1px solid #ddd;"><b>Teléfono</b></td><td style="padding:6px 12px; border:1px solid #ddd;">%s</td></tr>
                    <tr><td style="padding:6px 12px; border:1px solid #ddd;"><b>Dirección</b></td><td style="padding:6px 12px; border:1px solid #ddd;">%s</td></tr>
                    <tr><td style="padding:6px 12px; border:1px solid #ddd;"><b>Monto</b></td><td style="padding:6px 12px; border:1px solid #ddd;">$%s COP</td></tr>
                </table>
                """.formatted(
                    order.getOrderNumber(),
                    order.getCustomerName(),
                    order.getCustomerEmail(),
                    order.getPhoneNumber() != null ? order.getPhoneNumber() : "—",
                    order.getShippingAddress() != null ? order.getShippingAddress() : "—",
                    String.format("%,.0f", order.getTotalAmount())
            );
            emailService.sendHtmlEmail(
                    "nata.ltda1412@gmail.com",
                    "✅ VENTA PAGADA — Orden #" + order.getOrderNumber(),
                    internoBody
            );
        } catch (Exception e) {
            log.error("Email interno error: {}", e.getMessage());
        }
    }
}
