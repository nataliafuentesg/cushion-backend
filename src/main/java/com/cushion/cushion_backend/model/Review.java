package com.cushion.cushion_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String author;
    @Column(length = 500, nullable = false)
    private String comment;
    @Min(1) @Max(5)
    private int rating;
    private LocalDate date;
    @ManyToOne
    @JsonIgnore
    private Product product;
}