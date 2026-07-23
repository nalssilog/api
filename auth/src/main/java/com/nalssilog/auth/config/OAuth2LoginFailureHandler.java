package com.nalssilog.auth.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/**
 * 소셜 인증 실패 시에도 성공과 동일한 단일 콜백으로 보낸다. 사용자가 provider 에서 취소하면 OAUTH_CANCELLED.
 */
@Slf4j
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    @Value("${nalssilog.frontend.base-url}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String code = resolveCode(exception);
        log.warn("OAuth2 login failed ({}): {}", code, exception.getMessage());

        response.sendRedirect(frontendBaseUrl + "/auth/callback?result=FAILED&code=" + code);
    }

    private String resolveCode(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException oauthException
                && "access_denied".equals(oauthException.getError().getErrorCode())) {
            return "OAUTH_CANCELLED";
        }

        return "OAUTH_FAILED";
    }
}
