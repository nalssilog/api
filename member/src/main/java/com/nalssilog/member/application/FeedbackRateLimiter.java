package com.nalssilog.member.application;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.common.security.SecretFingerprint;
import com.nalssilog.member.config.FeedbackRateLimitProperties;
import com.nalssilog.member.domain.MemberErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 서비스 피드백 제출 제한. 회원은 memberId, 비회원은 원격 IP의 SHA-256 fingerprint를 Redis key로 사용한다.
 * INCR와 최초 TTL 설정은 Lua로 원자 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeedbackRateLimiter {

    private static final String KEY_PREFIX = "feedback:rate:";
    private static final DefaultRedisScript<Long> INCREMENT_SCRIPT = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final FeedbackRateLimitProperties properties;

    public void check(Long memberId, String remoteAddress) {
        String actor = actor(memberId, remoteAddress);
        Long count;

        try {
            count = redisTemplate.execute(
                    INCREMENT_SCRIPT,
                    List.of(KEY_PREFIX + actor),
                    String.valueOf(properties.window().toMillis()));
        } catch (DataAccessException exception) {
            // 피드백 저장 자체보다 보조 보호장치 장애의 영향이 커지지 않게 제한기만 fail-open 한다.
            log.warn("feedback.rate_limit_unavailable actor={} reason={}",
                    safeActor(actor), exception.getClass().getSimpleName());

            return;
        }

        if (count != null && count > properties.maxSubmissions()) {
            log.warn("feedback.rate_limited actor={} count={}", safeActor(actor), count);
            throw new NalssiLogException(MemberErrorCode.FEEDBACK_RATE_LIMITED);
        }
    }

    String actor(Long memberId, String remoteAddress) {
        if (memberId != null) {

            return "member:" + memberId;
        }

        String address = StringUtils.hasText(remoteAddress) ? remoteAddress.strip() : "unknown";

        return "guest:" + SecretFingerprint.hmacSha256(properties.ipHmacSecret(), address);
    }

    private String safeActor(String actor) {
        int separator = actor.indexOf(':');
        String type = separator < 0 ? "unknown" : actor.substring(0, separator);
        String identifier = separator < 0 ? actor : actor.substring(separator + 1);

        return type + ":" + identifier.substring(0, Math.min(12, identifier.length()));
    }

}
