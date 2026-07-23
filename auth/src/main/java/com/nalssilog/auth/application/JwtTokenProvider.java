package com.nalssilog.auth.application;

import com.nalssilog.auth.config.AuthProperties;
import com.nalssilog.member.domain.MemberStatus;
import com.nalssilog.member.domain.Provider;
import io.jsonwebtoken.Claims;
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
        if (secret == null || secret.isBlank()) {
            log.warn("JWT secret 이 설정되지 않아 임시 키를 생성합니다. 재시작하면 모든 액세스 토큰이 무효화됩니다.");
            this.key = Jwts.SIG.HS256.key().build();
        } else {
            this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
        this.accessTokenTtl = properties.jwt().accessTokenTtl();
    }

    public String createAccessToken(Long memberId, MemberStatus status, Provider provider) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim("status", status.name())
                .claim("provider", provider.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(key)
                .compact();
    }

    public Optional<AccessTokenPayload> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String status = claims.get("status", String.class);
            String provider = claims.get("provider", String.class);

            if (status == null || provider == null) {
                return Optional.empty();
            }

            return Optional.of(new AccessTokenPayload(
                    Long.parseLong(claims.getSubject()),
                    MemberStatus.valueOf(status),
                    Provider.valueOf(provider)
            ));
        } catch (JwtException | IllegalArgumentException _) {
            return Optional.empty();
        }
    }

    public record AccessTokenPayload(Long memberId, MemberStatus status, Provider provider) {
    }
}
