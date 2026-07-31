package com.nalssilog.auth.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.nalssilog.auth.config.AuthProperties;
import com.nalssilog.auth.device.DeviceInfo;
import com.nalssilog.auth.member.MemberClient;
import com.nalssilog.auth.ticket.AuthTicketStore.SignupClaim;
import com.nalssilog.auth.ticket.AuthTicketStore.SignupClaimStatus;
import com.nalssilog.auth.ticket.AuthTicketStore.SignupCompletion;
import com.nalssilog.auth.ticket.AuthTicketStore;
import com.nalssilog.auth.token.AuthSessionService;
import com.nalssilog.auth.token.AuthTokenService;
import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.member.application.dto.MemberInfo;
import com.nalssilog.member.domain.AvatarType;
import com.nalssilog.member.domain.MemberStatus;
import com.nalssilog.member.domain.Provider;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S5960")
class AuthServiceSignupTest {

    private final MemberClient memberClient = mock(MemberClient.class);
    private final AuthTokenService tokenService =
            mock(AuthTokenService.class);
    private final AuthSessionService sessionService =
            mock(AuthSessionService.class);
    private final AuthTicketStore ticketStore =
            mock(AuthTicketStore.class);
    private final AuthService service = new AuthService(
            memberClient,
            tokenService,
            sessionService,
            ticketStore,
            properties());

    @Test
    void completedSignupRetryReturnsTheSameTokenPair() {
        SignupCompletion completion = new SignupCompletion(
                7L,
                Provider.KAKAO,
                "same-access-token",
                "same-refresh-token",
                Duration.ofDays(14).toMillis(),
                Instant.now().toEpochMilli());

        when(ticketStore.claimSignup(
                org.mockito.ArgumentMatchers.eq("ticket"),
                anyString(),
                org.mockito.ArgumentMatchers.eq(Duration.ofMinutes(10))))
                .thenReturn(new SignupClaim(
                        SignupClaimStatus.COMPLETED,
                        null,
                        completion));
        when(memberClient.getMemberInfo(7L)).thenReturn(member());

        AuthService.SignupResult result = service.signupMobile(
                "ticket",
                List.of(),
                new DeviceInfo(
                        "ANDROID · Galaxy · 0.1.0",
                        "client-a.test"));

        assertThat(result.tokens().accessToken())
                .isEqualTo("same-access-token");
        assertThat(result.tokens().refreshToken())
                .isEqualTo("same-refresh-token");
        assertThat(result.tokens().refreshTokenMaxAge())
                .isLessThanOrEqualTo(Duration.ofDays(14))
                .isGreaterThan(Duration.ofDays(13));
        verifyNoInteractions(tokenService);
    }

    @Test
    void mobileRefreshFailureDoesNotRequestWebCookieDeletion() {
        DeviceInfo device = new DeviceInfo(
                "ANDROID · Galaxy · 0.1.0",
                "client-a.test");

        when(tokenService.refresh("refresh-token", device))
                .thenThrow(new NalssiLogException(
                        AuthErrorCode.AUTH_SESSION_EXPIRED));

        NalssiLogException exception = catchThrowableOfType(
                NalssiLogException.class,
                () -> service.refreshMobile("refresh-token", device));

        assertThat(exception.getErrorCode())
                .isEqualTo(AuthErrorCode.AUTH_SESSION_EXPIRED);
        assertThat(exception)
                .isNotInstanceOf(RefreshRejectedException.class);
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

    private AuthProperties properties() {
        return new AuthProperties(
                new AuthProperties.Jwt(
                        "test-secret-must-be-at-least-thirty-two-bytes",
                        Duration.ofMinutes(30),
                        Duration.ofDays(14)),
                new AuthProperties.Cookie(false),
                new AuthProperties.Ticket(Duration.ofMinutes(10)),
                new AuthProperties.Csrf("XSRF-TOKEN", null),
                new AuthProperties.Refresh(Duration.ofSeconds(5)));
    }
}
