package com.perfumeshop.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payments.yookassa")
public class YooKassaProperties {

    private boolean enabled = true;
    /**
     * API base, default https://api.yookassa.ru/v3
     * Point at payment-emulator for local tests, e.g. http://localhost:8091/v3
     */
    private String baseUrl = "https://api.yookassa.ru/v3";
    private String shopId = "";
    private String secretKey = "";
    /**
     * Browser return URL after redirect confirmation.
     */
    private String returnUrl = "http://localhost:8080/";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getShopId() {
        return shopId;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public boolean isLive() {
        return enabled
                && shopId != null && !shopId.isBlank()
                && secretKey != null && !secretKey.isBlank();
    }

    public String mode() {
        if (!enabled) {
            return "disabled";
        }
        return isLive() ? "live" : "stub";
    }
}
