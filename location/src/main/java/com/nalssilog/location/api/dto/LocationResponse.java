package com.nalssilog.location.api.dto;

import com.nalssilog.location.application.dto.LocationInfo;

/**
 * 프론트 응답. 위경도는 노출하지 않고, 시도/시군구/동을 분리해 주며 label 은 조합된 풀 문자열.
 * id 는 JS 안전 정수 초과 방지를 위해 문자열로 내려준다.
 */
public record LocationResponse(
        String id,
        String sido,
        String sigungu,
        String dong,
        String label
) {

    public static LocationResponse from(LocationInfo info) {
        return new LocationResponse(
                String.valueOf(info.id()), info.sido(), info.sigungu(), info.dong(), info.label());
    }
}
