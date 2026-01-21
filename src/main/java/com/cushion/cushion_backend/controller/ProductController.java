package com.cushion.cushion_backend.controller;
import com.cushion.cushion_backend.dto.ProductDTO;
import com.cushion.cushion_backend.dto.ReviewDTO;
import com.cushion.cushion_backend.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping
    public List<ProductDTO> getAll() {
        return productService.getAllProducts();
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ProductDTO> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(productService.getProductBySlug(slug));
    }

    @PostMapping
    public ResponseEntity<ProductDTO> create(@RequestBody ProductDTO productDTO) {
        return ResponseEntity.ok(productService.createProduct(productDTO));
    }

    @PostMapping("/{slug}/reviews")
    public ResponseEntity<ReviewDTO> addReview(
            @PathVariable String slug,
            @Valid @RequestBody ReviewDTO reviewDto
    ) {
        return ResponseEntity.ok(productService.addReview(slug, reviewDto));
    }
}