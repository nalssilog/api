package com.nalssilog.auth.mobile.guest;

import com.nalssilog.auth.config.AuthProperties;
import com.nalssilog.auth.core.AuthErrorCode;
import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.common.security.SecretFingerprint;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MobileGuestIssuanceRateLimiter {

    private static final String KEY_PREFIX = "auth:guest:issue:";
    private static final String GLOBAL_KEY = KEY_PREFIX + "global";
    private static final DefaultRedisScript<Long> INCREMENT_SCRIPT = new DefaultRedisScript<>("""
            local ipCount = redis.call('INCR', KEYS[1])
            if ipCount == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            local globalCount = redis.call('INCR', KEYS[2])
            if globalCount == 1 then
                redis.call('PEXPIRE', KEYS[2], ARGV[2])
            end
            if globalCount > tonumber(ARGV[4]) then
                return -globalCount
            end
            if ipCount > tonumber(ARGV[3]) then
                return ipCount
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final AuthProperties properties;

    public void check(String clientIp) {
        String fingerprint = SecretFingerprint.hmacSha256(
                properties.mobile().ipHmacSecret(),
                clientIp == null || clientIp.isBlank() ? "unknown" : clientIp.strip());
        Long result;

        try {
            result = redisTemplate.execute(
                    INCREMENT_SCRIPT,
                    List.of(KEY_PREFIX + "ip:" + fingerprint, GLOBAL_KEY),
                    String.valueOf(properties.guest().rateLimitWindow().toMillis()),
                    String.valueOf(properties.guest().globalRateLimitWindow().toMillis()),
                    String.valueOf(properties.guest().maxIssuances()),
                    String.valueOf(properties.guest().globalMaxIssuances()));
        } catch (DataAccessException exception) {
            log.warn("auth.guest.issue_rate_limit_unavailable ip={}",
                    fingerprint.substring(0, 12));
            throw new NalssiLogException(AuthErrorCode.GUEST_ISSUANCE_UNAVAILABLE);
        }

        if (result == null) {
            log.warn("auth.guest.issue_rate_limit_unavailable ip={}",
                    fingerprint.substring(0, 12));
            throw new NalssiLogException(AuthErrorCode.GUEST_ISSUANCE_UNAVAILABLE);
        }
        if (result > 0) {
            log.warn("auth.guest.issue_rate_limited scope=ip ip={} count={}",
                    fingerprint.substring(0, 12), result);
            throw new NalssiLogException(AuthErrorCode.GUEST_ISSUANCE_RATE_LIMITED);
        }
        if (result < 0) {
            log.warn("auth.guest.issue_rate_limited scope=global count={}", -result);
            throw new NalssiLogException(AuthErrorCode.GUEST_ISSUANCE_RATE_LIMITED);
        }
    }
}
