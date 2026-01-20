package com.cushion.cushion_backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProductDTO {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private Double price;
    private String category;
    private boolean isFeatured;
    private Integer stock;
    private String gemstoneType;
    private String cutType;
    private String caratWeight;
    private String metalType;
    private String clarity;
    private List<ProductImageDTO> images;
    private List<ReviewDTO> reviews;
}