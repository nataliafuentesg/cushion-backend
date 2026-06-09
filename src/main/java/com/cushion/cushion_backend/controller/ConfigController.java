package com.cushion.cushion_backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Configuración pública que el frontend necesita conocer.
 * Por ahora: las tarifas de envío (para que el resumen del checkout muestre el
 * mismo total que cobrará el backend). Son la única fuente de verdad y se
 * cambian en application.properties del servidor.
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Value("${shipping.fee.nacional:25000}")
    private double shippingNacional;

    @Value("${shipping.fee.internacional:150000}")
    private double shippingInternacional;

    @GetMapping("/shipping")
    public Map<String, Object> getShipping() {
        return Map.of(
                "nacional", shippingNacional,
                "internacional", shippingInternacional
        );
    }
}
