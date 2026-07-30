package com.nalssilog.auth.mobile.guest;

import com.nalssilog.auth.config.AuthProperties;
import com.nalssilog.auth.core.AuthErrorCode;
import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.common.security.SecretFingerprint;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MobileGuestCredentialService {

    public static final String HEADER = "X-Nalssilog-Guest-Token";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final MobileGuestCredentialRepository repository;
    private final MobileGuestIssuanceRateLimiter rateLimiter;
    private final AuthProperties properties;

    @Transactional
    public IssuedGuestCredential issue(String clientIp) {
        rateLimiter.check(clientIp);

        String rawToken = randomToken();
        Duration ttl = properties.guest().ttl();
        MobileGuestCredential credential = MobileGuestCredential.issue(
                SecretFingerprint.sha256(rawToken),
                UUID.randomUUID().toString(),
                Instant.now().plus(ttl));

        repository.save(credential);

        return new IssuedGuestCredential(rawToken, ttl);
    }

    public String authenticate(String rawToken) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 200) {
            throw new NalssiLogException(AuthErrorCode.GUEST_CREDENTIAL_INVALID);
        }

        MobileGuestCredential credential = repository.findByTokenHash(SecretFingerprint.sha256(rawToken))
                .orElseThrow(() -> new NalssiLogException(AuthErrorCode.GUEST_CREDENTIAL_INVALID));

        if (credential.isRevoked()) {
            throw new NalssiLogException(AuthErrorCode.GUEST_CREDENTIAL_INVALID);
        }
        if (credential.isExpired(Instant.now())) {
            throw new NalssiLogException(AuthErrorCode.GUEST_CREDENTIAL_EXPIRED);
        }

        return credential.getAnonymousKey();
    }

    private String randomToken() {
        byte[] bytes = new byte[TOKEN_BYTES];

        SECURE_RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record IssuedGuestCredential(String token, Duration expiresIn) {
    }
}
