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

    @Transactional
    public Order createOrderFromCart(OrderRequestDTO dto, String sessionId) {
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
        if (cart.getClient() != null) {
            order.setClient(cart.getClient());
        }

        if (dto.getClientId() != null) {
            Client client = clientRepository.findById(dto.getClientId()).orElse(null);
            order.setClient(client);
        } else if (cart.getClient() != null) {
            order.setClient(cart.getClient());
        }

        Order savedOrder = orderRepository.save(order);
        cart.getItems().clear();
        cartRepository.save(cart);

        try {
            String mensaje = "🚨 <b>¡NUEVA VENTA CUSHION!</b> 🚨\n\n" +
                    "<b>Pedido:</b> #" + savedOrder.getOrderNumber() + "\n" +
                    "<b>Cliente:</b> " + savedOrder.getCustomerName() + "\n" +
                    "<b>Valor:</b> $" + String.format("%,.0f", savedOrder.getTotalAmount()) + "\n\n" +
                    "Revisa el panel de administración para ver detalles de envío.";
            telegramService.sendNotification(mensaje);
        } catch (Exception e) {
            System.err.println("No se pudo enviar la alerta de Telegram: " + e.getMessage());
        }

        // --- CORREO PARA EL CLIENTE ---
        String clienteBody = """
    <h2 style="color: #B89B6A;">¡GRACIAS POR TU ELECCIÓN!</h2>
    <p>Hola <b>%s</b>, hemos registrado tu pedido <b>#%s</b>.</p>
    <p>Nuestro equipo de expertos está verificando la disponibilidad de tus piezas. 
    En breve nos pondremos en contacto contigo para coordinar el pago y envío.</p>
    <p><b>Total:</b> $%s</p>
    """.formatted(savedOrder.getCustomerName(), savedOrder.getOrderNumber(), String.format("%,.0f", savedOrder.getTotalAmount()));

        emailService.sendHtmlEmail(savedOrder.getCustomerEmail(), "Confirmación de Pedido - Cushion #" + savedOrder.getOrderNumber(), clienteBody);

// --- CORREO INTERNO (PARA USTEDES) ---
        String internoBody = """
    <h2 style="color: #d9534f;">ALERTA DE NUEVA VENTA</h2>
    <p>Se ha generado un nuevo pedido en la web:</p>
    <ul>
        <li><b>Orden:</b> #%s</li>
        <li><b>Cliente:</b> %s</li>
        <li><b>Email:</b> %s</li>
        <li><b>Monto:</b> $%s</li>
    </ul>
    <p>Por favor, revisa el panel administrativo para los detalles de envío.</p>
    """.formatted(savedOrder.getOrderNumber(), savedOrder.getCustomerName(), savedOrder.getCustomerEmail(), String.format("%,.0f", savedOrder.getTotalAmount()));

        emailService.sendHtmlEmail("admin@cushion.com", "🚨 NUEVA VENTA - Orden #" + savedOrder.getOrderNumber(), internoBody);

        return savedOrder;
    }
}