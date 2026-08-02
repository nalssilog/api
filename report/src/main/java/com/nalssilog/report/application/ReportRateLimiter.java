package com.nalssilog.report.application;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.common.security.SecretFingerprint;
import com.nalssilog.member.config.FeedbackRateLimitProperties;
import com.nalssilog.report.application.dto.ReportActor;
import com.nalssilog.report.config.ReportRateLimitProperties;
import com.nalssilog.report.domain.ReportErrorCode;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReportRateLimiter {

    private static final String KEY_PREFIX = "report:rate:";
    private static final DefaultRedisScript<Long> CHECK_SCRIPT = new DefaultRedisScript<>("""
            local actorCount = redis.call('INCR', KEYS[1])
            if actorCount == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end

            local ipCount = redis.call('INCR', KEYS[2])
            if ipCount == 1 then
                redis.call('PEXPIRE', KEYS[2], ARGV[1])
            end

            if tonumber(ARGV[4]) > 0 then
                local dailyCount = redis.call('INCR', KEYS[3])
                if dailyCount == 1 then
                    redis.call('PEXPIRE', KEYS[3], ARGV[3])
                end
                local ipDailyCount = redis.call('INCR', KEYS[4])
                if ipDailyCount == 1 then
                    redis.call('PEXPIRE', KEYS[4], ARGV[3])
                end
                if dailyCount > tonumber(ARGV[4]) then
                    return 3
                end
                if ipDailyCount > tonumber(ARGV[4]) * tonumber(ARGV[5]) * tonumber(ARGV[5]) then
                    return 4
                end
            end

            if actorCount > tonumber(ARGV[2]) then
                return 1
            end
            if ipCount > tonumber(ARGV[2]) * tonumber(ARGV[5]) then
                return 2
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final ReportRateLimitProperties properties;
    private final String hmacSecret;

    public ReportRateLimiter(
            StringRedisTemplate redisTemplate,
            ReportRateLimitProperties properties,
            FeedbackRateLimitProperties fallbackProperties
    ) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        String baseSecret = properties.hmacSecret() == null
                ? fallbackProperties.ipHmacSecret()
                : properties.hmacSecret();

        // 기존 서버 비밀값을 그대로 재사용하지 않고 용도 문자열로 한 번 파생해 fingerprint 간 상관관계를 끊는다.
        this.hmacSecret = SecretFingerprint.hmacSha256(
                baseSecret, "nalssilog:report-rate-limit:v1");
    }

    public void checkCreate(ReportActor actor, String clientIp) {
        check(Action.CREATE, actor, clientIp,
                properties.createMaxRequests(), properties.createWindow(),
                properties.createDailyMaxRequests());
    }

    public void checkPresign(ReportActor actor, String clientIp) {
        check(Action.PRESIGN, actor, clientIp,
                properties.presignMaxRequests(), properties.presignWindow(),
                properties.presignDailyMaxRequests());
    }

    public void checkFlag(ReportActor actor, String clientIp) {
        check(Action.FLAG, actor, clientIp,
                properties.flagMaxRequests(), properties.flagWindow(),
                properties.flagDailyMaxRequests());
    }

    private void check(
            Action action,
            ReportActor actor,
            String clientIp,
            int maxRequests,
            Duration window,
            int dailyMaxRequests
    ) {
        String actorFingerprint = SecretFingerprint.hmacSha256(
                hmacSecret, actor.type() + ":" + actor.actorKey());
        String ipFingerprint = SecretFingerprint.hmacSha256(
                hmacSecret, clientIp == null || clientIp.isBlank() ? "unknown" : clientIp.strip());
        String prefix = KEY_PREFIX + action.key + ":";
        Long result;

        try {
            result = redisTemplate.execute(
                    CHECK_SCRIPT,
                    List.of(
                            prefix + "actor:" + actorFingerprint,
                            prefix + "ip:" + ipFingerprint,
                            prefix + "daily:" + actorFingerprint,
                            prefix + "ip-daily:" + ipFingerprint),
                    String.valueOf(window.toMillis()),
                    String.valueOf(maxRequests),
                    String.valueOf(Duration.ofDays(1).toMillis()),
                    String.valueOf(dailyMaxRequests),
                    String.valueOf(properties.ipMultiplier()));
        } catch (DataAccessException exception) {
            log.warn("report.rate_limit_unavailable action={} actor={} reason={}",
                    action.key, actorFingerprint.substring(0, 12), exception.getClass().getSimpleName());
            throw new NalssiLogException(ReportErrorCode.RATE_LIMIT_UNAVAILABLE);
        }

        if (result == null) {
            throw new NalssiLogException(ReportErrorCode.RATE_LIMIT_UNAVAILABLE);
        }

        if (result != 0) {
            log.warn("report.rate_limited action={} scope={} actor={}",
                    action.key, scope(result), actorFingerprint.substring(0, 12));
            throw new NalssiLogException(action.errorCode);
        }
    }

    private String scope(long result) {
        return switch ((int) result) {
            case 1 -> "actor";
            case 2 -> "ip";
            case 3 -> "daily";
            case 4 -> "ip-daily";
            default -> "unknown";
        };
    }

    private enum Action {
        CREATE("create", ReportErrorCode.REPORT_RATE_LIMITED),
        PRESIGN("presign", ReportErrorCode.IMAGE_PRESIGN_RATE_LIMITED),
        FLAG("flag", ReportErrorCode.REPORT_FLAG_RATE_LIMITED);

        private final String key;
        private final ReportErrorCode errorCode;

        Action(String key, ReportErrorCode errorCode) {
            this.key = key;
            this.errorCode = errorCode;
        }
    }
}
