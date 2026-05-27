package com.cushion.cushion_backend.controller;

import com.cushion.cushion_backend.dto.ProductInquiryDTO;
import com.cushion.cushion_backend.model.ProductInquiry;
import com.cushion.cushion_backend.service.ProductInquiryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product-inquiries")
public class ProductInquiryController {

    @Autowired private ProductInquiryService service;

    @PostMapping
    public ResponseEntity<ProductInquiry> register(@RequestBody ProductInquiryDTO dto,
                                                    HttpServletRequest request) {
        String ip = extractIp(request);
        String ua = request.getHeader("User-Agent");
        return ResponseEntity.ok(service.registerInquiry(dto, ip, ua));
    }

    @GetMapping("/admin")
    public ResponseEntity<List<ProductInquiry>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/admin/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(service.getStats());
    }

    private String extractIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        return ip != null ? ip.split(",")[0].trim() : null;
    }
}
