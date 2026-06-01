package com.cushion.cushion_backend.dto;

import lombok.Data;

@Data
public class ProductInquiryDTO {
    private String productSlug;
    private String productName;
    private String channel;
    private String clientEmail;
    private String eventId;      // event_id del navegador para deduplicar el Contact en Meta
    private String utmSource;
    private String utmMedium;
    private String utmCampaign;
}
