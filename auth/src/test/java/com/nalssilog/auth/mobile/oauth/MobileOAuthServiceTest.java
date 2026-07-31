package com.nalssilog.auth.mobile.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.nalssilog.auth.config.AuthProperties;
import com.nalssilog.auth.core.AuthErrorCode;
import com.nalssilog.auth.device.DeviceInfo;
import com.nalssilog.auth.member.MemberClient;
import com.nalssilog.auth.oauth.OAuthUserInfo;
import com.nalssilog.auth.oauth.SocialAuthPrincipal;
import com.nalssilog.auth.ticket.AuthChannel;
import com.nalssilog.auth.ticket.AuthTicketStore;
import com.nalssilog.auth.ticket.LinkTicket;
import com.nalssilog.auth.token.AuthTokenService;
import com.nalssilog.auth.token.TokenPair;
import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.member.application.dto.MemberInfo;
import com.nalssilog.member.application.dto.SocialLoginResult;
import com.nalssilog.member.domain.AvatarType;
import com.nalssilog.member.domain.MemberStatus;
import com.nalssilog.member.domain.Provider;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

@SuppressWarnings("java:S5960")
class MobileOAuthServiceTest {

    private static final String REDIRECT_URI =
            "nalssilog://auth/callback";
    private static final String CHALLENGE = "A".repeat(43);
    private static final String STATE = "state-01234567890";

    private final MobileOAuthTransactionStore transactionStore =
            mock(MobileOAuthTransactionStore.class);
    private final MobileOAuthCodeStore codeStore =
            mock(MobileOAuthCodeStore.class);
    private final AuthTicketStore ticketStore =
            mock(AuthTicketStore.class);
    private final MemberClient memberClient = mock(MemberClient.class);
    private final AuthTokenService authTokenService =
            mock(AuthTokenService.class);
    private final ClientRegistrationRepository registrations =
            mock(ClientRegistrationRepository.class);
    private final MobileOAuthService service = new MobileOAuthService(
            transactionStore,
            codeStore,
            ticketStore,
            memberClient,
            authTokenService,
            properties(),
            registrations);

    @Test
    void loginStartStoresServerTransactionAndRedirectsThroughSpringOAuth() {
        when(registrations.findByRegistrationId("kakao"))
                .thenReturn(mock(ClientRegistration.class));

        String authorizationUrl = service.startLogin(
                "kakao",
                REDIRECT_URI,
                CHALLENGE,
                "S256",
                STATE);

        ArgumentCaptor<String> idCaptor =
                ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MobileOAuthTransaction> transactionCaptor =
                ArgumentCaptor.forClass(MobileOAuthTransaction.class);

        verify(transactionStore).save(
                idCaptor.capture(),
                transactionCaptor.capture());

        assertThat(idCaptor.getValue()).hasSize(43);
        assertThat(transactionCaptor.getValue())
                .isEqualTo(new MobileOAuthTransaction(
                        MobileOAuthPurpose.LOGIN,
                        Provider.KAKAO,
                        REDIRECT_URI,
                        CHALLENGE,
                        STATE,
                        null,
                        null));
        assertThat(authorizationUrl)
                .startsWith("/oauth2/authorization/kakao?mobile_transaction=")
                .doesNotContain(REDIRECT_URI, CHALLENGE, STATE);
    }

