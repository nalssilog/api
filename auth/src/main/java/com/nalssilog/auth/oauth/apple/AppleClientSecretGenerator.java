package com.nalssilog.auth.oauth.apple;

import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import org.springframework.stereotype.Component;

@Component
public class AppleClientSecretGenerator {

    private static final String APPLE_AUDIENCE = "https://appleid.apple.com";

    private final AppleOAuthProperties properties;
    private volatile CachedSecret cachedSecret;

    public AppleClientSecretGenerator(AppleOAuthProperties properties) {
        this.properties = properties;
    }

    public String generate(String clientId) {
        properties.requireConfigured();

        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("Apple OAuth client-id is missing");
        }

        Instant now = Instant.now();
        CachedSecret current = cachedSecret;

        if (isReusable(current, clientId, now)) {
            return current.value();
        }

        synchronized (this) {
            current = cachedSecret;

            if (isReusable(current, clientId, now)) {
                return current.value();
            }

            Instant expiresAt = now.plus(properties.clientSecretTtl());
            String value = Jwts.builder()
                    .header()
                    .keyId(properties.keyId())
                    .and()
                    .issuer(properties.teamId())
                    .subject(clientId)
                    .audience()
                    .add(APPLE_AUDIENCE)
                    .and()
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(expiresAt))
                    .signWith(privateKey(), Jwts.SIG.ES256)
                    .compact();

            cachedSecret = new CachedSecret(clientId, value, expiresAt);

            return value;
        }
    }

    private boolean isReusable(
            CachedSecret secret,
            String clientId,
            Instant now
    ) {
        return secret != null
                && secret.clientId().equals(clientId)
                && secret.expiresAt()
                        .minus(properties.refreshBeforeExpiry())
                        .isAfter(now);
    }

    private PrivateKey privateKey() {
        try {
            byte[] encoded = encodedPrivateKey();
            PrivateKey key = KeyFactory.getInstance("EC")
                    .generatePrivate(new PKCS8EncodedKeySpec(encoded));

            if (!(key instanceof ECPrivateKey ecPrivateKey)
                    || ecPrivateKey.getParams().getCurve().getField().getFieldSize() != 256) {
                throw new IllegalArgumentException("Apple key must use the P-256 curve");
            }

            return key;
        } catch (RuntimeException | java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("Apple OAuth private key is invalid", exception);
        }
    }

    private byte[] encodedPrivateKey() {
        if (properties.privateKey() != null
                && !properties.privateKey().isBlank()) {
            return decodePemOrBase64(properties.privateKey());
        }

        byte[] decoded = Base64.getDecoder().decode(
                properties.privateKeyBase64().replaceAll("\\s", ""));
        String possiblePem = new String(decoded, StandardCharsets.UTF_8);

        return possiblePem.contains("-----BEGIN PRIVATE KEY-----")
                ? decodePemOrBase64(possiblePem)
                : decoded;
    }

    private byte[] decodePemOrBase64(String value) {
        String normalized = value
                .replace("\\n", "\n")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        return Base64.getDecoder().decode(normalized);
    }

    private record CachedSecret(
            String clientId,
            String value,
            Instant expiresAt
    ) {
    }
}
