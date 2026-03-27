package com.cushion.cushion_backend.service;
import com.cushion.cushion_backend.dto.ClientDTO;
import com.cushion.cushion_backend.model.Cart;
import com.cushion.cushion_backend.model.Client;
import com.cushion.cushion_backend.repository.ClientRepository;
import com.cushion.cushion_backend.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;
    @Autowired private JwtService jwtService; // NUEVO
    @Autowired private CustomUserDetailsService userDetailsService;
    @Autowired private EmailService emailService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional(readOnly = true)
    public ClientDTO getClientByEmail(String email) {
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        return convertToDTO(client);
    }

    @Transactional
    public ClientDTO registerClient(Client client) {
        if (clientRepository.findByEmail(client.getEmail()).isPresent()) {
            throw new RuntimeException("El correo ya existe");
        }
        client.setPassword(passwordEncoder.encode(client.getPassword()));

        Cart cart = new Cart();
        cart.setClient(client);
        client.setCart(cart);

        Client savedClient = clientRepository.save(client);

        UserDetails userDetails = userDetailsService.loadUserByUsername(savedClient.getEmail());
        String token = jwtService.generateToken(userDetails);

        ClientDTO dto = convertToDTO(savedClient);
        dto.setToken(token);
        return dto;
    }

    public ClientDTO authenticate(String email, String rawPassword) {
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        if (!passwordEncoder.matches(rawPassword, client.getPassword())) {
            throw new RuntimeException("Contraseña incorrecta");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(client.getEmail());
        String token = jwtService.generateToken(userDetails);

        ClientDTO dto = convertToDTO(client);
        dto.setToken(token); // Se lo adjuntamos a la respuesta
        return dto;
    }

    @Transactional
    public void createPasswordResetToken(String email) {
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No existe un usuario con ese correo"));

        client.setResetPasswordToken(UUID.randomUUID().toString());
        client.setTokenExpiration(LocalDateTime.now().plusMinutes(30));
        clientRepository.save(client);

        // Preparamos el cuerpo del correo con diseño HTML
        String body = """
            <h2 style="color: #B89B6A;">RECUPERACIÓN DE CONTRASEÑA</h2>
            <p>Has solicitado restablecer tu acceso a <b>Cushion</b>.</p>
            <p>Haz clic en el siguiente botón para continuar:</p>
            <div style="text-align: center; margin: 30px 0;">
                <a href="https://cushion.com/reset-password?token=%s" 
                   style="background: #1a1a1a; color: #fff; padding: 12px 25px; text-decoration: none; font-weight: bold; border: 1px solid #B89B6A; display: inline-block;">
                   RESTABLECER MI CLAVE
                </a>
            </div>
            <p style="font-size: 11px; color: #888;">Este enlace expirará en 30 minutos por seguridad.</p>
            <p style="font-size: 11px; color: #888;">Si no solicitaste este cambio, puedes ignorar este correo.</p>
            """.formatted(client.getResetPasswordToken());

        emailService.sendHtmlEmail(client.getEmail(), "Recuperar Contraseña - Cushion", body);
    }

    // --- NUEVO: 2. VALIDAR TOKEN Y CAMBIAR CONTRASEÑA ---
    @Transactional
    public void updatePassword(String token, String newPassword) {
        Client client = clientRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new RuntimeException("El enlace de recuperación es inválido"));

        // Verificamos si el token ya expiró
        if (client.getTokenExpiration().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("El enlace de recuperación ha expirado");
        }

        // Encriptamos la nueva contraseña antes de guardarla
        client.setPassword(passwordEncoder.encode(newPassword));

        // Limpiamos los campos de recuperación para que el token no se use de nuevo
        client.setResetPasswordToken(null);
        client.setTokenExpiration(null);

        clientRepository.save(client);

        // Opcional: Enviar correo de confirmación de cambio exitoso
        String confirmBody = "<p>Tu contraseña de <b>Cushion</b> ha sido actualizada exitosamente.</p>";
        emailService.sendHtmlEmail(client.getEmail(), "Contraseña Actualizada - Cushion", confirmBody);
    }

    private ClientDTO convertToDTO(Client client) {
        ClientDTO dto = new ClientDTO();
        dto.setId(client.getId());
        dto.setEmail(client.getEmail());
        dto.setFirstName(client.getFirstName());
        dto.setLastName(client.getLastName());
        dto.setPhone(client.getPhone());
        dto.setRole(client.getRole());
        return dto;
    }
}