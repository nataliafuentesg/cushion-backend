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
@CrossOrigin(origins = "*")
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
}