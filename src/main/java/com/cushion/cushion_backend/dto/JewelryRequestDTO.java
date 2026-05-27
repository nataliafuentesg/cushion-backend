package com.cushion.cushion_backend.dto;

import lombok.Data;

@Data
public class JewelryRequestDTO {
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String occasion;
    private String jewelryType;
    private String gemstonePreference;
    private String metalType;
    private String budgetRange;
    private String ideas;
    // "FORMULARIO" o "WHATSAPP" — para medir cuál canal funciona más
    private String contactMethod;
}
