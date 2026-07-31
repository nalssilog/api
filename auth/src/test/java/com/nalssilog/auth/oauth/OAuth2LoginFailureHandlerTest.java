package com.nalssilog.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.nalssilog.auth.mobile.oauth.MobileOAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.test.util.ReflectionTestUtils;

@SuppressWarnings("java:S5960") // 표준 src/test 소스의 AssertJ 검증을 운영 코드 assertion으로 오인하는 경고.
class OAuth2LoginFailureHandlerTest {

    @Test
    void missingEmailRedirectsWithDedicatedFailureCode() throws Exception {
        OAuth2LoginFailureHandler handler = new OAuth2LoginFailureHandler(
                mock(MobileOAuthService.class));

        ReflectionTestUtils.setField(handler, "frontendBaseUrl", "https://dev.nalssilog.com");

        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(
                new MockHttpServletRequest(),
                response,
                new OAuth2AuthenticationException(
                        new OAuth2Error(CustomOAuth2UserService.EMAIL_REQUIRED_ERROR),
                        "Social account email is required")
        );

        assertThat(response.getRedirectedUrl()).isEqualTo(
                "https://dev.nalssilog.com/auth/callback?result=FAILED&code=OAUTH_EMAIL_REQUIRED");
    }
}
