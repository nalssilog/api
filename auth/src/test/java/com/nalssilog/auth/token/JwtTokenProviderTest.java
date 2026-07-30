package com.nalssilog.auth.token;

import static org.assertj.core.api.Assertions.assertThat;

import com.nalssilog.auth.config.AuthProperties;
import com.nalssilog.member.domain.MemberStatus;
import com.nalssilog.member.domain.Provider;
import java.time.Duration;
import org.junit.jupiter.api.Test;

@SuppressWarnings("java:S5960") // 표준 src/test 소스의 AssertJ 검증을 운영 코드 assertion으로 오인하는 경고.
class JwtTokenProviderTest {

    @Test
    void accessTokenPreservesAuthenticatedProvider() {
        JwtTokenProvider provider = new JwtTokenProvider(properties());

        String token = provider.createAccessToken(1L, MemberStatus.ACTIVE, Provider.KAKAO);

        assertThat(provider.parse(token))
                .contains(new JwtTokenProvider.AccessTokenPayload(
                        1L, MemberStatus.ACTIVE, Provider.KAKAO));
    }

    @Test
    void accessTokenCarriesTheServerIssuedSessionId() {
        JwtTokenProvider provider = new JwtTokenProvider(properties());

        String token = provider.createAccessToken(
                1L,
                MemberStatus.ACTIVE,
                Provider.KAKAO,
                "session-1");

        assertThat(provider.parse(token))
                .contains(new JwtTokenProvider.AccessTokenPayload(
                        1L,
                        MemberStatus.ACTIVE,
                        Provider.KAKAO,
                        "session-1"));
    }

    private AuthProperties properties() {

        return new AuthProperties(
                new AuthProperties.Jwt(
                        "test-secret-must-be-at-least-thirty-two-bytes",
                        Duration.ofMinutes(5),
                        Duration.ofDays(14)),
                new AuthProperties.Cookie(false),
                new AuthProperties.Ticket(Duration.ofMinutes(10)),
                new AuthProperties.Csrf("XSRF-TOKEN", null),
                new AuthProperties.Refresh(Duration.ofSeconds(5))
        );
    }
}
