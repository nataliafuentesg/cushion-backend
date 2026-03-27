package com.cushion.cushion_backend.dto;

import lombok.Data;

@Data
public class ClientDTO {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String role;
}
