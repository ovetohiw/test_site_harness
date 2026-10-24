package com.perfumeshop.paymentemulator;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class AuthSupport {

    private final EmulatorProperties properties;

    public AuthSupport(EmulatorProperties properties) {
        this.properties = properties;
    }

    public void requireBasicAuth(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Basic ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Basic auth required");
        }
        String encoded = authorizationHeader.substring("Basic ".length()).trim();
        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Basic auth");
        }
        int colon = decoded.indexOf(':');
        if (colon < 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Basic auth");
        }
        String shopId = decoded.substring(0, colon);
        String secret = decoded.substring(colon + 1);
        if (!properties.getShopId().equals(shopId) || !properties.getSecretKey().equals(secret)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
    }
}
