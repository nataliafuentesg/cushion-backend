package com.cushion.cushion_backend.controller;

import com.cushion.cushion_backend.dto.ProductInquiryDTO;
import com.cushion.cushion_backend.model.ProductInquiry;
import com.cushion.cushion_backend.service.ProductInquiryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product-inquiries")
public class ProductInquiryController {

    @Autowired private ProductInquiryService service;

    // Público — cualquier visitante puede registrar una consulta
    @PostMapping
    public ResponseEntity<ProductInquiry> register(@RequestBody ProductInquiryDTO dto) {
        return ResponseEntity.ok(service.registerInquiry(dto));
    }

    // Admin — ver todas las consultas
    @GetMapping("/admin")
    public ResponseEntity<List<ProductInquiry>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    // Admin — estadísticas por producto
    @GetMapping("/admin/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(service.getStats());
    }
}
