package com.cushion.cushion_backend.repository;

import com.cushion.cushion_backend.model.ProductInquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Map;

public interface ProductInquiryRepository extends JpaRepository<ProductInquiry, Long> {
    List<ProductInquiry> findByProductSlug(String productSlug);
    long countByChannel(String channel);

    @Query("SELECT p.productSlug, p.productName, COUNT(p) as total FROM ProductInquiry p GROUP BY p.productSlug, p.productName ORDER BY total DESC")
    List<Object[]> findTopProducts();
}
