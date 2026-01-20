package com.cushion.cushion_backend.dto;

import lombok.Data;

@Data
public class ProductImageDTO {
    private String imageUrl;
    private String altText;
    private boolean isThumbnail;
}
