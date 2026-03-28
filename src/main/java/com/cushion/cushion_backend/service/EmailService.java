package com.cushion.cushion_backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    private final String LOGO_URL = "https://raw.githubusercontent.com/nataliafuentesg/cushion-frontend/main/src/assets/images/logo-cushion-black.png";

    @Async
    public void sendHtmlEmail(String to, String subject, String bodyContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // El "true" indica que es un mensaje multipart (HTML + Imágenes)
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String htmlTemplate = """
                <div style="font-family: 'Helvetica', Arial, sans-serif; max-width: 600px; margin: auto; border: 1px solid #eee; padding: 40px; background-color: #ffffff;">
                    <div style="text-align: center; margin-bottom: 30px;">
                        <img src="%s" alt="Cushion Logo" style="width: 180px;">
                    </div>
                    <div style="color: #1a1a1a; line-height: 1.8; font-size: 15px;">
                        %s
                    </div>
                    <hr style="border: 0; border-top: 1px solid #f0f0f0; margin: 40px 0;">
                    <div style="text-align: center; color: #999; font-size: 11px; text-transform: uppercase; letter-spacing: 3px;">
                        Cushion | Alta Joyería & Esmeraldas
                        <br>Atención Exclusiva
                    </div>
                </div>
                """.formatted(LOGO_URL, bodyContent);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlTemplate, true);
            helper.setFrom("Cushion Joyería <" + senderEmail + ">");

            mailSender.send(message);
            System.out.println("✅ Correo enviado con éxito a: " + to);

        } catch (Exception e) {
            System.err.println("❌ ERROR CRÍTICO AL ENVIAR CORREO:");
            e.printStackTrace();
        }
    }
}