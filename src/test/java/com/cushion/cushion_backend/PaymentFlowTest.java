package com.cushion.cushion_backend;

import com.cushion.cushion_backend.dto.OrderRequestDTO;
import com.cushion.cushion_backend.model.*;
import com.cushion.cushion_backend.repository.*;
import com.cushion.cushion_backend.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de extremo a extremo del flujo de pago con Bold:
 *
 *   1. Se crea la orden  → queda PENDIENTE_PAGO y NO se descuenta inventario.
 *   2. Llega el webhook   → confirmPayment marca PAGADO, descuenta inventario
 *                           y vacía el carrito.
 *   3. Reintento del webhook → es idempotente (no descuenta dos veces).
 *
 * Usa H2 en memoria (vía @DataJpaTest), sin tocar la base de datos real.
 */
@DataJpaTest
@Import(OrderService.class)
class PaymentFlowTest {

    @Autowired private OrderService orderService;
    @Autowired private ProductRepository productRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private OrderRepository orderRepository;

    // Dependencias no-JPA del OrderService → se simulan (no nos interesan aquí).
    @MockitoBean private TelegramService telegramService;
    @MockitoBean private EmailService emailService;
    @MockitoBean private OrderEmailService orderEmailService;
    @MockitoBean private MetaConversionsService metaConversionsService;

    @Test
    void flujoCompleto_ordenSeCreaPendiente_yAlPagarSeDescuentaInventario() {
        // ── Preparar: un producto con 5 en stock y un carrito con 2 unidades ──
        Product anillo = new Product();
        anillo.setName("Anillo Esmeralda Test");
        anillo.setSlug("anillo-esmeralda-test");
        anillo.setPrice(2_000_000.0);
        anillo.setCategory("Anillos");
        anillo.setStock(5);
        anillo = productRepository.save(anillo);

        Cart cart = new Cart();
        cart.setSessionId("session-test-123");
        CartItem item = new CartItem();
        item.setProduct(anillo);
        item.setQuantity(2);
        item.setCart(cart);
        cart.getItems().add(item);
        cartRepository.save(cart);

        OrderRequestDTO dto = new OrderRequestDTO();
        dto.setCustomerName("Cliente Prueba");
        dto.setCustomerEmail("prueba@cushion.com");
        dto.setPhoneNumber("3001234567");
        dto.setShippingAddress("[COLOMBIA] - Calle 123, Bogotá");

        // ── PASO 1: crear la orden ──
        Order pendiente = orderService.createPendingOrder(dto, "session-test-123");

        assertThat(pendiente.getStatus()).isEqualTo("PENDIENTE_PAGO");
        // El total = subtotal (2 x 2.000.000) + envío nacional (25.000) = 4.025.000
        assertThat(pendiente.getTotalAmount()).isEqualTo(4_025_000.0);
        // 🔑 El inventario NO se ha tocado todavía
        assertThat(productRepository.findById(anillo.getId()).get().getStock()).isEqualTo(5);

        // ── PASO 2: llega el webhook de Bold confirmando el pago ──
        Order pagada = orderService.confirmPayment(pendiente.getOrderNumber(), "BOLD-TX-ABC123");

        assertThat(pagada).isNotNull();
        assertThat(pagada.getStatus()).isEqualTo("PAGADO");
        assertThat(pagada.getPaymentId()).isEqualTo("BOLD-TX-ABC123");
        // 🔑 Ahora SÍ se descontó el inventario: 5 - 2 = 3
        assertThat(productRepository.findById(anillo.getId()).get().getStock()).isEqualTo(3);
        // 🔑 El carrito quedó vacío
        assertThat(cartRepository.findBySessionId("session-test-123").get().getItems()).isEmpty();

        // ── PASO 3: Bold reintenta el webhook → debe ser idempotente ──
        Order reintento = orderService.confirmPayment(pendiente.getOrderNumber(), "BOLD-TX-ABC123");

        assertThat(reintento).isNull(); // no procesa de nuevo
        // 🔑 El stock sigue en 3 (no se descontó dos veces)
        assertThat(productRepository.findById(anillo.getId()).get().getStock()).isEqualTo(3);

        System.out.println("\n✅ FLUJO COMPLETO OK:");
        System.out.println("   Orden " + pagada.getOrderNumber() + " → " + pagada.getStatus());
        System.out.println("   Total cobrado: $" + String.format("%,.0f", pagada.getTotalAmount()) + " COP");
        System.out.println("   Stock: 5 → 3 (descontado solo al pagar)");
        System.out.println("   Idempotencia: reintento de webhook ignorado\n");
    }
}
