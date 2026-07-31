package com.nalssilog.auth.token;

import com.nalssilog.auth.config.AuthProperties;
import com.nalssilog.member.domain.MemberStatus;
import com.nalssilog.member.domain.Provider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
// S2143: jjwt 빌더가 Instant 오버로드를 제공하지 않아 토큰 발급 경계에서만 java.util.Date 로 변환(내부 계산은 java.time).
@SuppressWarnings("java:S2143")
public class JwtTokenProvider {

    private final SecretKey key;
    private final Duration accessTokenTtl;

    public JwtTokenProvider(AuthProperties properties) {
        String secret = properties.jwt().secret();

        this.key = resolveKey(secret);
        this.accessTokenTtl = properties.jwt().accessTokenTtl();
    }

    private static SecretKey resolveKey(String secret) {
        if (secret == null || secret.isBlank()) {
            log.warn("JWT secret 이 설정되지 않아 임시 키를 생성합니다. 재시작하면 모든 액세스 토큰이 무효화됩니다.");

            return Jwts.SIG.HS256.key().build();
        }

        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long memberId, MemberStatus status, Provider provider) {
        return createAccessToken(memberId, status, provider, null);
    }

    public String createAccessToken(
            Long memberId,
            MemberStatus status,
            Provider provider,
            String sessionId
    ) {
        Instant now = Instant.now();

        var builder = Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim("status", status.name())
                .claim("provider", provider.name())
                .claim("token_type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)));

        if (sessionId != null && !sessionId.isBlank()) {
            builder.claim("sid", sessionId);
        }

        return builder
                .signWith(key)
                .compact();
    }

    public Optional<AccessTokenPayload> parse(String token) {
        TokenValidation validation = validate(token);

        return validation.status() == TokenValidationStatus.VALID
                ? Optional.of(validation.payload())
                : Optional.empty();
    }

    public TokenValidation validate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String status = claims.get("status", String.class);
            String provider = claims.get("provider", String.class);
            String tokenType = claims.get("token_type", String.class);

            if (status == null || provider == null
                    || (tokenType != null && !"access".equals(tokenType))) {
                return TokenValidation.invalid();
            }

            return TokenValidation.valid(new AccessTokenPayload(
                    Long.parseLong(claims.getSubject()),
                    MemberStatus.valueOf(status),
                    Provider.valueOf(provider),
                    claims.get("sid", String.class)
            ));
        } catch (ExpiredJwtException _) {
            return TokenValidation.expired();
        } catch (JwtException | IllegalArgumentException _) {
            return TokenValidation.invalid();
        }
    }

    public enum TokenValidationStatus {
        VALID,
        EXPIRED,
        INVALID
    }

    public record TokenValidation(TokenValidationStatus status, AccessTokenPayload payload) {

        private static TokenValidation valid(AccessTokenPayload payload) {
            return new TokenValidation(TokenValidationStatus.VALID, payload);
        }

        private static TokenValidation expired() {
            return new TokenValidation(TokenValidationStatus.EXPIRED, null);
        }

        private static TokenValidation invalid() {
            return new TokenValidation(TokenValidationStatus.INVALID, null);
        }
    }

    public record AccessTokenPayload(
            Long memberId,
            MemberStatus status,
            Provider provider,
            String sessionId
    ) {

        public AccessTokenPayload(Long memberId, MemberStatus status, Provider provider) {
            this(memberId, status, provider, null);
        }
    }
}
