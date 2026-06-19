package com.cushion.cushion_backend.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String orderNumber;

    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Double totalAmount;
    private String status;
    private String customerName;
    private String customerEmail;
    private String shippingAddress;
    private String phoneNumber;
    private String notes;

    // Sesión del carrito — para vaciarlo solo cuando el pago se confirme
    private String sessionId;

    // ID de la transacción en Bold (se llena al confirmar el pago vía webhook)
    private String paymentId;

    // Datos de envío — se llenan cuando el admin despacha el pedido
    private String trackingNumber;   // número de guía
    private String shippingCarrier;  // transportadora (Servientrega, Coordinadora, etc.)

    // Vencimiento de la reserva — si no se paga antes de esta hora, el stock se libera
    private LocalDateTime expiresAt;

    // Atribución de campaña — de qué anuncio/campaña vino esta venta
    private String utmSource;
    private String utmMedium;
    private String utmCampaign;
    private String utmContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    @JsonIgnore
    private Client client;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "PENDIENTE_PAGO";
    }
}