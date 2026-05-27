package com.cushion.cushion_backend.dto;

import lombok.Data;

@Data
public class ProductInquiryDTO {
    private String productSlug;
    private String productName;
    private String channel;
    private String clientEmail;
    private String utmSource;
    private String utmMedium;
    private String utmCampaign;
}
