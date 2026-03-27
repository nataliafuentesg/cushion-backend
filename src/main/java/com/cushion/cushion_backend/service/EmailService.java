package com.cushion.cushion_backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    private final String LOGO_URL = "https://raw.githubusercontent.com/nataliafuentesg/cushion-frontend/main/src/assets/images/logo-cushion-black.png";

    public void sendHtmlEmail(String to, String subject, String bodyContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String htmlTemplate = """
                <div style="font-family: 'Helvetica', Arial, sans-serif; max-width: 600px; margin: auto; border: 1px solid #eee; padding: 20px;">
                    <div style="text-align: center; margin-bottom: 30px;">
                        <img src="%s" alt="Cushion Logo" style="width: 150px;">
                    </div>
                    <div style="color: #1a1a1a; line-height: 1.6;">
                        %s
                    </div>
                    <hr style="border: 0; border-top: 1px solid #eee; margin: 30px 0;">
                    <div style="text-align: center; color: #888; font-size: 12px; text-transform: uppercase; letter-spacing: 2px;">
                        Cushion | Alta Joyería & Esmeraldas
                        <br>Atención Personalizada 24/7
                    </div>
                </div>
                """.formatted(LOGO_URL, bodyContent);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlTemplate, true);
            helper.setFrom("Cushion Joyería <ventas@cushion.com>");

            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Error al enviar correo: " + e.getMessage());
        }
    }
}