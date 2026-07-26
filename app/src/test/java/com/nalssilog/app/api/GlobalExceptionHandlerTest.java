package com.nalssilog.app.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.nalssilog.auth.config.AuthCookieManager;
import com.nalssilog.auth.config.AuthProperties;
import com.nalssilog.auth.domain.AuthErrorCode;
import com.nalssilog.auth.domain.RefreshRejectedException;
import com.nalssilog.common.exception.ErrorResponse;
import com.nalssilog.common.exception.NalssiLogException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;

@SuppressWarnings("java:S5960")
class GlobalExceptionHandlerTest {

    private final AuthCookieManager cookieManager = new AuthCookieManager(properties());
    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler(cookieManager);

    @Test
    void refreshRejectionDeletesBothAuthenticationCookies() {
        RefreshRejectedException exception = new RefreshRejectedException(
                new NalssiLogException(AuthErrorCode.AUTH_SESSION_EXPIRED));
        MockHttpServletResponse response = new MockHttpServletResponse();

        ErrorResponse errorResponse =
                exceptionHandler.handleNalssiLogException(exception, response);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(errorResponse).isEqualTo(new ErrorResponse(
                        AuthErrorCode.AUTH_SESSION_EXPIRED.getCode(),
                        AuthErrorCode.AUTH_SESSION_EXPIRED.getMessage()));
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
