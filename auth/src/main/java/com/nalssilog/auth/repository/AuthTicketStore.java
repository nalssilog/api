package com.nalssilog.auth.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nalssilog.auth.domain.LinkTicket;
import com.nalssilog.auth.domain.SignupTicket;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/** 확정 전 상태(가입 대기/연동 대기)를 잠깐 들고 있는 티켓 저장소 (Redis, 단기 TTL, JSON 직렬화). */
@Repository
@RequiredArgsConstructor
public class AuthTicketStore {

    private static final String SIGNUP_PREFIX = "auth:ticket:signup:";
    private static final String LINK_PREFIX = "auth:ticket:link:";
    private static final String LINK_CONSENT_PREFIX = "auth:ticket:link:consent:";
    private static final String LINK_INTENT_PREFIX = "auth:ticket:link-intent:";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final StringRedisTemplate redisTemplate;

    public void saveSignup(String ticketId, SignupTicket ticket, Duration ttl) {
        redisTemplate.opsForValue().set(SIGNUP_PREFIX + ticketId, serialize(ticket), ttl);
    }

    public Optional<SignupTicket> findSignup(String ticketId) {
        return read(SIGNUP_PREFIX + ticketId, SignupTicket.class);
    }

    public void deleteSignup(String ticketId) {
        redisTemplate.delete(SIGNUP_PREFIX + ticketId);
    }

    public void saveLink(String ticketId, LinkTicket ticket, Duration ttl) {
        redisTemplate.opsForValue().set(LINK_PREFIX + ticketId, serialize(ticket), ttl);
    }

    public Optional<LinkTicket> findLink(String ticketId) {
        return read(LINK_PREFIX + ticketId, LinkTicket.class);
    }

    public void deleteLink(String ticketId) {
        redisTemplate.delete(LINK_PREFIX + ticketId);
    }

    /** '연동' 명시 동의 플래그. 이게 있어야만 재인증 후 실제 연동됨(방치 티켓 자동연동 방지). */
    public void markLinkConsented(String ticketId, Duration ttl) {
        redisTemplate.opsForValue().set(LINK_CONSENT_PREFIX + ticketId, "1", ttl);
    }

    public boolean isLinkConsented(String ticketId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(LINK_CONSENT_PREFIX + ticketId));
    }

    public void deleteLinkConsent(String ticketId) {
        redisTemplate.delete(LINK_CONSENT_PREFIX + ticketId);
    }

    /** 설정에서 시작한 '소셜 추가 연동' 의도(memberId 를 잠깐 보관, 성공 핸들러가 연동에 사용). */
    public void saveLinkIntent(String intentId, Long memberId, Duration ttl) {
        redisTemplate.opsForValue().set(LINK_INTENT_PREFIX + intentId, String.valueOf(memberId), ttl);
    }

    public Optional<Long> findLinkIntent(String intentId) {
        String value = redisTemplate.opsForValue().get(LINK_INTENT_PREFIX + intentId);

        return Optional.ofNullable(value).map(Long::valueOf);
    }

    public void deleteLinkIntent(String intentId) {
        redisTemplate.delete(LINK_INTENT_PREFIX + intentId);
    }

    private <T> Optional<T> read(String key, Class<T> type) {
        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(OBJECT_MAPPER.readValue(value, type));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("인증 티켓 역직렬화 실패", e);
        }
    }

    private String serialize(Object ticket) {
        try {
            return OBJECT_MAPPER.writeValueAsString(ticket);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("인증 티켓 직렬화 실패", e);
        }
    }
}
