package com.nalssilog.auth.mobile.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nalssilog.auth.config.AuthProperties;
import com.nalssilog.auth.ticket.AuthChannel;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.user.OAuth2User;
import tools.jackson.databind.ObjectMapper;

@SuppressWarnings("java:S5960")
class MobileOAuthSessionlessCallbackTest {

    private static final String TRANSACTION_ID = "T".repeat(43);
    private static final String MOBILE_STATE =
            MobileOAuthRequestAttributes.mobileState(TRANSACTION_ID);
    private static final Duration TRANSACTION_TTL = Duration.ofMinutes(10);

    @Test
    void springOAuthFilterAuthenticatesCallbackWithoutHttpSession() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        AuthProperties properties = mock(AuthProperties.class);
        AtomicReference<String> storedValue = new AtomicReference<>();
        MobileOAuthAuthorizationRequestStore store =
                new MobileOAuthAuthorizationRequestStore(
                        redisTemplate,
                        new ObjectMapper(),
                        properties);
        MobileOAuthAuthorizationRequestRepository repository =
                new MobileOAuthAuthorizationRequestRepository(store);
        ClientRegistration registration = kakaoRegistration();
        InMemoryClientRegistrationRepository registrations =
                new InMemoryClientRegistrationRepository(registration);
        OAuth2AuthorizedClientRepository authorizedClients =
                mock(OAuth2AuthorizedClientRepository.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        OAuth2LoginAuthenticationFilter filter = new OAuth2LoginAuthenticationFilter(
                registrations,
                authorizedClients,
                "/login/oauth2/code/*");

        when(redisTemplate.opsForValue()).thenReturn(values);
        when(properties.mobile()).thenReturn(new AuthProperties.Mobile(
                List.of("nalssilog-dev://auth/callback"),
                TRANSACTION_TTL,
                Duration.ofSeconds(90),
                "test-hmac-secret",
                List.of()));
        doAnswer(invocation -> {
            storedValue.set(invocation.getArgument(1));
            return null;
        }).when(values).set(anyString(), anyString(), eq(TRANSACTION_TTL));
        when(values.getAndDelete(anyString()))
                .thenAnswer(invocation -> storedValue.getAndSet(null));

        OAuth2AuthorizationRequest authorizationRequest = authorizationRequest();
        MockHttpServletRequest authorization = new MockHttpServletRequest();

        repository.saveAuthorizationRequest(
                authorizationRequest,
                authorization,
                new MockHttpServletResponse());

        assertThat(authorization.getSession(false)).isNull();

        OAuth2User principal = mock(OAuth2User.class);

        when(principal.getName()).thenReturn("kakao-user");
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenAnswer(invocation -> {
                    OAuth2LoginAuthenticationToken requestToken = invocation.getArgument(0);
                    OAuth2AccessToken accessToken = new OAuth2AccessToken(
                            OAuth2AccessToken.TokenType.BEARER,
                            "provider-access-token",
                            Instant.now(),
                            Instant.now().plusSeconds(300));

                    return new OAuth2LoginAuthenticationToken(
                            registration,
                            requestToken.getAuthorizationExchange(),
                            principal,
                            List.of(),
                            accessToken);
                });

        filter.setAuthorizationRequestRepository(repository);
        filter.setAuthenticationManager(authenticationManager);

        MockHttpServletRequest callback = callback();
        MockHttpServletResponse callbackResponse = new MockHttpServletResponse();
        Authentication authenticationResult = filter.attemptAuthentication(
                callback,
                callbackResponse);

        assertThat(authenticationResult).isInstanceOf(OAuth2AuthenticationToken.class);
        assertThat(callback.getSession(false)).isNull();
        assertThat(MobileOAuthRequestAttributes.channel(callback))
                .contains(AuthChannel.MOBILE);
        assertThat(MobileOAuthRequestAttributes.transactionId(callback))
                .contains(TRANSACTION_ID);
        assertThat(storedValue.get()).isNull();
        verify(authorizedClients).saveAuthorizedClient(
                any(),
                any(),
                eq(callback),
                eq(callbackResponse));
    }

    private MockHttpServletRequest callback() {
        MockHttpServletRequest callback = new MockHttpServletRequest(
                "GET",
                "/login/oauth2/code/kakao");

        callback.setScheme("https");
        callback.setServerName("dev-api.nalssilog.com");
        callback.setServerPort(443);
        callback.addParameter(OAuth2ParameterNames.CODE, "provider-code");
        callback.addParameter(OAuth2ParameterNames.STATE, MOBILE_STATE);

        return callback;
    }

    private OAuth2AuthorizationRequest authorizationRequest() {
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .clientId("client-id")
                .redirectUri("https://dev-api.nalssilog.com/login/oauth2/code/kakao")
                .scopes(Set.of("account_email", "profile_nickname"))
                .state(MOBILE_STATE)
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

    private ClientRegistration kakaoRegistration() {
        return ClientRegistration
                .withRegistrationId("kakao")
                .clientId("client-id")
                .clientSecret("client-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("account_email", "profile_nickname")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v2/user/me")
                .userNameAttributeName("id")
                .clientName("Kakao")
                .build();
    }
}
