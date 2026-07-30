package com.nalssilog.auth.mobile.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

class MobileOAuthAuthorizationRequestResolverTest {

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
        assertThat(authorization.getAuthorizationRequestUri())
                .contains("response_mode=form_post");
    }
}
