package com.cushion.cushion_backend.task;

import com.cushion.cushion_backend.model.Order;
import com.cushion.cushion_backend.repository.OrderRepository;
import com.cushion.cushion_backend.service.TelegramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DailyReportTask {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TelegramService telegramService;

    // Se ejecuta todos los días a las 8:00 AM hora del servidor (Cron expression)
    @Scheduled(cron = "0 0 8 * * ?")
    public void sendDailyReport() {
        // Filtramos solo los pedidos que importan para despachar
        List<Order> pendientes = orderRepository.findAll().stream()
                .filter(o -> "PENDIENTE_PAGO".equals(o.getStatus()) || "PAGADO".equals(o.getStatus()))
                .collect(Collectors.toList());

        if (pendientes.isEmpty()) {
            telegramService.sendNotification("🌅 <b>REPORTE DIARIO CUSHION</b> 🌅\n\nNo hay pedidos pendientes de despacho hoy. ¡Vamos a vender! 💎");
            return;
        }

        StringBuilder report = new StringBuilder();
        report.append("🌅 <b>REPORTE DIARIO CUSHION</b> 🌅\n\n");
        report.append("Tienes <b>").append(pendientes.size()).append("</b> pedidos en cola:\n\n");

        for (Order o : pendientes) {
            report.append("🔸 <b>#").append(o.getOrderNumber()).append("</b> - ")
                    .append(o.getCustomerName())
                    .append(" (").append(o.getStatus()).append(")\n");
        }

        report.append("\n¡Que tengas un excelente día! ✨");
        telegramService.sendNotification(report.toString());
    }
}