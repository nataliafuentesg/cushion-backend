package com.cushion.cushion_backend.service;

import com.cushion.cushion_backend.model.Cart;
import com.cushion.cushion_backend.model.Client;
import com.cushion.cushion_backend.repository.CartRepository;
import com.cushion.cushion_backend.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    @Autowired private CartRepository cartRepository;
    @Autowired private ClientRepository clientRepository;

    @Transactional
    public void migrateCart(String sessionId, String clientEmail) {
        Client client = clientRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        cartRepository.findBySessionId(sessionId).ifPresent(guestCart -> {
            Cart clientCart = cartRepository.findByClientId(client.getId()).orElse(new Cart());

            guestCart.getItems().forEach(item -> {
                item.setCart(clientCart);
                clientCart.getItems().add(item);
            });

            clientCart.setClient(client);
            clientCart.setSessionId(null);
            cartRepository.save(clientCart);
            cartRepository.delete(guestCart);
        });
    }
}