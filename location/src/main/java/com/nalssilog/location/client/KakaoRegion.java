package com.nalssilog.location.client;

public record KakaoRegion(
        String adminCode,
        String sido,
        String sigungu,
        String dong,
        double latitude,
        double longitude
) {
}
