package com.cushion.cushion_backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_inquiries")
@Getter
@Setter
public class ProductInquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Datos del producto consultado
    @Column(nullable = false)
    private String productSlug;

    private String productName;

    // Canal de la consulta (siempre WHATSAPP por ahora, escalable)
    @Column(nullable = false)
    private String channel;

    // Datos opcionales del visitante (si está logueado)
    private String clientEmail;

    // Fuente de tráfico (UTM params del frontend si los hay)
    private String utmSource;
    private String utmMedium;
    private String utmCampaign;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.channel == null) this.channel = "WHATSAPP";
    }
}
