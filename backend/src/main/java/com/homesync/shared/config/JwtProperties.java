package com.homesync.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "homesync.jwt")
public record JwtProperties(
        String secret,
        long expirationMs) {
}