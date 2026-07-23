package com.nalssilog.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nalssilog.auth")
public record AuthProperties(Jwt jwt, Cookie cookie, Ticket ticket, Csrf csrf) {

    public record Jwt(String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {
    }

    /** 인증 쿠키(access·refresh·ticket)는 항상 host-only(Domain 생략). secure 만 env 별. */
    public record Cookie(boolean secure) {
    }

    public record Ticket(Duration ttl) {
    }

    /** CSRF 쿠키는 프론트 JS 가 서브도메인 넘어 읽어야 해서 env 별 이름 분리 + Domain 지정. */
    public record Csrf(String cookieName, String cookieDomain) {
    }
}
