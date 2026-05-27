package com.cushion.cushion_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ProductImageDTO {
    private String imageUrl;
    private String altText;
    @JsonProperty("isThumbnail")
    private boolean isThumbnail;
}
