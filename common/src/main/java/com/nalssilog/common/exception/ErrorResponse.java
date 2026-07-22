package com.nalssilog.common.exception;

/**
 * 공통 에러 응답. code 는 프론트 분기용 안정 문자열, message 는 사용자 표시용.
 */
public record ErrorResponse(String code, String message) {
}
