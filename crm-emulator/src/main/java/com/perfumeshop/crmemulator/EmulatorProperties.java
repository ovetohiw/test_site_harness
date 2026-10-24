package com.perfumeshop.crmemulator;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.emulator")
public class EmulatorProperties {

    private String amocrmAccessToken = "test-token";

    public String getAmocrmAccessToken() {
        return amocrmAccessToken;
    }

    public void setAmocrmAccessToken(String amocrmAccessToken) {
        this.amocrmAccessToken = amocrmAccessToken;
    }
}
