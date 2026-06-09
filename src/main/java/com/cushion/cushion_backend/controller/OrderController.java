package com.cushion.cushion_backend.controller;

import com.cushion.cushion_backend.dto.OrderRequestDTO;
import com.cushion.cushion_backend.dto.OrderResponseDTO;
import com.cushion.cushion_backend.model.Order;
import com.cushion.cushion_backend.repository.OrderRepository;
import com.cushion.cushion_backend.service.BoldService;
import com.cushion.cushion_backend.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired private OrderService orderService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private BoldService boldService;

    @PostMapping("/create")
    public ResponseEntity<?> createOrder(
            @RequestBody OrderRequestDTO orderDto,
            @RequestParam String sessionId) {
        try {
            // Crea la orden PENDIENTE_PAGO (no descuenta inventario aún)
            Order savedOrder = orderService.createPendingOrder(orderDto, sessionId);

            // Monto entero en COP (Bold no acepta decimales)
            long boldAmount = Math.round(savedOrder.getTotalAmount());
            String currency = "COP";
            String signature = boldService.generateIntegritySignature(
                    savedOrder.getOrderNumber(), boldAmount, currency);

            OrderResponseDTO response = new OrderResponseDTO();
            response.setOrderNumber(savedOrder.getOrderNumber());
            response.setStatus(savedOrder.getStatus());
            response.setTotalAmount(savedOrder.getTotalAmount());
            response.setCustomerEmail(savedOrder.getCustomerEmail());
            // Datos para que el frontend arme el botón de Bold
            response.setBoldAmount(boldAmount);
            response.setBoldCurrency(currency);
            response.setBoldApiKey(boldService.getApiKey());
            response.setBoldIntegritySignature(signature);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<Order>> getClientOrders(@PathVariable Long clientId) {
        return ResponseEntity.ok(orderRepository.findByClient_Id(clientId));
    }

    /**
     * Estado de una orden — usado por la página de resultado de pago para saber
     * si Bold ya confirmó el pago (vía webhook) y mostrar el mensaje correcto.
     * Público: solo devuelve estado/monto, sin datos sensibles.
     */
    @GetMapping("/{orderNumber}/status")
    public ResponseEntity<?> getOrderStatus(@PathVariable String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .<ResponseEntity<?>>map(order -> ResponseEntity.ok(Map.of(
                        "orderNumber", order.getOrderNumber(),
                        "status", order.getStatus(),
                        "totalAmount", order.getTotalAmount()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Rastreo público de un pedido — para clientes SIN cuenta.
     * Requiere el número de orden + el correo o teléfono con que se hizo el pedido.
     */
    @GetMapping("/track")
    public ResponseEntity<?> trackOrder(
            @RequestParam String orderNumber,
            @RequestParam String contact) {
        Order order = orderService.trackOrder(orderNumber.trim(), contact);
        if (order == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "message", "No encontramos un pedido con esos datos. Verifica el número de orden y el correo o teléfono."));
        }
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("orderNumber", order.getOrderNumber());
        result.put("status", order.getStatus());
        result.put("createdAt", order.getCreatedAt());
        result.put("totalAmount", order.getTotalAmount());
        result.put("customerName", order.getCustomerName());
        result.put("trackingNumber", order.getTrackingNumber());
        result.put("shippingCarrier", order.getShippingCarrier());
        result.put("items", order.getItems().stream().map(it -> Map.of(
                "name", it.getProduct() != null ? it.getProduct().getName() : "Pieza",
                "quantity", it.getQuantity(),
                "price", it.getPriceAtPurchase()
        )).toList());
        return ResponseEntity.ok(result);
    }
}
