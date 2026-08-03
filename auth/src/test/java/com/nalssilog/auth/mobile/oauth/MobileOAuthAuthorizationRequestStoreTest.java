package com.nalssilog.auth.mobile.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nalssilog.auth.config.AuthProperties;
import com.nalssilog.auth.ticket.AuthChannel;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import tools.jackson.databind.ObjectMapper;

@SuppressWarnings("java:S5960")
class MobileOAuthAuthorizationRequestStoreTest {

    private static final String TRANSACTION_ID = "T".repeat(43);
    private static final Duration TRANSACTION_TTL = Duration.ofMinutes(10);

    @Test
    void authorizationRequestRoundTripsAndIsConsumedOnce() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        AuthProperties properties = mock(AuthProperties.class);
        MobileOAuthAuthorizationRequestStore store =
                new MobileOAuthAuthorizationRequestStore(
                        redisTemplate,
                        new ObjectMapper(),
                        properties);
        OAuth2AuthorizationRequest authorizationRequest =
                authorizationRequest();

        when(redisTemplate.opsForValue()).thenReturn(values);
        when(properties.mobile()).thenReturn(new AuthProperties.Mobile(
                List.of("nalssilog-dev://auth/callback"),
                TRANSACTION_TTL,
                Duration.ofSeconds(90),
                "test-hmac-secret",
                List.of()));

        store.save(TRANSACTION_ID, authorizationRequest);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        verify(values).set(
                keyCaptor.capture(),
                valueCaptor.capture(),
                eq(TRANSACTION_TTL));

        when(values.get(keyCaptor.getValue())).thenReturn(valueCaptor.getValue());
        when(values.getAndDelete(keyCaptor.getValue()))
                .thenReturn(valueCaptor.getValue())
                .thenReturn(null);

        OAuth2AuthorizationRequest found = store.find(TRANSACTION_ID).orElseThrow();

        assertThat(found.getState()).isEqualTo(authorizationRequest.getState());
        assertThat(found.getClientId()).isEqualTo("client-id");
        assertThat(found.getAdditionalParameters()).containsEntry("nonce", "nonce-hash");
        assertThat(found.<String>getAttribute(OAuth2ParameterNames.REGISTRATION_ID))
                .isEqualTo("kakao");
        assertThat(found.<String>getAttribute(
                MobileOAuthRequestAttributes.AUTHORIZATION_CHANNEL_ATTRIBUTE))
                .isEqualTo("MOBILE");
        assertThat(found.<String>getAttribute(
                MobileOAuthRequestAttributes.AUTHORIZATION_ATTRIBUTE))
                .isEqualTo(TRANSACTION_ID);

        assertThat(store.take(TRANSACTION_ID)).isPresent();
        assertThat(store.take(TRANSACTION_ID)).isEmpty();
    }

    private OAuth2AuthorizationRequest authorizationRequest() {
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .clientId("client-id")
                .redirectUri("https://dev-api.nalssilog.com/login/oauth2/code/kakao")
                .scopes(Set.of("account_email", "profile_nickname"))
                .state(MobileOAuthRequestAttributes.mobileState(TRANSACTION_ID))
                .additionalParameters(parameters ->
                        parameters.put("nonce", "nonce-hash"))
                .attributes(attributes -> {
                    attributes.put(OAuth2ParameterNames.REGISTRATION_ID, "kakao");
                    attributes.put(
                            MobileOAuthRequestAttributes.AUTHORIZATION_CHANNEL_ATTRIBUTE,
                            AuthChannel.MOBILE);
                    attributes.put(
                            MobileOAuthRequestAttributes.AUTHORIZATION_ATTRIBUTE,
                            TRANSACTION_ID);
                })
                .build();
    }
}
