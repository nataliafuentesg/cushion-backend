package com.cushion.cushion_backend.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ReviewDTO {
    private Long id;
    private String author;
    private int rating;
    private String comment;
    private LocalDate date;
    private String subtitleVerification;
}