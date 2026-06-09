package com.cushion.cushion_backend.service;

import com.cushion.cushion_backend.model.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Correos del ciclo de vida de un pedido. Cada uno usa la plantilla con logo
 * de EmailService (que ya envía de forma asíncrona).
 *
 *   1. Orden creada      → "Recibimos tu orden, esperamos tu pago"
 *   2. Pago confirmado   → "Tu pago fue confirmado"
 *   3. Pedido enviado    → "Tu joya va en camino" (guía + transportadora)
 *   4. Pedido entregado  → "Tu pedido fue entregado" (agradecimiento)
 */
@Service
public class OrderEmailService {

    @Autowired
    private EmailService emailService;

    private static final String GOLD = "#B89B6A";
    private static final String DARK = "#1a1a1a";
    private static final String TRACK_URL = "https://cushionjewelry.com/rastrear";

    // ── 1. Orden creada — esperamos el pago ──────────────────────────────────
    public void sendOrderCreatedEmail(Order order) {
        String body = """
            <h2 style="color:%s; font-family:Georgia,serif; font-weight:normal;">Recibimos tu orden</h2>
            <p>Hola <b>%s</b>,</p>
            <p>Tu pedido <b>#%s</b> quedó registrado y está <b>reservado a tu nombre</b>.
            Estamos esperando la confirmación de tu pago para comenzar a preparar tu pieza.</p>
            <div style="background:#faf7f2; border:1px solid #eee; padding:20px; margin:24px 0; text-align:center;">
                <p style="margin:0; font-size:13px; color:#777; letter-spacing:1px;">TOTAL DEL PEDIDO</p>
                <p style="margin:8px 0 0; font-size:24px; color:%s; font-family:Georgia,serif;">$ %s COP</p>
            </div>
            <p style="font-size:13px; color:#777;">Si ya completaste el pago, recibirás un correo de confirmación en breve.
            Si aún no lo has hecho, puedes finalizarlo desde el enlace de pago.</p>
            %s
            <p style="margin-top:30px;">Gracias por elegir Cushion.</p>
            """.formatted(
                GOLD, safe(order.getCustomerName()), order.getOrderNumber(),
                GOLD, money(order.getTotalAmount()), trackingBox(order)
        );
        emailService.sendHtmlEmail(order.getCustomerEmail(),
                "Recibimos tu orden #" + order.getOrderNumber() + " — Cushion", body);
    }

    // ── 2. Pago confirmado ───────────────────────────────────────────────────
    public void sendPaymentConfirmedEmail(Order order) {
        String body = """
            <h2 style="color:%s; font-family:Georgia,serif; font-weight:normal;">¡Pago confirmado!</h2>
            <p>Hola <b>%s</b>,</p>
            <p>Recibimos tu pago y confirmamos tu pedido <b>#%s</b>. Ya comenzamos a preparar tu pieza
            con el mayor cuidado. Muy pronto coordinaremos el envío y te compartiremos el número de guía.</p>
            <div style="background:#faf7f2; border:1px solid #eee; padding:20px; margin:24px 0; text-align:center;">
                <p style="margin:0; font-size:13px; color:#777; letter-spacing:1px;">TOTAL PAGADO</p>
                <p style="margin:8px 0 0; font-size:24px; color:%s; font-family:Georgia,serif;">$ %s COP</p>
            </div>
            %s
            <p style="margin-top:30px; font-style:italic; color:%s;">La esmeralda perfecta para ti ya está en camino a tus manos.</p>
            """.formatted(
                GOLD, safe(order.getCustomerName()), order.getOrderNumber(),
                GOLD, money(order.getTotalAmount()), trackingBox(order), GOLD
        );
        emailService.sendHtmlEmail(order.getCustomerEmail(),
                "Pago confirmado — Pedido #" + order.getOrderNumber() + " | Cushion", body);
    }

