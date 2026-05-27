package com.cushion.cushion_backend.service;

import com.cushion.cushion_backend.dto.OrderRequestDTO;
import com.cushion.cushion_backend.model.*;
import com.cushion.cushion_backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private TelegramService telegramService;
    @Autowired private EmailService emailService;
    @Autowired private MetaConversionsService metaConversions;

    @Transactional
    public Order createOrderFromCart(OrderRequestDTO dto, String sessionId,
                                     String clientIp, String userAgent) {
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
        order.setStatus("PENDIENTE_PAGO");

        double subtotal = 0;
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            if (product.getStock() < cartItem.getQuantity()) {
                throw new RuntimeException("Stock insuficiente para: " + product.getName() +
                        " (Disponible: " + product.getStock() + ")");
            }
            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPriceAtPurchase(product.getPrice());
            orderItem.setOrder(order);
            order.getItems().add(orderItem);
            subtotal += product.getPrice() * cartItem.getQuantity();
        }

        double shippingFee = 25000.0;
        if (dto.getShippingAddress() != null && !dto.getShippingAddress().toUpperCase().startsWith("[COLOMBIA]")) {
            shippingFee = 150000.0;
        }
        order.setTotalAmount(subtotal + shippingFee);

        if (dto.getClientId() != null) {
            clientRepository.findById(dto.getClientId()).ifPresent(order::setClient);
        }
        if (order.getClient() == null && cart.getClient() != null) {
            order.setClient(cart.getClient());
        }

        Order savedOrder = orderRepository.save(order);
        cart.getItems().clear();
        cartRepository.save(cart);

        // ── Meta CAPI — Purchase (server-side, asíncrono) ──
        try {
            metaConversions.sendPurchase(
                    savedOrder.getOrderNumber(),
                    savedOrder.getTotalAmount(),
                    savedOrder.getCustomerEmail(),
                    clientIp, userAgent
            );
        } catch (Exception e) {
            System.err.println("[Meta CAPI] Purchase error: " + e.getMessage());
        }

        // ── Telegram ──
        try {
            String msg = "🚨 <b>¡NUEVA VENTA CUSHION!</b> 🚨\n\n" +
                    "<b>Pedido:</b> #" + savedOrder.getOrderNumber() + "\n" +
                    "<b>Cliente:</b> " + savedOrder.getCustomerName() + "\n" +
                    "<b>Valor:</b> $" + String.format("%,.0f", savedOrder.getTotalAmount()) + "\n\n" +
                    "Revisa el panel de administración para ver detalles de envío.";
            telegramService.sendNotification(msg);
        } catch (Exception e) {
            System.err.println("Telegram error: " + e.getMessage());
        }

        // ── Email al cliente ──
        try {
            String clienteBody = """
                <h2 style="color:#B89B6A; font-family:Georgia,serif;">¡Gracias por tu elección!</h2>
                <p>Hola <b>%s</b>, hemos registrado tu pedido <b>#%s</b>.</p>
                <p>Nuestro equipo está verificando la disponibilidad de tus piezas.
                En breve nos pondremos en contacto para coordinar el pago y envío.</p>
                <p><b>Total:</b> $%s COP</p>
                <p style="color:#B89B6A; font-style:italic;">Equipo Cushion — Alta Joyería</p>
                """.formatted(
                    savedOrder.getCustomerName(),
                    savedOrder.getOrderNumber(),
                    String.format("%,.0f", savedOrder.getTotalAmount())
                );
            emailService.sendHtmlEmail(
                    savedOrder.getCustomerEmail(),
                    "Confirmación de Pedido — Cushion #" + savedOrder.getOrderNumber(),
                    clienteBody
            );
        } catch (Exception e) {
            System.err.println("Email cliente error: " + e.getMessage());
        }

        // ── Email interno ──
        try {
            String internoBody = """
                <h2 style="color:#d9534f;">ALERTA DE NUEVA VENTA</h2>
                <table style="border-collapse:collapse; font-family:Arial,sans-serif; font-size:14px;">
                    <tr><td style="padding:6px 12px; border:1px solid #ddd;"><b>Orden</b></td><td style="padding:6px 12px; border:1px solid #ddd;">#%s</td></tr>
                    <tr><td style="padding:6px 12px; border:1px solid #ddd;"><b>Cliente</b></td><td style="padding:6px 12px; border:1px solid #ddd;">%s</td></tr>
                    <tr><td style="padding:6px 12px; border:1px solid #ddd;"><b>Email</b></td><td style="padding:6px 12px; border:1px solid #ddd;">%s</td></tr>
                    <tr><td style="padding:6px 12px; border:1px solid #ddd;"><b>Monto</b></td><td style="padding:6px 12px; border:1px solid #ddd;">$%s COP</td></tr>
                </table>
                """.formatted(
                    savedOrder.getOrderNumber(),
                    savedOrder.getCustomerName(),
                    savedOrder.getCustomerEmail(),
                    String.format("%,.0f", savedOrder.getTotalAmount())
                );
            emailService.sendHtmlEmail(
                    "nata.ltda1412@gmail.com",
                    "🚨 NUEVA VENTA — Orden #" + savedOrder.getOrderNumber(),
                    internoBody
            );
        } catch (Exception e) {
            System.err.println("Email interno error: " + e.getMessage());
        }

        return savedOrder;
    }
}
