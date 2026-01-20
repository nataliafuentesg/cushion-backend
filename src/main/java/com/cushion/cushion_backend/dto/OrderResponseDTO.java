package com.cushion.cushion_backend.dto;

import lombok.Data;

@Data
public class OrderResponseDTO {
    private String orderNumber;
    private String status;
    private Double totalAmount;
    private String customerEmail;
}
