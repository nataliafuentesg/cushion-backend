package com.cushion.cushion_backend.service;
import com.cushion.cushion_backend.dto.OrderRequestDTO;
import com.cushion.cushion_backend.model.*;
import com.cushion.cushion_backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private ProductRepository productRepository;

    @Transactional
    public Order createOrderFromCart(OrderRequestDTO dto, String sessionId) {
        Cart cart = cartRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("No se encontró un carrito para la sesión: " + sessionId));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("El carrito está vacío. No se puede generar la orden.");
        }
        Order order = new Order();
        order.setOrderNumber("CUSH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setCustomerName(dto.getCustomerName());
        order.setCustomerEmail(dto.getCustomerEmail());
        order.setShippingAddress(dto.getShippingAddress());
        order.setPhoneNumber(dto.getPhoneNumber());
        order.setNotes(dto.getNotes());
        order.setStatus("PENDIENTE_PAGO");

        double subtotal = 0;
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            if (product.getStock() < cartItem.getQuantity()) {
                throw new RuntimeException("Stock insuficiente para: " + product.getName() +
                        " (Disponible: " + product.getStock() + ")");
            }
            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPriceAtPurchase(product.getPrice());
            orderItem.setOrder(order);
            order.getItems().add(orderItem);

            subtotal += product.getPrice() * cartItem.getQuantity();
        }
        double shippingFee = 25000.0;
        order.setTotalAmount(subtotal + shippingFee);
        Order savedOrder = orderRepository.save(order);
        cart.getItems().clear();
        cartRepository.save(cart);

        return savedOrder;
    }
}