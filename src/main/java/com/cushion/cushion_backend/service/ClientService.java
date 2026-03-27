package com.cushion.cushion_backend.service;
import com.cushion.cushion_backend.dto.ClientDTO;
import com.cushion.cushion_backend.model.Cart;
import com.cushion.cushion_backend.model.Client;
import com.cushion.cushion_backend.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

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

        // Ahora save(client) funcionará porque el repositorio es de tipo Client
        return convertToDTO(clientRepository.save(client));
    }

    public ClientDTO authenticate(String email, String rawPassword) {
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        if (!passwordEncoder.matches(rawPassword, client.getPassword())) {
            throw new RuntimeException("Contraseña incorrecta");
        }
        return convertToDTO(client);
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