    @Test
    void oauthCallbackOnlyIssuesOneTimeCodeAndDoesNotCreateSession() {
        MobileOAuthTransaction transaction = new MobileOAuthTransaction(
                MobileOAuthPurpose.LOGIN,
                Provider.KAKAO,
                REDIRECT_URI,
                CHALLENGE,
                STATE,
                null,
                null);
        SocialAuthPrincipal principal = new SocialAuthPrincipal(
                SocialLoginResult.existing(7L, MemberStatus.ACTIVE),
                new OAuthUserInfo(
                        Provider.KAKAO,
                        "provider-user",
                        "user@example.com",
                        "사용자"),
                Map.of());

        when(transactionStore.take("transaction"))
                .thenReturn(Optional.of(transaction));

        String callback = service.complete("transaction", principal);

        ArgumentCaptor<String> codeCaptor =
                ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MobileOAuthGrant> grantCaptor =
                ArgumentCaptor.forClass(MobileOAuthGrant.class);

        verify(codeStore).save(
                codeCaptor.capture(),
                grantCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(REDIRECT_URI),
                org.mockito.ArgumentMatchers.eq(CHALLENGE));

        assertThat(codeCaptor.getValue()).hasSize(43);
        assertThat(grantCaptor.getValue())
                .isEqualTo(MobileOAuthGrant.success(
                        7L,
                        Provider.KAKAO));
        assertThat(callback)
                .contains("code=" + codeCaptor.getValue(), "state=" + STATE)
                .doesNotContain(
                        "accessToken",
                        "refreshToken",
                        "user@example.com",
                        "provider-user");
        verifyNoInteractions(authTokenService);
    }

    @Test
    void tokensAreCreatedOnlyAfterSuccessfulCodeExchange() {
        String verifier = "v".repeat(43);
        DeviceInfo device = new DeviceInfo(
                "ANDROID · Galaxy · 0.1.0",
                "client-a.test");
        MemberInfo member = member();
        TokenPair tokens = new TokenPair(
                "access-token",
                "refresh-token",
                Duration.ofDays(14));

        when(codeStore.consume(
                org.mockito.ArgumentMatchers.eq("one-time-code"),
                org.mockito.ArgumentMatchers.eq(REDIRECT_URI),
                any()))
                .thenReturn(MobileOAuthGrant.success(
                        7L,
                        Provider.KAKAO));
        when(memberClient.getMemberInfo(7L)).thenReturn(member);
        when(authTokenService.issue(
                7L,
                MemberStatus.ACTIVE,
                Provider.KAKAO,
                device)).thenReturn(tokens);

        MobileOAuthService.ExchangeResult result = service.exchange(
                "one-time-code",
                verifier,
                REDIRECT_URI,
                device);

        assertThat(result.result()).isEqualTo(MobileAuthResult.SUCCESS);
        assertThat(result.tokens()).isEqualTo(tokens);
        assertThat(result.member()).isEqualTo(member);
        verify(authTokenService).issue(
                7L,
                MemberStatus.ACTIVE,
                Provider.KAKAO,
                device);
        verify(memberClient).recordLogin(7L, Provider.KAKAO);
    }

