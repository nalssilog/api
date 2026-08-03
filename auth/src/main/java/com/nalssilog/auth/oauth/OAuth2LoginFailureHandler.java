package com.nalssilog.auth.oauth;

import static com.nalssilog.auth.oauth.CustomOAuth2UserService.EMAIL_REQUIRED_ERROR;

import com.nalssilog.auth.mobile.oauth.MobileOAuthRequestAttributes;
import com.nalssilog.auth.mobile.oauth.MobileOAuthService;
import com.nalssilog.auth.ticket.AuthChannel;
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
            redirectMobileFailure(request, response, code);

            return;
        }

        response.sendRedirect(frontendBaseUrl + "/auth/callback?result=FAILED&code=" + code);
    }

    private void redirectMobileFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            String code
    ) throws IOException {
        var mobileTransaction = MobileOAuthRequestAttributes.transactionId(request);

        if (mobileTransaction.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);

            return;
        }

        var callback = mobileOAuthService.completeFailure(
                mobileTransaction.get(),
                code);

        if (callback.isPresent()) {
            response.sendRedirect(callback.get());

            return;
        }

        response.sendError(HttpServletResponse.SC_GONE);
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
}
