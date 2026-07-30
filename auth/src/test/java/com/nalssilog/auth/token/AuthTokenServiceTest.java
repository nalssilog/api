package com.nalssilog.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nalssilog.auth.config.AuthProperties;
import com.nalssilog.auth.core.AuthErrorCode;
import com.nalssilog.auth.device.DeviceInfo;
import com.nalssilog.auth.member.MemberClient;
import com.nalssilog.auth.token.RefreshTokenStore.RotationResult;
import com.nalssilog.auth.token.RefreshTokenStore.RotationStatus;
import com.nalssilog.auth.token.RefreshTokenStore.UsedToken;
import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.member.application.dto.MemberInfo;
import com.nalssilog.member.domain.AvatarType;
import com.nalssilog.member.domain.MemberStatus;
import com.nalssilog.member.domain.Provider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S5960")
class AuthTokenServiceTest {

    private static final String CURRENT_TOKEN = "current-refresh-token";
    private static final String CURRENT_HASH = sha256(CURRENT_TOKEN);
    private static final String SESSION_ID = "session-1";

    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final RefreshTokenStore refreshTokenStore = mock(RefreshTokenStore.class);
    private final MemberClient memberClient = mock(MemberClient.class);
    private final AuthProperties properties = properties();
    private final AuthTokenService service = new AuthTokenService(
            jwtTokenProvider, refreshTokenStore, memberClient, properties);

    private SessionData current;
    private MemberInfo member;

