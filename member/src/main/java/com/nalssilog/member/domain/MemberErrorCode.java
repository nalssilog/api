package com.nalssilog.member.domain;

import com.nalssilog.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {

    MEMBER_NOT_FOUND("MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_NICKNAME("NICKNAME_DUPLICATED", "이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT),
    NICKNAME_GENERATION_FAILED("NICKNAME_GENERATION_FAILED", "기본 닉네임을 생성하지 못했습니다.",
            HttpStatus.INTERNAL_SERVER_ERROR),
    ALREADY_ONBOARDED("ALREADY_ONBOARDED", "이미 가입이 완료된 회원입니다.", HttpStatus.CONFLICT),
    ALREADY_WITHDRAWN("ALREADY_WITHDRAWN", "이미 탈퇴한 회원입니다.", HttpStatus.CONFLICT),
    SOCIAL_ACCOUNT_IN_USE("SOCIAL_ACCOUNT_IN_USE", "이미 다른 계정에 연동된 소셜 로그인입니다.", HttpStatus.CONFLICT),
    ACCOUNT_ALREADY_LINKED("ACCOUNT_ALREADY_LINKED", "이미 연동된 소셜 계정입니다.", HttpStatus.CONFLICT),
    SOCIAL_ACCOUNT_NOT_FOUND("SOCIAL_ACCOUNT_NOT_FOUND", "연동되지 않은 소셜 계정입니다.", HttpStatus.NOT_FOUND),
    LAST_SOCIAL_ACCOUNT("LAST_SOCIAL_ACCOUNT", "마지막 로그인 수단은 연동 해제할 수 없습니다.", HttpStatus.CONFLICT),
    INVALID_AVATAR("INVALID_AVATAR", "아바타 정보가 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_IMAGE_TYPE("UNSUPPORTED_IMAGE_TYPE", "지원하지 않는 이미지 형식입니다.", HttpStatus.BAD_REQUEST),
    IMAGE_TOO_LARGE("IMAGE_TOO_LARGE", "이미지는 2MB 이하여야 합니다.", HttpStatus.BAD_REQUEST),
    INVALID_IMAGE_KEY("INVALID_IMAGE_KEY", "잘못된 이미지 키입니다.", HttpStatus.BAD_REQUEST),
    IMAGE_NOT_FOUND("IMAGE_NOT_FOUND", "업로드된 이미지를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST),
    TERMS_NOT_AGREED("TERMS_NOT_AGREED", "필수 약관에 모두 동의해야 합니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
