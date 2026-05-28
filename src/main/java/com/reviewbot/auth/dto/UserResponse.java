package com.reviewbot.auth.dto;

import com.reviewbot.auth.domain.User;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String githubLogin,
        String email,
        String avatarUrl,
        String plan
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getGithubLogin(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getPlan().name().toLowerCase()
        );
    }
}
