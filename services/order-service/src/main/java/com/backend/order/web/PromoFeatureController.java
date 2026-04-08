package com.backend.order.web;

import com.backend.order.config.PromoFeatureProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RefreshScope
@RestController
public class PromoFeatureController {
    private final PromoFeatureProperties properties;

    public PromoFeatureController(PromoFeatureProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/api/config/promo")
    public boolean promoEnabled() {
        return properties.isPromoEnabled();
    }
}
