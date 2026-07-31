package com.nalssilog.auth.mobile.guest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nalssilog.auth.config.AuthProperties;
import com.nalssilog.auth.core.AuthErrorCode;
import com.nalssilog.common.exception.NalssiLogException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

@SuppressWarnings({"unchecked", "java:S5960"})
class MobileGuestIssuanceRateLimiterTest {

    private final StringRedisTemplate redisTemplate =
            mock(StringRedisTemplate.class);
    private final MobileGuestIssuanceRateLimiter limiter =
            new MobileGuestIssuanceRateLimiter(redisTemplate, properties());

    @Test
    void sharedIpUsesHmacFingerprintAndGenerousHardLimit() {
        when(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                any(),
                any(),
                any(),
                any())).thenReturn(0L);

        limiter.check("client-a.test");

        ArgumentCaptor<List<String>> keys = ArgumentCaptor.forClass(List.class);

        org.mockito.Mockito.verify(redisTemplate).execute(
                any(RedisScript.class),
                keys.capture(),
                org.mockito.ArgumentMatchers.eq("600000"),
                org.mockito.ArgumentMatchers.eq("60000"),
                org.mockito.ArgumentMatchers.eq("300"),
                org.mockito.ArgumentMatchers.eq("3000"));
        assertThat(keys.getValue())
                .hasSize(2)
                .allMatch(key -> !key.contains("client-a.test"));
        assertThat(keys.getValue().getFirst()).startsWith("auth:guest:issue:ip:");
        assertThat(keys.getValue().getLast()).isEqualTo("auth:guest:issue:global");
    }

    @Test
    void positiveScriptResultMeansIpHardLimitWasExceeded() {
        when(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                any(),
                any(),
                any(),
                any())).thenReturn(301L);

        NalssiLogException exception = catchThrowableOfType(
                NalssiLogException.class,
                () -> limiter.check("client-a.test"));

        assertThat(exception.getErrorCode())
                .isEqualTo(AuthErrorCode.GUEST_ISSUANCE_RATE_LIMITED);
    }

    @Test
    void negativeScriptResultMeansGlobalSafetyLimitWasExceeded() {
        when(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                any(),
                any(),
                any(),
                any())).thenReturn(-3_001L);

        NalssiLogException exception = catchThrowableOfType(
                NalssiLogException.class,
                () -> limiter.check("client-b.test"));

        assertThat(exception.getErrorCode())
                .isEqualTo(AuthErrorCode.GUEST_ISSUANCE_RATE_LIMITED);
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
                new AuthProperties.Mobile(
                        List.of("nalssilog-dev://auth/callback"),
                        Duration.ofMinutes(10),
                        Duration.ofSeconds(90),
                        "test-hmac-secret",
                        List.of()),
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
