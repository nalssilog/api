package com.nalssilog.auth.ticket;

import com.nalssilog.member.domain.Provider;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** 확정 전 상태(가입 대기/연동 대기)를 잠깐 들고 있는 티켓 저장소 (Redis, 단기 TTL, JSON 직렬화). */
@Repository
@RequiredArgsConstructor
public class AuthTicketStore {

    private static final String SIGNUP_PREFIX = "auth:ticket:signup:";
    private static final String LINK_PREFIX = "auth:ticket:link:";
    private static final String LINK_CONSENT_PREFIX = "auth:ticket:link:consent:";
    private static final String LINK_INTENT_PREFIX = "auth:ticket:link-intent:";
    private static final String SIGNUP_LOCK_PREFIX = "auth:ticket:signup-lock:";
    private static final String SIGNUP_COMPLETED_PREFIX = "auth:ticket:signup-completed:";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final DefaultRedisScript<Long> COMPLETE_SIGNUP_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) ~= ARGV[1] then
                return 0
            end
            redis.call('SET', KEYS[2], ARGV[2], 'PX', ARGV[3])
            redis.call('DEL', KEYS[3])
            redis.call('DEL', KEYS[1])
            return 1
            """, Long.class);

    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

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

    public SignupClaim claimSignup(String ticketId, String claimId, Duration ttl) {
        Optional<SignupCompletion> completed = findSignupCompletion(ticketId);

        if (completed.isPresent()) {

            return SignupClaim.completed(completed.get());
        }

        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                SIGNUP_LOCK_PREFIX + ticketId,
                claimId,
                ttl);

        if (!Boolean.TRUE.equals(acquired)) {

            return findSignupCompletion(ticketId)
                    .map(SignupClaim::completed)
                    .orElseGet(SignupClaim::inProgress);
        }

        Optional<SignupTicket> ticket = findSignup(ticketId);

        if (ticket.isEmpty()) {
            releaseSignupClaim(ticketId, claimId);

            return findSignupCompletion(ticketId)
                    .map(SignupClaim::completed)
                    .orElseGet(SignupClaim::missing);
        }

        return SignupClaim.claimed(ticket.get());
    }

    public void completeSignup(
            String ticketId,
            String claimId,
            SignupCompletion completion,
            Duration ttl
    ) {
        Long completed = redisTemplate.execute(
                COMPLETE_SIGNUP_SCRIPT,
                List.of(
                        SIGNUP_LOCK_PREFIX + ticketId,
                        SIGNUP_COMPLETED_PREFIX + ticketId,
                        SIGNUP_PREFIX + ticketId),
                claimId,
                serialize(completion),
                String.valueOf(ttl.toMillis()));

        if (completed == null || completed != 1L) {
            throw new IllegalStateException("signup ticket claim was lost");
        }
    }

    public void releaseSignupClaim(String ticketId, String claimId) {
        redisTemplate.execute(
                RELEASE_LOCK_SCRIPT,
                List.of(SIGNUP_LOCK_PREFIX + ticketId),
                claimId);
    }

    public Optional<SignupCompletion> findSignupCompletion(String ticketId) {

        return read(SIGNUP_COMPLETED_PREFIX + ticketId, SignupCompletion.class);
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
        } catch (JacksonException e) {
            throw new IllegalStateException("인증 티켓 역직렬화 실패", e);
        }
    }

    private String serialize(Object ticket) {
        try {

            return OBJECT_MAPPER.writeValueAsString(ticket);
        } catch (JacksonException e) {
            throw new IllegalStateException("인증 티켓 직렬화 실패", e);
        }
    }

    public enum SignupClaimStatus {
        CLAIMED,
        COMPLETED,
        IN_PROGRESS,
        MISSING
    }

    public record SignupCompletion(
            Long memberId,
            Provider provider,
            String accessToken,
            String refreshToken,
            long refreshTokenMaxAgeMillis,
            long completedAtEpochMillis
    ) {
    }

    public record SignupClaim(
            SignupClaimStatus status,
            SignupTicket ticket,
            SignupCompletion completion
    ) {

        private static SignupClaim claimed(SignupTicket ticket) {

            return new SignupClaim(SignupClaimStatus.CLAIMED, ticket, null);
        }

        private static SignupClaim completed(SignupCompletion completion) {

            return new SignupClaim(SignupClaimStatus.COMPLETED, null, completion);
        }

        private static SignupClaim inProgress() {

            return new SignupClaim(SignupClaimStatus.IN_PROGRESS, null, null);
        }

        private static SignupClaim missing() {

            return new SignupClaim(SignupClaimStatus.MISSING, null, null);
        }
    }
}
