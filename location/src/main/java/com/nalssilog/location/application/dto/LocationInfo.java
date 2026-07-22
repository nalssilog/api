package com.nalssilog.location.application.dto;

import com.nalssilog.location.domain.Location;

/**
 * 지역 정보 내부 계약. 위·경도는 내부용(거리·역지오코딩)이고 프론트 응답 DTO 로는 넘기지 않는다.
 */
public record LocationInfo(
        Long id,
        String sido,
        String sigungu,
        String dong,
        double latitude,
        double longitude
) {

    public static LocationInfo of(Location location) {
        return new LocationInfo(
                location.getId(),
                location.getSido(),
                location.getSigungu(),
                location.getDong(),
                location.getLatitude(),
                location.getLongitude()
        );
    }

    public String label() {
        return sido + " " + sigungu + " " + dong;
    }
}
