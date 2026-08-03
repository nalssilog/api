package com.nalssilog.auth.mobile.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nalssilog.auth.ticket.AuthChannel;
import com.nalssilog.member.domain.Provider;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

class MobileOAuthAuthorizationRequestResolverTest {

    private static final String TRANSACTION_ID = "T".repeat(43);

    @Test
    void appleAuthorizationUsesFormPostResponseMode() {
        ClientRegistration apple = ClientRegistration
                .withRegistrationId("apple")
                .clientId("com.nalssilog.login")
                .clientSecret("generated-per-request")
                .clientAuthenticationMethod(
                        ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(
                        AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(
                        "{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "email", "name")
                .authorizationUri(
                        "https://appleid.apple.com/auth/authorize")
                .tokenUri("https://appleid.apple.com/auth/token")
                .jwkSetUri("https://appleid.apple.com/auth/keys")
                .userNameAttributeName("sub")
                .clientName("Apple")
                .build();
        MobileOAuthAuthorizationRequestResolver resolver =
                new MobileOAuthAuthorizationRequestResolver(
                        new InMemoryClientRegistrationRepository(apple),
                        mock(MobileOAuthTransactionStore.class));
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/oauth2/authorization/apple");

        request.setServletPath("/oauth2/authorization/apple");

        OAuth2AuthorizationRequest authorization =
                resolver.resolve(request, "apple");

        assertThat(authorization).isNotNull();
        assertThat(authorization.getAdditionalParameters())
                .containsEntry("response_mode", "form_post");
        assertThat(authorization.getState()).startsWith("w.");
        assertThat(authorization.<AuthChannel>getAttribute(
                MobileOAuthRequestAttributes.AUTHORIZATION_CHANNEL_ATTRIBUTE))
                .isEqualTo(AuthChannel.WEB);
        assertThat(authorization.getAuthorizationRequestUri())
                .contains("response_mode=form_post");
    }

    @Test
    void mobileAuthorizationBindsFlowAndTransactionToProviderState() {
        ClientRegistration kakao = ClientRegistration
                .withRegistrationId("kakao")
                .clientId("client-id")
                .clientSecret("client-secret")
                .clientAuthenticationMethod(
                        ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(
                        AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(
                        "{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("account_email", "profile_nickname")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v2/user/me")
                .userNameAttributeName("id")
                .clientName("Kakao")
                .build();
        MobileOAuthTransactionStore transactionStore =
                mock(MobileOAuthTransactionStore.class);
        MobileOAuthTransaction transaction = new MobileOAuthTransaction(
                MobileOAuthPurpose.LOGIN,
                Provider.KAKAO,
                "nalssilog-dev://auth/callback",
                "A".repeat(43),
                "app-state-0123456",
                null,
                null);

        when(transactionStore.find(TRANSACTION_ID))
                .thenReturn(Optional.of(transaction));

        MobileOAuthAuthorizationRequestResolver resolver =
                new MobileOAuthAuthorizationRequestResolver(
                        new InMemoryClientRegistrationRepository(kakao),
                        transactionStore);
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/oauth2/authorization/kakao");

        request.setServletPath("/oauth2/authorization/kakao");
        request.addParameter(
                MobileOAuthRequestAttributes.TRANSACTION_PARAMETER,
                TRANSACTION_ID);

        OAuth2AuthorizationRequest authorization =
                resolver.resolve(request, "kakao");

        assertThat(authorization).isNotNull();
        assertThat(authorization.getState())
                .isEqualTo(MobileOAuthRequestAttributes.mobileState(TRANSACTION_ID));
        assertThat(authorization.<AuthChannel>getAttribute(
                MobileOAuthRequestAttributes.AUTHORIZATION_CHANNEL_ATTRIBUTE))
                .isEqualTo(AuthChannel.MOBILE);
        assertThat(authorization.<String>getAttribute(
                MobileOAuthRequestAttributes.AUTHORIZATION_ATTRIBUTE))
                .isEqualTo(TRANSACTION_ID);
        assertThat(authorization.getAuthorizationRequestUri())
                .contains("state=m." + TRANSACTION_ID);
    }
}
