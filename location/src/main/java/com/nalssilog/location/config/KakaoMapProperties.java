package com.nalssilog.location.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("nalssilog.location.kakao")
public record KakaoMapProperties(
        String baseUrl,
        String reverseGeocodePath,
        String restApiKey,
        Duration connectTimeout,
        Duration readTimeout
) {

    private static final String DEFAULT_BASE_URL = "https://dapi.kakao.com";
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(3);

    public KakaoMapProperties {
        baseUrl = baseUrl == null || baseUrl.isBlank() ? DEFAULT_BASE_URL : baseUrl.strip();
        if (reverseGeocodePath == null || reverseGeocodePath.isBlank()) {
            throw new IllegalArgumentException("Kakao reverse-geocode-path must be configured");
        }
        reverseGeocodePath = reverseGeocodePath.strip();
        restApiKey = restApiKey == null ? "" : restApiKey.strip();
        connectTimeout = connectTimeout == null ? DEFAULT_CONNECT_TIMEOUT : connectTimeout;
        readTimeout = readTimeout == null ? DEFAULT_READ_TIMEOUT : readTimeout;
    }
}
