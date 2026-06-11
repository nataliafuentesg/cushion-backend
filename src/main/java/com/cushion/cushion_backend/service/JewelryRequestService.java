package com.cushion.cushion_backend.service;

import com.cushion.cushion_backend.dto.JewelryRequestDTO;
import com.cushion.cushion_backend.model.JewelryRequest;
import com.cushion.cushion_backend.repository.JewelryRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class JewelryRequestService {

    @Autowired private JewelryRequestRepository jewelryRequestRepository;
    @Autowired private EmailService emailService;
    @Autowired private TelegramService telegramService;
    @Autowired private MetaConversionsService metaConversions;

    @org.springframework.beans.factory.annotation.Value("${notifications.admin.email:nata.ltda1412@gmail.com}")
    private String adminEmail;

    @Transactional
    public JewelryRequest createRequest(JewelryRequestDTO dto, String clientIp, String userAgent) {
        JewelryRequest request = new JewelryRequest();
        request.setCustomerName(dto.getCustomerName());
        request.setCustomerEmail(dto.getCustomerEmail());
        request.setCustomerPhone(dto.getCustomerPhone());
        request.setOccasion(dto.getOccasion());
        request.setJewelryType(dto.getJewelryType());
        request.setGemstonePreference(dto.getGemstonePreference());
        request.setMetalType(dto.getMetalType());
        request.setBudgetRange(dto.getBudgetRange());
        request.setIdeas(dto.getIdeas());
        request.setContactMethod(dto.getContactMethod() != null ? dto.getContactMethod() : "FORMULARIO");

        JewelryRequest saved = jewelryRequestRepository.save(request);

        // ── Meta CAPI — Lead ──
        try {
            // Usa el event_id que envía el navegador (para deduplicar contra el Pixel).
            // Si no viene, genera uno propio como respaldo.
            String eventId = (dto.getEventId() != null && !dto.getEventId().isBlank())
                    ? dto.getEventId()
                    : "lead_" + saved.getId() + "_" + UUID.randomUUID().toString().substring(0, 8);
            metaConversions.sendLead(eventId, saved.getCustomerEmail(), clientIp, userAgent);
        } catch (Exception e) {
            System.err.println("[Meta CAPI] Lead error: " + e.getMessage());
        }

        // ── Telegram ──
        try {
            String canal = saved.getContactMethod().equals("WHATSAPP") ? "💬 WhatsApp" : "📋 Formulario";
            String msg = "💎 <b>NUEVA CONSULTA DE ESMERALDA</b>\n\n" +
                    "<b>Canal:</b> " + canal + "\n" +
                    "<b>Cliente:</b> " + saved.getCustomerName() + "\n" +
                    "<b>Email:</b> " + saved.getCustomerEmail() + "\n" +
                    "<b>Tel:</b> " + (saved.getCustomerPhone() != null ? saved.getCustomerPhone() : "No indicado") + "\n" +
                    "<b>Ocasión:</b> " + saved.getOccasion() + "\n" +
                    "<b>Joya:</b> " + saved.getJewelryType() + "\n" +
                    "<b>Presupuesto:</b> " + saved.getBudgetRange() + "\n\n" +
                    "Revisa el panel admin para ver los detalles completos.";
            telegramService.sendNotification(msg);
        } catch (Exception e) {
            System.err.println("Telegram error: " + e.getMessage());
        }

        // ── Email de confirmación al cliente ──
        if ("FORMULARIO".equals(saved.getContactMethod())) {
            try {
                String clientBody = """
                    <h2 style="color:#4C7F62; font-family:Georgia,serif;">¡Recibimos tu consulta!</h2>
                    <p>Hola <b>%s</b>,</p>
                    <p>Hemos registrado tu solicitud de diseño personalizado con esmeraldas colombianas.
                    Nuestro equipo de expertos revisará tu consulta y se pondrá en contacto contigo
                    en las próximas <b>24 horas hábiles</b> al correo <b>%s</b>%s.</p>
                    <p style="color:#B89B6A; font-style:italic;">
                        La esmeralda perfecta para ti está a punto de encontrarte.
                    </p>
                    <p>Equipo Cushion — Alta Joyería</p>
                    """.formatted(
                        saved.getCustomerName(),
                        saved.getCustomerEmail(),
                        saved.getCustomerPhone() != null ? " o al WhatsApp " + saved.getCustomerPhone() : ""
                    );
                emailService.sendHtmlEmail(
                    saved.getCustomerEmail(),
                    "Cushion — Recibimos tu consulta de diseño personalizado",
                    clientBody
                );
            } catch (Exception e) {
                System.err.println("Email cliente error: " + e.getMessage());
            }
        }

        // ── Email interno ──
        try {
            String adminBody = """
                <h2 style="color:#4C7F62;">Nueva consulta de esmeralda personalizada</h2>
                <table style="border-collapse:collapse; width:100%; font-family:Arial,sans-serif; font-size:14px;">
                    <tr><td style="padding:8px; border:1px solid #ddd;"><b>Canal</b></td><td style="padding:8px; border:1px solid #ddd;">%s</td></tr>
                    <tr><td style="padding:8px; border:1px solid #ddd;"><b>Nombre</b></td><td style="padding:8px; border:1px solid #ddd;">%s</td></tr>
                    <tr><td style="padding:8px; border:1px solid #ddd;"><b>Email</b></td><td style="padding:8px; border:1px solid #ddd;">%s</td></tr>
                    <tr><td style="padding:8px; border:1px solid #ddd;"><b>Teléfono</b></td><td style="padding:8px; border:1px solid #ddd;">%s</td></tr>
                    <tr><td style="padding:8px; border:1px solid #ddd;"><b>Ocasión</b></td><td style="padding:8px; border:1px solid #ddd;">%s</td></tr>
                    <tr><td style="padding:8px; border:1px solid #ddd;"><b>Tipo de joya</b></td><td style="padding:8px; border:1px solid #ddd;">%s</td></tr>
                    <tr><td style="padding:8px; border:1px solid #ddd;"><b>Gema</b></td><td style="padding:8px; border:1px solid #ddd;">%s</td></tr>
                    <tr><td style="padding:8px; border:1px solid #ddd;"><b>Metal</b></td><td style="padding:8px; border:1px solid #ddd;">%s</td></tr>
                    <tr><td style="padding:8px; border:1px solid #ddd;"><b>Presupuesto</b></td><td style="padding:8px; border:1px solid #ddd;">%s</td></tr>
                    <tr><td style="padding:8px; border:1px solid #ddd;"><b>Ideas</b></td><td style="padding:8px; border:1px solid #ddd;">%s</td></tr>
                </table>
                """.formatted(
                    saved.getContactMethod(),
                    saved.getCustomerName(),
                    saved.getCustomerEmail(),
                    saved.getCustomerPhone() != null ? saved.getCustomerPhone() : "No indicado",
                    saved.getOccasion(),
                    saved.getJewelryType(),
                    saved.getGemstonePreference(),
                    saved.getMetalType(),
                    saved.getBudgetRange(),
                    saved.getIdeas() != null ? saved.getIdeas() : "Sin descripción adicional"
                );
            emailService.sendHtmlEmail(adminEmail,
                    "💎 Nueva consulta esmeralda — " + saved.getContactMethod(), adminBody);
        } catch (Exception e) {
            System.err.println("Email admin error: " + e.getMessage());
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<JewelryRequest> getAllRequests() {
        return jewelryRequestRepository.findAll();
    }

    @Transactional
    public JewelryRequest updateStatus(Long id, String newStatus) {
        JewelryRequest request = jewelryRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Consulta no encontrada"));
        request.setStatus(newStatus);
        return jewelryRequestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getStats() {
        long formulario = jewelryRequestRepository.countByContactMethod("FORMULARIO");
        long whatsapp   = jewelryRequestRepository.countByContactMethod("WHATSAPP");
        return Map.of("FORMULARIO", formulario, "WHATSAPP", whatsapp, "TOTAL", formulario + whatsapp);
    }
}
