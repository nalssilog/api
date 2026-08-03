package com.nalssilog.auth.mobile.oauth;

import com.nalssilog.auth.config.AuthProperties;
import com.nalssilog.common.security.SecretFingerprint;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Repository
@RequiredArgsConstructor
public class MobileOAuthAuthorizationRequestStore {

    private static final String KEY_PREFIX = "auth:mobile:authorization-request:";
    private static final Pattern TRANSACTION_ID_PATTERN =
            Pattern.compile("^[A-Za-z0-9_-]{43}$");

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AuthProperties properties;

    public void save(
            String transactionId,
            OAuth2AuthorizationRequest authorizationRequest
    ) {
        redisTemplate.opsForValue().set(
                key(transactionId),
                serialize(AuthorizationRequestSnapshot.from(authorizationRequest)),
                properties.mobile().transactionTtl());
    }

    public Optional<OAuth2AuthorizationRequest> find(String transactionId) {
        if (!validTransactionId(transactionId)) {
            return Optional.empty();
        }

        return deserialize(redisTemplate.opsForValue().get(key(transactionId)), transactionId);
    }

    public Optional<OAuth2AuthorizationRequest> take(String transactionId) {
        if (!validTransactionId(transactionId)) {
            return Optional.empty();
        }

        return deserialize(
                redisTemplate.opsForValue().getAndDelete(key(transactionId)),
                transactionId);
    }

    private String key(String transactionId) {
        return KEY_PREFIX + SecretFingerprint.sha256(transactionId);
    }

    private boolean validTransactionId(String transactionId) {
        return transactionId != null
                && TRANSACTION_ID_PATTERN.matcher(transactionId).matches();
    }

    private String serialize(AuthorizationRequestSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "mobile OAuth authorization request serialization failed",
                    exception);
        }
    }

    private Optional<OAuth2AuthorizationRequest> deserialize(
            String value,
            String transactionId
    ) {
        if (value == null) {
            return Optional.empty();
        }

        try {
            OAuth2AuthorizationRequest authorizationRequest = objectMapper
                    .readValue(value, AuthorizationRequestSnapshot.class)
                    .toAuthorizationRequest();

            if (!MobileOAuthRequestAttributes.mobileState(transactionId)
                    .equals(authorizationRequest.getState())) {
                return Optional.empty();
            }

            return Optional.of(authorizationRequest);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "mobile OAuth authorization request deserialization failed",
                    exception);
        }
    }

    private record AuthorizationRequestSnapshot(
            String authorizationUri,
            String clientId,
            String redirectUri,
            Set<String> scopes,
            String state,
            Map<String, String> additionalParameters,
            Map<String, String> attributes,
            String authorizationRequestUri
    ) {

        private static AuthorizationRequestSnapshot from(
                OAuth2AuthorizationRequest authorizationRequest
        ) {
            return new AuthorizationRequestSnapshot(
                    authorizationRequest.getAuthorizationUri(),
                    authorizationRequest.getClientId(),
                    authorizationRequest.getRedirectUri(),
                    Set.copyOf(authorizationRequest.getScopes()),
                    authorizationRequest.getState(),
                    stringMap(authorizationRequest.getAdditionalParameters()),
                    stringMap(authorizationRequest.getAttributes()),
                    authorizationRequest.getAuthorizationRequestUri());
        }

        private OAuth2AuthorizationRequest toAuthorizationRequest() {
            return OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri(authorizationUri)
                    .clientId(clientId)
                    .redirectUri(redirectUri)
                    .scopes(scopes)
                    .state(state)
                    .additionalParameters(new LinkedHashMap<>(additionalParameters))
                    .attributes(new LinkedHashMap<>(attributes))
                    .authorizationRequestUri(authorizationRequestUri)
                    .build();
        }

        private static Map<String, String> stringMap(Map<String, Object> values) {
            LinkedHashMap<String, String> result = new LinkedHashMap<>();

            values.forEach((key, value) -> {
                if (value != null) {
                    result.put(key, String.valueOf(value));
                }
            });

            return result;
        }
    }
}
