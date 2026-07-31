package com.nalssilog.auth.core;

import com.nalssilog.auth.config.AuthProperties;
import com.nalssilog.auth.device.DeviceInfo;
import com.nalssilog.auth.member.MemberClient;
import com.nalssilog.auth.oauth.OAuthUserInfo;
import com.nalssilog.auth.ticket.AuthChannel;
import com.nalssilog.auth.ticket.AuthTicketStore.SignupClaim;
import com.nalssilog.auth.ticket.AuthTicketStore.SignupClaimStatus;
import com.nalssilog.auth.ticket.AuthTicketStore.SignupCompletion;
import com.nalssilog.auth.ticket.AuthTicketStore;
import com.nalssilog.auth.ticket.LinkTicket;
import com.nalssilog.auth.ticket.SignupTicket;
import com.nalssilog.auth.token.AuthSessionService;
import com.nalssilog.auth.token.AuthTokenService;
import com.nalssilog.auth.token.SessionView;
import com.nalssilog.auth.token.TokenPair;
import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.member.application.dto.MemberInfo;
import com.nalssilog.member.application.dto.TermsAgreement;
import com.nalssilog.member.domain.Provider;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * 인증 API 유스케이스 조정자.
 * 컨트롤러는 쿠키와 리다이렉트 같은 HTTP 입출력만 처리하고, 인증 상태·티켓·세션 흐름은 여기서 결정한다.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberClient memberClient;
    private final AuthTokenService authTokenService;
    private final AuthSessionService authSessionService;
    private final AuthTicketStore ticketStore;
    private final AuthProperties properties;

    public MeState me(
            Long memberId,
            Optional<String> signupTicketId,
            Optional<String> linkTicketId,
            boolean hasAuthenticationCookie
    ) {
        if (memberId != null) {
            return MeState.authenticated(memberClient.getMemberInfo(memberId));
        }

        Optional<SignupTicket> signup = signupTicketId.flatMap(ticketStore::findSignup);

        if (signup.isPresent()) {
            SignupTicket ticket = signup.get();

            return MeState.signupRequired(ticket.provider(), ticket.email());
        }

        Optional<LinkTicket> link = linkTicketId.flatMap(ticketStore::findLink);

        if (link.isPresent()) {
            LinkTicket ticket = link.get();

            return MeState.linkRequired(ticket.provider(), ticket.email(), ticket.existingProviders());
        }

        if (hasAuthenticationCookie) {
            throw new NalssiLogException(AuthErrorCode.AUTH_ACCESS_TOKEN_EXPIRED);
        }

        return MeState.none();
    }

    public SignupResult signup(String ticketId, List<TermsAgreement> agreedTerms, DeviceInfo device) {
        return signup(ticketId, agreedTerms, device, AuthChannel.WEB);
    }

    public SignupResult signupMobile(
            String ticketId,
            List<TermsAgreement> agreedTerms,
            DeviceInfo device
    ) {
        return signup(ticketId, agreedTerms, device, AuthChannel.MOBILE);
    }

    private SignupResult signup(
            String ticketId,
            List<TermsAgreement> agreedTerms,
            DeviceInfo device,
            AuthChannel channel
    ) {
        if (ticketId == null || ticketId.isBlank()) {
            throw new NalssiLogException(AuthErrorCode.TICKET_NOT_FOUND);
        }

        String claimId = UUID.randomUUID().toString();
        SignupClaim claim = ticketStore.claimSignup(
                ticketId,
                claimId,
                properties.ticket().ttl());

        if (claim.status() == SignupClaimStatus.COMPLETED) {
            return replayCompletedSignup(claim.completion());
        }

        if (claim.status() == SignupClaimStatus.IN_PROGRESS) {
            throw new NalssiLogException(AuthErrorCode.AUTH_FLOW_IN_PROGRESS);
        }

        if (claim.status() == SignupClaimStatus.MISSING) {
            throw new NalssiLogException(AuthErrorCode.TICKET_NOT_FOUND);
        }

        SignupTicket ticket = claim.ticket();

        if (ticket.effectiveChannel() != channel) {
            ticketStore.releaseSignupClaim(ticketId, claimId);
            throw new NalssiLogException(AuthErrorCode.AUTH_TICKET_CHANNEL_MISMATCH);
        }

        try {
            MemberInfo member = registerOrRecover(ticket, agreedTerms);
            TokenPair tokens = authTokenService.issue(
                    member.id(), member.status(), ticket.provider(), device);

            ticketStore.completeSignup(
                    ticketId,
                    claimId,
                    new SignupCompletion(
                            member.id(),
                            ticket.provider(),
                            tokens.accessToken(),
                            tokens.refreshToken(),
                            tokens.refreshTokenMaxAge().toMillis(),
                            java.time.Instant.now().toEpochMilli()),
                    properties.refresh().retryGrace());

            return new SignupResult(member, tokens);
        } catch (RuntimeException exception) {
            ticketStore.releaseSignupClaim(ticketId, claimId);
            throw exception;
        }
    }

    public TokenPair refresh(String refreshToken, DeviceInfo device) {
        return refresh(refreshToken, device, true);
    }

    public TokenPair refreshMobile(String refreshToken, DeviceInfo device) {
        return refresh(refreshToken, device, false);
    }

    private TokenPair refresh(
            String refreshToken,
            DeviceInfo device,
            boolean clearWebCookiesOnFailure
    ) {
        try {
            if (refreshToken == null || refreshToken.isBlank()) {
                throw new NalssiLogException(AuthErrorCode.AUTH_SESSION_EXPIRED);
            }

            return authTokenService.refresh(refreshToken, device);
        } catch (NalssiLogException exception) {
            if (clearWebCookiesOnFailure
                    && isTerminalRefreshError(exception)) {
                throw new RefreshRejectedException(exception);
            }

            throw exception;
        }
    }

    public void logout(Optional<String> refreshToken) {
        refreshToken.ifPresent(authTokenService::revoke);
    }

    public void withdraw(Long memberId) {
        memberClient.withdraw(memberId);
        authTokenService.revokeAllSessions(memberId);
    }

    public List<SessionView> sessions(Long memberId, String currentSessionId) {
        return authSessionService.listSessions(memberId, currentSessionId);
    }

    public boolean revokeSession(
            Long memberId,
            String sessionId,
            String currentSessionId
    ) {
        return authSessionService.revokeSession(memberId, sessionId, currentSessionId);
    }

    public SocialLinkStart startSocialLink(Long memberId, String provider) {
        Provider target = provider(provider);
        MemberInfo member = memberClient.getMemberInfo(memberId);

        if (member.connectedProviders().contains(target)) {
            throw new NalssiLogException(AuthErrorCode.ALREADY_LINKED_PROVIDER);
        }

        String intentId = UUID.randomUUID().toString();

        ticketStore.saveLinkIntent(intentId, memberId, properties.ticket().ttl());

        return new SocialLinkStart(intentId, loginUrl(target));
    }

    public String consentLink(String ticketId) {
        LinkTicket ticket = findLinkTicket(ticketId);

        ticketStore.markLinkConsented(ticketId, properties.ticket().ttl());

        MemberInfo target = memberClient.getMemberInfo(ticket.targetMemberId());
        Provider reauthenticationProvider = target.lastLoginProvider() != null
                ? target.lastLoginProvider()
                : target.connectedProviders().getFirst();

        return reauthenticationUrl(reauthenticationProvider);
    }

    public void cancelLink(Optional<String> ticketId) {
        ticketId.ifPresent(id -> {
            ticketStore.deleteLink(id);
            ticketStore.deleteLinkConsent(id);
        });
    }

    public String oauthAuthorizationUrl(String provider) {
        return "/oauth2/authorization/" + provider(provider).name().toLowerCase(Locale.ROOT);
    }

    private SignupTicket findSignupTicket(String ticketId) {
        if (ticketId == null || ticketId.isBlank()) {
            throw new NalssiLogException(AuthErrorCode.TICKET_NOT_FOUND);
        }

        return ticketStore.findSignup(ticketId)
                .orElseThrow(() -> new NalssiLogException(AuthErrorCode.TICKET_NOT_FOUND));
    }

    private MemberInfo registerOrRecover(
            SignupTicket ticket,
            List<TermsAgreement> agreedTerms
    ) {
        try {
            return memberClient.registerMember(
                    new OAuthUserInfo(
                            ticket.provider(),
                            ticket.providerUserId(),
                            ticket.email(),
                            ticket.socialName()),
                    agreedTerms);
        } catch (DataIntegrityViolationException exception) {
            return memberClient.findMemberInfo(
                            ticket.provider(),
                            ticket.providerUserId())
                    .orElseThrow(() -> exception);
        }
    }

    private SignupResult replayCompletedSignup(SignupCompletion completion) {
        MemberInfo member = memberClient.getMemberInfo(completion.memberId());
        long elapsedMillis = Math.max(
                0,
                java.time.Instant.now().toEpochMilli()
                        - completion.completedAtEpochMillis());
        long remainingMillis =
                completion.refreshTokenMaxAgeMillis() - elapsedMillis;

        if (remainingMillis < 1_000) {
            throw new NalssiLogException(AuthErrorCode.TICKET_NOT_FOUND);
        }

        TokenPair tokens = new TokenPair(
                completion.accessToken(),
                completion.refreshToken(),
                java.time.Duration.ofMillis(remainingMillis));

        return new SignupResult(member, tokens);
    }

    private LinkTicket findLinkTicket(String ticketId) {
        if (ticketId == null || ticketId.isBlank()) {
            throw new NalssiLogException(AuthErrorCode.TICKET_NOT_FOUND);
        }

        return ticketStore.findLink(ticketId)
                .orElseThrow(() -> new NalssiLogException(AuthErrorCode.TICKET_NOT_FOUND));
    }

    private boolean isTerminalRefreshError(NalssiLogException exception) {
        return exception.getErrorCode() == AuthErrorCode.AUTH_SESSION_EXPIRED
                || exception.getErrorCode() == AuthErrorCode.AUTH_REFRESH_REUSED;
    }

    private Provider provider(String provider) {
        try {
            return Provider.from(provider);
        } catch (IllegalArgumentException _) {
            throw new NalssiLogException(AuthErrorCode.UNSUPPORTED_PROVIDER);
        }
    }

    private String loginUrl(Provider provider) {
        return "/api/auth/login/" + provider.name().toLowerCase(Locale.ROOT);
    }

    private String reauthenticationUrl(Provider provider) {
        return "/api/auth/link/reauth/" + provider.name().toLowerCase(Locale.ROOT);
    }

    public enum MeStatus {
        AUTHENTICATED,
        SIGNUP_REQUIRED,
        LINK_REQUIRED,
        NONE
    }

    public record MeState(
            MeStatus status,
            MemberInfo member,
            Provider provider,
            String email,
            List<Provider> existingProviders
    ) {

        public static MeState authenticated(MemberInfo member) {
            return new MeState(MeStatus.AUTHENTICATED, member, null, null, List.of());
        }

        public static MeState signupRequired(Provider provider, String email) {
            return new MeState(MeStatus.SIGNUP_REQUIRED, null, provider, email, List.of());
        }

        public static MeState linkRequired(
                Provider provider,
                String email,
                List<Provider> existingProviders
        ) {
            return new MeState(MeStatus.LINK_REQUIRED, null, provider, email, List.copyOf(existingProviders));
        }

        public static MeState none() {
            return new MeState(MeStatus.NONE, null, null, null, List.of());
        }
    }

    public record SignupResult(MemberInfo member, TokenPair tokens) {
    }

    public record SocialLinkStart(String intentId, String authorizationUrl) {
    }
}
