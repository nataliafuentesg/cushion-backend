package com.cushion.cushion_backend.controller;

import com.cushion.cushion_backend.dto.ReviewDTO;
import com.cushion.cushion_backend.model.Order;
import com.cushion.cushion_backend.model.Product;
import com.cushion.cushion_backend.model.Review;
import com.cushion.cushion_backend.repository.OrderRepository;
import com.cushion.cushion_backend.repository.ProductRepository;
import com.cushion.cushion_backend.repository.ReviewRepository;
import com.cushion.cushion_backend.service.GoogleMerchantService;
import com.cushion.cushion_backend.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired private OrderRepository orderRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ReviewRepository reviewRepository; // Asume que tienes este repositorio creado
    @Autowired private GoogleMerchantService googleMerchantService;
    @Autowired private OrderService orderService;

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

    /**
     * Confirmar pago manualmente — para órdenes internacionales coordinadas por
     * correo (que no pasan por el webhook de Bold). Descuenta inventario, marca
     * PAGADO y dispara las notificaciones, igual que un pago automático.
     */
    @PostMapping("/orders/{orderNumber}/confirm-payment")
    public ResponseEntity<?> confirmPaymentManually(@PathVariable String orderNumber) {
        Order confirmed = orderService.confirmPayment(orderNumber, "MANUAL_ADMIN");
        if (confirmed == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "La orden no existe o ya estaba pagada."));
        }
        orderService.sendPaidOrderNotifications(confirmed);
        return ResponseEntity.ok(Map.of(
                "message", "Pago confirmado y notificaciones enviadas.",
                "orderNumber", confirmed.getOrderNumber()));
    }

    /**
     * Marcar pedido como ENVIADO — guarda guía + transportadora y envía el correo
     * de envío al cliente.
     */
    @PostMapping("/orders/{id}/ship")
    public ResponseEntity<?> shipOrder(
            @PathVariable Long id,
            @RequestParam String trackingNumber,
            @RequestParam String carrier) {
        Order order = orderService.markShipped(id, trackingNumber, carrier);
        return ResponseEntity.ok(order);
    }

    /** Marcar pedido como ENTREGADO — envía el correo de agradecimiento. */
    @PostMapping("/orders/{id}/deliver")
    public ResponseEntity<?> deliverOrder(@PathVariable Long id) {
        Order order = orderService.markDelivered(id);
        return ResponseEntity.ok(order);
    }

    /**
     * Exporta TODAS las ventas a un archivo CSV (se abre en Excel/Google Sheets).
     * Incluye el ID de transacción de Bold para control de ventas y comisiones.
     */
    @GetMapping(value = "/orders/export", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<byte[]> exportOrders() {
        List<Order> orders = orderRepository.findAll();
        orders.sort((a, b) -> {
            if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
            return b.getCreatedAt().compareTo(a.getCreatedAt()); // más recientes primero
        });

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        StringBuilder sb = new StringBuilder();
        sb.append('﻿'); // BOM — para que Excel reconozca los acentos (UTF-8)
        sb.append("Numero de Orden,Fecha,Estado,Cliente,Email,Telefono,Total (COP),")
          .append("ID Transaccion Bold,Transportadora,Guia,Direccion\n");

        for (Order o : orders) {
            sb.append(csv(o.getOrderNumber())).append(',')
              .append(o.getCreatedAt() != null ? o.getCreatedAt().format(fmt) : "").append(',')
              .append(csv(o.getStatus())).append(',')
              .append(csv(o.getCustomerName())).append(',')
              .append(csv(o.getCustomerEmail())).append(',')
              .append(csv(o.getPhoneNumber())).append(',')
              .append(o.getTotalAmount() != null ? String.format("%.0f", o.getTotalAmount()) : "0").append(',')
              .append(csv(o.getPaymentId())).append(',')
              .append(csv(o.getShippingCarrier())).append(',')
              .append(csv(o.getTrackingNumber())).append(',')
              .append(csv(o.getShippingAddress())).append('\n');
        }

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        String filename = "cushion-ventas-" + java.time.LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(bytes);
    }

    /** Escapa un valor para CSV (comillas si tiene comas, comillas o saltos). */
    private String csv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    // --- GESTIÓN DE PRODUCTOS (ALTA JOYERÍA) ---
    @PostMapping("/products")
    public Product addProduct(@RequestBody Product product) {
        // Enlazar las imágenes al producto antes de guardar
        if (product.getImages() != null) {
            product.getImages().forEach(img -> img.setProduct(product));
        }
        Product saved = productRepository.save(product);
        googleMerchantService.upsertProduct(saved); // Sincroniza con Google Merchant
        return saved;
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
                    existingProduct.setGemstoneType(productDetails.getGemstoneType());
                    existingProduct.setCutType(productDetails.getCutType());
                    existingProduct.setCaratWeight(productDetails.getCaratWeight());
                    existingProduct.setMetalType(productDetails.getMetalType());
                    existingProduct.setClarity(productDetails.getClarity());

                    existingProduct.setTotalWeight(productDetails.getTotalWeight());
                    existingProduct.setDiamondDetails(productDetails.getDiamondDetails());

                    existingProduct.getImages().clear();
                    if (productDetails.getImages() != null) {
                        productDetails.getImages().forEach(img -> {
                            img.setProduct(existingProduct);
                            existingProduct.getImages().add(img);
                        });
                    }

                    existingProduct.getOccasions().clear();
                    if (productDetails.getOccasions() != null) {
                        existingProduct.getOccasions().addAll(productDetails.getOccasions());
                    }
                    Product savedProduct = productRepository.save(existingProduct);

                    savedProduct.getImages().size();
                    savedProduct.getOccasions().size(); // Despertamos la nueva lista de ocasiones
                    if (savedProduct.getReviews() != null) {
                        savedProduct.getReviews().size();
                    }

                    googleMerchantService.upsertProduct(savedProduct); // Sincroniza con Google Merchant

                    return ResponseEntity.ok(savedProduct);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    @Transactional
    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productRepository.deleteById(id);
        googleMerchantService.deleteProduct(id); // Elimina también de Google Merchant
        return ResponseEntity.noContent().build();
    }

    /**
     * Sincronización masiva manual — sube TODO el catálogo a Google Merchant.
     * Úsalo una vez para la primera carga, o si necesitas re-sincronizar todo.
     * GET /api/admin/merchant/sync-all
     */
    @PostMapping("/merchant/sync-all")
    public ResponseEntity<Map<String, Object>> syncAllToMerchant() {
        if (!googleMerchantService.isEnabled()) {
            return ResponseEntity.ok(Map.of(
                    "status", "disabled",
                    "message", "Google Merchant no está configurado en el servidor."
            ));
        }
        List<Product> all = productRepository.findAllWithImages(); // imágenes precargadas para el hilo async
        googleMerchantService.syncAll(all); // asíncrono — corre en background
        return ResponseEntity.ok(Map.of(
                "status", "started",
                "message", "Sincronización de " + all.size() + " productos iniciada en segundo plano.",
                "total", all.size()
        ));
    }

    @GetMapping("/reviews")
    public List<ReviewDTO> getAllReviews() {
        List<Review> reviews = reviewRepository.findAll();

        return reviews.stream().map(review -> {
            ReviewDTO dto = new ReviewDTO();
            dto.setId(review.getId());
            dto.setAuthor(review.getAuthor());
            dto.setRating(review.getRating());
            dto.setComment(review.getComment());
            dto.setDate(review.getDate());

            if (review.getProduct() != null) {
                dto.setProductName(review.getProduct().getName());
            } else {
                dto.setProductName("Producto no disponible");
            }

            return dto;
        }).collect(Collectors.toList());
    }

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}