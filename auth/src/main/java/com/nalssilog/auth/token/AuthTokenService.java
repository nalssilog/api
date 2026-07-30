package com.nalssilog.auth.token;

import com.nalssilog.auth.config.AuthProperties;
import com.nalssilog.auth.core.AuthErrorCode;
import com.nalssilog.auth.device.DeviceInfo;
import com.nalssilog.auth.member.MemberClient;
import com.nalssilog.auth.token.RefreshTokenStore.RotationResult;
import com.nalssilog.auth.token.RefreshTokenStore.RotationStatus;
import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.member.application.dto.MemberInfo;
import com.nalssilog.member.domain.MemberStatus;
import com.nalssilog.member.domain.Provider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int FINGERPRINT_LENGTH = 12;

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final MemberClient memberClient;
    private final AuthProperties properties;

    /** 새 로그인 세션 발급(새 sessionId + 기기 정보 저장). */
    public TokenPair issue(Long memberId, MemberStatus status, Provider provider, DeviceInfo device) {
        String sessionId = UUID.randomUUID().toString();
        TokenPair tokens = issueWithSession(memberId, status, provider, sessionId, Instant.now(), device);

        log.info("auth.refresh.issued memberId={} sessionId={} provider={}", memberId, sessionId, provider);

        return tokens;
    }

    /**
     * RT rotation. Redis가 기존 RT 소비와 새 RT 저장을 원자적으로 처리한다.
     * retry grace 안의 중복 요청은 최초 요청과 같은 새 RT로 수렴하고, grace 이후 재사용은 해당 기기 세션을 폐기한다.
     */
    public TokenPair refresh(String refreshToken, DeviceInfo device) {
        String currentHash = hash(refreshToken);
        Optional<SessionData> current = refreshTokenStore.findSession(currentHash);

        if (current.isEmpty()) {

            return resolveInactiveRefresh(currentHash);
        }

        SessionData currentSession = current.get();
        MemberInfo member = activeMember(currentSession.memberId(), currentSession.sessionId(), currentHash);
        String replacementToken = generateRefreshToken();
        String replacementHash = hash(replacementToken);
        DeviceInfo preserved = new DeviceInfo(currentSession.deviceName(), device.ip());
        SessionData replacement = sessionData(
                replacementHash,
                member.id(),
                currentSession.provider(),
                currentSession.sessionId(),
                currentSession.loginAt(),
                preserved);

        RotationResult result = refreshTokenStore.rotate(
                currentHash,
                replacementToken,
                replacement,
                properties.jwt().refreshTokenTtl(),
                properties.refresh().retryGrace());

        return switch (result.status()) {
            case ROTATED -> {
                log.info("auth.refresh.rotated memberId={} sessionId={} token={}",
                        member.id(), currentSession.sessionId(), fingerprint(currentHash));
                yield tokenPair(
                        member,
                        currentSession.provider(),
                        currentSession.sessionId(),
                        replacementToken,
                        properties.jwt().refreshTokenTtl());
            }
            case RETRIED -> retry(result, currentHash);
            case REUSED -> rejectReuse(result, currentHash);
            case REVOKED, MISSING -> rejectExpired(currentHash, result.status().name());
        };
    }

    public void revoke(String refreshToken) {
        String tokenHash = hash(refreshToken);

        refreshTokenStore.revokeByTokenHash(tokenHash, properties.jwt().refreshTokenTtl())
                .ifPresentOrElse(
                        session -> log.info("auth.refresh.revoked memberId={} sessionId={} reason=logout token={}",
                                session.memberId(), session.sessionId(), fingerprint(tokenHash)),
                        () -> log.info(
                                "auth.refresh.revoke_skipped reason=not_found token={}",
                                fingerprint(tokenHash)));
    }

    /** refresh 토큰의 세션 키(해시). '현재 세션' 판별용. */
    public String tokenHash(String refreshToken) {

        return hash(refreshToken);
    }

    /** 해당 회원의 모든 refresh 세션 강제 만료(전 기기). 탈퇴 시 사용. */
    public void revokeAllSessions(Long memberId) {
        long revoked = refreshTokenStore.deleteAllByMember(memberId, properties.jwt().refreshTokenTtl());

        log.info("auth.refresh.revoked_all memberId={} sessions={} reason=withdrawal", memberId, revoked);
    }

    private TokenPair resolveInactiveRefresh(String currentHash) {
        RefreshTokenStore.UsedToken used = refreshTokenStore.findUsedToken(currentHash).orElse(null);

        if (used == null) {

            return rejectExpired(currentHash, "not_found_or_expired");
        }
        if (refreshTokenStore.isSessionRevoked(used.sessionId())) {

            return rejectExpired(currentHash, "session_revoked");
        }

        // retry key의 실제 존재 여부와 replacement 활성 여부는 원자 rotate 스크립트가 최종 판정한다.
        SessionData replacement = refreshTokenStore.findSession(used.replacementHash()).orElse(null);

        if (replacement == null) {

            return rejectReuse(
                    new RotationResult(RotationStatus.REUSED, "", used.replacementHash(),
                            used.memberId(), used.sessionId(), 0),
                    currentHash);
        }

        // 후보 세션으로 rotate를 다시 호출하면 old key가 없으므로 Lua가 retry key 유무에 따라 RETRIED/REUSED를 판정한다.
        RotationResult result = refreshTokenStore.rotate(
                currentHash,
                "",
                replacement,
                properties.jwt().refreshTokenTtl(),
                properties.refresh().retryGrace());

        return switch (result.status()) {
            case RETRIED -> retry(result, currentHash);
            case REUSED -> rejectReuse(result, currentHash);
            case REVOKED, MISSING -> rejectExpired(currentHash, result.status().name());
            case ROTATED -> throw new IllegalStateException("inactive refresh token was unexpectedly rotated");
        };
    }

    private TokenPair retry(RotationResult result, String currentHash) {
        SessionData replacement = refreshTokenStore.findSession(result.replacementHash())
                .orElseThrow(() -> expired(currentHash, "replacement_missing"));
        MemberInfo member = activeMember(replacement.memberId(), replacement.sessionId(), currentHash);

        if (result.refreshTokenTtlMillis() < 1_000) {
            throw expired(currentHash, "replacement_expiring");
        }

        log.info("auth.refresh.retry_replayed memberId={} sessionId={} token={}",
                member.id(), replacement.sessionId(), fingerprint(currentHash));

        return tokenPair(
                member,
                replacement.provider(),
                replacement.sessionId(),
                result.replacementToken(),
                Duration.ofMillis(result.refreshTokenTtlMillis()));
    }

    private TokenPair rejectReuse(RotationResult result, String currentHash) {
        if (result.memberId() != null && result.sessionId() != null && !result.sessionId().isBlank()) {
            long revoked = refreshTokenStore.revokeSession(
                    result.memberId(), result.sessionId(), properties.jwt().refreshTokenTtl());

            log.warn("auth.refresh.reuse_detected memberId={} sessionId={} revokedTokens={} token={}",
                    result.memberId(), result.sessionId(), revoked, fingerprint(currentHash));
        } else {
            log.warn("auth.refresh.reuse_detected memberId=unknown sessionId=unknown token={}",
                    fingerprint(currentHash));
        }

        throw new NalssiLogException(AuthErrorCode.AUTH_REFRESH_REUSED);
    }

    private TokenPair rejectExpired(String currentHash, String reason) {
        throw expired(currentHash, reason);
    }

    private NalssiLogException expired(String currentHash, String reason) {
        log.info("auth.refresh.rejected reason={} token={}", reason, fingerprint(currentHash));

        return new NalssiLogException(AuthErrorCode.AUTH_SESSION_EXPIRED);
    }

    private MemberInfo activeMember(Long memberId, String sessionId, String tokenHash) {

        return memberClient.findMemberInfo(memberId).orElseThrow(() -> {
            refreshTokenStore.revokeSession(memberId, sessionId, properties.jwt().refreshTokenTtl());

            return expired(tokenHash, "member_missing");
        });
    }

    private TokenPair issueWithSession(Long memberId, MemberStatus status, Provider provider, String sessionId,
                                       Instant loginAt, DeviceInfo device) {
        String refreshToken = generateRefreshToken();
        String tokenHash = hash(refreshToken);
        SessionData session = sessionData(tokenHash, memberId, provider, sessionId, loginAt, device);

        refreshTokenStore.save(tokenHash, session, properties.jwt().refreshTokenTtl());

        return new TokenPair(
                jwtTokenProvider.createAccessToken(memberId, status, provider, sessionId),
                refreshToken,
                properties.jwt().refreshTokenTtl());
    }

    private SessionData sessionData(String tokenHash, Long memberId, Provider provider, String sessionId,
                                    Instant loginAt, DeviceInfo device) {

        return new SessionData(
                tokenHash,
                sessionId,
                memberId,
                provider,
                device.deviceName(),
                device.ip(),
                loginAt,
                Instant.now());
    }

    private TokenPair tokenPair(
            MemberInfo member,
            Provider provider,
            String sessionId,
            String refreshToken,
            Duration maxAge
    ) {

        return new TokenPair(
                jwtTokenProvider.createAccessToken(member.id(), member.status(), provider, sessionId),
                refreshToken,
                maxAge);
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[32];

        SECURE_RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String fingerprint(String tokenHash) {

        return tokenHash.substring(0, Math.min(FINGERPRINT_LENGTH, tokenHash.length()));
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
