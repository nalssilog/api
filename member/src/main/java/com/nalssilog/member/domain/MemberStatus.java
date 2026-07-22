package com.nalssilog.member.domain;

public enum MemberStatus {
    /** 소셜 인증은 됐지만 온보딩(닉네임·약관 동의)을 마치지 않은 가입 대기 상태 */
    PENDING,
    ACTIVE,
    WITHDRAWN
}
