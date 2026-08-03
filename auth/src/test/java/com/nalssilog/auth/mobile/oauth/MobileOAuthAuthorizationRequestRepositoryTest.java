package com.nalssilog.auth.mobile.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import com.nalssilog.auth.ticket.AuthChannel;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

@SuppressWarnings("java:S5960")
class MobileOAuthAuthorizationRequestRepositoryTest {

    private static final String TRANSACTION_ID = "T".repeat(43);
    private static final String MOBILE_STATE =
            MobileOAuthRequestAttributes.mobileState(TRANSACTION_ID);
    private static final String WEB_STATE =
            MobileOAuthRequestAttributes.webState("web-state-0123456789");

    private final MobileOAuthAuthorizationRequestRepository repository =
            new MobileOAuthAuthorizationRequestRepository();

    @Test
    void webAndMobileRequestsInSameBrowserAreStoredByState() {
        MockHttpSession session = new MockHttpSession();
        OAuth2AuthorizationRequest web = authorizationRequest(
                WEB_STATE,
                AuthChannel.WEB,
                null);
        OAuth2AuthorizationRequest mobile = authorizationRequest(
                MOBILE_STATE,
                AuthChannel.MOBILE,
                TRANSACTION_ID);

        save(mobile, session);
        save(web, session);

        MockHttpServletRequest mobileCallback = callback(session, MOBILE_STATE);
        OAuth2AuthorizationRequest removedMobile =
                repository.removeAuthorizationRequest(
                        mobileCallback,
                        new MockHttpServletResponse());

        assertThat(removedMobile).isEqualTo(mobile);
        assertThat(MobileOAuthRequestAttributes.channel(mobileCallback))
                .contains(AuthChannel.MOBILE);
        assertThat(MobileOAuthRequestAttributes.transactionId(mobileCallback))
                .contains(TRANSACTION_ID);

        MockHttpServletRequest webCallback = callback(session, WEB_STATE);

        assertThat(repository.loadAuthorizationRequest(webCallback))
                .isEqualTo(web);
        assertThat(repository.removeAuthorizationRequest(
                webCallback,
                new MockHttpServletResponse()))
                .isEqualTo(web);
        assertThat(MobileOAuthRequestAttributes.channel(webCallback))
                .contains(AuthChannel.WEB);
    }

    @Test
    void mobileStateStillIdentifiesFlowWhenSessionCookieIsMissing() {
        MockHttpServletRequest callback = new MockHttpServletRequest();

        callback.addParameter("state", MOBILE_STATE);

        assertThat(repository.removeAuthorizationRequest(
                callback,
                new MockHttpServletResponse()))
                .isNull();
        assertThat(MobileOAuthRequestAttributes.channel(callback))
                .contains(AuthChannel.MOBILE);
        assertThat(MobileOAuthRequestAttributes.transactionId(callback))
                .contains(TRANSACTION_ID);
    }

    private void save(
            OAuth2AuthorizationRequest authorizationRequest,
            MockHttpSession session
    ) {
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setSession(session);
        repository.saveAuthorizationRequest(
                authorizationRequest,
                request,
                new MockHttpServletResponse());
    }

    private MockHttpServletRequest callback(
            MockHttpSession session,
            String state
    ) {
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setSession(session);
        request.addParameter("state", state);

        return request;
    }

    private OAuth2AuthorizationRequest authorizationRequest(
            String state,
            AuthChannel channel,
            String transactionId
    ) {
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://provider.test/oauth/authorize")
                .clientId("client-id")
                .redirectUri("https://api.test/login/oauth2/code/provider")
                .scopes(Set.of("profile"))
                .state(state)
                .attributes(attributes -> {
                    attributes.put(
                            MobileOAuthRequestAttributes.AUTHORIZATION_CHANNEL_ATTRIBUTE,
                            channel);

                    if (transactionId != null) {
                        attributes.put(
                                MobileOAuthRequestAttributes.AUTHORIZATION_ATTRIBUTE,
                                transactionId);
                    }
                })
                .build();
    }
}