    // ── 3. Pedido enviado — con guía y transportadora ────────────────────────
    public void sendShippedEmail(Order order) {
        String guia = order.getTrackingNumber() != null ? order.getTrackingNumber() : "—";
        String carrier = order.getShippingCarrier() != null ? order.getShippingCarrier() : "Transportadora asignada";

        String body = """
            <h2 style="color:%s; font-family:Georgia,serif; font-weight:normal;">Tu joya va en camino</h2>
            <p>Hola <b>%s</b>,</p>
            <p>Tu pedido <b>#%s</b> ya fue despachado. Pronto estará en tus manos.</p>
            <div style="background:#faf7f2; border:1px solid #eee; padding:24px; margin:24px 0;">
                <table style="width:100%%; font-size:14px; color:%s;">
                    <tr>
                        <td style="padding:6px 0; color:#777;">Transportadora</td>
                        <td style="padding:6px 0; text-align:right; font-weight:bold;">%s</td>
                    </tr>
                    <tr>
                        <td style="padding:6px 0; color:#777;">Número de guía</td>
                        <td style="padding:6px 0; text-align:right; font-weight:bold; letter-spacing:1px;">%s</td>
                    </tr>
                    <tr>
                        <td style="padding:6px 0; color:#777;">Enviado a</td>
                        <td style="padding:6px 0; text-align:right;">%s</td>
                    </tr>
                </table>
            </div>
            <p style="font-size:13px; color:#777;">Puedes hacer seguimiento con la transportadora usando tu número de guía.</p>
            %s
            <p style="margin-top:30px;">Gracias por confiar en Cushion.</p>
            """.formatted(
                GOLD, safe(order.getCustomerName()), order.getOrderNumber(),
                DARK, carrier, guia, safe(order.getShippingAddress()), trackingBox(order)
        );
        emailService.sendHtmlEmail(order.getCustomerEmail(),
                "Tu pedido #" + order.getOrderNumber() + " va en camino 🚚 | Cushion", body);
    }

    // ── 4. Pedido entregado — agradecimiento ─────────────────────────────────
    public void sendDeliveredEmail(Order order) {
        String body = """
            <h2 style="color:%s; font-family:Georgia,serif; font-weight:normal;">Tu pedido fue entregado</h2>
            <p>Hola <b>%s</b>,</p>
            <p>Tu pedido <b>#%s</b> llegó a su destino. Esperamos que tu nueva pieza te acompañe en
            muchos momentos especiales.</p>
            <p>Para nosotros es un honor que hayas elegido Cushion. Cada esmeralda colombiana que entregamos
            lleva consigo nuestra pasión por la alta joyería.</p>
            <div style="text-align:center; margin:30px 0;">
                <a href="https://cushionjewelry.com/coleccion" style="display:inline-block; padding:14px 32px; border:1px solid %s; color:%s; text-decoration:none; font-size:12px; letter-spacing:2px;">DESCUBRIR MÁS PIEZAS</a>
            </div>
            <p style="font-style:italic; color:%s; text-align:center;">Gracias por ser parte de la familia Cushion.</p>
            """.formatted(
                GOLD, safe(order.getCustomerName()), order.getOrderNumber(),
                GOLD, GOLD, GOLD
        );
        emailService.sendHtmlEmail(order.getCustomerEmail(),
                "Tu pedido #" + order.getOrderNumber() + " fue entregado 💎 | Cushion", body);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Caja con el enlace de rastreo (para clientes sin cuenta). */
    private String trackingBox(Order order) {
        return """
            <div style="border-top:1px solid #f0f0f0; margin-top:24px; padding-top:20px; text-align:center;">
                <p style="font-size:12px; color:#999; margin:0 0 4px;">¿Quieres seguir tu pedido?</p>
                <p style="font-size:13px; color:#555; margin:0;">
                    Rastréalo en <a href="%s" style="color:%s;">cushionjewelry.com/rastrear</a>
                    con tu número de pedido <b>#%s</b> y tu correo o teléfono.
                </p>
            </div>
            """.formatted(TRACK_URL, GOLD, order.getOrderNumber());
    }

    private String money(Double amount) {
        return amount != null ? String.format("%,.0f", amount) : "0";
    }

    private String safe(String s) {
        return s != null ? s : "";
    }
}
