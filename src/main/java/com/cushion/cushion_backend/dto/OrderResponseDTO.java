package com.cushion.cushion_backend.dto;

import lombok.Data;

@Data
public class OrderResponseDTO {
    private String orderNumber;
    private String status;
    private Double totalAmount;
    private String customerEmail;

    // ── Datos para el botón de pago de Bold ──
    private Long boldAmount;          // monto entero en COP (sin decimales)
    private String boldCurrency;      // "COP"
    private String boldApiKey;        // llave de identidad (pública)
    private String boldIntegritySignature; // firma SHA256 del botón
}
