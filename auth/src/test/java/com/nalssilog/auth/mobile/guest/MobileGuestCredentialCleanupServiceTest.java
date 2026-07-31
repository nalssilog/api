package com.nalssilog.auth.mobile.guest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nalssilog.auth.config.AuthProperties;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MobileGuestCredentialCleanupServiceTest {

    private final MobileGuestCredentialRepository repository =
            mock(MobileGuestCredentialRepository.class);
    private final MobileGuestCredentialCleanupService service =
            new MobileGuestCredentialCleanupService(repository, properties());

    @Test
    void deletesOnlyCredentialsPastTheConfiguredRetention() {
        Instant now = Instant.parse("2026-07-30T00:00:00Z");
        Instant cutoff = now.minus(Duration.ofDays(7));

        when(repository.deleteByExpiresAtLessThanEqual(cutoff)).thenReturn(12);

        int deleted = service.cleanupExpiredCredentialsAt(now);

        assertThat(deleted).isEqualTo(12);
        verify(repository).deleteByExpiresAtLessThanEqual(cutoff);
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
                new AuthProperties.Refresh(Duration.ofSeconds(5)),
                null,
                new AuthProperties.Guest(
                        Duration.ofDays(365),
                        300,
                        Duration.ofMinutes(10),
                        3_000,
                        Duration.ofMinutes(1),
                        Duration.ofDays(7),
                        Duration.ofHours(6),
                        Duration.ofMinutes(1)));
    }
}
