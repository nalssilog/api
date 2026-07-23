package com.nalssilog.auth.domain;

import com.nalssilog.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    AUTH_SESSION_EXPIRED("AUTH_SESSION_EXPIRED", "인증 세션이 만료되었습니다. 다시 로그인해 주세요.", HttpStatus.UNAUTHORIZED),
    TICKET_NOT_FOUND("AUTH_SESSION_EXPIRED", "인증 세션이 만료되었습니다. 다시 로그인해 주세요.", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_PROVIDER("OAUTH_FAILED", "지원하지 않는 소셜 로그인입니다.", HttpStatus.BAD_REQUEST),
    OAUTH_FAILED("OAUTH_FAILED", "소셜 로그인에 실패했습니다.", HttpStatus.UNAUTHORIZED),
    OAUTH_CANCELLED("OAUTH_CANCELLED", "소셜 로그인이 취소되었습니다.", HttpStatus.UNAUTHORIZED),
    SESSION_NOT_FOUND("SESSION_NOT_FOUND", "세션을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ALREADY_LINKED_PROVIDER("ACCOUNT_ALREADY_LINKED", "이미 연동된 소셜 로그인입니다.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