    @BeforeEach
    void setUp() {
        current = new SessionData(
                CURRENT_HASH,
                SESSION_ID,
                1L,
                Provider.KAKAO,
                "Chrome · Windows",
                "127.0.0.1",
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-01T00:00:00Z"));
        member = new MemberInfo(
                1L,
                "닉네임",
                "이름",
                "user@example.com",
                AvatarType.PRESET,
                "1",
                MemberStatus.ACTIVE,
                Provider.KAKAO,
                List.of(Provider.KAKAO));
        when(jwtTokenProvider.createAccessToken(
                        1L, MemberStatus.ACTIVE, Provider.KAKAO, SESSION_ID))
                .thenReturn("access-token");
    }

    @Test
    void rotatesCurrentRefreshTokenThroughAtomicStoreOperation() {
        when(refreshTokenStore.findSession(CURRENT_HASH)).thenReturn(Optional.of(current));
        when(memberClient.findMemberInfo(1L)).thenReturn(Optional.of(member));
        when(refreshTokenStore.rotate(
                eq(CURRENT_HASH),
                anyString(),
                any(SessionData.class),
                eq(Duration.ofDays(14)),
                eq(Duration.ofSeconds(5))))
                .thenAnswer(invocation -> {
                    String token = invocation.getArgument(1);
                    SessionData replacement = invocation.getArgument(2);

                    return new RotationResult(
                            RotationStatus.ROTATED,
                            token,
                            replacement.tokenHash(),
                            1L,
                            SESSION_ID,
                            Duration.ofDays(14).toMillis());
                });

        TokenPair tokens = service.refresh(CURRENT_TOKEN, new DeviceInfo("ignored", "203.0.113.1"));

        assertThat(tokens.accessToken()).isEqualTo("access-token");
        assertThat(tokens.refreshToken()).isNotBlank().isNotEqualTo(CURRENT_TOKEN);
        assertThat(tokens.refreshTokenMaxAge()).isEqualTo(Duration.ofDays(14));
        verify(refreshTokenStore).rotate(
                eq(CURRENT_HASH),
                eq(tokens.refreshToken()),
                any(SessionData.class),
                eq(Duration.ofDays(14)),
                eq(Duration.ofSeconds(5)));
        verify(refreshTokenStore, never()).revokeSession(any(), anyString(), any());
    }

    @Test
    void duplicateWithinGraceReplaysTheFirstReplacementToken() {
        String replacementHash = sha256("replacement-token");
        SessionData replacement = new SessionData(
                replacementHash,
                SESSION_ID,
                1L,
                Provider.KAKAO,
                "Chrome · Windows",
                "203.0.113.1",
                current.loginAt(),
                Instant.now());

        when(refreshTokenStore.findSession(CURRENT_HASH)).thenReturn(Optional.empty());
        when(refreshTokenStore.findUsedToken(CURRENT_HASH))
                .thenReturn(Optional.of(new UsedToken(1L, SESSION_ID, replacementHash, Instant.now())));
        when(refreshTokenStore.findSession(replacementHash)).thenReturn(Optional.of(replacement));
        when(refreshTokenStore.rotate(
                eq(CURRENT_HASH),
                eq(""),
                eq(replacement),
                eq(Duration.ofDays(14)),
                eq(Duration.ofSeconds(5))))
                .thenReturn(new RotationResult(
                        RotationStatus.RETRIED,
                        "replacement-token",
                        replacementHash,
                        1L,
                        SESSION_ID,
                        Duration.ofDays(13).toMillis()));
        when(memberClient.findMemberInfo(1L)).thenReturn(Optional.of(member));

        TokenPair tokens = service.refresh(CURRENT_TOKEN, new DeviceInfo("ignored", "203.0.113.1"));

        assertThat(tokens.refreshToken()).isEqualTo("replacement-token");
        assertThat(tokens.accessToken()).isEqualTo("access-token");
        assertThat(tokens.refreshTokenMaxAge()).isEqualTo(Duration.ofDays(13));
        verify(refreshTokenStore, never()).revokeSession(any(), anyString(), any());
    }

    @Test
    void reuseOutsideGraceRevokesTheWholeDeviceSession() {
        String replacementHash = sha256("replacement-token");
        SessionData replacement = new SessionData(
                replacementHash,
                SESSION_ID,
                1L,
                Provider.KAKAO,
                "Chrome · Windows",
                "203.0.113.1",
                current.loginAt(),
                Instant.now());

        when(refreshTokenStore.findSession(CURRENT_HASH)).thenReturn(Optional.empty());
        when(refreshTokenStore.findUsedToken(CURRENT_HASH))
                .thenReturn(Optional.of(new UsedToken(1L, SESSION_ID, replacementHash, Instant.now())));
        when(refreshTokenStore.findSession(replacementHash)).thenReturn(Optional.of(replacement));
        when(refreshTokenStore.rotate(
                eq(CURRENT_HASH),
                eq(""),
                eq(replacement),
                eq(Duration.ofDays(14)),
                eq(Duration.ofSeconds(5))))
                .thenReturn(new RotationResult(
                        RotationStatus.REUSED,
                        "",
                        replacementHash,
                        1L,
                        SESSION_ID,
                        0));
        when(refreshTokenStore.revokeSession(1L, SESSION_ID, Duration.ofDays(14))).thenReturn(1L);

        NalssiLogException exception = catchThrowableOfType(
                NalssiLogException.class,
                () -> service.refresh(CURRENT_TOKEN, new DeviceInfo("ignored", "203.0.113.1")));

        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.AUTH_REFRESH_REUSED);
        verify(refreshTokenStore).revokeSession(1L, SESSION_ID, Duration.ofDays(14));
    }

    @Test
    void unknownRefreshTokenIsRejectedWithoutRevokingUnrelatedSessions() {
        when(refreshTokenStore.findSession(CURRENT_HASH)).thenReturn(Optional.empty());
        when(refreshTokenStore.findUsedToken(CURRENT_HASH)).thenReturn(Optional.empty());

        NalssiLogException exception = catchThrowableOfType(
                NalssiLogException.class,
                () -> service.refresh(CURRENT_TOKEN, new DeviceInfo("ignored", "203.0.113.1")));

        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.AUTH_SESSION_EXPIRED);
        verify(refreshTokenStore, never()).revokeSession(any(), anyString(), any());
    }

    private static AuthProperties properties() {

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

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
