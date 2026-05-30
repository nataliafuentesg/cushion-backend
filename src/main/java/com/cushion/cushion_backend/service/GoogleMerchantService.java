package com.cushion.cushion_backend.service;

import com.cushion.cushion_backend.model.ProductImage;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.content.ShoppingContent;
import com.google.api.services.content.model.CustomAttribute;
import com.google.api.services.content.model.Price;
import com.google.api.services.content.model.Product;
import com.google.api.services.content.model.ProductShipping;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sincroniza productos de Cushion con Google Merchant Center (ID: 5797017548)
 * usando la Content API for Shopping v2.1.
 *
 * Se llama automáticamente cada vez que se crea, actualiza o elimina un producto
 * en el admin panel.
 *
 * Configuración requerida en application.properties DEL SERVIDOR (nunca local):
 *   google.merchant.id=5797017548
 *   google.merchant.credentials.path=/etc/cushion/google-credentials.json
 *
 * Si las propiedades no están configuradas, el servicio se desactiva solo
 * y no rompe nada (los métodos hacen no-op).
 */
@Service
public class GoogleMerchantService {

    private static final Logger log = LoggerFactory.getLogger(GoogleMerchantService.class);

    private static final String SITE_URL  = "https://cushionjewelry.com";
    private static final String BRAND     = "Cushion Jewelry";
    private static final String COUNTRY   = "CO";
    private static final String LANGUAGE  = "es";
    private static final String CHANNEL   = "online";
    private static final String CONDITION = "new";
    private static final String SCOPE     = "https://www.googleapis.com/auth/content";

    @Value("${google.merchant.id:}")
    private String merchantIdStr;

    @Value("${google.merchant.credentials.path:}")
    private String credentialsPath;

    private ShoppingContent shoppingContent;
    private BigInteger merchantId;
    private boolean enabled = false;

    @PostConstruct
    public void init() {
        if (merchantIdStr == null || merchantIdStr.isBlank()
                || credentialsPath == null || credentialsPath.isBlank()) {
            log.warn("[GoogleMerchant] Credenciales no configuradas — sincronización desactivada.");
            return;
        }
        try {
            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(new FileInputStream(credentialsPath))
                    .createScoped(Collections.singletonList(SCOPE));

            shoppingContent = new ShoppingContent.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials)
            ).setApplicationName("Cushion Backend").build();

