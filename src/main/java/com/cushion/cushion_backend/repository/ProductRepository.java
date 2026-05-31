package com.cushion.cushion_backend.repository;

import com.cushion.cushion_backend.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySlug(String slug);
    List<Product> findByCategory(String category);
    List<Product> findByFeaturedTrue();

    // Trae todos los productos con sus imágenes ya cargadas (evita LazyInitializationException
    // al sincronizar en un hilo asíncrono fuera de la sesión de Hibernate).
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.images")
    List<Product> findAllWithImages();
}
