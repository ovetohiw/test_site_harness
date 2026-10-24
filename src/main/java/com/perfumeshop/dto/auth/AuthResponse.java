package com.perfumeshop.dto.auth;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMs,
        String login
) {
    public static AuthResponse bearer(String token, long expiresInMs, String login) {
        return new AuthResponse(token, "Bearer", expiresInMs, login);
    }
}
