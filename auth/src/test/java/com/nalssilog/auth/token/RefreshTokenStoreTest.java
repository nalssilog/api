package com.nalssilog.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nalssilog.auth.token.RefreshTokenStore.RotationStatus;
import com.nalssilog.member.domain.Provider;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

@SuppressWarnings({"java:S5960", "unchecked"})
class RefreshTokenStoreTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final RefreshTokenStore store = new RefreshTokenStore(redisTemplate);

    @Test
    void mapsAtomicRotationScriptResultToDomainContract() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(List.of(
                        "ROTATED",
                        "replacement-token",
                        "replacement-hash",
                        "1",
                        "session-1",
                        String.valueOf(Duration.ofDays(14).toMillis())));

        SessionData replacement = new SessionData(
                "replacement-hash",
                "session-1",
                1L,
                Provider.KAKAO,
                "Chrome · Windows",
                "203.0.113.1",
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-24T00:00:00Z"));

        RefreshTokenStore.RotationResult result = store.rotate(
                "current-hash",
                "replacement-token",
                replacement,
                Duration.ofDays(14),
                Duration.ofSeconds(5));

        assertThat(result.status()).isEqualTo(RotationStatus.ROTATED);
        assertThat(result.replacementToken()).isEqualTo("replacement-token");
        assertThat(result.replacementHash()).isEqualTo("replacement-hash");
        assertThat(result.memberId()).isEqualTo(1L);
        assertThat(result.sessionId()).isEqualTo("session-1");
        assertThat(result.refreshTokenTtlMillis()).isEqualTo(Duration.ofDays(14).toMillis());
    }
}
