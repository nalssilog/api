package com.nalssilog.report.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nalssilog.report.rate-limit")
public record ReportRateLimitProperties(
        int createMaxRequests,
        Duration createWindow,
        int createDailyMaxRequests,
        int presignMaxRequests,
        Duration presignWindow,
        int presignDailyMaxRequests,
        int flagMaxRequests,
        Duration flagWindow,
        int flagDailyMaxRequests,
        int ipMultiplier,
        String hmacSecret,
        List<String> trustedProxies
) {

    public ReportRateLimitProperties {
        createMaxRequests = positiveOr(createMaxRequests, 5);
        createWindow = validOr(createWindow, Duration.ofMinutes(10));
        createDailyMaxRequests = positiveOr(createDailyMaxRequests, 20);
        presignMaxRequests = positiveOr(presignMaxRequests, 15);
        presignWindow = validOr(presignWindow, Duration.ofMinutes(10));
        presignDailyMaxRequests = positiveOr(presignDailyMaxRequests, 60);
        flagMaxRequests = positiveOr(flagMaxRequests, 10);
        flagWindow = validOr(flagWindow, Duration.ofMinutes(10));
        flagDailyMaxRequests = positiveOr(flagDailyMaxRequests, 50);
        ipMultiplier = positiveOr(ipMultiplier, 6);
        hmacSecret = hmacSecret == null || hmacSecret.isBlank()
                ? null
                : hmacSecret;
        trustedProxies = trustedProxies == null ? List.of() : List.copyOf(trustedProxies);
    }

    private static int positiveOr(int value, int fallback) {
        return value > 0 ? value : fallback;
    }

    private static Duration validOr(Duration value, Duration fallback) {
        return value == null || value.isNegative() || value.isZero() ? fallback : value;
    }
}
