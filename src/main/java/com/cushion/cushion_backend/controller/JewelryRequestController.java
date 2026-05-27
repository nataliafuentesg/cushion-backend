package com.cushion.cushion_backend.controller;

import com.cushion.cushion_backend.dto.JewelryRequestDTO;
import com.cushion.cushion_backend.model.JewelryRequest;
import com.cushion.cushion_backend.service.JewelryRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jewelry-requests")
public class JewelryRequestController {

    @Autowired private JewelryRequestService jewelryRequestService;

    @PostMapping
    public ResponseEntity<JewelryRequest> create(@RequestBody JewelryRequestDTO dto) {
        JewelryRequest saved = jewelryRequestService.createRequest(dto);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/admin")
    public ResponseEntity<List<JewelryRequest>> getAll() {
        return ResponseEntity.ok(jewelryRequestService.getAllRequests());
    }

    @PutMapping("/admin/{id}/status")
    public ResponseEntity<JewelryRequest> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(jewelryRequestService.updateStatus(id, status));
    }

    @GetMapping("/admin/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(jewelryRequestService.getStats());
    }
}
