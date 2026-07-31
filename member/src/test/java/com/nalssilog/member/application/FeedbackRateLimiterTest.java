package com.nalssilog.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.member.config.FeedbackRateLimitProperties;
import com.nalssilog.member.domain.MemberErrorCode;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

@SuppressWarnings({"java:S5960", "unchecked"})
class FeedbackRateLimiterTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final FeedbackRateLimiter limiter = new FeedbackRateLimiter(
            redisTemplate,
            new FeedbackRateLimitProperties(
                    5,
                    Duration.ofMinutes(10),
                    "test-feedback-hmac-secret",
                    List.of()));

    @Test
    void rejectsSubmissionAboveConfiguredLimit() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenReturn(6L);

        NalssiLogException exception = catchThrowableOfType(
                NalssiLogException.class,
                () -> limiter.check(null, "client-a.test"));

        assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.FEEDBACK_RATE_LIMITED);
    }

    @Test
    void redisFailureDoesNotBlockFeedbackSubmission() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenThrow(new RedisConnectionFailureException("test"));

        assertThatCode(() -> limiter.check(null, "client-a.test"))
                .doesNotThrowAnyException();
    }

    @Test
    void guestActorDoesNotExposeRawAddress() {
        String actor = limiter.actor(null, "client-a.test");

        assertThat(actor)
                .startsWith("guest:")
                .doesNotContain("client-a.test");
        assertThat(limiter.actor(7L, "client-a.test")).isEqualTo("member:7");
    }

    @Test
    void guestFingerprintIsKeyedByHmacSecret() {
        FeedbackRateLimiter otherSecretLimiter = new FeedbackRateLimiter(
                redisTemplate,
                new FeedbackRateLimitProperties(
                        5,
                        Duration.ofMinutes(10),
                        "different-feedback-hmac-secret",
                        List.of()));

        assertThat(otherSecretLimiter.actor(null, "client-a.test"))
                .isNotEqualTo(limiter.actor(null, "client-a.test"));
    }
}
