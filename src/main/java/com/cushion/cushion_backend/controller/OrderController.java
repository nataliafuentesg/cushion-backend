package com.cushion.cushion_backend.controller;

import com.cushion.cushion_backend.dto.OrderRequestDTO;
import com.cushion.cushion_backend.dto.OrderResponseDTO;
import com.cushion.cushion_backend.model.Order;
import com.cushion.cushion_backend.repository.OrderRepository;
import com.cushion.cushion_backend.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
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

    @PostMapping("/create")
    public ResponseEntity<?> createOrder(
            @RequestBody OrderRequestDTO orderDto,
            @RequestParam String sessionId,
            HttpServletRequest request) {
        try {
            String ip = extractIp(request);
            String ua = request.getHeader("User-Agent");

            Order savedOrder = orderService.createOrderFromCart(orderDto, sessionId, ip, ua);

            OrderResponseDTO response = new OrderResponseDTO();
            response.setOrderNumber(savedOrder.getOrderNumber());
            response.setStatus(savedOrder.getStatus());
            response.setTotalAmount(savedOrder.getTotalAmount());
            response.setCustomerEmail(savedOrder.getCustomerEmail());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<Order>> getClientOrders(@PathVariable Long clientId) {
        return ResponseEntity.ok(orderRepository.findByClient_Id(clientId));
    }

    private String extractIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        return ip != null ? ip.split(",")[0].trim() : null;
    }
}
