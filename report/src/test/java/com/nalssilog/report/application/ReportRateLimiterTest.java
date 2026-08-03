package com.nalssilog.report.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.report.application.dto.ReportActor;
import com.nalssilog.report.config.ReportRateLimitProperties;
import com.nalssilog.report.domain.ReportErrorCode;
import com.nalssilog.member.config.FeedbackRateLimitProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

@SuppressWarnings({"java:S5960", "unchecked"})
class ReportRateLimiterTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ReportRateLimiter limiter = new ReportRateLimiter(
            redisTemplate,
            new ReportRateLimitProperties(
                    5, Duration.ofMinutes(10), 20,
                    15, Duration.ofMinutes(10), 60,
                    10, Duration.ofMinutes(10), 50,
                    6, "test-secret", List.of()),
            new FeedbackRateLimitProperties(
                    5, Duration.ofMinutes(10), "fallback-secret", List.of()));

    @Test
    void createLimitReturnsStableErrorCode() {
        when(redisTemplate.execute(
                any(RedisScript.class), anyList(), any(), any(), any(), any(), any()))
                .thenReturn(1L);

        NalssiLogException exception = catchThrowableOfType(
                NalssiLogException.class,
                () -> limiter.checkCreate(ReportActor.anonymous("guest-id"), "198.51.100.1"));

        assertThat(exception.getErrorCode()).isEqualTo(ReportErrorCode.REPORT_RATE_LIMITED);
    }

    @Test
    void redisFailureFailsClosedForUgcWrite() {
        when(redisTemplate.execute(
                any(RedisScript.class), anyList(), any(), any(), any(), any(), any()))
                .thenThrow(new RedisConnectionFailureException("test"));

        NalssiLogException exception = catchThrowableOfType(
                NalssiLogException.class,
                () -> limiter.checkPresign(ReportActor.member(7L), "198.51.100.1"));

        assertThat(exception.getErrorCode()).isEqualTo(ReportErrorCode.RATE_LIMIT_UNAVAILABLE);
    }
}
