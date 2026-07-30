package com.nalssilog.auth.mobile;

import com.nalssilog.auth.config.AuthProperties;
import com.nalssilog.auth.token.TokenPair;

public record MobileRefreshResponse(
        String tokenType,
        String accessToken,
        long accessTokenExpiresIn,
        String refreshToken,
        long refreshTokenExpiresIn
) {

    public static MobileRefreshResponse from(
            TokenPair tokens,
            AuthProperties properties
    ) {

        return new MobileRefreshResponse(
                "Bearer",
                tokens.accessToken(),
                properties.jwt().accessTokenTtl().toSeconds(),
                tokens.refreshToken(),
                tokens.refreshTokenMaxAge().toSeconds());
    }
}
