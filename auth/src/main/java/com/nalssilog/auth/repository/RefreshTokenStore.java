package com.nalssilog.auth.repository;

import com.nalssilog.auth.application.dto.SessionData;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * refresh 세션 저장소 (Redis). key=refresh 토큰 SHA-256 해시, value=세션 메타데이터 Hash. TTL 자동만료.
 * memberId → 세션 해시 set 역인덱스로 (a)탈퇴 시 전 기기 만료 (b)로그인 기기 목록을 지원.
 */
@Repository
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "auth:refresh:";
    private static final String MEMBER_SESSIONS_PREFIX = "auth:member-sessions:";

    private static final String FIELD_MEMBER_ID = "memberId";
    private static final String FIELD_SESSION_ID = "sessionId";
    private static final String FIELD_DEVICE_NAME = "deviceName";
    private static final String FIELD_IP = "ip";
    private static final String FIELD_LOGIN_AT = "loginAt";
    private static final String FIELD_LAST_ACTIVE_AT = "lastActiveAt";

    private final StringRedisTemplate redisTemplate;

    public void save(String tokenHash, SessionData session, Duration ttl) {
        Map<String, String> fields = Map.of(
                FIELD_MEMBER_ID, String.valueOf(session.memberId()),
                FIELD_SESSION_ID, session.sessionId(),
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

    public Optional<Long> findMemberId(String tokenHash) {
        Object value = redisTemplate.opsForHash().get(key(tokenHash), FIELD_MEMBER_ID);

        return Optional.ofNullable(value).map(Object::toString).map(Long::valueOf);
    }

    public Optional<SessionData> findSession(String tokenHash) {
        Map<Object, Object> raw = redisTemplate.opsForHash().entries(key(tokenHash));

        if (raw.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(toSessionData(tokenHash, raw));
    }

    public void delete(String tokenHash) {
        findMemberId(tokenHash).ifPresent(memberId ->
                redisTemplate.opsForSet().remove(memberKey(memberId), tokenHash));
        redisTemplate.delete(key(tokenHash));
    }

    /** 해당 회원의 모든 refresh 세션 만료(전 기기 로그아웃). 탈퇴 시 호출. */
    public void deleteAllByMember(Long memberId) {
        String memberKey = memberKey(memberId);
        Set<String> hashes = redisTemplate.opsForSet().members(memberKey);

        if (hashes != null && !hashes.isEmpty()) {
            redisTemplate.delete(hashes.stream().map(this::key).collect(Collectors.toSet()));
        }
        redisTemplate.delete(memberKey);
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

    private SessionData toSessionData(String tokenHash, Map<Object, Object> raw) {
        return new SessionData(
                tokenHash,
                str(raw, FIELD_SESSION_ID),
                Long.valueOf(str(raw, FIELD_MEMBER_ID)),
                str(raw, FIELD_DEVICE_NAME),
                str(raw, FIELD_IP),
                Instant.ofEpochMilli(Long.parseLong(str(raw, FIELD_LOGIN_AT))),
                Instant.ofEpochMilli(Long.parseLong(str(raw, FIELD_LAST_ACTIVE_AT))));
    }

    private String str(Map<Object, Object> raw, String field) {
        Object value = raw.get(field);

        return value == null ? "" : value.toString();
    }

    private String key(String tokenHash) {
        return KEY_PREFIX + tokenHash;
    }

    private String memberKey(Long memberId) {
        return MEMBER_SESSIONS_PREFIX + memberId;
    }
}
