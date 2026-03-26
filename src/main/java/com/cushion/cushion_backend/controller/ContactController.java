package com.cushion.cushion_backend.controller;

import com.cushion.cushion_backend.model.ContactMessage;
import com.cushion.cushion_backend.repository.ContactMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contact")
@CrossOrigin(origins = "*")
public class ContactController {

    @Autowired
    private ContactMessageRepository contactRepository;

    // Endpoint para que el Frontend envíe el formulario
    @PostMapping
    public ResponseEntity<ContactMessage> submitContactForm(@RequestBody ContactMessage message) {
        ContactMessage savedMessage = contactRepository.save(message);
        return ResponseEntity.ok(savedMessage);
    }

    // Endpoint para que el Admin Panel vea todos los mensajes que han llegado
    @GetMapping
    public ResponseEntity<List<ContactMessage>> getAllMessages() {
        return ResponseEntity.ok(contactRepository.findAll());
    }
}