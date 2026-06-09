package com.cushion.cushion_backend.repository;

import com.cushion.cushion_backend.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByClient_Id(Long clientId);

    // Órdenes pendientes cuya reserva ya venció (para liberar su stock)
    List<Order> findByStatusAndExpiresAtBefore(String status, LocalDateTime time);
}
