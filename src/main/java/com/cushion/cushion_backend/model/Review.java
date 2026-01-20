package com.cushion.cushion_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String author;
    private int rating; // 1-5
    @Column(length = 1000)
    private String comment;
    private LocalDate date;
    @ManyToOne
    @JsonIgnore
    private Product product;
}