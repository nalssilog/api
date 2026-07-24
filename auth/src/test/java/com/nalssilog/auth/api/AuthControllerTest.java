package com.nalssilog.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nalssilog.auth.api.dto.AuthResult;
import com.nalssilog.auth.api.dto.MeResponse;
import com.nalssilog.auth.application.AuthSessionService;
import com.nalssilog.auth.application.AuthTokenService;
import com.nalssilog.auth.application.TokenPair;
import com.nalssilog.auth.client.MemberClient;
import com.nalssilog.auth.config.AuthCookieManager;
import com.nalssilog.auth.config.AuthProperties;
import com.nalssilog.auth.config.DeviceInfoResolver;
import com.nalssilog.auth.domain.AuthErrorCode;
import com.nalssilog.auth.repository.AuthTicketStore;
import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.member.application.dto.MemberInfo;
import com.nalssilog.member.domain.AvatarType;
import com.nalssilog.member.domain.MemberStatus;
import com.nalssilog.member.domain.Provider;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@SuppressWarnings("java:S5960")
class AuthControllerTest {

    private final MemberClient memberClient = mock(MemberClient.class);
    private final AuthTokenService tokenService = mock(AuthTokenService.class);
    private final AuthSessionService sessionService = mock(AuthSessionService.class);
    private final AuthTicketStore ticketStore = mock(AuthTicketStore.class);
    private final DeviceInfoResolver deviceInfoResolver = mock(DeviceInfoResolver.class);
    private final AuthProperties properties = properties();
    private final AuthCookieManager cookieManager = new AuthCookieManager(properties);

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(
                memberClient,
                tokenService,
                sessionService,
                cookieManager,
                ticketStore,
                properties,
                deviceInfoResolver);
    }

    @Test
    void meReturnsNoneOnlyWhenNoAuthenticationCookieExists() {
        MeResponse response = controller.me(null, new MockHttpServletRequest());

        assertThat(response.authenticated()).isFalse();
        assertThat(response.result()).isEqualTo(AuthResult.NONE);
    }

    @Test
    void meReturnsSuccessBeforeInspectingCookiesWhenAccessTokenIsValid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
                new Cookie(AuthCookieManager.ACCESS_TOKEN_COOKIE, "valid-access"),
                new Cookie(AuthCookieManager.REFRESH_TOKEN_COOKIE, "valid-refresh"));
        when(memberClient.getMemberInfo(1L)).thenReturn(member());

        MeResponse response = controller.me(1L, request);

        assertThat(response.authenticated()).isTrue();
        assertThat(response.result()).isEqualTo(AuthResult.SUCCESS);
        assertThat(response.user().id()).isEqualTo("1");
    }

    @Test
    void meRequestsRefreshWhenAccessCookieCouldNotBeAuthenticated() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(AuthCookieManager.ACCESS_TOKEN_COOKIE, "expired-access"));

        NalssiLogException exception = catchThrowableOfType(
                NalssiLogException.class,
                () -> controller.me(null, request));

        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.AUTH_ACCESS_TOKEN_EXPIRED);
    }

    @Test
    void meRequestsRefreshWhenOnlyRefreshCookieRemains() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(AuthCookieManager.REFRESH_TOKEN_COOKIE, "refresh-token"));

        NalssiLogException exception = catchThrowableOfType(
                NalssiLogException.class,
                () -> controller.me(null, request));

        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.AUTH_ACCESS_TOKEN_EXPIRED);
    }

    @Test
    void terminalRefreshErrorDeletesBothAuthenticationCookies() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(AuthCookieManager.REFRESH_TOKEN_COOKIE, "expired-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(deviceInfoResolver.resolve(request)).thenReturn(null);
        when(tokenService.refresh("expired-token", null))
                .thenThrow(new NalssiLogException(AuthErrorCode.AUTH_SESSION_EXPIRED));

        NalssiLogException exception = catchThrowableOfType(
                NalssiLogException.class,
                () -> controller.refresh(request, response));

        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.AUTH_SESSION_EXPIRED);
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE))
                .anySatisfy(header -> assertThat(header)
                        .contains("access_token=")
                        .contains("Path=/")
                        .contains("Max-Age=0")
                        .contains("Expires=Thu, 1 Jan 1970 00:00:00 GMT")
                        .contains("Secure")
                        .contains("HttpOnly")
                        .contains("SameSite=Lax")
                        .doesNotContain("Domain="))
                .anySatisfy(header -> assertThat(header)
                        .contains("refresh_token=")
                        .contains("Path=/")
                        .contains("Max-Age=0")
                        .contains("Expires=Thu, 1 Jan 1970 00:00:00 GMT")
                        .contains("Secure")
                        .contains("HttpOnly")
                        .contains("SameSite=Lax")
                        .doesNotContain("Domain="));
    }

    @Test
    void refreshUsesServerRemainingTtlForHardenedHostOnlyCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(AuthCookieManager.REFRESH_TOKEN_COOKIE, "current-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(deviceInfoResolver.resolve(request)).thenReturn(null);
        when(tokenService.refresh("current-token", null))
                .thenReturn(new TokenPair("new-access", "new-refresh", Duration.ofDays(13)));

        controller.refresh(request, response);

        String refreshCookie = response.getHeaders(HttpHeaders.SET_COOKIE).stream()
                .filter(header -> header.startsWith("refresh_token="))
                .findFirst()
                .orElseThrow();
        assertThat(refreshCookie)
                .contains("Max-Age=1123200")
                .contains("Path=/")
                .contains("Expires=")
                .contains("Secure")
                .contains("HttpOnly")
                .contains("SameSite=Lax")
                .doesNotContain("Domain=");
    }

    private MemberInfo member() {
        return new MemberInfo(
                1L,
                "닉네임",
                "이름",
                "user@example.com",
                AvatarType.PRESET,
                "1",
                MemberStatus.ACTIVE,
                Provider.KAKAO,
                List.of(Provider.KAKAO));
    }

    private AuthProperties properties() {
        return new AuthProperties(
                new AuthProperties.Jwt(
                        "test-secret-must-be-at-least-thirty-two-bytes",
                        Duration.ofMinutes(30),
                        Duration.ofDays(14)),
                new AuthProperties.Cookie(true),
                new AuthProperties.Ticket(Duration.ofMinutes(10)),
                new AuthProperties.Csrf("XSRF-TOKEN", null),
                new AuthProperties.Refresh(Duration.ofSeconds(5)));
    }
}
