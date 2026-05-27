package com.cushion.cushion_backend.repository;

import com.cushion.cushion_backend.model.JewelryRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JewelryRequestRepository extends JpaRepository<JewelryRequest, Long> {
    List<JewelryRequest> findByContactMethod(String contactMethod);
    List<JewelryRequest> findByStatus(String status);
    long countByContactMethod(String contactMethod);
}
