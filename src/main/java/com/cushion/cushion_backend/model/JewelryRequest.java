package com.cushion.cushion_backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "jewelry_requests")
@Getter
@Setter
public class JewelryRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String customerEmail;

    private String customerPhone;

    // Para qué ocasión busca la joya
    private String occasion;

    // Tipo de joya (Anillo, Collar, Aretes, Pulsera, Libre)
    private String jewelryType;

    // Preferencia de gema
    private String gemstonePreference;

    // Preferencia de metal
    private String metalType;

    // Rango de presupuesto
    private String budgetRange;

    // Ideas e inspiración del cliente
    @Column(length = 2000)
    private String ideas;

    // CLAVE: tracking del canal → "FORMULARIO" o "WHATSAPP"
    @Column(nullable = false)
    private String contactMethod;

    // Estado de seguimiento
    @Column(nullable = false)
    private String status;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "PENDIENTE";
    }
}
