package com.cushion.cushion_backend.config;

import com.cushion.cushion_backend.model.Product;
import com.cushion.cushion_backend.model.ProductImage;
import com.cushion.cushion_backend.model.Review;
import com.cushion.cushion_backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.cushion.cushion_backend.model.Client;
import com.cushion.cushion_backend.repository.ClientRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        if (clientRepository.findByEmail("admin@cushion.com").isEmpty()) {
            Client admin = new Client();
            admin.setEmail("admin@cushion.com");
            // Encriptamos la clave directamente para que coincida con el sistema
            admin.setPassword(new BCryptPasswordEncoder().encode("CushionAdmin2026"));
            admin.setFirstName("Luis Carlos"); // O tu nombre
            admin.setLastName("Admin");
            admin.setRole("ADMIN");
            clientRepository.save(admin);
            System.out.println("Usuario Administrador creado con éxito.");
        }

        if (productRepository.count() > 0) return;

        List<Product> products = new ArrayList<>();

        // 1. ANILLO AURA ESMERALDA
        Product p1 = createProduct("Anillo Aura Esmeralda", "anillo-aura-esmeralda",
                "Una pieza maestra que captura la esencia de las montañas de Muzo. Esta esmeralda central está rodeada por un halo de diamantes de talla brillante, montada en una banda de oro sólido.",
                4500000.0, "Anillos", "Esmeralda Colombiana", "Oval", "2.5 ct", "Oro Amarillo 18k", "VVS1", 5, true);
        addImages(p1, "https://images.unsplash.com/photo-1605100804763-247f67b3557e?q=80&w=800");
        addReview(p1, "Isabella M.", 5, "El color de la esmeralda es simplemente hipnotizante. El empaque de lujo es de otro nivel.");
        products.add(p1);

        // 2. COLLAR GOTA DEL ETERNO
        Product p2 = createProduct("Collar Gota del Eterno", "collar-gota-del-eterno",
                "Inspirado en la pureza del agua, este collar presenta una esmeralda en forma de pera de una claridad excepcional, suspendida de una cadena de platino.",
                3200000.0, "Collares", "Esmeralda Natural", "Gota / Pera", "1.8 ct", "Plata Ley 950", "VS1", 3, true);
        addImages(p2, "https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?q=80&w=800");
        addReview(p2, "Andrés Q.", 4, "Excelente calidad. Fue el regalo perfecto de aniversario. La cadena es muy fina.");
        products.add(p2);

        // 3. ARETES SOL Y LUNA
        Product p3 = createProduct("Aretes Sol y Luna", "aretes-sol-y-luna",
                "Un diseño asimétrico contemporáneo que combina la luz de los diamantes con el verde profundo de las esmeraldas de Chivor.",
                1850000.0, "Aretes", "Esmeralda y Diamante", "Brillante", "1.8 ct total", "Oro Blanco 18k", "VVS2", 8, false);
        addImages(p3, "https://images.unsplash.com/photo-1630030532634-2170161e8ec9?q=80&w=800");
        addReview(p3, "Camila R.", 5, "Son mucho más hermosos en persona. Muy elegantes para eventos de gala.");
        products.add(p3);

        // 4. ANILLO TRAPICHE PRESTIGE
        Product p4 = createProduct("Anillo Trapiche Prestige", "anillo-trapiche-prestige",
                "La gema más rara de Colombia: la esmeralda Trapiche, conocida por su patrón natural de estrella de seis puntas, una pieza de colección.",
                8900000.0, "Anillos", "Esmeralda Trapiche", "Cabuchón", "4.0 ct", "Oro Amarillo 18k", "Natural / Única", 1, true);
        addImages(p4, "https://images.unsplash.com/photo-1573408339305-c9441f714397?q=80&w=800");
        products.add(p4);

        // 5. PULSERA RIVIÈRE ESMERALDA
        Product p5 = createProduct("Pulsera Rivière Esmeralda", "pulsera-riviere-esmeralda",
                "Una línea continua de esmeraldas de corte cuadrado que envuelve la muñeca con una elegancia atemporal y un cierre de seguridad invisible.",
                5600000.0, "Pulseras", "Esmeralda", "Corte Carré", "12.0 ct total", "Oro Blanco 18k", "VS", 2, true);
        addImages(p5, "https://images.unsplash.com/photo-1611591437281-460bfbe1220a?q=80&w=800");
        products.add(p5);

        // 6. COLLAR CATEDRAL VERDE
        Product p6 = createProduct("Collar Catedral Verde", "collar-catedral-verde",
                "Un diseño geométrico inspirado en la arquitectura clásica, centrado en una esmeralda de corte rectangular de color intenso.",
                4100000.0, "Collares", "Esmeralda", "Corte Esmeralda", "3.0 ct", "Oro Amarillo 18k", "VVS1", 4, false);
        addImages(p6, "https://images.unsplash.com/photo-1515562141207-7a88fb7ce338?q=80&w=800");
        products.add(p6);

        // 7. ARETES ESMERALDA Y PERLA
        Product p7 = createProduct("Aretes Esmeralda y Perla", "aretes-esmeralda-y-perla",
                "La sofisticación de las perlas de los mares del sur combinada con esmeraldas seleccionadas a mano por su brillo.",
                2400000.0, "Aretes", "Esmeralda y Perla", "Redondo", "1.2 ct", "Oro Rosa 18k", "VS", 6, false);
        addImages(p7, "https://images.unsplash.com/photo-1535632066927-ab7c9ab60908?q=80&w=800");
        products.add(p7);

        // 8. ANILLO SOLITARIO MUZO
        Product p8 = createProduct("Anillo Solitario Muzo", "anillo-solitario-muzo",
                "Minimalismo puro para resaltar la gema. Una sola esmeralda de gran tamaño de las minas de Muzo que habla por sí misma.",
                12000000.0, "Anillos", "Esmeralda de Muzo", "Cojín / Cushion", "5.5 ct", "Platino 950", "Flawless", 2, true);
        addImages(p8, "https://images.unsplash.com/photo-1586822330090-c443660370d3?q=80&w=800");
        products.add(p8);

        // 9. DIJE CORAZÓN COLOMBIANO
        Product p9 = createProduct("Dije Corazón Colombiano", "dije-corazon-colombiano",
                "El regalo ideal para demostrar amor eterno con el verde profundo de nuestra tierra, tallado artesanalmente.",
                1200000.0, "Collares", "Esmeralda", "Corazón", "1.0 ct", "Oro de 14k", "SI1", 15, false);
        addImages(p9, "https://images.unsplash.com/photo-1598560945507-63901ba4b673?q=80&w=800");
        products.add(p9);

        // 10. ANILLO COMPROMISO VERDE REAL
        Product p10 = createProduct("Anillo Compromiso Verde Real", "anillo-compromiso-verde-real",
                "Para una propuesta inolvidable, una esmeralda central rodeada de diamantes en una montura de alta joyería.",
                6700000.0, "Anillos", "Esmeralda y Diamante", "Radiante", "2.8 ct", "Oro Blanco 18k", "VVS1", 3, true);
        addImages(p10, "https://images.unsplash.com/photo-1603561591411-071c4f723918?q=80&w=800");
        products.add(p10);

        productRepository.saveAll(products);
    }

    private Product createProduct(String name, String slug, String desc, Double price, String cat,
                                  String gem, String cut, String weight, String metal, String clarity,
                                  Integer stock, boolean featured) {
        Product p = new Product();
        p.setName(name);
        p.setSlug(slug);
        p.setDescription(desc);
        p.setPrice(price);
        p.setCategory(cat);
        p.setGemstoneType(gem);
        p.setCutType(cut);
        p.setCaratWeight(weight);
        p.setMetalType(metal);
        p.setClarity(clarity);
        p.setStock(stock);
        p.setFeatured(featured);
        return p;
    }

    private void addImages(Product p, String url) {
        ProductImage img = new ProductImage();
        img.setImageUrl(url);
        img.setAltText(p.getName());
        p.addImage(img);
    }

    private void addReview(Product p, String author, int rating, String comment) {
        Review r = new Review();
        r.setAuthor(author);
        r.setRating(rating);
        r.setComment(comment);
        r.setDate(LocalDate.now());
        p.addReview(r);
    }
}