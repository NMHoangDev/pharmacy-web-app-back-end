package com.backend.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.feature")
public class PromoFeatureProperties {
    private boolean promoEnabled;

    public boolean isPromoEnabled() {
        return promoEnabled;
    }

    public void setPromoEnabled(boolean promoEnabled) {
        this.promoEnabled = promoEnabled;
    }
}
