package com.nalssilog.report.domain;

import com.nalssilog.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReportErrorCode implements ErrorCode {

    TERMS_NOT_AGREED("TERMS_NOT_AGREED", "필수 약관에 모두 동의해야 합니다.", HttpStatus.BAD_REQUEST),
    REPORT_RATE_LIMITED("REPORT_RATE_LIMITED", "제보를 너무 자주 작성했습니다. 잠시 후 다시 시도해 주세요.",
            HttpStatus.TOO_MANY_REQUESTS),
    IMAGE_PRESIGN_RATE_LIMITED("IMAGE_PRESIGN_RATE_LIMITED", "이미지 업로드 요청이 너무 많습니다.",
            HttpStatus.TOO_MANY_REQUESTS),
    REPORT_FLAG_RATE_LIMITED("REPORT_FLAG_RATE_LIMITED", "신고를 너무 자주 보냈습니다. 잠시 후 다시 시도해 주세요.",
            HttpStatus.TOO_MANY_REQUESTS),
    RATE_LIMIT_UNAVAILABLE("RATE_LIMIT_UNAVAILABLE", "요청 제한을 확인할 수 없습니다. 잠시 후 다시 시도해 주세요.",
            HttpStatus.SERVICE_UNAVAILABLE),
    REPORT_ALREADY_FLAGGED("REPORT_ALREADY_FLAGGED", "이미 신고한 제보입니다.", HttpStatus.CONFLICT),
    CANNOT_FLAG_OWN_REPORT("CANNOT_FLAG_OWN_REPORT", "본인의 제보는 신고할 수 없습니다.", HttpStatus.BAD_REQUEST),
    REPORT_FLAG_NOT_FOUND("REPORT_FLAG_NOT_FOUND", "신고 내역을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    REPORT_FLAG_ALREADY_PROCESSED("REPORT_FLAG_ALREADY_PROCESSED", "이미 처리된 신고입니다.", HttpStatus.CONFLICT),
    INVALID_REPORT_FLAG_STATUS("INVALID_REPORT_FLAG_STATUS", "신고 처리 상태가 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    CANNOT_BLOCK_SELF("CANNOT_BLOCK_SELF", "본인은 차단할 수 없습니다.", HttpStatus.BAD_REQUEST),
    BLOCK_MEMBER_REQUIRED("BLOCK_MEMBER_REQUIRED", "차단 기능은 로그인한 회원만 사용할 수 있습니다.",
            HttpStatus.FORBIDDEN),
    BLOCK_TARGET_MEMBER_NOT_FOUND("BLOCK_TARGET_MEMBER_NOT_FOUND", "차단할 회원을 찾을 수 없습니다.",
            HttpStatus.NOT_FOUND),
    BLOCK_LIMIT_REACHED("BLOCK_LIMIT_REACHED", "차단 가능한 작성자 수를 초과했습니다.", HttpStatus.CONFLICT),
    ACTOR_POSTING_RESTRICTED("ACTOR_POSTING_RESTRICTED", "운영 정책에 따라 제보 작성이 제한되었습니다.",
            HttpStatus.FORBIDDEN),
    AUTHOR_ALREADY_RESTRICTED("AUTHOR_ALREADY_RESTRICTED", "이미 작성 제한 중인 작성자입니다.", HttpStatus.CONFLICT),
    AUTHOR_RESTRICTION_NOT_FOUND("AUTHOR_RESTRICTION_NOT_FOUND", "활성 작성 제한을 찾을 수 없습니다.",
            HttpStatus.NOT_FOUND),
    INVALID_RESTRICTION_EXPIRY("INVALID_RESTRICTION_EXPIRY", "작성 제한 만료 시각은 현재보다 이후여야 합니다.",
            HttpStatus.BAD_REQUEST),
    REPORT_NOT_FOUND("REPORT_NOT_FOUND", "제보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    REPORT_DELETE_FORBIDDEN("REPORT_DELETE_FORBIDDEN", "본인이 작성한 제보만 삭제할 수 있습니다.",
            HttpStatus.FORBIDDEN),
    POPULAR_SNAPSHOT_LOCK_UNAVAILABLE(
            "POPULAR_SNAPSHOT_LOCK_UNAVAILABLE",
            "인기 지역 순위를 갱신할 수 없습니다.",
            HttpStatus.SERVICE_UNAVAILABLE),
    INVALID_CURSOR("INVALID_CURSOR", "잘못된 커서입니다.", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_IMAGE_TYPE("UNSUPPORTED_IMAGE_TYPE", "지원하지 않는 이미지 형식입니다.", HttpStatus.BAD_REQUEST),
    IMAGE_TOO_LARGE("IMAGE_TOO_LARGE", "이미지 한 장은 5MB 이하여야 합니다.", HttpStatus.BAD_REQUEST),
    TOO_MANY_IMAGES("TOO_MANY_IMAGES", "사진은 최대 3장까지 첨부할 수 있습니다.", HttpStatus.BAD_REQUEST),
    INVALID_IMAGE_KEY("INVALID_IMAGE_KEY", "잘못된 이미지 키입니다.", HttpStatus.BAD_REQUEST),
    IMAGE_NOT_FOUND("IMAGE_NOT_FOUND", "업로드된 이미지를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
