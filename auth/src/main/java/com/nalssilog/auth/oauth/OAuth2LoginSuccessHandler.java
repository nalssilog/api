package com.nalssilog.auth.oauth;

import com.nalssilog.auth.device.DeviceInfoResolver;
import com.nalssilog.auth.mobile.oauth.MobileOAuthRequestAttributes;
import com.nalssilog.auth.mobile.oauth.MobileOAuthService;
import com.nalssilog.auth.oauth.WebOAuthService.Completion;
import com.nalssilog.auth.ticket.AuthChannel;
import com.nalssilog.auth.web.AuthCookieManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthCookieManager cookieManager;
    private final DeviceInfoResolver deviceInfoResolver;
    private final MobileOAuthService mobileOAuthService;
    private final WebOAuthService webOAuthService;

    @Value("${nalssilog.frontend.base-url}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        SocialPrincipal principal = Objects.requireNonNull(
                (SocialPrincipal) authentication.getPrincipal(),
                "principal must not be null");

        if (MobileOAuthRequestAttributes.channel(request)
                .filter(channel -> channel == AuthChannel.MOBILE)
                .isPresent()) {
            Optional<String> mobileTransaction =
                    MobileOAuthRequestAttributes.transactionId(request);

            if (mobileTransaction.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

                return;
            }

            response.sendRedirect(mobileOAuthService.complete(
                    mobileTransaction.get(),
                    principal));

            return;
        }

        Completion completion = webOAuthService.complete(
                principal,
                cookieManager.readLinkIntent(request),
                cookieManager.readLinkTicket(request),
                deviceInfoResolver.resolve(request));

        applyCookies(response, completion);
        response.sendRedirect(callbackUrl(
                completion.result(),
                completion.errorCode()));
    }

    private void applyCookies(
            HttpServletResponse response,
            Completion completion
    ) {
        if (completion.clearLinkIntent()) {
            cookieManager.clearLinkIntentCookie(response);
        }

        if (completion.clearLinkTicket()) {
            cookieManager.clearLinkTicketCookie(response);
        }

        if (completion.signupTicket() != null) {
            cookieManager.addSignupTicketCookie(
                    response,
                    completion.signupTicket());
        }

        if (completion.linkTicket() != null) {
            cookieManager.addLinkTicketCookie(
                    response,
                    completion.linkTicket());
        }

        if (completion.tokens() != null) {
            cookieManager.addAuthCookies(
                    response,
                    completion.tokens().accessToken(),
                    completion.tokens().refreshToken(),
                    completion.tokens().refreshTokenMaxAge());
        }
    }

    private String callbackUrl(String result, String errorCode) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(frontendBaseUrl)
                .path("/auth/callback")
                .queryParam("result", result);

        if (errorCode != null && !errorCode.isBlank()) {
            builder.queryParam("code", errorCode);
        }

        return builder.build().encode().toUriString();
    }
}
