package com.nalssilog.auth.application;

import java.time.Duration;

public record TokenPair(String accessToken, String refreshToken, Duration refreshTokenMaxAge) {

    public TokenPair(String accessToken, String refreshToken) {
        this(accessToken, refreshToken, null);
    }
}
