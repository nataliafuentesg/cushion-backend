package com.cushion.cushion_backend.service;

import com.cushion.cushion_backend.dto.OrderRequestDTO;
import com.cushion.cushion_backend.model.*;
import com.cushion.cushion_backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
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
    @Autowired private OrderEmailService orderEmailService;
    @Autowired private MetaConversionsService metaConversions;

    // Tarifas de envío configurables desde application.properties (valores
    // provisionales por defecto mientras se definen los definitivos).
    @Value("${shipping.fee.nacional:25000}")
    private double shippingNacional;

    @Value("${shipping.fee.internacional:150000}")
    private double shippingInternacional;

    // Tiempo que se reserva una pieza mientras el cliente paga (minutos)
    @Value("${order.reservation.minutes:60}")
    private int reservationMinutes;

    // Correo interno que recibe las notificaciones (nueva venta, etc.)
    @Value("${notifications.admin.email:nata.ltda1412@gmail.com}")
    private String adminEmail;

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
            // Bloqueamos la fila del producto (FOR UPDATE): si dos clientes intentan
            // la última unidad al mismo tiempo, el segundo espera y verá stock = 0.
            Product product = productRepository.findByIdForUpdate(cartItem.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + cartItem.getProduct().getId()));
            int stock = product.getStock() != null ? product.getStock() : 0;
            if (stock < cartItem.getQuantity()) {
                throw new RuntimeException("Stock insuficiente para: " + product.getName() +
                        " (Disponible: " + stock + ")");
            }
            // RESERVAR: descontamos el stock ahora para que nadie más pueda ordenar
            // esta pieza mientras el cliente paga. Si no paga a tiempo, se libera.
            product.setStock(stock - cartItem.getQuantity());
            productRepository.save(product);

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPriceAtPurchase(product.getPrice());
            orderItem.setOrder(order);
            order.getItems().add(orderItem);
            subtotal += product.getPrice() * cartItem.getQuantity();
        }

        // La reserva vence en N minutos si no se paga
        order.setExpiresAt(LocalDateTime.now().plusMinutes(reservationMinutes));

        double shippingFee = shippingNacional;
        if (dto.getShippingAddress() != null && !dto.getShippingAddress().toUpperCase().startsWith("[COLOMBIA]")) {
            shippingFee = shippingInternacional;
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

        // Correo 1/4: "Recibimos tu orden, esperamos tu pago" (async)
        try { orderEmailService.sendOrderCreatedEmail(savedOrder); }
        catch (Exception e) { log.error("[Email] orden creada error: {}", e.getMessage()); }

        return savedOrder;
    }

    // ───────────────────────────────────────────────────────────────────────
    // PASO 2 — Confirmar el pago (lo llama el webhook de Bold tras SALE_APPROVED).
    // El inventario YA fue reservado al crear la orden, así que aquí solo se
    // marca PAGADO y se vacía el carrito. Es idempotente (reintentos de Bold).
    //
    // Caso especial: si la orden ya había EXPIRADO (su reserva se liberó), se
    // intenta re-reservar; si la pieza ya no está, se marca PAGO_SIN_STOCK y se
    // alerta al equipo para gestionar el reembolso.
    //
    // Devuelve la orden si se confirmó AHORA (para notificaciones), o null si no
    // aplica (ya pagada, no encontrada, o pago sin stock).
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

        // Si la reserva había expirado, intentamos recuperar el stock.
        if ("EXPIRADO".equals(order.getStatus())) {
            boolean disponible = order.getItems().stream().allMatch(it ->
                    it.getProduct().getStock() != null && it.getProduct().getStock() >= it.getQuantity());
            if (disponible) {
                for (OrderItem item : order.getItems()) {
                    Product p = item.getProduct();
                    p.setStock(p.getStock() - item.getQuantity());
                    productRepository.save(p);
                }
                log.info("[Pago] Orden {} expirada pero re-reservada (pago tardío con stock disponible).", orderNumber);
            } else {
                // Pago llegó tarde y la pieza ya no está → requiere gestión manual.
                order.setStatus("PAGO_SIN_STOCK");
                order.setPaymentId(paymentId);
                orderRepository.save(order);
                try {
                    telegramService.sendNotification("⚠️ <b>PAGO SIN STOCK</b>\n\n" +
                            "Orden <b>" + orderNumber + "</b> se pagó pero su reserva ya había expirado y la pieza " +
                            "no está disponible.\n\n<b>Requiere reembolso o gestión manual.</b>");
                } catch (Exception ignored) {}
                log.warn("[Pago] ⚠️ Orden {} pagada SIN STOCK (reserva expirada). Requiere reembolso.", orderNumber);
                return null;
            }
        }
        // Para PENDIENTE_PAGO el stock ya está reservado desde la creación → no se toca.

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
    // Tarea programada: libera el stock de las órdenes cuya reserva venció sin
    // pagarse. Corre cada 5 minutos. Restaura el inventario y marca EXPIRADO.
    // ───────────────────────────────────────────────────────────────────────
    @Scheduled(fixedRate = 300000) // cada 5 minutos
    @Transactional
    public void releaseExpiredOrders() {
        List<Order> expiradas = orderRepository.findByStatusAndExpiresAtBefore("PENDIENTE_PAGO", LocalDateTime.now());
        if (expiradas.isEmpty()) return;

        for (Order order : expiradas) {
            for (OrderItem item : order.getItems()) {
                Product p = item.getProduct();
                int stock = p.getStock() != null ? p.getStock() : 0;
                p.setStock(stock + item.getQuantity()); // devolver al inventario
                productRepository.save(p);
            }
            order.setStatus("EXPIRADO");
            orderRepository.save(order);
            log.info("[Orden] {} EXPIRADA — reserva liberada al inventario.", order.getOrderNumber());
        }
        log.info("[Órdenes] {} reservas vencidas liberadas.", expiradas.size());
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

        // ── Correo 2/4 al cliente: pago confirmado ──
        try { orderEmailService.sendPaymentConfirmedEmail(order); }
        catch (Exception e) { log.error("Email pago confirmado error: {}", e.getMessage()); }

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
                    adminEmail,
                    "✅ VENTA PAGADA — Orden #" + order.getOrderNumber(),
                    internoBody
            );
        } catch (Exception e) {
            log.error("Email interno error: {}", e.getMessage());
        }
    }

    // ───────────────────────────────────────────────────────────────────────
    // Marcar como ENVIADO — guarda guía + transportadora y envía el correo 3/4.
    // ───────────────────────────────────────────────────────────────────────
    @Transactional
    public Order markShipped(Long orderId, String trackingNumber, String carrier) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        order.setTrackingNumber(trackingNumber);
        order.setShippingCarrier(carrier);
        order.setStatus("ENVIADO");
        Order saved = orderRepository.save(order);

        try { orderEmailService.sendShippedEmail(saved); }
        catch (Exception e) { log.error("[Email] envío error: {}", e.getMessage()); }

        log.info("[Orden] {} marcada ENVIADO (guía {}, {})", saved.getOrderNumber(), trackingNumber, carrier);
        return saved;
    }

    // ───────────────────────────────────────────────────────────────────────
    // Marcar como ENTREGADO — envía el correo 4/4 de agradecimiento.
    // ───────────────────────────────────────────────────────────────────────
    @Transactional
    public Order markDelivered(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        order.setStatus("ENTREGADO");
        Order saved = orderRepository.save(order);

        try { orderEmailService.sendDeliveredEmail(saved); }
        catch (Exception e) { log.error("[Email] entrega error: {}", e.getMessage()); }

        log.info("[Orden] {} marcada ENTREGADO", saved.getOrderNumber());
        return saved;
    }

    // ───────────────────────────────────────────────────────────────────────
    // Rastreo público — busca por número de orden y valida que el contacto
    // (correo o teléfono) coincida con el de la orden.
    // ───────────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Order trackOrder(String orderNumber, String contact) {
        Order order = orderRepository.findByOrderNumber(orderNumber).orElse(null);
        if (order == null || contact == null) return null;
        String c = contact.trim().toLowerCase();
        boolean emailMatch = order.getCustomerEmail() != null
                && order.getCustomerEmail().trim().toLowerCase().equals(c);
        boolean phoneMatch = order.getPhoneNumber() != null
                && order.getPhoneNumber().replaceAll("\\s+", "").equals(contact.replaceAll("\\s+", ""));
        return (emailMatch || phoneMatch) ? order : null;
    }
}
