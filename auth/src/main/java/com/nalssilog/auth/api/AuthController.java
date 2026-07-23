package com.nalssilog.auth.api;

import com.nalssilog.auth.api.dto.LinkConsentResponse;
import com.nalssilog.auth.api.dto.MeResponse;
import com.nalssilog.auth.api.dto.SessionResponse;
import com.nalssilog.auth.api.dto.SignupRequest;
import com.nalssilog.auth.application.AuthSessionService;
import com.nalssilog.auth.application.AuthTokenService;
import com.nalssilog.auth.application.TokenPair;
import com.nalssilog.auth.client.MemberClient;
import com.nalssilog.auth.client.OAuthUserInfo;
import com.nalssilog.auth.config.AuthCookieManager;
import com.nalssilog.auth.config.AuthProperties;
import com.nalssilog.auth.config.DeviceInfoResolver;
import com.nalssilog.auth.domain.AuthErrorCode;
import com.nalssilog.auth.domain.LinkTicket;
import com.nalssilog.auth.domain.SignupTicket;
import com.nalssilog.auth.repository.AuthTicketStore;
import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.member.application.dto.MemberInfo;
import com.nalssilog.member.domain.Provider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberClient memberClient;
    private final AuthTokenService authTokenService;
    private final AuthSessionService authSessionService;
    private final AuthCookieManager cookieManager;
    private final AuthTicketStore ticketStore;
    private final AuthProperties properties;
    private final DeviceInfoResolver deviceInfoResolver;

    /** 소셜 로그인 진입. 내부 Spring OAuth 경로를 은닉하고 302 시킨다. */
    @GetMapping("/login/{provider}")
    public void login(@PathVariable String provider, HttpServletResponse response) throws IOException {
        redirectToOAuth(response, provider);
    }

    /** 인증 상태 조회(stateless — AT/티켓 쿠키로 판단). */
    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Long memberId, HttpServletRequest request) {
        if (memberId != null) {
            return MeResponse.authenticated(memberClient.getMemberInfo(memberId));
        }

        Optional<SignupTicket> signup = cookieManager.readSignupTicket(request).flatMap(ticketStore::findSignup);
        if (signup.isPresent()) {
            return MeResponse.signupRequired(signup.get().provider(), signup.get().email());
        }

        Optional<LinkTicket> link = cookieManager.readLinkTicket(request).flatMap(ticketStore::findLink);
        if (link.isPresent()) {
            return MeResponse.linkRequired(link.get().provider(), link.get().email(), link.get().existingProviders());
        }

        return MeResponse.none();
    }

    /** 회원가입 확정. OAuth 정보는 signup 티켓에서 읽고, Member 는 여기서 처음 생성된다. */
    @PostMapping("/signup")
    public MeResponse signup(@Valid @RequestBody SignupRequest request,
                             HttpServletRequest httpRequest, HttpServletResponse response) {
        String ticketId = cookieManager.readSignupTicket(httpRequest)
                .orElseThrow(() -> new NalssiLogException(AuthErrorCode.TICKET_NOT_FOUND));
        SignupTicket ticket = ticketStore.findSignup(ticketId)
                .orElseThrow(() -> new NalssiLogException(AuthErrorCode.TICKET_NOT_FOUND));

        MemberInfo member = memberClient.registerMember(
                new OAuthUserInfo(ticket.provider(), ticket.providerUserId(), ticket.email(), ticket.socialName()),
                request.agreedTerms());
        TokenPair tokens = authTokenService.issue(
                member.id(), member.status(), ticket.provider(), deviceInfoResolver.resolve(httpRequest));
        cookieManager.addAuthCookies(response, tokens.accessToken(), tokens.refreshToken());
        ticketStore.deleteSignup(ticketId);
        cookieManager.clearSignupTicketCookie(response);

        return MeResponse.authenticated(member);
    }

    @PostMapping("/refresh")
    public void refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieManager.readRefreshToken(request)
                .orElseThrow(() -> new NalssiLogException(AuthErrorCode.AUTH_SESSION_EXPIRED));
        TokenPair tokens = authTokenService.refresh(refreshToken, deviceInfoResolver.resolve(request));

        cookieManager.addAuthCookies(response, tokens.accessToken(), tokens.refreshToken());
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        cookieManager.readRefreshToken(request).ifPresent(authTokenService::revoke);
        cookieManager.clearAuthCookies(response);
    }

    /** 회원 탈퇴: 익명화(member) + 제보 익명화(event) + 전 기기 세션 만료 + 쿠키 정리. 세션·쿠키 때문에 auth 소유. */
    @DeleteMapping("/withdraw")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@AuthenticationPrincipal Long memberId, HttpServletResponse response) {
        memberClient.withdraw(memberId);
        authTokenService.revokeAllSessions(memberId);
        cookieManager.clearAuthCookies(response);
    }

    /** 로그인된 기기 목록(current=이 기기). */
    @GetMapping("/sessions")
    public List<SessionResponse> sessions(@AuthenticationPrincipal Long memberId, HttpServletRequest request) {
        String currentHash = cookieManager.readRefreshToken(request)
                .map(authTokenService::tokenHash)
                .orElse(null);

        return authSessionService.listSessions(memberId, currentHash).stream()
                .map(SessionResponse::from)
                .toList();
    }

    /** 특정 기기 로그아웃. 대상이 현재 기기면 인증 쿠키까지 정리한다. */
    @DeleteMapping("/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeSession(@AuthenticationPrincipal Long memberId, @PathVariable String sessionId,
                              HttpServletRequest request, HttpServletResponse response) {
        String currentHash = cookieManager.readRefreshToken(request)
                .map(authTokenService::tokenHash)
                .orElse(null);
        boolean revokedCurrent = authSessionService.revokeSession(memberId, sessionId, currentHash);

        if (revokedCurrent) {
            cookieManager.clearAuthCookies(response);
        }
    }

    /** 설정에서 소셜 추가 연동 시작. intent 세팅 후 OAuth 진입 URL 반환(실제 연동은 성공 핸들러). */
    @PostMapping("/link/social/{provider}")
    public LinkConsentResponse startSocialLink(@AuthenticationPrincipal Long memberId, @PathVariable String provider,
                                               HttpServletResponse response) {
        validateProvider(provider);
        Provider target = Provider.from(provider);

        MemberInfo member = memberClient.getMemberInfo(memberId);
        if (member.connectedProviders().contains(target)) {
            throw new NalssiLogException(AuthErrorCode.ALREADY_LINKED_PROVIDER);
        }

        String intentId = UUID.randomUUID().toString();
        ticketStore.saveLinkIntent(intentId, memberId, properties.ticket().ttl());
        cookieManager.addLinkIntentCookie(response, intentId);

        return new LinkConsentResponse("/api/auth/login/" + provider.toLowerCase(Locale.ROOT));
    }

    /** 로그인-시점 연동 동의 + 기존 수단 재인증 URL 반환. */
    @PostMapping("/link/consent")
    public LinkConsentResponse consentLink(HttpServletRequest request) {
        String ticketId = cookieManager.readLinkTicket(request)
                .orElseThrow(() -> new NalssiLogException(AuthErrorCode.TICKET_NOT_FOUND));
        LinkTicket ticket = ticketStore.findLink(ticketId)
                .orElseThrow(() -> new NalssiLogException(AuthErrorCode.TICKET_NOT_FOUND));

        ticketStore.markLinkConsented(ticketId, properties.ticket().ttl());

        MemberInfo target = memberClient.getMemberInfo(ticket.targetMemberId());
        Provider reauthProvider = target.lastLoginProvider() != null
                ? target.lastLoginProvider()
                : target.connectedProviders().get(0);

        return new LinkConsentResponse("/api/auth/link/reauth/" + reauthProvider.name().toLowerCase(Locale.ROOT));
    }

    @GetMapping("/link/reauth/{provider}")
    public void linkReauth(@PathVariable String provider, HttpServletResponse response) throws IOException {
        redirectToOAuth(response, provider);
    }

    @PostMapping("/link/cancel")
    public void cancelLink(HttpServletRequest request, HttpServletResponse response) {
        cookieManager.readLinkTicket(request).ifPresent(ticketId -> {
            ticketStore.deleteLink(ticketId);
            ticketStore.deleteLinkConsent(ticketId);
        });
        cookieManager.clearLinkTicketCookie(response);
    }

    private void redirectToOAuth(HttpServletResponse response, String provider) throws IOException {
        validateProvider(provider);

        response.sendRedirect("/oauth2/authorization/" + provider.toLowerCase(Locale.ROOT));
    }

    private void validateProvider(String provider) {
        try {
            Provider.from(provider);
        } catch (IllegalArgumentException _) {
            throw new NalssiLogException(AuthErrorCode.UNSUPPORTED_PROVIDER);
        }
    }
}
