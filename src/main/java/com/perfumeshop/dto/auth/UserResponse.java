package com.perfumeshop.dto.auth;

import com.perfumeshop.domain.user.User;

import java.time.Instant;

public record UserResponse(
        String login,
        String avatarUrl,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getLogin(), user.getAvatarUrl(), user.getCreatedAt());
    }
}
