package com.nalssilog.member.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nalssilog.feedback.rate-limit")
public record FeedbackRateLimitProperties(
        int maxSubmissions,
        Duration window,
        String ipHmacSecret,
        List<String> trustedProxies
) {

    public FeedbackRateLimitProperties {
        if (maxSubmissions <= 0) {
            maxSubmissions = 5;
        }
        if (window == null || window.isNegative() || window.isZero()) {
            window = Duration.ofMinutes(10);
        }
        if (ipHmacSecret == null || ipHmacSecret.isBlank()) {
            ipHmacSecret = "local-feedback-rate-limit-key";
        }
        if (trustedProxies == null || trustedProxies.isEmpty()) {
            trustedProxies = List.of("127.0.0.0/8", "::1/128", "172.16.0.0/12");
        } else {
            trustedProxies = List.copyOf(trustedProxies);
        }
    }
}
