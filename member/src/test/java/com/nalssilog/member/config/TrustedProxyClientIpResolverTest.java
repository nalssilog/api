package com.nalssilog.member.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

@SuppressWarnings("java:S5960")
class TrustedProxyClientIpResolverTest {

    private final TrustedProxyClientIpResolver resolver = new TrustedProxyClientIpResolver(
            new FeedbackRateLimitProperties(
                    5,
                    Duration.ofMinutes(10),
                    "test-secret",
                    List.of("127.0.0.0/8", "172.16.0.0/12")));

    @Test
    void ignoresForwardedHeaderFromUntrustedSocketPeer() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.20");
        request.addHeader("X-Forwarded-For", "1.2.3.4");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.20");
    }

    @Test
    void selectsRightmostUntrustedHopBehindTrustedProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.17.0.1");
        request.addHeader("X-Forwarded-For", "1.2.3.4, 203.0.113.10");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void walksAcrossMultipleTrustedProxies() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.17.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 172.18.0.2");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.10");
    }
}
