package com.nalssilog.auth.oauth.apple;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nalssilog.auth.apple")
public record AppleOAuthProperties(
        String teamId,
        String keyId,
        String privateKey,
        String privateKeyBase64,
        Duration clientSecretTtl,
        Duration refreshBeforeExpiry
) {

    public static final String REGISTRATION_ID = "apple";
    private static final Duration MAX_CLIENT_SECRET_TTL = Duration.ofDays(180);

    public AppleOAuthProperties {
        if (clientSecretTtl == null) {
            clientSecretTtl = Duration.ofDays(30);
        }

        if (clientSecretTtl.isNegative()
                || clientSecretTtl.isZero()
                || clientSecretTtl.compareTo(MAX_CLIENT_SECRET_TTL) > 0) {
            throw new IllegalArgumentException(
                    "Apple OAuth client-secret-ttl must be between 1ms and 180 days");
        }

        if (refreshBeforeExpiry == null) {
            refreshBeforeExpiry = Duration.ofDays(1);
        }

        if (refreshBeforeExpiry.isNegative()
                || refreshBeforeExpiry.compareTo(clientSecretTtl) >= 0) {
            throw new IllegalArgumentException(
                    "Apple OAuth refresh-before-expiry must be shorter than client-secret-ttl");
        }
    }

    public void requireConfigured() {
        requireText(teamId, "team-id");
        requireText(keyId, "key-id");

        if (!hasText(privateKey) && !hasText(privateKeyBase64)) {
            throw new IllegalStateException(
                    "Apple OAuth is active but neither private-key nor "
                            + "private-key-base64 is configured");
        }
    }

    private void requireText(String value, String property) {
        if (!hasText(value)) {
            throw new IllegalStateException(
                    "Apple OAuth is active but nalssilog.auth.apple."
                            + property + " is missing");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Override
    public String toString() {
        return "AppleOAuthProperties[teamId=<redacted>, keyId=<redacted>, "
                + "privateKey=<redacted>, privateKeyBase64=<redacted>, clientSecretTtl="
                + clientSecretTtl
                + ", refreshBeforeExpiry=" + refreshBeforeExpiry + "]";
    }
}
