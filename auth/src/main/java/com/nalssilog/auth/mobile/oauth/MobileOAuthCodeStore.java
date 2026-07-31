package com.nalssilog.auth.mobile.oauth;

import com.nalssilog.auth.config.AuthProperties;
import com.nalssilog.auth.core.AuthErrorCode;
import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.common.security.SecretFingerprint;
import com.nalssilog.member.domain.Provider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MobileOAuthCodeStore {

    private static final String KEY_PREFIX = "auth:mobile:code:";
    private static final String FIELD_RESULT = "result";
    private static final String FIELD_PROVIDER = "provider";
    private static final String FIELD_MEMBER_ID = "memberId";
    private static final String FIELD_TICKET_ID = "ticketId";
    private static final String FIELD_ERROR_CODE = "errorCode";
    private static final String FIELD_ISSUE_TOKENS = "issueTokens";
    private static final String FIELD_REDIRECT_URI = "redirectUri";
    private static final String FIELD_CODE_CHALLENGE = "codeChallenge";

    @SuppressWarnings("rawtypes")
    private static final DefaultRedisScript<List> CONSUME_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[1]) == 0 then
                return {'MISSING'}
            end
            if redis.call('HGET', KEYS[1], 'redirectUri') ~= ARGV[1] then
                return {'REDIRECT_MISMATCH'}
            end
            if redis.call('HGET', KEYS[1], 'codeChallenge') ~= ARGV[2] then
                return {'PKCE_MISMATCH'}
            end

            local result = redis.call('HGET', KEYS[1], 'result') or ''
            local provider = redis.call('HGET', KEYS[1], 'provider') or ''
            local memberId = redis.call('HGET', KEYS[1], 'memberId') or ''
            local ticketId = redis.call('HGET', KEYS[1], 'ticketId') or ''
            local errorCode = redis.call('HGET', KEYS[1], 'errorCode') or ''
            local issueTokens = redis.call('HGET', KEYS[1], 'issueTokens') or 'false'
            redis.call('DEL', KEYS[1])
            return {'OK', result, provider, memberId, ticketId, errorCode, issueTokens}
            """, List.class);

    private final StringRedisTemplate redisTemplate;
    private final AuthProperties properties;

    public void save(
            String rawCode,
            MobileOAuthGrant grant,
            String redirectUri,
            String codeChallenge
    ) {
        Map<String, String> fields = new HashMap<>();

        fields.put(FIELD_RESULT, grant.result().name());
        fields.put(FIELD_PROVIDER, grant.provider() == null ? "" : grant.provider().name());
        fields.put(FIELD_MEMBER_ID, grant.memberId() == null ? "" : String.valueOf(grant.memberId()));
        fields.put(FIELD_TICKET_ID, blank(grant.ticketId()));
        fields.put(FIELD_ERROR_CODE, blank(grant.errorCode()));
        fields.put(FIELD_ISSUE_TOKENS, String.valueOf(grant.issueTokens()));
        fields.put(FIELD_REDIRECT_URI, redirectUri);
        fields.put(FIELD_CODE_CHALLENGE, codeChallenge);

        String key = key(rawCode);

        redisTemplate.opsForHash().putAll(key, fields);
        redisTemplate.expire(key, properties.mobile().codeTtl());
    }

    public MobileOAuthGrant consume(
            String rawCode,
            String redirectUri,
            String calculatedChallenge
    ) {
        if (rawCode == null || rawCode.isBlank() || rawCode.length() > 200) {
            throw new NalssiLogException(AuthErrorCode.AUTH_MOBILE_CODE_INVALID);
        }

        @SuppressWarnings("unchecked")
        List<Object> values = redisTemplate.execute(
                CONSUME_SCRIPT,
                List.of(key(rawCode)),
                redirectUri,
                calculatedChallenge);
        String status = value(values, 0);

        if ("REDIRECT_MISMATCH".equals(status)) {
            throw new NalssiLogException(AuthErrorCode.AUTH_REDIRECT_URI_INVALID);
        }

        if ("PKCE_MISMATCH".equals(status)) {
            throw new NalssiLogException(AuthErrorCode.AUTH_PKCE_VERIFICATION_FAILED);
        }

        if (!"OK".equals(status)) {
            throw new NalssiLogException(AuthErrorCode.AUTH_MOBILE_CODE_INVALID);
        }

        return new MobileOAuthGrant(
                MobileAuthResult.valueOf(value(values, 1)),
                enumValue(Provider.class, value(values, 2)),
                longValue(value(values, 3)),
                nullIfBlank(value(values, 4)),
                nullIfBlank(value(values, 5)),
                Boolean.parseBoolean(value(values, 6)));
    }

    private String key(String rawCode) {
        return KEY_PREFIX + SecretFingerprint.sha256(rawCode);
    }

    private String blank(String value) {
        return value == null ? "" : value;
    }

    private String value(List<Object> values, int index) {
        if (values == null || index >= values.size() || values.get(index) == null) {
            return "";
        }

        return String.valueOf(values.get(index));
    }

    private Long longValue(String value) {
        return value == null || value.isBlank() ? null : Long.valueOf(value);
    }

    private String nullIfBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private <T extends Enum<T>> T enumValue(Class<T> type, String value) {
        return value == null || value.isBlank() ? null : Enum.valueOf(type, value);
    }
}
