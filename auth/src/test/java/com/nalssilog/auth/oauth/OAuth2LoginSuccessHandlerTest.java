package com.nalssilog.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.nalssilog.auth.device.DeviceInfoResolver;
import com.nalssilog.auth.mobile.oauth.MobileOAuthRequestAttributes;
import com.nalssilog.auth.mobile.oauth.MobileOAuthService;
import com.nalssilog.auth.web.AuthCookieManager;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;

@SuppressWarnings("java:S5960")
class OAuth2LoginSuccessHandlerTest {

    private static final String TRANSACTION_ID = "T".repeat(43);

    @Test
    void mobileSuccessUsesDeepLinkFlowWithoutReadingWebCookies() throws Exception {
        AuthCookieManager cookieManager = mock(AuthCookieManager.class);
        DeviceInfoResolver deviceInfoResolver = mock(DeviceInfoResolver.class);
        MobileOAuthService mobileOAuthService = mock(MobileOAuthService.class);
        WebOAuthService webOAuthService = mock(WebOAuthService.class);
        OAuth2LoginSuccessHandler handler = new OAuth2LoginSuccessHandler(
                cookieManager,
                deviceInfoResolver,
                mobileOAuthService,
                webOAuthService);
        SocialPrincipal principal = mock(SocialPrincipal.class);
        Authentication authentication = mock(Authentication.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        String callback = "nalssilog-dev://auth/callback?code=one-time-code&state=app-state";

        request.addParameter(
                "state",
                MobileOAuthRequestAttributes.mobileState(TRANSACTION_ID));

        when(authentication.getPrincipal()).thenReturn(principal);
        when(mobileOAuthService.complete(TRANSACTION_ID, principal))
                .thenReturn(callback);

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getRedirectedUrl()).isEqualTo(callback);
        verifyNoInteractions(cookieManager, deviceInfoResolver, webOAuthService);
    }
}
