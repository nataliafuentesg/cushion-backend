package com.cushion.cushion_backend.service;

import com.cushion.cushion_backend.dto.CartDTO;
import com.cushion.cushion_backend.dto.CartItemDTO;
import com.cushion.cushion_backend.model.Cart;
import com.cushion.cushion_backend.model.CartItem;
import com.cushion.cushion_backend.model.Client;
import com.cushion.cushion_backend.model.Product;
import com.cushion.cushion_backend.repository.CartRepository;
import com.cushion.cushion_backend.repository.ClientRepository;
import com.cushion.cushion_backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired private CartRepository cartRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ClientRepository clientRepository;

    @Transactional
    public void migrateCart(String sessionId, String clientEmail) {
        Client client = clientRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        cartRepository.findBySessionId(sessionId).ifPresent(guestCart -> {
            // Buscar si el cliente ya tiene un carrito, si no, crear uno nuevo
            Cart clientCart = cartRepository.findByClientId(client.getId())
                    .orElseGet(() -> {
                        Cart newCart = new Cart();
                        newCart.setClient(client);
                        return newCart;
                    });

            // Pasar los items del carrito de invitado al de cliente
            for (CartItem guestItem : guestCart.getItems()) {
                // Verificar si el producto ya existe en el carrito del cliente para sumar cantidades
                Optional<CartItem> existingItem = clientCart.getItems().stream()
                        .filter(item -> item.getProduct().getId().equals(guestItem.getProduct().getId()))
                        .findFirst();

                if (existingItem.isPresent()) {
                    existingItem.get().setQuantity(existingItem.get().getQuantity() + guestItem.getQuantity());
                } else {
                    guestItem.setCart(clientCart);
                    clientCart.getItems().add(guestItem);
                }
            }

            clientCart.setSessionId(null); // El carrito ya no es de sesión, es de cliente
            cartRepository.save(clientCart);

            // Borramos el carrito temporal de invitado
            guestCart.getItems().clear();
            cartRepository.delete(guestCart);
        });
    }

    @Transactional
    public CartDTO addItemToCart(String sessionId, CartItemDTO itemDTO) {
        Cart cart = cartRepository.findBySessionId(sessionId).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setSessionId(sessionId);
            return cartRepository.save(newCart);
        });
        Product product = productRepository.findById(itemDTO.getProductId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst();

        int currentQtyInCart = existingItem.map(CartItem::getQuantity).orElse(0);
        int newTotalQty = currentQtyInCart + itemDTO.getQuantity();
        if (newTotalQty <= 0) {
            existingItem.ifPresent(item -> {
                cart.getItems().remove(item);
                item.setCart(null);
            });
        }
        else if (newTotalQty > product.getStock()) {
            throw new RuntimeException("Solo quedan " + product.getStock() + " unidades disponibles de esta pieza.");
        }
        else {
            if (existingItem.isPresent()) {
                existingItem.get().setQuantity(newTotalQty);
            } else {
                CartItem newItem = new CartItem();
                newItem.setProduct(product);
                newItem.setQuantity(newTotalQty);
                newItem.setCart(cart);
                cart.getItems().add(newItem);
            }
        }
        Cart savedCart = cartRepository.save(cart);
        return convertToDTO(savedCart);
    }

    @Transactional(readOnly = true)
    public CartDTO getCartBySession(String sessionId) {
        Cart cart = cartRepository.findBySessionId(sessionId).orElse(new Cart());
        return convertToDTO(cart);
    }

    private CartDTO convertToDTO(Cart cart) {
        CartDTO dto = new CartDTO();
        List<CartItemDTO> itemDTOs = cart.getItems().stream().map(item -> {
            CartItemDTO iDto = new CartItemDTO();
            iDto.setProductId(item.getProduct().getId());
            iDto.setProductName(item.getProduct().getName());
            iDto.setQuantity(item.getQuantity());
            iDto.setPrice(item.getProduct().getPrice());
            iDto.setStock(item.getProduct().getStock());

            if (!item.getProduct().getImages().isEmpty()) {
                iDto.setImageUrl(item.getProduct().getImages().get(0).getImageUrl());
            }
            return iDto;
        }).collect(Collectors.toList());

        dto.setItems(itemDTOs);
        dto.setTotal(itemDTOs.stream().mapToDouble(i -> i.getPrice() * i.getQuantity()).sum());
        return dto;
    }

    @Transactional
    public void clearCart(String sessionId) {
        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Carrito no encontrado"));
        cart.getItems().clear();
        cartRepository.save(cart);
    }
}