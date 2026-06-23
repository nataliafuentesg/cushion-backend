package com.cushion.cushion_backend.dto;

import lombok.Data;

@Data
public class CartItemDTO {
    private Long productId;
    private String productName;
    private Integer quantity;
    private Double price;
    private String imageUrl;
    private Integer stock;
    private String selectedSize; // talla elegida (anillo/collar/pulsera)
}