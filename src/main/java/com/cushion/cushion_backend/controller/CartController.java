package com.cushion.cushion_backend.controller;

import com.cushion.cushion_backend.dto.CartDTO;
import com.cushion.cushion_backend.dto.CartItemDTO;
import com.cushion.cushion_backend.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    // Obtener el carrito por Session ID o por ID de Cliente
    @GetMapping("/{sessionId}")
    public ResponseEntity<CartDTO> getCart(@PathVariable String sessionId) {
        return ResponseEntity.ok(cartService.getCartBySession(sessionId));
    }

    // Agregar un producto al carrito
    @PostMapping("/{sessionId}/add")
    public ResponseEntity<CartDTO> addToCart(@PathVariable String sessionId, @RequestBody CartItemDTO itemDTO) {
        return ResponseEntity.ok(cartService.addItemToCart(sessionId, itemDTO));
    }

    // Limpiar el carrito
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> clearCart(@PathVariable String sessionId) {
        cartService.clearCart(sessionId);
        return ResponseEntity.noContent().build();
    }
}