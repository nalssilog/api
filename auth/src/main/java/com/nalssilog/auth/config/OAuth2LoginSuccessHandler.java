package com.nalssilog.auth.config;

import com.nalssilog.auth.application.AuthTokenService;
import com.nalssilog.auth.application.SocialAuthPrincipal;
import com.nalssilog.auth.application.TokenPair;
import com.nalssilog.auth.client.MemberClient;
import com.nalssilog.auth.client.OAuthUserInfo;
import com.nalssilog.auth.domain.LinkTicket;
import com.nalssilog.auth.domain.SignupTicket;
import com.nalssilog.auth.repository.AuthTicketStore;
import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.member.application.dto.MemberInfo;
import com.nalssilog.member.domain.MemberStatus;
import com.nalssilog.member.domain.Provider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/** 소셜 인증 성공 후 항상 `{front}/auth/callback?result=...` 로 리다이렉트. 토큰·민감정보는 URL 에 안 넣고 HttpOnly 쿠키만. */
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final String LINK_FAILED = "LINK_FAILED";

    private final AuthTokenService authTokenService;
    private final AuthCookieManager cookieManager;
    private final AuthTicketStore ticketStore;
    private final MemberClient memberClient;
    private final AuthProperties properties;
    private final DeviceInfoResolver deviceInfoResolver;

    @Value("${nalssilog.frontend.base-url}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        SocialAuthPrincipal principal =
                Objects.requireNonNull((SocialAuthPrincipal) authentication.getPrincipal(), "principal must not be null");

        // 설정 화면에서 시작한 '소셜 추가 연동'이면 로그인 분기보다 먼저 처리한다(현재 회원에 새 소셜 연동).
        Optional<String> linkIntent = cookieManager.readLinkIntent(request);
        if (linkIntent.isPresent()) {
            handleSettingsLink(response, linkIntent.get(), principal);
            return;
        }

        switch (principal.result().outcome()) {
            case EXISTING -> handleExisting(request, response, principal);
            case NEW -> handleNew(response, principal);
            case LINK_REQUIRED -> handleLinkRequired(response, principal);
        }
    }

    // 설정에서 로그인 상태로 새 소셜 추가 연동. intent 의 memberId 에 붙이고 기존 세션 유지(토큰 재발급 없음).
    private void handleSettingsLink(HttpServletResponse response, String intentId, SocialAuthPrincipal principal)
            throws IOException {
        Optional<Long> memberId = ticketStore.findLinkIntent(intentId);

        cookieManager.clearLinkIntentCookie(response);
        ticketStore.deleteLinkIntent(intentId);

        if (memberId.isEmpty()) {
            response.sendRedirect(callbackUrl(LINK_FAILED));
            return;
        }

        try {
            memberClient.linkSocial(memberId.get(), principal.userInfo());
            response.sendRedirect(callbackUrl("LINK_SUCCESS"));
        } catch (NalssiLogException e) {
            response.sendRedirect(callbackUrl(LINK_FAILED) + "&code=" + e.getErrorCode().getCode());
        }
    }

    private void handleExisting(HttpServletRequest request, HttpServletResponse response,
                                SocialAuthPrincipal principal) throws IOException {
        Long memberId = principal.result().memberId();
        MemberStatus status = principal.result().status();

        if (status == MemberStatus.WITHDRAWN) {
            response.sendRedirect(callbackUrl("FAILED"));
            return;
        }

        Optional<String> pendingLink = cookieManager.readLinkTicket(request);

        if (pendingLink.isPresent()) {
            completeLink(
                    request, response, pendingLink.get(), memberId, status, principal.userInfo().provider());
            return;
        }

        loginAndRedirect(
                request, response, memberId, status, principal.userInfo().provider(), "SUCCESS");
    }

    // 연동 완료 시도. 명시 동의 + 대상 일치일 때만 실제 연동(방치/미동의 티켓 자동연동 방지), 아니면 일반 로그인.
    private void completeLink(HttpServletRequest request, HttpServletResponse response, String ticketId,
                              Long authenticatedMemberId, MemberStatus status, Provider authenticatedProvider)
            throws IOException {
        LinkTicket ticket = ticketStore.findLink(ticketId).orElse(null);
        boolean consented = ticketStore.isLinkConsented(ticketId);

        cookieManager.clearLinkTicketCookie(response);
        ticketStore.deleteLink(ticketId);
        ticketStore.deleteLinkConsent(ticketId);

        if (ticket == null || !consented) {
            loginAndRedirect(
                    request, response, authenticatedMemberId, status, authenticatedProvider, "SUCCESS");
            return;
        }

        if (!ticket.targetMemberId().equals(authenticatedMemberId)) {
            response.sendRedirect(callbackUrl(LINK_FAILED));
            return;
        }

        MemberInfo member = memberClient.linkSocial(
                ticket.targetMemberId(),
                new OAuthUserInfo(ticket.provider(), ticket.providerUserId(), ticket.email(), null));

        loginAndRedirect(
                request, response, member.id(), member.status(), authenticatedProvider, "LINK_SUCCESS");
    }

    private void handleNew(HttpServletResponse response, SocialAuthPrincipal principal) throws IOException {
        OAuthUserInfo userInfo = principal.userInfo();
        String ticketId = UUID.randomUUID().toString();

        ticketStore.saveSignup(ticketId,
                new SignupTicket(userInfo.provider(), userInfo.providerUserId(), userInfo.email(), userInfo.socialName()),
                properties.ticket().ttl());
        cookieManager.addSignupTicketCookie(response, ticketId);

        response.sendRedirect(callbackUrl("SIGNUP_REQUIRED"));
    }

    private void handleLinkRequired(HttpServletResponse response, SocialAuthPrincipal principal) throws IOException {
        OAuthUserInfo userInfo = principal.userInfo();
        Long targetMemberId = principal.result().memberId();
        String ticketId = UUID.randomUUID().toString();

        ticketStore.saveLink(ticketId,
                new LinkTicket(userInfo.provider(), userInfo.providerUserId(), userInfo.email(), targetMemberId,
                        principal.result().existingProviders()),
                properties.ticket().ttl());
        cookieManager.addLinkTicketCookie(response, ticketId);

        response.sendRedirect(callbackUrl("LINK_REQUIRED"));
    }

    private void loginAndRedirect(HttpServletRequest request, HttpServletResponse response, Long memberId,
                                  MemberStatus status, Provider provider, String result) throws IOException {
        TokenPair tokens = authTokenService.issue(
                memberId, status, provider, deviceInfoResolver.resolve(request));
        cookieManager.addAuthCookies(
                response, tokens.accessToken(), tokens.refreshToken(), tokens.refreshTokenMaxAge());

        response.sendRedirect(callbackUrl(result));
    }

    private String callbackUrl(String result) {
        return frontendBaseUrl + "/auth/callback?result=" + result;
    }
}
