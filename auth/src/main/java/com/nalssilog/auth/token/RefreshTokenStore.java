package com.nalssilog.auth.token;

import com.nalssilog.member.domain.Provider;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

/**
 * refresh 세션 저장소 (Redis).
 *
 * <p>활성 RT는 SHA-256 해시만 저장한다. rotation 시 Lua 스크립트가 기존 키 소비·새 키 생성·역인덱스 교체·
 * 사용 완료 tombstone 생성을 한 번에 수행한다. 같은 RT가 짧은 retry grace 안에 다시 오면 별도 5초 키에만
 * 보관한 첫 응답의 새 RT를 재전달하고, grace 이후 재사용은 sessionId 단위로 전부 폐기할 수 있게 식별정보만 남긴다.
 */
@Repository
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "auth:refresh:";
    private static final String MEMBER_SESSIONS_PREFIX = "auth:member-sessions:";
    private static final String USED_PREFIX = "auth:refresh-used:";
    private static final String RETRY_PREFIX = "auth:refresh-retry:";
    private static final String REVOKED_SESSION_PREFIX = "auth:session-revoked:";

    private static final String FIELD_MEMBER_ID = "memberId";
    private static final String FIELD_SESSION_ID = "sessionId";
    private static final String FIELD_PROVIDER = "provider";
    private static final String FIELD_DEVICE_NAME = "deviceName";
    private static final String FIELD_IP = "ip";
    private static final String FIELD_LOGIN_AT = "loginAt";
    private static final String FIELD_LAST_ACTIVE_AT = "lastActiveAt";
    private static final String FIELD_REPLACEMENT_HASH = "replacementHash";
    private static final String FIELD_USED_AT = "usedAt";

    @SuppressWarnings("rawtypes")
    private static final DefaultRedisScript<List> ROTATE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[6]) == 1 then
                return {'REVOKED', '', '', '', ARGV[6], '0'}
            end

            if redis.call('EXISTS', KEYS[1]) == 1 then
                local actualMemberId = redis.call('HGET', KEYS[1], 'memberId')
                local actualSessionId = redis.call('HGET', KEYS[1], 'sessionId')
                if actualMemberId ~= ARGV[7] or actualSessionId ~= ARGV[6] then
                    return {'MISSING', '', '', '', ''}
                end

                redis.call('DEL', KEYS[1])
                redis.call('SREM', KEYS[3], ARGV[1])

                redis.call('HSET', KEYS[2],
                    'memberId', ARGV[7],
                    'sessionId', ARGV[6],
                    'provider', ARGV[8],
                    'deviceName', ARGV[9],
                    'ip', ARGV[10],
                    'loginAt', ARGV[11],
                    'lastActiveAt', ARGV[12])
                redis.call('PEXPIRE', KEYS[2], ARGV[4])
                redis.call('SADD', KEYS[3], ARGV[2])
                redis.call('PEXPIRE', KEYS[3], ARGV[4])

                redis.call('HSET', KEYS[4],
                    'memberId', ARGV[7],
                    'sessionId', ARGV[6],
                    'replacementHash', ARGV[2],
                    'usedAt', ARGV[12])
                redis.call('PEXPIRE', KEYS[4], ARGV[4])
                redis.call('SET', KEYS[5], ARGV[3], 'PX', ARGV[5])

                -- 구버전의 비원자 rotation으로 같은 sessionId에 활성 해시가 여러 개 남았어도
                -- 이번 rotation 결과 하나로 수렴시킨다.
                local indexedHashes = redis.call('SMEMBERS', KEYS[3])
                for _, indexedHash in ipairs(indexedHashes) do
                    if indexedHash ~= ARGV[2] then
                        local indexedKey = 'auth:refresh:' .. indexedHash
                        if redis.call('HGET', indexedKey, 'sessionId') == ARGV[6] then
                            local indexedUsedKey = 'auth:refresh-used:' .. indexedHash
                            local indexedRetryKey = 'auth:refresh-retry:' .. indexedHash
                            redis.call('DEL', indexedKey)
                            redis.call('SREM', KEYS[3], indexedHash)
                            redis.call('HSET', indexedUsedKey,
                                'memberId', ARGV[7],
                                'sessionId', ARGV[6],
                                'replacementHash', ARGV[2],
                                'usedAt', ARGV[12])
                            redis.call('PEXPIRE', indexedUsedKey, ARGV[4])
                            redis.call('SET', indexedRetryKey, ARGV[3], 'PX', ARGV[5])
                        end
                    end
                end

                return {'ROTATED', ARGV[3], ARGV[2], ARGV[7], ARGV[6], ARGV[4]}
            end

            if redis.call('EXISTS', KEYS[4]) == 1 then
                local memberId = redis.call('HGET', KEYS[4], 'memberId')
                local sessionId = redis.call('HGET', KEYS[4], 'sessionId')
                local replacementHash = redis.call('HGET', KEYS[4], 'replacementHash')
                local revokedKey = 'auth:session-revoked:' .. sessionId
                local replacementKey = 'auth:refresh:' .. replacementHash

                if redis.call('EXISTS', revokedKey) == 1 then
                    return {'REVOKED', '', replacementHash, memberId, sessionId, '0'}
                end

                local replacementToken = redis.call('GET', KEYS[5])
                if replacementToken and redis.call('EXISTS', replacementKey) == 1 then
                    local replacementTtl = redis.call('PTTL', replacementKey)
                    return {'RETRIED', replacementToken, replacementHash, memberId, sessionId, tostring(replacementTtl)}
                end

                return {'REUSED', '', replacementHash, memberId, sessionId, '0'}
            end

            return {'MISSING', '', '', '', '', '0'}
            """, List.class);

    private static final DefaultRedisScript<Long> REVOKE_SESSION_SCRIPT = new DefaultRedisScript<>("""
            local hashes = redis.call('SMEMBERS', KEYS[1])
            local deleted = 0
            for _, hash in ipairs(hashes) do
                local refreshKey = ARGV[3] .. hash
                if redis.call('HGET', refreshKey, 'sessionId') == ARGV[1] then
                    redis.call('DEL', refreshKey)
                    redis.call('SREM', KEYS[1], hash)
                    deleted = deleted + 1
                end
            end
            redis.call('SET', KEYS[2], '1', 'PX', ARGV[2])
            return deleted
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public void save(String tokenHash, SessionData session, Duration ttl) {
        Map<String, String> fields = Map.of(
                FIELD_MEMBER_ID, String.valueOf(session.memberId()),
                FIELD_SESSION_ID, session.sessionId(),
                FIELD_PROVIDER, session.provider().name(),
                FIELD_DEVICE_NAME, session.deviceName(),
                FIELD_IP, session.ip(),
                FIELD_LOGIN_AT, String.valueOf(session.loginAt().toEpochMilli()),
                FIELD_LAST_ACTIVE_AT, String.valueOf(session.lastActiveAt().toEpochMilli()));

        String key = key(tokenHash);

        redisTemplate.opsForHash().putAll(key, fields);
        redisTemplate.expire(key, ttl);

        String memberKey = memberKey(session.memberId());

        redisTemplate.opsForSet().add(memberKey, tokenHash);
        redisTemplate.expire(memberKey, ttl);
    }

    public RotationResult rotate(String currentHash, String replacementToken, SessionData replacement,
                                 Duration ttl, Duration retryGrace) {
        List<String> keys = List.of(
                key(currentHash),
                key(replacement.tokenHash()),
                memberKey(replacement.memberId()),
                usedKey(currentHash),
                retryKey(currentHash),
                revokedSessionKey(replacement.sessionId()));

        @SuppressWarnings("unchecked")
        List<Object> raw = redisTemplate.execute(
                ROTATE_SCRIPT,
                keys,
                currentHash,
                replacement.tokenHash(),
                replacementToken,
                String.valueOf(ttl.toMillis()),
                String.valueOf(retryGrace.toMillis()),
                replacement.sessionId(),
                String.valueOf(replacement.memberId()),
                replacement.provider().name(),
                replacement.deviceName(),
                replacement.ip(),
                String.valueOf(replacement.loginAt().toEpochMilli()),
                String.valueOf(replacement.lastActiveAt().toEpochMilli()));

        return toRotationResult(raw);
    }

    public Optional<SessionData> findSession(String tokenHash) {
        Map<Object, Object> raw = redisTemplate.opsForHash().entries(key(tokenHash));

        if (raw.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(toSessionData(tokenHash, raw));
        } catch (IllegalArgumentException _) {
            // provider 필드가 없던 구버전 세션은 실제 인증 수단을 보장할 수 없으므로 재로그인시킨다.

            return Optional.empty();
        }
    }

    public Optional<UsedToken> findUsedToken(String tokenHash) {
        Map<Object, Object> raw = redisTemplate.opsForHash().entries(usedKey(tokenHash));

        if (raw.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(new UsedToken(
                    Long.valueOf(str(raw, FIELD_MEMBER_ID)),
                    str(raw, FIELD_SESSION_ID),
                    str(raw, FIELD_REPLACEMENT_HASH),
                    Instant.ofEpochMilli(Long.parseLong(str(raw, FIELD_USED_AT)))));
        } catch (IllegalArgumentException _) {
            return Optional.empty();
        }
    }

    public boolean isSessionRevoked(String sessionId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(revokedSessionKey(sessionId)));
    }

    /**
     * 활성 토큰 또는 이미 rotation 된 토큰으로 세션 전체를 폐기한다.
     * 로그아웃 응답을 잃은 클라이언트가 직전 RT를 다시 보내도 새 RT가 살아나지 않게 used tombstone도 확인한다.
     */
    public Optional<SessionRef> revokeByTokenHash(String tokenHash, Duration markerTtl) {
        Optional<SessionRef> active = findSession(tokenHash)
                .map(session -> new SessionRef(session.memberId(), session.sessionId()));
        Optional<SessionRef> target = active.or(() -> findUsedToken(tokenHash)
                .map(used -> new SessionRef(used.memberId(), used.sessionId())));

        target.ifPresent(session -> revokeSession(session.memberId(), session.sessionId(), markerTtl));

        return target;
    }

    /** 같은 sessionId 아래 남아 있는 모든 활성 해시를 지우고 재발급 차단 marker를 남긴다. */
    public long revokeSession(Long memberId, String sessionId, Duration markerTtl) {
        Long deleted = redisTemplate.execute(
                REVOKE_SESSION_SCRIPT,
                List.of(memberKey(memberId), revokedSessionKey(sessionId)),
                sessionId,
                String.valueOf(markerTtl.toMillis()),
                KEY_PREFIX);

        return deleted == null ? 0 : deleted;
    }

    /** 해당 회원의 모든 refresh 세션 만료(전 기기 로그아웃). 탈퇴 시 호출. */
    public long deleteAllByMember(Long memberId, Duration markerTtl) {
        Set<String> sessionIds = findSessionsByMember(memberId).stream()
                .map(SessionData::sessionId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        long deleted = 0;

        for (String sessionId : sessionIds) {
            deleted += revokeSession(memberId, sessionId, markerTtl);
        }

        redisTemplate.delete(memberKey(memberId));

        return deleted;
    }

    /** 회원의 활성 세션 목록(만료된 해시는 제외). */
    public List<SessionData> findSessionsByMember(Long memberId) {
        Set<String> hashes = redisTemplate.opsForSet().members(memberKey(memberId));

        if (hashes == null || hashes.isEmpty()) {
            return List.of();
        }

        List<SessionData> sessions = new ArrayList<>();

        for (String tokenHash : hashes) {
            findSession(tokenHash).ifPresent(sessions::add);
        }

        return sessions;
    }

    private RotationResult toRotationResult(List<Object> raw) {
        if (raw == null || raw.isEmpty()) {
            return RotationResult.missing();
        }

        RotationStatus status;

        try {
            status = RotationStatus.valueOf(value(raw, 0));
        } catch (IllegalArgumentException _) {
            return RotationResult.missing();
        }

        return new RotationResult(
                status,
                value(raw, 1),
                value(raw, 2),
                longValue(raw, 3),
                value(raw, 4),
                primitiveLongValue(raw, 5));
    }

    private SessionData toSessionData(String tokenHash, Map<Object, Object> raw) {
        return new SessionData(
                tokenHash,
                str(raw, FIELD_SESSION_ID),
                Long.valueOf(str(raw, FIELD_MEMBER_ID)),
                Provider.valueOf(str(raw, FIELD_PROVIDER)),
                str(raw, FIELD_DEVICE_NAME),
                str(raw, FIELD_IP),
                Instant.ofEpochMilli(Long.parseLong(str(raw, FIELD_LOGIN_AT))),
                Instant.ofEpochMilli(Long.parseLong(str(raw, FIELD_LAST_ACTIVE_AT))));
    }

    private String str(Map<Object, Object> raw, String field) {
        Object value = raw.get(field);

        return value == null ? "" : value.toString();
    }

    private String value(List<Object> raw, int index) {
        if (index >= raw.size() || raw.get(index) == null) {
            return "";
        }

        return raw.get(index).toString();
    }

    private Long longValue(List<Object> raw, int index) {
        String value = value(raw, index);

        return value.isBlank() ? null : Long.valueOf(value);
    }

    private long primitiveLongValue(List<Object> raw, int index) {
        String value = value(raw, index);

        return value.isBlank() ? 0 : Long.parseLong(value);
    }

    private String key(String tokenHash) {
        return KEY_PREFIX + tokenHash;
    }

    private String memberKey(Long memberId) {
        return MEMBER_SESSIONS_PREFIX + memberId;
    }

    private String usedKey(String tokenHash) {
        return USED_PREFIX + tokenHash;
    }

    private String retryKey(String tokenHash) {
        return RETRY_PREFIX + tokenHash;
    }

    private String revokedSessionKey(String sessionId) {
        return REVOKED_SESSION_PREFIX + sessionId;
    }

    public enum RotationStatus {
        ROTATED,
        RETRIED,
        REUSED,
        REVOKED,
        MISSING
    }

    public record RotationResult(
            RotationStatus status,
            String replacementToken,
            String replacementHash,
            Long memberId,
            String sessionId,
            long refreshTokenTtlMillis
    ) {
        private static RotationResult missing() {
            return new RotationResult(RotationStatus.MISSING, "", "", null, "", 0);
        }
    }

    public record UsedToken(Long memberId, String sessionId, String replacementHash, Instant usedAt) {
    }

    public record SessionRef(Long memberId, String sessionId) {
    }
}
