package com.homesync.identity.dto;

import com.homesync.identity.entity.User;

import java.util.UUID;

public record LoginResponse(
        UUID id,
        String email,
        String displayName,
        String tokenType,
        String accessToken) {

    public static LoginResponse from(User user, String accessToken) {
        return new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                "Bearer",
                accessToken);
    }

}