    @Test
    void loginLinkIssuesSessionForNewlyLinkedProvider() {
        String verifier = "v".repeat(43);
        DeviceInfo device = new DeviceInfo(
                "IOS · iPhone · 0.1.0",
                "client-a.test");
        MobileOAuthTransaction transaction = new MobileOAuthTransaction(
                MobileOAuthPurpose.LOGIN_LINK_REAUTH,
                Provider.NAVER,
                REDIRECT_URI,
                CHALLENGE,
                STATE,
                "link-ticket",
                7L);
        LinkTicket ticket = new LinkTicket(
                Provider.KAKAO,
                "new-kakao-user",
                "user@example.com",
                7L,
                List.of(Provider.NAVER),
                AuthChannel.MOBILE);
        OAuthUserInfo linkedUserInfo = new OAuthUserInfo(
                Provider.KAKAO,
                "new-kakao-user",
                "user@example.com",
                null);
        SocialAuthPrincipal reauthenticatedPrincipal = new SocialAuthPrincipal(
                SocialLoginResult.existing(7L, MemberStatus.ACTIVE),
                new OAuthUserInfo(
                        Provider.NAVER,
                        "existing-naver-user",
                        "user@example.com",
                        "사용자"),
                Map.of());
        MemberInfo member = linkedMember();
        TokenPair tokens = new TokenPair(
                "access-token",
                "refresh-token",
                Duration.ofDays(14));

        when(transactionStore.take("transaction"))
                .thenReturn(Optional.of(transaction));
        when(ticketStore.findLink("link-ticket")).thenReturn(Optional.of(ticket));
        when(ticketStore.isLinkConsented("link-ticket")).thenReturn(true);
        when(memberClient.linkSocial(7L, linkedUserInfo)).thenReturn(member);

        service.complete("transaction", reauthenticatedPrincipal);

        verify(memberClient, never()).recordLogin(any(), any());

        ArgumentCaptor<MobileOAuthGrant> grantCaptor =
                ArgumentCaptor.forClass(MobileOAuthGrant.class);

        verify(codeStore).save(
                any(),
                grantCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(REDIRECT_URI),
                org.mockito.ArgumentMatchers.eq(CHALLENGE));

        MobileOAuthGrant grant = grantCaptor.getValue();

        assertThat(grant).isEqualTo(MobileOAuthGrant.linkSuccess(
                7L,
                Provider.KAKAO,
                true));

        when(codeStore.consume(
                org.mockito.ArgumentMatchers.eq("one-time-code"),
                org.mockito.ArgumentMatchers.eq(REDIRECT_URI),
                any())).thenReturn(grant);
        when(memberClient.getMemberInfo(7L)).thenReturn(member);
        when(authTokenService.issue(
                7L,
                MemberStatus.ACTIVE,
                Provider.KAKAO,
                device)).thenReturn(tokens);

        MobileOAuthService.ExchangeResult result = service.exchange(
                "one-time-code",
                verifier,
                REDIRECT_URI,
                device);

        assertThat(result.result()).isEqualTo(MobileAuthResult.LINK_SUCCESS);
        assertThat(result.tokens()).isEqualTo(tokens);

        verify(authTokenService).issue(
                7L,
                MemberStatus.ACTIVE,
                Provider.KAKAO,
                device);
        verify(authTokenService, never()).issue(
                7L,
                MemberStatus.ACTIVE,
                Provider.NAVER,
                device);
        verify(memberClient).recordLogin(7L, Provider.KAKAO);
        verify(memberClient, never()).recordLogin(7L, Provider.NAVER);
    }

    @Test
    void unregisteredRedirectIsRejectedBeforeOAuthStateIsCreated() {
        NalssiLogException exception = catchThrowableOfType(
                NalssiLogException.class,
                () -> service.startLogin(
                        "kakao",
                        "nalssilog://attacker/callback",
                        CHALLENGE,
                        "S256",
                        STATE));

        assertThat(exception.getErrorCode())
                .isEqualTo(AuthErrorCode.AUTH_REDIRECT_URI_INVALID);
        verify(transactionStore, never()).save(any(), any());
    }

    private MemberInfo member() {
        return new MemberInfo(
                7L,
                "사용자",
                "구름산책",
                "user@example.com",
                AvatarType.PRESET,
                "1",
                MemberStatus.ACTIVE,
                Provider.KAKAO,
                List.of(Provider.KAKAO));
    }

    private MemberInfo linkedMember() {
        return new MemberInfo(
                7L,
                "사용자",
                "구름산책",
                "user@example.com",
                AvatarType.PRESET,
                "1",
                MemberStatus.ACTIVE,
                Provider.KAKAO,
                List.of(Provider.NAVER, Provider.KAKAO));
    }

    private AuthProperties properties() {
        return new AuthProperties(
                new AuthProperties.Jwt(
                        "test-secret-must-be-at-least-thirty-two-bytes",
                        Duration.ofMinutes(30),
                        Duration.ofDays(14)),
                new AuthProperties.Cookie(false),
                new AuthProperties.Ticket(Duration.ofMinutes(10)),
                new AuthProperties.Csrf("XSRF-TOKEN", null),
                new AuthProperties.Refresh(Duration.ofSeconds(5)),
                new AuthProperties.Mobile(
                        List.of(REDIRECT_URI),
                        Duration.ofMinutes(10),
                        Duration.ofSeconds(90),
                        "test-hmac-secret",
                        List.of()),
                new AuthProperties.Guest(
                        Duration.ofDays(365),
                        5,
                        Duration.ofMinutes(10)));
    }
}
