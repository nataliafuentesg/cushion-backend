package com.cushion.cushion_backend.controller;

import com.cushion.cushion_backend.dto.LoginRequest;
import com.cushion.cushion_backend.dto.ClientDTO;
import com.cushion.cushion_backend.model.Client;
import com.cushion.cushion_backend.service.CartService;
import com.cushion.cushion_backend.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    @Autowired
    private ClientService clientService;

    @Autowired
    private CartService cartService;

    @PostMapping("/login")
    public ClientDTO login(@RequestBody LoginRequest req, @RequestParam(required = false) String sessionId) {
        ClientDTO client = clientService.authenticate(req.getEmail(), req.getPassword());
        if (sessionId != null && !sessionId.isEmpty()) {
            cartService.migrateCart(sessionId, req.getEmail());
        }
        return client;
    }

    @PostMapping("/register")
    public ResponseEntity<ClientDTO> register(@RequestBody Client client) {
        return ResponseEntity.ok(clientService.registerClient(client));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody java.util.Map<String, String> request) {
        String email = request.get("email");
        clientService.createPasswordResetToken(email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody java.util.Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("password");
        clientService.updatePassword(token, newPassword);
        return ResponseEntity.ok().build();
    }
}