            merchantId = new BigInteger(merchantIdStr);
            enabled = true;
            log.info("[GoogleMerchant] Servicio inicializado para Merchant ID: {}", merchantIdStr);

        } catch (Exception e) {
            log.error("[GoogleMerchant] Error al inicializar: {}", e.getMessage());
        }
    }

    // ─── API pública ─────────────────────────────────────────────────────────

    /** Inserta o actualiza un producto en Merchant Center (asíncrono). */
    @Async
    public void upsertProduct(com.cushion.cushion_backend.model.Product product) {
        if (!enabled) return;
        try {
            Product item = buildGoogleProduct(product);
            shoppingContent.products().insert(merchantId, item).execute();
            log.info("[GoogleMerchant] Producto sincronizado: {} (id={})", product.getName(), product.getId());
        } catch (IOException e) {
            log.error("[GoogleMerchant] Error al sincronizar producto {}: {}", product.getId(), e.getMessage());
        }
    }

    /** Elimina un producto de Merchant Center (asíncrono). */
    @Async
    public void deleteProduct(Long productId) {
        if (!enabled) return;
        try {
            // ID compuesto de Google: online:es:CO:{offerId}
            String googleProductId = CHANNEL + ":" + LANGUAGE + ":" + COUNTRY + ":" + productId;
            shoppingContent.products().delete(merchantId, googleProductId).execute();
            log.info("[GoogleMerchant] Producto eliminado de Merchant: id={}", productId);
        } catch (IOException e) {
            log.error("[GoogleMerchant] Error al eliminar producto {}: {}", productId, e.getMessage());
        }
    }

    /** Sincronización masiva — primera carga o re-sincronización completa (asíncrono). */
    @Async
    public void syncAll(List<com.cushion.cushion_backend.model.Product> products) {
        if (!enabled) {
            log.warn("[GoogleMerchant] syncAll ignorado — servicio desactivado.");
            return;
        }
        log.info("[GoogleMerchant] Iniciando sincronización masiva de {} productos...", products.size());

        int ok = 0, fail = 0;
        for (com.cushion.cushion_backend.model.Product product : products) {
            try {
                shoppingContent.products().insert(merchantId, buildGoogleProduct(product)).execute();
                ok++;
            } catch (IOException e) {
                log.error("[GoogleMerchant] Fallo en producto {}: {}", product.getId(), e.getMessage());
                fail++;
            }
        }
        log.info("[GoogleMerchant] Sincronización masiva completada: {} OK, {} errores.", ok, fail);
    }

    public boolean isEnabled() {
        return enabled;
    }

    // ─── Construcción del objeto Product de Google ───────────────────────────

    private Product buildGoogleProduct(com.cushion.cushion_backend.model.Product p) {
        Product item = new Product();

        // Identificadores
        item.setOfferId(String.valueOf(p.getId()));
        item.setChannel(CHANNEL);
        item.setContentLanguage(LANGUAGE);
        item.setTargetCountry(COUNTRY);

        // Información básica
        item.setTitle(p.getName());
        item.setDescription(truncate(
                p.getDescription() != null ? p.getDescription() : p.getName(), 5000));
        item.setLink(SITE_URL + "/producto/" + p.getSlug());
        item.setBrand(BRAND);
        item.setCondition(CONDITION);
        item.setIdentifierExists(false);

        // Categoría
        item.setGoogleProductCategory(googleCategory(p.getCategory()));
        item.setProductTypes(Collections.singletonList(p.getCategory()));

        // Imágenes
        if (p.getImages() != null && !p.getImages().isEmpty()) {
            ProductImage main = p.getImages().stream()
                    .filter(ProductImage::isThumbnail)
                    .findFirst()
                    .orElse(p.getImages().get(0));
            item.setImageLink(main.getImageUrl());

            List<String> additional = new ArrayList<>();
            for (ProductImage img : p.getImages()) {
                if (img != main && additional.size() < 9) {
                    additional.add(img.getImageUrl());
                }
            }
            if (!additional.isEmpty()) item.setAdditionalImageLinks(additional);
        }

        // Precio
        Price price = new Price();
        price.setValue(String.format("%.2f", p.getPrice()));
        price.setCurrency("COP");
        item.setPrice(price);

        // Disponibilidad
        item.setAvailability(
                (p.getStock() != null && p.getStock() > 0) ? "in_stock" : "out_of_stock");

        // Material (metal)
        if (hasValue(p.getMetalType())) {
            item.setMaterial(p.getMetalType());
        }

        // Gema como atributo personalizado
        if (hasValue(p.getGemstoneType())) {
            CustomAttribute gemAttr = new CustomAttribute();
            gemAttr.setName("gemstone_type");
            gemAttr.setValue(p.getGemstoneType());
            item.setCustomAttributes(Collections.singletonList(gemAttr));
        }

        // Envío Colombia gratis
        ProductShipping shipping = new ProductShipping();
        shipping.setCountry(COUNTRY);
        shipping.setService("Envío Asegurado");
        Price shippingPrice = new Price();
        shippingPrice.setValue("0.00");
        shippingPrice.setCurrency("COP");
        shipping.setPrice(shippingPrice);
        item.setShipping(Collections.singletonList(shipping));

        return item;
    }

    // ─── Utilidades ──────────────────────────────────────────────────────────

    private String googleCategory(String category) {
        if (category == null) return "Apparel & Accessories > Jewelry";
        String lower = category.toLowerCase();
        if (lower.contains("anillo"))                                  return "Apparel & Accessories > Jewelry > Rings";
        if (lower.contains("collar") || lower.contains("gargantilla")) return "Apparel & Accessories > Jewelry > Necklaces";
        if (lower.contains("arete"))                                   return "Apparel & Accessories > Jewelry > Earrings";
        if (lower.contains("pulsera"))                                 return "Apparel & Accessories > Jewelry > Bracelets";
        if (lower.contains("dije"))                                    return "Apparel & Accessories > Jewelry > Charms & Pendants";
        if (lower.contains("piedra") || lower.contains("esmeralda"))   return "Arts & Entertainment > Hobbies & Creative Arts > Collectibles > Gemstones";
        return "Apparel & Accessories > Jewelry";
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }

    private boolean hasValue(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
