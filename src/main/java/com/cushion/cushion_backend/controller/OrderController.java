package com.cushion.cushion_backend.controller;

import com.cushion.cushion_backend.dto.OrderRequestDTO;
import com.cushion.cushion_backend.dto.OrderResponseDTO;
import com.cushion.cushion_backend.model.Order;
import com.cushion.cushion_backend.repository.OrderRepository;
import com.cushion.cushion_backend.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired private OrderService orderService;

    @Autowired private OrderRepository orderRepository;

    @PostMapping("/create")
    public ResponseEntity<OrderResponseDTO> createOrder(
            @RequestBody OrderRequestDTO orderDto,
            @RequestParam String sessionId) {

        Order savedOrder = orderService.createOrderFromCart(orderDto, sessionId);
        OrderResponseDTO response = new OrderResponseDTO();
        response.setOrderNumber(savedOrder.getOrderNumber());
        response.setStatus(savedOrder.getStatus());
        response.setTotalAmount(savedOrder.getTotalAmount());
        response.setCustomerEmail(savedOrder.getCustomerEmail());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<Order>> getClientOrders(@PathVariable Long clientId) {
        return ResponseEntity.ok(orderRepository.findByClient_Id(clientId));
    }
}