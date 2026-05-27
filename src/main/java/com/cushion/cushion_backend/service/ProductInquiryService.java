package com.cushion.cushion_backend.service;

import com.cushion.cushion_backend.dto.ProductInquiryDTO;
import com.cushion.cushion_backend.model.ProductInquiry;
import com.cushion.cushion_backend.repository.ProductInquiryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductInquiryService {

    @Autowired private ProductInquiryRepository repository;
    @Autowired private TelegramService telegramService;

    @Transactional
    public ProductInquiry registerInquiry(ProductInquiryDTO dto) {
        ProductInquiry inquiry = new ProductInquiry();
        inquiry.setProductSlug(dto.getProductSlug());
        inquiry.setProductName(dto.getProductName());
        inquiry.setChannel(dto.getChannel() != null ? dto.getChannel() : "WHATSAPP");
        inquiry.setClientEmail(dto.getClientEmail());
        inquiry.setUtmSource(dto.getUtmSource());
        inquiry.setUtmMedium(dto.getUtmMedium());
        inquiry.setUtmCampaign(dto.getUtmCampaign());

        ProductInquiry saved = repository.save(inquiry);

        // Notificación Telegram al equipo
        try {
            String msg = "💬 <b>CONSULTA WHATSAPP — JOYA</b>\n\n" +
                    "<b>Producto:</b> " + saved.getProductName() + "\n" +
                    "<b>Slug:</b> " + saved.getProductSlug() + "\n" +
                    (saved.getClientEmail() != null ? "<b>Cliente:</b> " + saved.getClientEmail() + "\n" : "") +
                    (saved.getUtmSource() != null ? "<b>Fuente:</b> " + saved.getUtmSource() + "\n" : "") +
                    "\nEl visitante abrió WhatsApp desde la página del producto.";
            telegramService.sendNotification(msg);
        } catch (Exception e) {
            System.err.println("Error Telegram inquiry: " + e.getMessage());
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<ProductInquiry> getAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStats() {
        long total = repository.count();
        List<Object[]> top = repository.findTopProducts();

        List<Map<String, Object>> topList = top.stream().limit(10).map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("slug", row[0]);
            m.put("name", row[1]);
            m.put("count", row[2]);
            return m;
        }).toList();

        return Map.of("total", total, "topProducts", topList);
    }
}
