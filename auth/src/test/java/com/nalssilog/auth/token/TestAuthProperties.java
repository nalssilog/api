package com.nalssilog.auth.token;

import com.nalssilog.auth.config.AuthProperties;
import java.time.Duration;

final class TestAuthProperties {

    private TestAuthProperties() {
    }

    static AuthProperties create() {

        return create(Duration.ofMinutes(5));
    }

    static AuthProperties create(Duration accessTokenTtl) {

        return new AuthProperties(
                new AuthProperties.Jwt(
                        "test-secret-must-be-at-least-thirty-two-bytes",
                        accessTokenTtl,
                        Duration.ofDays(14)),
                new AuthProperties.Cookie(false),
                new AuthProperties.Ticket(Duration.ofMinutes(10)),
                new AuthProperties.Csrf("XSRF-TOKEN", null),
                new AuthProperties.Refresh(Duration.ofSeconds(5)));
    }
}
