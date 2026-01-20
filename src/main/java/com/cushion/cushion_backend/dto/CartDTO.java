package com.cushion.cushion_backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class CartDTO {
    private List<CartItemDTO> items;
    private Double total;
}
