package com.perfumeshop.crm;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.crm")
public class CrmProperties {

    private boolean enabled = true;
    private final Bitrix24 bitrix24 = new Bitrix24();
    private final AmoCrm amocrm = new AmoCrm();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Bitrix24 getBitrix24() {
        return bitrix24;
    }

    public AmoCrm getAmocrm() {
        return amocrm;
    }

    public static class Bitrix24 {
        private boolean enabled = true;
        /**
         * Inbound webhook base, e.g. https://example.bitrix24.ru/rest/1/xxxxx/
         * Empty → stub transport (local SUCCESS without HTTP).
         */
        private String webhookBaseUrl = "";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getWebhookBaseUrl() {
            return webhookBaseUrl;
        }

        public void setWebhookBaseUrl(String webhookBaseUrl) {
            this.webhookBaseUrl = webhookBaseUrl;
        }

        public boolean isLive() {
            return enabled && webhookBaseUrl != null && !webhookBaseUrl.isBlank();
        }
    }

    public static class AmoCrm {
        private boolean enabled = true;
        /**
         * Account API base, e.g. https://example.amocrm.ru
         * Empty → stub transport.
         */
        private String baseUrl = "";
        private String accessToken = "";

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

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public boolean isLive() {
            return enabled
                    && baseUrl != null && !baseUrl.isBlank()
                    && accessToken != null && !accessToken.isBlank();
        }
    }
}
