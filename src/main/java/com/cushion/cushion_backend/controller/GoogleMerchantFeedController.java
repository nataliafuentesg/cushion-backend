package com.cushion.cushion_backend.controller;

import com.cushion.cushion_backend.dto.ProductDTO;
import com.cushion.cushion_backend.dto.ProductImageDTO;
import com.cushion.cushion_backend.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/feed")
public class GoogleMerchantFeedController {

    private static final String SITE_URL  = "https://cushionjewelry.com";
    private static final String BRAND     = "Cushion Jewelry";

    @Autowired
    private ProductService productService;

    @GetMapping(value = "/google-merchant", produces = "application/xml; charset=UTF-8")
    public ResponseEntity<String> getGoogleMerchantFeed() {
        List<ProductDTO> products = productService.getAllProducts();
        String xml = buildFeedXml(products);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/xml; charset=UTF-8")
                .header(HttpHeaders.CACHE_CONTROL, "max-age=3600, public")
                .body(xml);
    }

    // ─── Construcción del RSS ────────────────────────────────────────────────

    private String buildFeedXml(List<ProductDTO> products) {
        StringBuilder sb = new StringBuilder(products.size() * 1024);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<rss version=\"2.0\" xmlns:g=\"http://base.google.com/ns/1.0\">\n");
        sb.append("  <channel>\n");
        sb.append("    <title>Cushion Jewelry</title>\n");
        sb.append("    <link>").append(SITE_URL).append("</link>\n");
        sb.append("    <description><![CDATA[Alta Joyería y Esmeraldas Colombianas — Bogotá, Colombia]]></description>\n\n");

        for (ProductDTO p : products) {
            sb.append(buildItem(p));
        }

        sb.append("  </channel>\n");
        sb.append("</rss>");
        return sb.toString();
    }

    private String buildItem(ProductDTO p) {
        StringBuilder item = new StringBuilder();
        item.append("    <item>\n");

        // ── Campos obligatorios ──────────────────────────────────────────────
        item.append("      <g:id>").append(p.getId()).append("</g:id>\n");
        item.append("      <g:title><![CDATA[").append(safe(p.getName())).append("]]></g:title>\n");

        String desc = p.getDescription() != null ? p.getDescription() : p.getName();
        if (desc.length() > 5000) desc = desc.substring(0, 5000);
        item.append("      <g:description><![CDATA[").append(desc).append("]]></g:description>\n");

        item.append("      <g:link>")
            .append(SITE_URL).append("/producto/").append(p.getSlug())
            .append("</g:link>\n");

        // ── Imágenes (principal + adicionales, máx 10 en total) ─────────────
        if (p.getImages() != null && !p.getImages().isEmpty()) {
            // Preferir la imagen marcada como thumbnail como imagen principal
            ProductImageDTO main = p.getImages().stream()
                    .filter(ProductImageDTO::isThumbnail)
                    .findFirst()
                    .orElse(p.getImages().get(0));

            item.append("      <g:image_link>").append(escape(main.getImageUrl())).append("</g:image_link>\n");

            p.getImages().stream()
                    .filter(img -> img != main)
                    .limit(9)
                    .forEach(img ->
                        item.append("      <g:additional_image_link>")
                            .append(escape(img.getImageUrl()))
                            .append("</g:additional_image_link>\n")
                    );
        }

        // ── Disponibilidad y precio ──────────────────────────────────────────
        boolean inStock = p.getStock() != null && p.getStock() > 0;
        item.append("      <g:availability>")
            .append(inStock ? "in_stock" : "out_of_stock")
            .append("</g:availability>\n");

        item.append("      <g:price>")
            .append(String.format("%.2f", p.getPrice()))
            .append(" COP</g:price>\n");

        // ── Marca, condición, categoría ──────────────────────────────────────
        item.append("      <g:brand>").append(BRAND).append("</g:brand>\n");
        item.append("      <g:condition>new</g:condition>\n");
        item.append("      <g:google_product_category><![CDATA[")
            .append(googleCategory(p.getCategory()))
            .append("]]></g:google_product_category>\n");
        item.append("      <g:product_type><![CDATA[")
            .append(safe(p.getCategory()))
            .append("]]></g:product_type>\n");

        // ── Atributos opcionales (ayudan a mejor posicionamiento en Shopping) ─
        if (hasValue(p.getMetalType())) {
            item.append("      <g:material><![CDATA[").append(p.getMetalType()).append("]]></g:material>\n");
        }
        if (hasValue(p.getGemstoneType())) {
            item.append("      <g:pattern><![CDATA[").append(p.getGemstoneType()).append("]]></g:pattern>\n");
        }

        // Sin GTIN/EAN — joyería artesanal bajo pedido
        item.append("      <g:identifier_exists>false</g:identifier_exists>\n");

        // ── Envío (Colombia, gratis, 2 días hábiles) ────────────────────────
        item.append("      <g:shipping>\n");
        item.append("        <g:country>CO</g:country>\n");
        item.append("        <g:service>Envío Asegurado</g:service>\n");
        item.append("        <g:price>0 COP</g:price>\n");
        item.append("        <g:min_handling_time>0</g:min_handling_time>\n");
        item.append("        <g:max_handling_time>1</g:max_handling_time>\n");
        item.append("        <g:min_transit_time>1</g:min_transit_time>\n");
        item.append("        <g:max_transit_time>2</g:max_transit_time>\n");
        item.append("      </g:shipping>\n");

        // ── Política de devoluciones (etiqueta configurada en Merchant Center) ─
        item.append("      <g:return_policy_label>default</g:return_policy_label>\n");

        item.append("    </item>\n\n");
        return item.toString();
    }


    private String googleCategory(String category) {
        if (category == null) return "Apparel & Accessories > Jewelry";
        String lower = category.toLowerCase();
        if (lower.contains("anillo"))                          return "Apparel & Accessories > Jewelry > Rings";
        if (lower.contains("collar") || lower.contains("gargantilla")) return "Apparel & Accessories > Jewelry > Necklaces";
        if (lower.contains("arete"))                           return "Apparel & Accessories > Jewelry > Earrings";
        if (lower.contains("pulsera"))                         return "Apparel & Accessories > Jewelry > Bracelets";
        if (lower.contains("dije"))                            return "Apparel & Accessories > Jewelry > Charms & Pendants";
        if (lower.contains("piedra") || lower.contains("esmeralda")) return "Arts & Entertainment > Hobbies & Creative Arts > Collectibles > Gemstones";
        return "Apparel & Accessories > Jewelry";
    }

    // ─── Utilidades ─────────────────────────────────────────────────────────

    /** Texto seguro para CDATA — nunca null */
    private String safe(String s) {
        return s != null ? s : "";
    }

    /** Escapa caracteres especiales XML para atributos y valores sin CDATA */
    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&",  "&amp;")
                .replace("<",  "&lt;")
                .replace(">",  "&gt;")
                .replace("\"", "&quot;")
                .replace("'",  "&apos;");
    }

    /** Verifica que el campo no sea null ni vacío */
    private boolean hasValue(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
