package com.nalssilog.auth.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "nalssilog.auth")
public record AuthProperties(
        Jwt jwt,
        Cookie cookie,
        Ticket ticket,
        Csrf csrf,
        Refresh refresh,
        Mobile mobile,
        Guest guest
) {

    @ConstructorBinding
    public AuthProperties {
        if (refresh == null) {
            refresh = new Refresh(Duration.ofSeconds(5));
        }

        String ipHmacFallback = jwt != null
                && jwt.secret() != null
                && !jwt.secret().isBlank()
                ? jwt.secret()
                : "local-mobile-auth-ip-key";

        if (mobile == null) {
            mobile = Mobile.defaults(ipHmacFallback);
        } else if (mobile.ipHmacSecret() == null
                || mobile.ipHmacSecret().isBlank()) {
            mobile = mobile.withIpHmacSecret(ipHmacFallback);
        }
        if (guest == null) {
            guest = Guest.defaults();
        }
    }

    public AuthProperties(Jwt jwt, Cookie cookie, Ticket ticket, Csrf csrf, Refresh refresh) {
        this(jwt, cookie, ticket, csrf, refresh, null, Guest.defaults());
    }

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

    /** refresh rotation 중 네트워크 재시도·동시 요청을 같은 결과로 수렴시키는 짧은 멱등 구간. */
    public record Refresh(Duration retryGrace) {

        public Refresh {
            if (retryGrace == null || retryGrace.isNegative() || retryGrace.isZero()) {
                retryGrace = Duration.ofSeconds(5);
            }
        }
    }

    public record Mobile(
            List<String> redirectUris,
            Duration transactionTtl,
            Duration codeTtl,
            String ipHmacSecret,
            List<String> trustedProxies
    ) {

        public Mobile {
            redirectUris = redirectUris == null || redirectUris.isEmpty()
                    ? List.of()
                    : List.copyOf(redirectUris);
            if (transactionTtl == null || transactionTtl.isNegative() || transactionTtl.isZero()) {
                transactionTtl = Duration.ofMinutes(10);
            }
            if (codeTtl == null || codeTtl.isNegative() || codeTtl.isZero()) {
                codeTtl = Duration.ofSeconds(90);
            }
            trustedProxies = trustedProxies == null || trustedProxies.isEmpty()
                    ? List.of("127.0.0.0/8", "::1/128", "172.16.0.0/12")
                    : List.copyOf(trustedProxies);
        }

        private Mobile withIpHmacSecret(String secret) {

            return new Mobile(
                    redirectUris,
                    transactionTtl,
                    codeTtl,
                    secret,
                    trustedProxies);
        }

        private static Mobile defaults(String ipHmacSecret) {

            return new Mobile(null, null, null, ipHmacSecret, null);
        }
    }

    public record Guest(
            Duration ttl,
            int maxIssuances,
            Duration rateLimitWindow,
            int globalMaxIssuances,
            Duration globalRateLimitWindow,
            Duration expiredRetention,
            Duration cleanupInterval,
            Duration cleanupInitialDelay
    ) {

        public Guest {
            if (ttl == null || ttl.isNegative() || ttl.isZero()) {
                ttl = Duration.ofDays(365);
            }
            if (maxIssuances <= 0) {
                maxIssuances = 300;
            }
            if (rateLimitWindow == null || rateLimitWindow.isNegative() || rateLimitWindow.isZero()) {
                rateLimitWindow = Duration.ofMinutes(10);
            }
            if (globalMaxIssuances <= 0) {
                globalMaxIssuances = 3_000;
            }
            if (globalRateLimitWindow == null
                    || globalRateLimitWindow.isNegative()
                    || globalRateLimitWindow.isZero()) {
                globalRateLimitWindow = Duration.ofMinutes(1);
            }
            if (expiredRetention == null || expiredRetention.isNegative()) {
                expiredRetention = Duration.ofDays(7);
            }
            if (cleanupInterval == null || cleanupInterval.isNegative() || cleanupInterval.isZero()) {
                cleanupInterval = Duration.ofHours(6);
            }
            if (cleanupInitialDelay == null
                    || cleanupInitialDelay.isNegative()
                    || cleanupInitialDelay.isZero()) {
                cleanupInitialDelay = Duration.ofMinutes(1);
            }
        }

        public Guest(
                Duration ttl,
                int maxIssuances,
                Duration rateLimitWindow
        ) {
            this(ttl, maxIssuances, rateLimitWindow, 0, null, null, null, null);
        }

        private static Guest defaults() {

            return new Guest(null, 0, null, 0, null, null, null, null);
        }
    }
}
