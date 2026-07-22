package com.nalssilog.report.domain;

import com.nalssilog.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReportErrorCode implements ErrorCode {

    REPORT_NOT_FOUND("REPORT_NOT_FOUND", "제보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_CURSOR("INVALID_CURSOR", "잘못된 커서입니다.", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_IMAGE_TYPE("UNSUPPORTED_IMAGE_TYPE", "지원하지 않는 이미지 형식입니다.", HttpStatus.BAD_REQUEST),
    IMAGE_TOO_LARGE("IMAGE_TOO_LARGE", "이미지 한 장은 5MB 이하여야 합니다.", HttpStatus.BAD_REQUEST),
    TOO_MANY_IMAGES("TOO_MANY_IMAGES", "사진은 최대 3장까지 첨부할 수 있습니다.", HttpStatus.BAD_REQUEST),
    INVALID_IMAGE_KEY("INVALID_IMAGE_KEY", "잘못된 이미지 키입니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
