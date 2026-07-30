package com.nalssilog.auth.core;

import com.nalssilog.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    AUTH_ACCESS_TOKEN_EXPIRED("AUTH_ACCESS_TOKEN_EXPIRED", "액세스 토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),
    AUTH_ACCESS_TOKEN_INVALID("AUTH_ACCESS_TOKEN_INVALID", "유효하지 않은 액세스 토큰입니다.", HttpStatus.UNAUTHORIZED),
    AUTH_SESSION_EXPIRED("AUTH_SESSION_EXPIRED", "인증 세션이 만료되었습니다. 다시 로그인해 주세요.",
            HttpStatus.UNAUTHORIZED),
    AUTH_REFRESH_REUSED("AUTH_REFRESH_REUSED", "재사용된 인증 세션이 감지되어 세션을 폐기했습니다.",
            HttpStatus.UNAUTHORIZED),
    TICKET_NOT_FOUND("AUTH_SESSION_EXPIRED", "인증 세션이 만료되었습니다. 다시 로그인해 주세요.",
            HttpStatus.BAD_REQUEST),
    UNSUPPORTED_PROVIDER("OAUTH_FAILED", "지원하지 않는 소셜 로그인입니다.", HttpStatus.BAD_REQUEST),
    OAUTH_FAILED("OAUTH_FAILED", "소셜 로그인에 실패했습니다.", HttpStatus.UNAUTHORIZED),
    OAUTH_CANCELLED("OAUTH_CANCELLED", "소셜 로그인이 취소되었습니다.", HttpStatus.UNAUTHORIZED),
    OAUTH_EMAIL_REQUIRED("OAUTH_EMAIL_REQUIRED", "소셜 계정 이메일 제공 동의가 필요합니다.",
            HttpStatus.BAD_REQUEST),
    AUTH_MOBILE_CODE_INVALID("AUTH_MOBILE_CODE_INVALID", "모바일 인증 코드가 만료되었거나 유효하지 않습니다.",
            HttpStatus.BAD_REQUEST),
    AUTH_PKCE_VERIFICATION_FAILED("AUTH_PKCE_VERIFICATION_FAILED", "PKCE 검증에 실패했습니다.",
            HttpStatus.BAD_REQUEST),
    AUTH_REDIRECT_URI_INVALID("AUTH_REDIRECT_URI_INVALID", "허용되지 않은 모바일 리다이렉트 URI입니다.",
            HttpStatus.BAD_REQUEST),
    AUTH_MOBILE_TRANSACTION_EXPIRED("AUTH_MOBILE_TRANSACTION_EXPIRED", "모바일 로그인 요청이 만료되었습니다.",
            HttpStatus.BAD_REQUEST),
    AUTH_FLOW_IN_PROGRESS("AUTH_FLOW_IN_PROGRESS", "인증 요청을 처리 중입니다.", HttpStatus.CONFLICT),
    AUTH_TICKET_CHANNEL_MISMATCH("AUTH_TICKET_CHANNEL_MISMATCH", "이 인증 티켓은 현재 요청에서 사용할 수 없습니다.",
            HttpStatus.BAD_REQUEST),
    GUEST_CREDENTIAL_INVALID("GUEST_CREDENTIAL_INVALID", "유효하지 않은 게스트 인증 정보입니다.",
            HttpStatus.UNAUTHORIZED),
    GUEST_CREDENTIAL_EXPIRED("GUEST_CREDENTIAL_EXPIRED", "게스트 인증 정보가 만료되었습니다.",
            HttpStatus.UNAUTHORIZED),
    GUEST_ISSUANCE_RATE_LIMITED("GUEST_ISSUANCE_RATE_LIMITED", "게스트 인증 정보 발급 요청이 너무 많습니다.",
            HttpStatus.TOO_MANY_REQUESTS),
    GUEST_ISSUANCE_UNAVAILABLE("GUEST_ISSUANCE_UNAVAILABLE", "게스트 인증 정보를 발급할 수 없습니다.",
            HttpStatus.SERVICE_UNAVAILABLE),
    SESSION_NOT_FOUND("SESSION_NOT_FOUND", "세션을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ALREADY_LINKED_PROVIDER("ACCOUNT_ALREADY_LINKED", "이미 연결된 소셜 로그인입니다.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
