package com.nalssilog.location.application.dto;

import com.nalssilog.location.domain.Location;

/**
 * 지역 정보 내부 계약. 전국 사전 데이터의 위·경도는 없을 수 있으며 프론트 응답에는 노출하지 않는다.
 */
public record LocationInfo(
        Long id,
        String sido,
        String sigungu,
        String dong,
        Double latitude,
        Double longitude
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
