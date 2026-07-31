package com.nalssilog.location.domain;

import com.nalssilog.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum LocationErrorCode implements ErrorCode {

    LOCATION_NOT_FOUND("LOCATION_NOT_FOUND", "지역을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    FAVORITE_ALREADY_EXISTS(
            "FAVORITE_ALREADY_EXISTS",
            "이미 즐겨찾기에 추가한 지역입니다.",
            HttpStatus.CONFLICT),
    INVALID_COORDINATES("INVALID_COORDINATES", "위도 또는 경도가 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    KAKAO_MAP_API_UNAVAILABLE("KAKAO_MAP_API_UNAVAILABLE", "위치 정보를 불러오지 못했습니다.",
            HttpStatus.BAD_GATEWAY);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
