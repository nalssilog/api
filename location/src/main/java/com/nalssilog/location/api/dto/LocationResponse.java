package com.nalssilog.location.api.dto;

import com.nalssilog.location.application.dto.LocationInfo;

/**
 * 프론트 응답. label 은 전체 주소, shortLabel 은 시도를 제외한 축약 주소다.
 * id 는 JS 안전 정수 초과 방지를 위해 문자열로 내려준다.
 */
public record LocationResponse(
        String id,
        String sido,
        String sigungu,
        String dong,
        String label,
        String shortLabel
) {

    public static LocationResponse from(LocationInfo info) {
        return new LocationResponse(
                String.valueOf(info.id()),
                info.sido(),
                info.sigungu(),
                info.dong(),
                info.label(),
                info.shortLabel());
    }
}
