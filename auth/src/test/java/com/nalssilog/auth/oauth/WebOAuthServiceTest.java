package com.nalssilog.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nalssilog.auth.config.AuthProperties;
import com.nalssilog.auth.device.DeviceInfo;
import com.nalssilog.auth.member.MemberClient;
import com.nalssilog.auth.ticket.AuthChannel;
import com.nalssilog.auth.ticket.AuthTicketStore;
import com.nalssilog.auth.ticket.LinkTicket;
import com.nalssilog.auth.token.AuthTokenService;
import com.nalssilog.auth.token.TokenPair;
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

@SuppressWarnings("java:S5960")
class WebOAuthServiceTest {

    private final AuthTokenService authTokenService = mock(AuthTokenService.class);
    private final AuthTicketStore ticketStore = mock(AuthTicketStore.class);
    private final MemberClient memberClient = mock(MemberClient.class);
    private final WebOAuthService service = new WebOAuthService(
            authTokenService,
            ticketStore,
            memberClient,
            mock(AuthProperties.class));

    @Test
    void loginLinkIssuesSessionForNewlyLinkedProvider() {
        DeviceInfo device = new DeviceInfo("Chrome", "client-a.test");
        LinkTicket ticket = new LinkTicket(
                Provider.KAKAO,
                "new-kakao-user",
                "user@example.com",
                7L,
                List.of(Provider.NAVER),
                AuthChannel.WEB);
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
        MemberInfo member = member();
        TokenPair tokens = new TokenPair(
                "access-token",
                "refresh-token",
                Duration.ofDays(14));

        when(ticketStore.findLink("link-ticket")).thenReturn(Optional.of(ticket));
        when(ticketStore.isLinkConsented("link-ticket")).thenReturn(true);
        when(memberClient.linkSocial(7L, linkedUserInfo)).thenReturn(member);
        when(authTokenService.issue(
                7L,
                MemberStatus.ACTIVE,
                Provider.KAKAO,
                device)).thenReturn(tokens);

        WebOAuthService.Completion completion = service.complete(
                reauthenticatedPrincipal,
                Optional.empty(),
                Optional.of("link-ticket"),
                device);

        assertThat(completion.result()).isEqualTo("LINK_SUCCESS");
        assertThat(completion.tokens()).isEqualTo(tokens);
        assertThat(completion.clearLinkTicket()).isTrue();

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
        verify(ticketStore).deleteLink("link-ticket");
        verify(ticketStore).deleteLinkConsent("link-ticket");
    }

    private MemberInfo member() {
        return new MemberInfo(
                7L,
                "구름산책",
                "사용자",
                "user@example.com",
                AvatarType.PRESET,
                "1",
                MemberStatus.ACTIVE,
                Provider.KAKAO,
                List.of(Provider.NAVER, Provider.KAKAO));
    }
}
