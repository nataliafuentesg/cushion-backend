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

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;
    @Autowired private JwtService jwtService; // NUEVO
    @Autowired private CustomUserDetailsService userDetailsService;

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