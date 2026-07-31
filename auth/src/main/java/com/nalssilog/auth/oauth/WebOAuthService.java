package com.nalssilog.auth.oauth;

import com.nalssilog.auth.config.AuthProperties;
import com.nalssilog.auth.device.DeviceInfo;
import com.nalssilog.auth.member.MemberClient;
import com.nalssilog.auth.ticket.AuthChannel;
import com.nalssilog.auth.ticket.AuthTicketStore;
import com.nalssilog.auth.ticket.LinkTicket;
import com.nalssilog.auth.ticket.SignupTicket;
import com.nalssilog.auth.token.AuthTokenService;
import com.nalssilog.auth.token.TokenPair;
import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.member.application.dto.MemberInfo;
import com.nalssilog.member.domain.MemberStatus;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Completes browser OAuth flows without depending on servlet APIs.
 *
 * <p>The success handler is an HTTP adapter: it reads and writes cookies and performs the final
 * redirect. Ticket decisions, account linking, and token issuance belong here.
 */
@Service
@RequiredArgsConstructor
public class WebOAuthService {

    private static final String SUCCESS = "SUCCESS";
    private static final String FAILED = "FAILED";
    private static final String SIGNUP_REQUIRED = "SIGNUP_REQUIRED";
    private static final String LINK_REQUIRED = "LINK_REQUIRED";
    private static final String LINK_SUCCESS = "LINK_SUCCESS";
    private static final String LINK_FAILED = "LINK_FAILED";

    private final AuthTokenService authTokenService;
    private final AuthTicketStore ticketStore;
    private final MemberClient memberClient;
    private final AuthProperties properties;

    public Completion complete(
            SocialPrincipal principal,
            Optional<String> linkIntentId,
            Optional<String> pendingLinkTicketId,
            DeviceInfo device
    ) {
        if (linkIntentId.isPresent()) {
            return completeSettingsLink(linkIntentId.get(), principal);
        }

        return switch (principal.result().outcome()) {
            case EXISTING -> completeExisting(principal, pendingLinkTicketId, device);
            case NEW -> requireSignup(principal.userInfo());
            case LINK_REQUIRED -> requireLink(principal);
        };
    }

    private Completion completeSettingsLink(
            String intentId,
            SocialPrincipal principal
    ) {
        Optional<Long> memberId = ticketStore.findLinkIntent(intentId);

        ticketStore.deleteLinkIntent(intentId);

        if (memberId.isEmpty()) {
            return Completion.clearIntent(LINK_FAILED, null);
        }

        try {
            memberClient.linkSocial(memberId.get(), principal.userInfo());

            return Completion.clearIntent(LINK_SUCCESS, null);
        } catch (NalssiLogException exception) {
            return Completion.clearIntent(
                    LINK_FAILED,
                    exception.getErrorCode().getCode());
        }
    }

    private Completion completeExisting(
            SocialPrincipal principal,
            Optional<String> pendingLinkTicketId,
            DeviceInfo device
    ) {
        if (principal.result().status() == MemberStatus.WITHDRAWN) {
            return Completion.redirect(FAILED);
        }

        if (pendingLinkTicketId.isPresent()) {
            return completeLoginLink(
                    pendingLinkTicketId.get(),
                    principal,
                    device);
        }

        return authenticated(
                principal.result().memberId(),
                principal.result().status(),
                principal.userInfo().provider(),
                SUCCESS,
                device,
                false);
    }

    private Completion completeLoginLink(
            String ticketId,
            SocialPrincipal principal,
            DeviceInfo device
    ) {
        LinkTicket ticket = ticketStore.findLink(ticketId).orElse(null);
        boolean consented = ticketStore.isLinkConsented(ticketId);

        ticketStore.deleteLink(ticketId);
        ticketStore.deleteLinkConsent(ticketId);

        if (ticket == null || !consented) {
            return authenticated(
                    principal.result().memberId(),
                    principal.result().status(),
                    principal.userInfo().provider(),
                    SUCCESS,
                    device,
                    true);
        }

        if (!ticket.targetMemberId().equals(principal.result().memberId())) {
            return Completion.clearLinkTicket(LINK_FAILED);
        }

        MemberInfo member = memberClient.linkSocial(
                ticket.targetMemberId(),
                new OAuthUserInfo(
                        ticket.provider(),
                        ticket.providerUserId(),
                        ticket.email(),
                        null));

        return authenticated(
                member.id(),
                member.status(),
                ticket.provider(),
                LINK_SUCCESS,
                device,
                true);
    }

    private Completion requireSignup(OAuthUserInfo userInfo) {
        String ticketId = UUID.randomUUID().toString();

        ticketStore.saveSignup(
                ticketId,
                new SignupTicket(
                        userInfo.provider(),
                        userInfo.providerUserId(),
                        userInfo.email(),
                        userInfo.socialName(),
                        AuthChannel.WEB),
                properties.ticket().ttl());

        return Completion.signup(ticketId);
    }

    private Completion requireLink(SocialPrincipal principal) {
        OAuthUserInfo userInfo = principal.userInfo();
        String ticketId = UUID.randomUUID().toString();

        ticketStore.saveLink(
                ticketId,
                new LinkTicket(
                        userInfo.provider(),
                        userInfo.providerUserId(),
                        userInfo.email(),
                        principal.result().memberId(),
                        principal.result().existingProviders(),
                        AuthChannel.WEB),
                properties.ticket().ttl());

        return Completion.link(ticketId);
    }

    private Completion authenticated(
            Long memberId,
            MemberStatus status,
            com.nalssilog.member.domain.Provider provider,
            String result,
            DeviceInfo device,
            boolean clearLinkTicket
    ) {
        TokenPair tokens = authTokenService.issue(
                memberId,
                status,
                provider,
                device);

        memberClient.recordLogin(memberId, provider);

        return new Completion(
                result,
                null,
                tokens,
                null,
                null,
                false,
                clearLinkTicket);
    }

    public record Completion(
            String result,
            String errorCode,
            TokenPair tokens,
            String signupTicket,
            String linkTicket,
            boolean clearLinkIntent,
            boolean clearLinkTicket
    ) {

        private static Completion redirect(String result) {
            return new Completion(
                    result, null, null, null, null, false, false);
        }

        private static Completion clearIntent(String result, String errorCode) {
            return new Completion(
                    result, errorCode, null, null, null, true, false);
        }

        private static Completion clearLinkTicket(String result) {
            return new Completion(
                    result, null, null, null, null, false, true);
        }

        private static Completion signup(String ticketId) {
            return new Completion(
                    SIGNUP_REQUIRED, null, null, ticketId, null, false, false);
        }

        private static Completion link(String ticketId) {
            return new Completion(
                    LINK_REQUIRED, null, null, null, ticketId, false, false);
        }
    }
}
