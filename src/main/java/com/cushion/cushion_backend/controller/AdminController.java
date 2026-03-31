package com.cushion.cushion_backend.controller;

import com.cushion.cushion_backend.model.Order;
import com.cushion.cushion_backend.model.Product;
import com.cushion.cushion_backend.model.Review;
import com.cushion.cushion_backend.repository.OrderRepository;
import com.cushion.cushion_backend.repository.ProductRepository;
import com.cushion.cushion_backend.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired private OrderRepository orderRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ReviewRepository reviewRepository; // Asume que tienes este repositorio creado

    @GetMapping("/orders")
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable Long id, @RequestParam String status) {
        Order order = orderRepository.findById(id).orElseThrow();
        order.setStatus(status);
        return ResponseEntity.ok(orderRepository.save(order));
    }

    // --- GESTIÓN DE PRODUCTOS (ALTA JOYERÍA) ---
    @PostMapping("/products")
    public Product addProduct(@RequestBody Product product) {
        // Enlazar las imágenes al producto antes de guardar
        if (product.getImages() != null) {
            product.getImages().forEach(img -> img.setProduct(product));
        }
        return productRepository.save(product);
    }

    @Transactional
    @PutMapping("/products/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product productDetails) {
        return productRepository.findById(id)
                .map(existingProduct -> {
                    // Datos básicos
                    existingProduct.setName(productDetails.getName());
                    existingProduct.setSlug(productDetails.getSlug());
                    existingProduct.setDescription(productDetails.getDescription());
                    existingProduct.setPrice(productDetails.getPrice());
                    existingProduct.setCategory(productDetails.getCategory());
                    existingProduct.setStock(productDetails.getStock());
                    existingProduct.setFeatured(productDetails.isFeatured());

                    // Especificaciones de Alta Joyería
                    existingProduct.setGemstoneType(productDetails.getGemstoneType());
                    existingProduct.setCutType(productDetails.getCutType());
                    existingProduct.setCaratWeight(productDetails.getCaratWeight());
                    existingProduct.setMetalType(productDetails.getMetalType());
                    existingProduct.setClarity(productDetails.getClarity());

                    // Actualización de Imágenes (Reemplazo total)
                    existingProduct.getImages().clear();
                    if (productDetails.getImages() != null) {
                        productDetails.getImages().forEach(img -> {
                            img.setProduct(existingProduct);
                            existingProduct.getImages().add(img);
                        });
                    }

                    // ✨ PASO 1: Guardamos el producto en una variable temporal
                    Product savedProduct = productRepository.save(existingProduct);

                    // ✨ PASO 2: "Despertamos" las colecciones perezosas antes de que Java arme el JSON
                    savedProduct.getImages().size();
                    if (savedProduct.getReviews() != null) {
                        savedProduct.getReviews().size();
                    }

                    // ✨ PASO 3: Retornamos el producto ya despierto y listo
                    return ResponseEntity.ok(savedProduct);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Transactional
    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- GESTIÓN DE RESEÑAS ---
    @GetMapping("/reviews")
    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}