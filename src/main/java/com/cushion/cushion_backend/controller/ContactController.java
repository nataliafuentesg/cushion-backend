package com.cushion.cushion_backend.controller;

import com.cushion.cushion_backend.model.ContactMessage;
import com.cushion.cushion_backend.repository.ContactMessageRepository;
import com.cushion.cushion_backend.service.EmailService;
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
    @Autowired private EmailService emailService;

    @PostMapping
    public ResponseEntity<ContactMessage> submitContactForm(@RequestBody ContactMessage message) {
        ContactMessage savedMessage = contactRepository.save(message); //

        String body = """
        <h2 style="color: #B89B6A;">NUEVO MENSAJE DE CONTACTO</h2>
        <p>Has recibido una consulta desde la web:</p>
        <p><b>De:</b> %s (%s)</p>
        <p><b>Mensaje:</b></p>
        <div style="background: #f9f9f9; padding: 15px; border-left: 4px solid #B89B6A;">
            %s
        </div>
        """.formatted(message.getName(), message.getEmail(), message.getMessage());

        emailService.sendHtmlEmail("admin@cushion.com", "📩 Nuevo Contacto: " + message.getName(), body);

        return ResponseEntity.ok(savedMessage);
    }

    @GetMapping
    public ResponseEntity<List<ContactMessage>> getAllMessages() {
        return ResponseEntity.ok(contactRepository.findAll());
    }
}