package com.nalssilog.member.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

@SuppressWarnings("java:S5960")
class TrustedProxyClientIpResolverTest {

    private static final String UNTRUSTED_PEER = ipv4(198, 51, 100, 20);
    private static final String SPOOFED_CLIENT = ipv4(1, 2, 3, 4);
    private static final String TRUSTED_PEER = ipv4(172, 17, 0, 1);
    private static final String TRUSTED_HOP = ipv4(172, 18, 0, 2);
    private static final String CLIENT = ipv4(203, 0, 113, 10);
    private static final String LOOPBACK_RANGE = cidr(ipv4(127, 0, 0, 0), 8);
    private static final String PRIVATE_RANGE = cidr(ipv4(172, 16, 0, 0), 12);

    private final TrustedProxyClientIpResolver resolver = new TrustedProxyClientIpResolver(
            new FeedbackRateLimitProperties(
                    5,
                    Duration.ofMinutes(10),
                    "test-secret",
                    List.of(LOOPBACK_RANGE, PRIVATE_RANGE)));

    @Test
    void ignoresForwardedHeaderFromUntrustedSocketPeer() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setRemoteAddr(UNTRUSTED_PEER);
        request.addHeader("X-Forwarded-For", SPOOFED_CLIENT);

        assertThat(resolver.resolve(request)).isEqualTo(UNTRUSTED_PEER);
    }

    @Test
    void selectsRightmostUntrustedHopBehindTrustedProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setRemoteAddr(TRUSTED_PEER);
        request.addHeader(
                "X-Forwarded-For",
                String.join(", ", SPOOFED_CLIENT, CLIENT));

        assertThat(resolver.resolve(request)).isEqualTo(CLIENT);
    }

    @Test
    void walksAcrossMultipleTrustedProxies() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setRemoteAddr(TRUSTED_PEER);
        request.addHeader(
                "X-Forwarded-For",
                String.join(", ", CLIENT, TRUSTED_HOP));

        assertThat(resolver.resolve(request)).isEqualTo(CLIENT);
    }

    private static String ipv4(int first, int second, int third, int fourth) {
        return "%d.%d.%d.%d".formatted(first, second, third, fourth);
    }

    private static String cidr(String address, int prefixLength) {
        return address + "/" + prefixLength;
    }
}
