package com.cushion.cushion_backend.controller;

import com.cushion.cushion_backend.dto.OrderRequestDTO;
import com.cushion.cushion_backend.model.Order;
import com.cushion.cushion_backend.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired private OrderService orderService;

    @PostMapping("/create")
    public ResponseEntity<Order> createOrder(
            @RequestBody OrderRequestDTO orderDto,
            @RequestParam Long cartId) {
        return ResponseEntity.ok(orderService.createOrderFromCart(orderDto, cartId));
    }
}