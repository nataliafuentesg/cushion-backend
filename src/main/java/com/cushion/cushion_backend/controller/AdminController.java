// src/main/java/com/cushion/cushion_backend/controller/AdminController.java
package com.cushion.cushion_backend.controller;

import com.cushion.cushion_backend.model.Order;
import com.cushion.cushion_backend.model.Product;
import com.cushion.cushion_backend.repository.OrderRepository;
import com.cushion.cushion_backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired private OrderRepository orderRepository;
    @Autowired private ProductRepository productRepository;

    // --- Gestión de Órdenes ---
    @GetMapping("/orders")
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable Long id, @RequestParam String status) {
        Order order = orderRepository.findById(id).orElseThrow();
        order.setStatus(status); // Ejemplo: "PAGADO", "ENVIADO"
        return ResponseEntity.ok(orderRepository.save(order));
    }

    // --- Gestión de Productos ---
    @PostMapping("/products")
    public Product addProduct(@RequestBody Product product) {
        return productRepository.save(product);
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}