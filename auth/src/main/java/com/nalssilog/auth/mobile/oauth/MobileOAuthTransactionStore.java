package com.nalssilog.auth.mobile.oauth;

import com.nalssilog.auth.config.AuthProperties;
import com.nalssilog.common.security.SecretFingerprint;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Repository
@RequiredArgsConstructor
public class MobileOAuthTransactionStore {

    private static final String KEY_PREFIX = "auth:mobile:transaction:";
    private static final Pattern TRANSACTION_ID_PATTERN =
            Pattern.compile("^[A-Za-z0-9_-]{43}$");

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AuthProperties properties;

    public void save(String transactionId, MobileOAuthTransaction transaction) {
        redisTemplate.opsForValue().set(
                key(transactionId),
                serialize(transaction),
                properties.mobile().transactionTtl());
    }

    public Optional<MobileOAuthTransaction> find(String transactionId) {
        if (!validTransactionId(transactionId)) {
            return Optional.empty();
        }

        return deserialize(redisTemplate.opsForValue().get(key(transactionId)));
    }

    public Optional<MobileOAuthTransaction> take(String transactionId) {
        if (!validTransactionId(transactionId)) {
            return Optional.empty();
        }

        return deserialize(redisTemplate.opsForValue().getAndDelete(key(transactionId)));
    }

    private String key(String transactionId) {
        return KEY_PREFIX + SecretFingerprint.sha256(transactionId);
    }

    private boolean validTransactionId(String transactionId) {
        return transactionId != null
                && TRANSACTION_ID_PATTERN.matcher(transactionId).matches();
    }

    private String serialize(MobileOAuthTransaction transaction) {
        try {
            return objectMapper.writeValueAsString(transaction);
        } catch (JacksonException exception) {
            throw new IllegalStateException("mobile OAuth transaction serialization failed", exception);
        }
    }

    private Optional<MobileOAuthTransaction> deserialize(String value) {
        if (value == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(value, MobileOAuthTransaction.class));
        } catch (JacksonException exception) {
            throw new IllegalStateException("mobile OAuth transaction deserialization failed", exception);
        }
    }
}
