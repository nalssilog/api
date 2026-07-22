package com.nalssilog.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nalssilog.auth")
public record AuthProperties(Jwt jwt, Cookie cookie, Ticket ticket) {

    public record Jwt(String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {
    }

    public record Cookie(String domain, boolean secure) {
    }

    public record Ticket(Duration ttl) {
    }
}
