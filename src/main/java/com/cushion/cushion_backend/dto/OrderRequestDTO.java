package com.cushion.cushion_backend.dto;

import lombok.Data;

@Data
public class OrderRequestDTO {
    private String customerName;
    private String customerEmail;
    private String shippingAddress;
    private String phoneNumber;
    private String notes;
    private Long clientId;

    // Atribución de campaña (de qué anuncio vino la venta)
    private String utmSource;
    private String utmMedium;
    private String utmCampaign;
    private String utmContent;
}