package com.cushion.cushion_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class TelegramService {

    @Value("${telegram.bot.token:NO_TOKEN}")
    private String botToken;

    @Value("${telegram.chat.ids:NO_IDS}")
    private String chatIds;

    public void sendNotification(String message) {
        // Si no hay token configurado (ej. entorno local), solo imprime en consola
        if ("NO_TOKEN".equals(botToken) || "NO_IDS".equals(chatIds)) {
            System.out.println("====== ALERTA TELEGRAM (SIMULADA EN LOCAL) ======\n" + message + "\n=================================================");
            return;
        }

        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
            RestTemplate restTemplate = new RestTemplate();

            // Separamos los IDs por coma para enviar a múltiples administradores
            String[] idsArray = chatIds.split(",");

            for (String id : idsArray) {
                String cleanId = id.trim(); // Limpia espacios accidentales
                if (cleanId.isEmpty()) continue;

                Map<String, String> body = new HashMap<>();
                body.put("chat_id", cleanId);
                body.put("text", message);
                body.put("parse_mode", "HTML"); // Permite usar <b> para negritas

                restTemplate.postForObject(url, body, String.class);
            }
        } catch (Exception e) {
            System.err.println("Error enviando notificación de Telegram: " + e.getMessage());
        }
    }
}