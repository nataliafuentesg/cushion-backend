package com.cushion.cushion_backend.model;
import jakarta.persistence.*;
import lombok.Data;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "products")
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private String category; // Anillos, Collares, etc.

    private Integer stock;
    private boolean featured;

    private String gemstoneType;
    private String cutType;
    private String caratWeight;
    private String metalType;
    private String clarity;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();

    public void addImage(ProductImage image) {
        images.add(image);
        image.setProduct(this);
    }

    public void addReview(Review review) {
        reviews.add(review);
        review.setProduct(this);
    }
}