package com.cushion.cushion_backend.config;

import com.cushion.cushion_backend.model.Product;
import com.cushion.cushion_backend.model.ProductImage;
import com.cushion.cushion_backend.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(ProductRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                // Producto 1: Anillo
                Product p1 = new Product();
                p1.setName("Anillo Aura Esmeralda");
                p1.setSlug("anillo-aura-esmeralda");
                p1.setPrice(1800.0);
                p1.setCategory("Anillos");
                p1.setFeatured(true);
                p1.setDescription("Un anillo exquisito con una esmeralda central de talla cushion, rodeado de diamantes finos.");
                p1.setGemstoneType("Esmeralda Natural");
                p1.setCutType("Cushion");
                p1.setCaratWeight("1.5 ct");
                p1.setMetalType("Oro Amarillo 18k");

                ProductImage img1 = new ProductImage();
                img1.setImageUrl("https://images.unsplash.com/photo-1605100804763-247f67b3557e?q=80&w=1000");
                img1.setAltText("Vista principal Anillo Aura");
                img1.setThumbnail(true);
                p1.addImage(img1);

                // Producto 2: Collar
                Product p2 = new Product();
                p2.setName("Collar Gota del Eterno");
                p2.setSlug("collar-gota-del-eterno");
                p2.setPrice(2500.0);
                p2.setCategory("Collares");
                p2.setFeatured(true);
                p2.setDescription("Collar elegante con esmeralda en forma de gota, símbolo de pureza y sofisticación.");
                p2.setGemstoneType("Esmeralda Colombiana");
                p2.setCutType("Gota / Pera");
                p2.setCaratWeight("2.0 ct");
                p2.setMetalType("Oro Blanco 18k");

                ProductImage img2 = new ProductImage();
                img2.setImageUrl("https://images.unsplash.com/photo-1515562141207-7a18b5ce7142?q=80&w=1000");
                img2.setAltText("Detalle Collar Gota");
                img2.setThumbnail(true);
                p2.addImage(img2);

                // Producto 3: Aretes
                Product p3 = new Product();
                p3.setName("Aretes Sol y Luna");
                p3.setSlug("aretes-sol-y-luna");
                p3.setPrice(1250.0);
                p3.setCategory("Aretes");
                p3.setFeatured(false);
                p3.setGemstoneType("Esmeralda");
                p3.setCutType("Redondo");
                p3.setCaratWeight("0.8 ct cada uno");

                ProductImage img3 = new ProductImage();
                img3.setImageUrl("https://images.unsplash.com/photo-1535632066927-ab7c9ab60908?q=80&w=1000");
                img3.setThumbnail(true);
                p3.addImage(img3);

                repository.saveAll(List.of(p1, p2, p3));
                System.out.println("Base de datos alimentada con productos de prueba.");
            }
        };
    }
}