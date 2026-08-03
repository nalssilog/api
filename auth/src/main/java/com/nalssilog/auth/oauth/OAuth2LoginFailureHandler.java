package com.nalssilog.auth.oauth;

import static com.nalssilog.auth.oauth.CustomOAuth2UserService.EMAIL_REQUIRED_ERROR;

import com.nalssilog.auth.mobile.oauth.MobileOAuthRequestAttributes;
import com.nalssilog.auth.mobile.oauth.MobileOAuthService;
import com.nalssilog.auth.ticket.AuthChannel;
import com.nalssilog.auth.web.AuthCookieManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private final MobileOAuthService mobileOAuthService;
    private final WebOAuthService webOAuthService;
    private final AuthCookieManager cookieManager;

    @Value("${nalssilog.frontend.base-url}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        String code = resolveCode(exception);

        log.warn("OAuth2 login failed code={} type={}",
                code, exception.getClass().getSimpleName());

        if (MobileOAuthRequestAttributes.channel(request)
                .filter(channel -> channel == AuthChannel.MOBILE)
                .isPresent()) {
            redirectMobileFailure(
                    request,
                    response,
                    resolveMobileFailure(exception));

            return;
        }

        if ("OAUTH_CANCELLED".equals(code)) {
            cancelWebPendingAuthentication(request, response);
        }

        response.sendRedirect(frontendBaseUrl + "/auth/callback?result=FAILED&code=" + code);
    }

    private void cancelWebPendingAuthentication(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        webOAuthService.cancelPendingAuthentication(
                cookieManager.readSignupTicket(request),
                cookieManager.readLinkTicket(request),
                cookieManager.readLinkIntent(request));

        cookieManager.clearSignupTicketCookie(response);
        cookieManager.clearLinkTicketCookie(response);
        cookieManager.clearLinkIntentCookie(response);
    }

    private void redirectMobileFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            MobileFailure failure
    ) throws IOException {
        var mobileTransaction = MobileOAuthRequestAttributes.transactionId(request);

        if (mobileTransaction.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

            return;
        }

        var callback = mobileOAuthService.completeFailure(
                mobileTransaction.get(),
                failure.error(),
                failure.description());

        if (callback.isPresent()) {
            response.sendRedirect(callback.get());

            return;
        }

        response.setStatus(HttpServletResponse.SC_GONE);
    }

    private MobileFailure resolveMobileFailure(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException oauthException) {
            String error = oauthException.getError().getErrorCode();

            return new MobileFailure(
                    hasText(error) ? error : "oauth_failed",
                    safeDescription(
                            oauthException.getError().getDescription(),
                            defaultDescription(error)));
        }

        return new MobileFailure(
                "oauth_failed",
                "OAuth authentication failed");
    }

    private String defaultDescription(String error) {
        return switch (error == null ? "" : error) {
            case "access_denied" -> "OAuth authorization was cancelled";
            case EMAIL_REQUIRED_ERROR -> "Social account email is required";
            default -> "OAuth authentication failed";
        };
    }

    private String safeDescription(String description, String fallback) {
        String value = hasText(description) ? description : fallback;
        String sanitized = value.replace('\r', ' ').replace('\n', ' ').trim();

        return sanitized.length() <= 500
                ? sanitized
                : sanitized.substring(0, 500);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String resolveCode(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException oauthException) {
            return switch (oauthException.getError().getErrorCode()) {
                case "access_denied" -> "OAUTH_CANCELLED";
                case EMAIL_REQUIRED_ERROR -> "OAUTH_EMAIL_REQUIRED";
                default -> "OAUTH_FAILED";
            };
        }

        return "OAUTH_FAILED";
    }

    private record MobileFailure(String error, String description) {
    }
}
