package com.nalssilog.auth.application;

import com.nalssilog.auth.application.dto.DeviceInfo;
import com.nalssilog.auth.application.dto.SessionData;
import com.nalssilog.auth.client.MemberClient;
import com.nalssilog.auth.config.AuthProperties;
import com.nalssilog.auth.domain.AuthErrorCode;
import com.nalssilog.auth.repository.RefreshTokenStore;
import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.member.application.dto.MemberInfo;
import com.nalssilog.member.domain.MemberStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final MemberClient memberClient;
    private final AuthProperties properties;

    /** 새 로그인 세션 발급(새 sessionId + 기기 정보 저장). */
    public TokenPair issue(Long memberId, MemberStatus status, DeviceInfo device) {
        return issueWithSession(memberId, status, UUID.randomUUID().toString(), Instant.now(), device);
    }

    /** rotation: 토큰만 교체하고 기기 정체성(sessionId·loginAt·deviceName)은 유지, lastActiveAt·ip 갱신. */
    public TokenPair refresh(String refreshToken, DeviceInfo device) {
        String currentHash = hash(refreshToken);
        SessionData current = refreshTokenStore.findSession(currentHash)
                .orElseThrow(() -> new NalssiLogException(AuthErrorCode.AUTH_SESSION_EXPIRED));

        MemberInfo member = memberClient.findMemberInfo(current.memberId())
                .orElseThrow(() -> new NalssiLogException(AuthErrorCode.AUTH_SESSION_EXPIRED));

        refreshTokenStore.delete(currentHash);

        DeviceInfo preserved = new DeviceInfo(current.deviceName(), device.ip());

        return issueWithSession(member.id(), member.status(), current.sessionId(), current.loginAt(), preserved);
    }

    public void revoke(String refreshToken) {
        refreshTokenStore.delete(hash(refreshToken));
    }

    /** refresh 토큰의 세션 키(해시). '현재 세션' 판별용. */
    public String tokenHash(String refreshToken) {
        return hash(refreshToken);
    }

    /** 해당 회원의 모든 refresh 세션 강제 만료(전 기기). 탈퇴 시 사용. */
    public void revokeAllSessions(Long memberId) {
        refreshTokenStore.deleteAllByMember(memberId);
    }

    private TokenPair issueWithSession(Long memberId, MemberStatus status, String sessionId,
                                       Instant loginAt, DeviceInfo device) {
        String accessToken = jwtTokenProvider.createAccessToken(memberId, status);
        String refreshToken = generateRefreshToken();
        String tokenHash = hash(refreshToken);
        SessionData session = new SessionData(
                tokenHash, sessionId, memberId, device.deviceName(), device.ip(), loginAt, Instant.now());

        refreshTokenStore.save(tokenHash, session, properties.jwt().refreshTokenTtl());

        return new TokenPair(accessToken, refreshToken);
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
