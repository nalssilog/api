package com.nalssilog.auth.mobile.oauth;

import com.nalssilog.auth.config.AuthProperties;
import com.nalssilog.auth.core.AuthErrorCode;
import com.nalssilog.auth.member.MemberClient;
import com.nalssilog.auth.oauth.OAuthUserInfo;
import com.nalssilog.auth.oauth.SocialPrincipal;
import com.nalssilog.auth.ticket.AuthChannel;
import com.nalssilog.auth.ticket.AuthTicketStore;
import com.nalssilog.auth.ticket.LinkTicket;
import com.nalssilog.auth.ticket.SignupTicket;
import com.nalssilog.auth.token.AuthTokenService;
import com.nalssilog.auth.token.TokenPair;
import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.member.application.dto.MemberInfo;
import com.nalssilog.member.domain.MemberStatus;
import com.nalssilog.member.domain.Provider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class MobileOAuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int OPAQUE_BYTES = 32;
    private static final Pattern CODE_CHALLENGE_PATTERN =
            Pattern.compile("^[A-Za-z0-9_-]{43}$");
    private static final Pattern CODE_VERIFIER_PATTERN =
            Pattern.compile("^[A-Za-z0-9\\-._~]{43,128}$");
    private static final Pattern STATE_PATTERN =
            Pattern.compile("^[A-Za-z0-9_-]{16,256}$");

    private final MobileOAuthTransactionStore transactionStore;
    private final MobileOAuthCodeStore codeStore;
    private final AuthTicketStore ticketStore;
    private final MemberClient memberClient;
    private final AuthTokenService authTokenService;
    private final AuthProperties properties;
    private final ClientRegistrationRepository clientRegistrationRepository;

    public String startLogin(
            String providerText,
            String redirectUri,
            String codeChallenge,
            String codeChallengeMethod,
            String appState
    ) {
        Provider provider = provider(providerText);

        validateStart(provider, redirectUri, codeChallenge, codeChallengeMethod, appState);

        MobileOAuthTransaction transaction = new MobileOAuthTransaction(
                MobileOAuthPurpose.LOGIN,
                provider,
                redirectUri,
                codeChallenge,
                appState,
                null,
                null);

        return saveAndAuthorizationUrl(transaction);
    }

    public String startLinkReauthentication(
            String linkTicketId,
            String redirectUri,
            String codeChallenge,
            String codeChallengeMethod,
            String appState
    ) {
        LinkTicket ticket = mobileLinkTicket(linkTicketId);

        ticketStore.markLinkConsented(linkTicketId, properties.ticket().ttl());

        MemberInfo target = memberClient.getMemberInfo(ticket.targetMemberId());
        Provider reauthenticationProvider = target.lastLoginProvider() != null
                ? target.lastLoginProvider()
                : target.connectedProviders().getFirst();

        validateStart(
                reauthenticationProvider,
                redirectUri,
                codeChallenge,
                codeChallengeMethod,
                appState);

        MobileOAuthTransaction transaction = new MobileOAuthTransaction(
                MobileOAuthPurpose.LOGIN_LINK_REAUTH,
                reauthenticationProvider,
                redirectUri,
                codeChallenge,
                appState,
                linkTicketId,
                ticket.targetMemberId());

        return saveAndAuthorizationUrl(transaction);
    }

    public String startSettingsLink(
            Long memberId,
            String providerText,
            String redirectUri,
            String codeChallenge,
            String codeChallengeMethod,
            String appState
    ) {
        Provider provider = provider(providerText);
        MemberInfo member = memberClient.getMemberInfo(memberId);

        if (member.connectedProviders().contains(provider)) {
            throw new NalssiLogException(AuthErrorCode.ALREADY_LINKED_PROVIDER);
        }

        validateStart(provider, redirectUri, codeChallenge, codeChallengeMethod, appState);

        MobileOAuthTransaction transaction = new MobileOAuthTransaction(
                MobileOAuthPurpose.SETTINGS_LINK,
                provider,
                redirectUri,
                codeChallenge,
                appState,
                null,
                memberId);

        return saveAndAuthorizationUrl(transaction);
    }

    public void cancelLink(String linkTicketId) {
        LinkTicket ticket = mobileLinkTicket(linkTicketId);

        ticketStore.deleteLink(linkTicketId);
        ticketStore.deleteLinkConsent(linkTicketId);
    }

    public String complete(String transactionId, SocialPrincipal principal) {
        MobileOAuthTransaction transaction = transactionStore.take(transactionId)
                .orElseThrow(() -> new NalssiLogException(
                        AuthErrorCode.AUTH_MOBILE_TRANSACTION_EXPIRED));
        MobileOAuthGrant grant = resolveGrant(transaction, principal);

        return issueCallback(transaction, grant);
    }

    private MobileOAuthGrant resolveGrant(
            MobileOAuthTransaction transaction,
            SocialPrincipal principal
    ) {
        if (transaction.provider() != principal.userInfo().provider()) {
            return MobileOAuthGrant.failed(
                    transaction.provider(),
                    AuthErrorCode.OAUTH_FAILED.getCode());
        }

        return switch (transaction.purpose()) {
            case LOGIN -> completeLogin(principal);
            case LOGIN_LINK_REAUTH -> completeLoginLink(transaction, principal);
            case SETTINGS_LINK -> completeSettingsLink(transaction, principal);
        };
    }

    public Optional<String> completeFailure(String transactionId, String errorCode) {
        return transactionStore.take(transactionId)
                .map(transaction -> issueCallback(
                        transaction,
                        MobileOAuthGrant.failed(transaction.provider(), errorCode)));
    }

    public ExchangeResult exchange(
            String rawCode,
            String codeVerifier,
            String redirectUri,
            com.nalssilog.auth.device.DeviceInfo device
    ) {
        validateRedirectUri(redirectUri);

        if (codeVerifier == null || !CODE_VERIFIER_PATTERN.matcher(codeVerifier).matches()) {
            throw new NalssiLogException(AuthErrorCode.AUTH_PKCE_VERIFICATION_FAILED);
        }

        MobileOAuthGrant grant = codeStore.consume(
                rawCode,
                redirectUri,
                codeChallenge(codeVerifier));

        return switch (grant.result()) {
            case SUCCESS -> authenticated(grant, MobileAuthResult.SUCCESS, device);
            case LINK_SUCCESS -> grant.issueTokens()
                    ? authenticated(grant, MobileAuthResult.LINK_SUCCESS, device)
                    : linkedWithoutTokens(grant);
            case SIGNUP_REQUIRED -> signupRequired(grant);
            case LINK_REQUIRED -> linkRequired(grant);
            case FAILED -> throw failure(grant.errorCode());
        };
    }

    public LinkTicket mobileLinkTicket(String ticketId) {
        if (ticketId == null || ticketId.isBlank()) {
            throw new NalssiLogException(AuthErrorCode.TICKET_NOT_FOUND);
        }

        LinkTicket ticket = ticketStore.findLink(ticketId)
                .orElseThrow(() -> new NalssiLogException(AuthErrorCode.TICKET_NOT_FOUND));

        requireMobileChannel(ticket.effectiveChannel());

        return ticket;
    }

    public SignupTicket mobileSignupTicket(String ticketId) {
        if (ticketId == null || ticketId.isBlank()) {
            throw new NalssiLogException(AuthErrorCode.TICKET_NOT_FOUND);
        }

        SignupTicket ticket = ticketStore.findSignup(ticketId)
                .orElseThrow(() -> new NalssiLogException(AuthErrorCode.TICKET_NOT_FOUND));

        requireMobileChannel(ticket.effectiveChannel());

        return ticket;
    }

    private MobileOAuthGrant completeLogin(SocialPrincipal principal) {
        OAuthUserInfo userInfo = principal.userInfo();

        return switch (principal.result().outcome()) {
            case EXISTING -> principal.result().status() == MemberStatus.WITHDRAWN
                    ? MobileOAuthGrant.failed(userInfo.provider(), AuthErrorCode.OAUTH_FAILED.getCode())
                    : MobileOAuthGrant.success(principal.result().memberId(), userInfo.provider());
            case NEW -> {
                String ticketId = UUID.randomUUID().toString();

                ticketStore.saveSignup(
                        ticketId,
                        new SignupTicket(
                                userInfo.provider(),
                                userInfo.providerUserId(),
                                userInfo.email(),
                                userInfo.socialName(),
                                AuthChannel.MOBILE),
                        properties.ticket().ttl());
                yield MobileOAuthGrant.signupRequired(userInfo.provider(), ticketId);
            }
            case LINK_REQUIRED -> {
                String ticketId = UUID.randomUUID().toString();

                ticketStore.saveLink(
                        ticketId,
                        new LinkTicket(
                                userInfo.provider(),
                                userInfo.providerUserId(),
                                userInfo.email(),
                                principal.result().memberId(),
                                principal.result().existingProviders(),
                                AuthChannel.MOBILE),
                        properties.ticket().ttl());
                yield MobileOAuthGrant.linkRequired(userInfo.provider(), ticketId);
            }
        };
    }

    private MobileOAuthGrant completeLoginLink(
            MobileOAuthTransaction transaction,
            SocialPrincipal principal
    ) {
        String ticketId = transaction.referenceId();

        try {
            LinkTicket ticket = mobileLinkTicket(ticketId);
            boolean validOwner = principal.result().outcome()
                    == com.nalssilog.member.application.dto.SocialLoginResult.Outcome.EXISTING
                    && ticket.targetMemberId().equals(principal.result().memberId())
                    && ticket.targetMemberId().equals(transaction.targetMemberId())
                    && ticketStore.isLinkConsented(ticketId);

            if (!validOwner) {
                return MobileOAuthGrant.failed(
                        transaction.provider(), AuthErrorCode.OAUTH_FAILED.getCode());
            }

            MemberInfo member = memberClient.linkSocial(
                    ticket.targetMemberId(),
                    new OAuthUserInfo(
                            ticket.provider(),
                            ticket.providerUserId(),
                            ticket.email(),
                            null));

            return MobileOAuthGrant.linkSuccess(
                    member.id(),
                    ticket.provider(),
                    true);
        } catch (NalssiLogException _) {
            return MobileOAuthGrant.failed(
                    transaction.provider(), AuthErrorCode.OAUTH_FAILED.getCode());
        } finally {
            if (ticketId != null) {
                ticketStore.deleteLink(ticketId);
                ticketStore.deleteLinkConsent(ticketId);
            }
        }
    }

    private MobileOAuthGrant completeSettingsLink(
            MobileOAuthTransaction transaction,
            SocialPrincipal principal
    ) {
        try {
            MemberInfo member = memberClient.linkSocial(
                    transaction.targetMemberId(),
                    principal.userInfo());

            return MobileOAuthGrant.linkSuccess(
                    member.id(),
                    principal.userInfo().provider(),
                    false);
        } catch (NalssiLogException _) {
            return MobileOAuthGrant.failed(
                    transaction.provider(), AuthErrorCode.OAUTH_FAILED.getCode());
        }
    }

    private ExchangeResult authenticated(
            MobileOAuthGrant grant,
            MobileAuthResult result,
            com.nalssilog.auth.device.DeviceInfo device
    ) {
        MemberInfo member = memberClient.getMemberInfo(grant.memberId());

        if (member.status() == MemberStatus.WITHDRAWN) {
            throw new NalssiLogException(AuthErrorCode.OAUTH_FAILED);
        }

        TokenPair tokens = authTokenService.issue(
                member.id(),
                member.status(),
                grant.provider(),
                device);

        memberClient.recordLogin(member.id(), grant.provider());

        return new ExchangeResult(
                result, tokens, member, null, null, null, null, List.of());
    }

    private ExchangeResult linkedWithoutTokens(MobileOAuthGrant grant) {
        MemberInfo member = memberClient.getMemberInfo(grant.memberId());

        return new ExchangeResult(
                MobileAuthResult.LINK_SUCCESS,
                null,
                member,
                null,
                null,
                null,
                null,
                List.of());
    }

    private ExchangeResult signupRequired(MobileOAuthGrant grant) {
        SignupTicket ticket = mobileSignupTicket(grant.ticketId());

        return new ExchangeResult(
                MobileAuthResult.SIGNUP_REQUIRED,
                null,
                null,
                grant.ticketId(),
                null,
                ticket.provider(),
                ticket.email(),
                List.of());
    }

    private ExchangeResult linkRequired(MobileOAuthGrant grant) {
        LinkTicket ticket = mobileLinkTicket(grant.ticketId());

        return new ExchangeResult(
                MobileAuthResult.LINK_REQUIRED,
                null,
                null,
                null,
                grant.ticketId(),
                ticket.provider(),
                ticket.email(),
                ticket.existingProviders());
    }

    private NalssiLogException failure(String errorCode) {
        if (AuthErrorCode.OAUTH_CANCELLED.getCode().equals(errorCode)) {
            return new NalssiLogException(AuthErrorCode.OAUTH_CANCELLED);
        }

        if (AuthErrorCode.OAUTH_EMAIL_REQUIRED.getCode().equals(errorCode)) {
            return new NalssiLogException(AuthErrorCode.OAUTH_EMAIL_REQUIRED);
        }

        return new NalssiLogException(AuthErrorCode.OAUTH_FAILED);
    }

    private String saveAndAuthorizationUrl(MobileOAuthTransaction transaction) {
        String transactionId = opaqueValue();

        transactionStore.save(transactionId, transaction);

        return UriComponentsBuilder
                .fromPath("/oauth2/authorization/"
                        + transaction.provider().name().toLowerCase(Locale.ROOT))
                .queryParam(MobileOAuthRequestAttributes.TRANSACTION_PARAMETER, transactionId)
                .build()
                .encode()
                .toUriString();
    }

    private String issueCallback(
            MobileOAuthTransaction transaction,
            MobileOAuthGrant grant
    ) {
        String code = opaqueValue();

        codeStore.save(code, grant, transaction.redirectUri(), transaction.codeChallenge());

        return UriComponentsBuilder
                .fromUriString(transaction.redirectUri())
                .queryParam("code", code)
                .queryParam("state", transaction.appState())
                .build()
                .encode()
                .toUriString();
    }

    private void validateStart(
            Provider provider,
            String redirectUri,
            String codeChallenge,
            String codeChallengeMethod,
            String appState
    ) {
        validateRedirectUri(redirectUri);

        if (!"S256".equals(codeChallengeMethod)
                || codeChallenge == null
                || !CODE_CHALLENGE_PATTERN.matcher(codeChallenge).matches()) {
            throw new NalssiLogException(AuthErrorCode.AUTH_PKCE_VERIFICATION_FAILED);
        }

        if (appState == null
                || !STATE_PATTERN.matcher(appState).matches()) {
            throw new NalssiLogException(AuthErrorCode.OAUTH_FAILED);
        }

        String registrationId = provider.name().toLowerCase(Locale.ROOT);

        if (clientRegistrationRepository.findByRegistrationId(registrationId) == null) {
            throw new NalssiLogException(AuthErrorCode.UNSUPPORTED_PROVIDER);
        }
    }

    private void validateRedirectUri(String redirectUri) {
        if (redirectUri == null
                || !properties.mobile().redirectUris().contains(redirectUri)) {
            throw new NalssiLogException(AuthErrorCode.AUTH_REDIRECT_URI_INVALID);
        }
    }

    private Provider provider(String text) {
        try {
            return Provider.from(text);
        } catch (IllegalArgumentException exception) {
            throw new NalssiLogException(AuthErrorCode.UNSUPPORTED_PROVIDER);
        }
    }

    private void requireMobileChannel(AuthChannel channel) {
        if (channel != AuthChannel.MOBILE) {
            throw new NalssiLogException(AuthErrorCode.AUTH_TICKET_CHANNEL_MISMATCH);
        }
    }

    private String codeChallenge(String verifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(verifier.getBytes(StandardCharsets.US_ASCII));

            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }

    private String opaqueValue() {
        byte[] bytes = new byte[OPAQUE_BYTES];

        SECURE_RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record ExchangeResult(
            MobileAuthResult result,
            TokenPair tokens,
            MemberInfo member,
            String signupTicket,
            String linkTicket,
            Provider pendingProvider,
            String pendingEmail,
            List<Provider> existingProviders
    ) {
    }
}
