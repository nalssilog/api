package com.nalssilog.auth.mobile.guest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nalssilog.auth.config.AuthProperties;
import com.nalssilog.common.security.SecretFingerprint;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@SuppressWarnings("java:S5960")
class MobileGuestCredentialServiceTest {

    private final MobileGuestCredentialRepository repository =
            mock(MobileGuestCredentialRepository.class);
    private final MobileGuestIssuanceRateLimiter rateLimiter =
            mock(MobileGuestIssuanceRateLimiter.class);
    private final MobileGuestCredentialService service =
            new MobileGuestCredentialService(
                    repository,
                    rateLimiter,
                    properties());

    @Test
    void issueReturnsRawSecretOnlyOnceAndPersistsItsHash() {
        var issued = service.issue("203.0.113.10");
        ArgumentCaptor<MobileGuestCredential> credentialCaptor =
                ArgumentCaptor.forClass(MobileGuestCredential.class);

        verify(rateLimiter).check("203.0.113.10");
        verify(repository).save(credentialCaptor.capture());

        MobileGuestCredential persisted = credentialCaptor.getValue();

        assertThat(issued.token()).hasSize(43);
        assertThat(issued.expiresIn()).isEqualTo(Duration.ofDays(365));
        assertThat(persisted.getTokenHash())
                .isEqualTo(SecretFingerprint.sha256(issued.token()))
                .doesNotContain(issued.token());
        assertThat(persisted.getAnonymousKey()).isNotBlank();
    }

    @Test
    void authenticateResolvesTheInternalAnonymousKey() {
        String token = "guest-token";
        MobileGuestCredential credential =
                MobileGuestCredential.issue(
                        SecretFingerprint.sha256(token),
                        "anonymous-key",
                        java.time.Instant.now().plus(Duration.ofHours(1)));

        when(repository.findByTokenHash(any()))
                .thenReturn(Optional.of(credential));

        assertThat(service.authenticate(token))
                .isEqualTo("anonymous-key");
        verify(repository).findByTokenHash(
                SecretFingerprint.sha256(token));
    }

    private AuthProperties properties() {

        return new AuthProperties(
                new AuthProperties.Jwt(
                        "test-secret-must-be-at-least-thirty-two-bytes",
                        Duration.ofMinutes(30),
                        Duration.ofDays(14)),
                new AuthProperties.Cookie(false),
                new AuthProperties.Ticket(Duration.ofMinutes(10)),
                new AuthProperties.Csrf("XSRF-TOKEN", null),
                new AuthProperties.Refresh(Duration.ofSeconds(5)));
    }
}
