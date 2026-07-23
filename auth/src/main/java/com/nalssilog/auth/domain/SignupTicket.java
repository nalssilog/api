package com.nalssilog.auth.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.nalssilog.member.domain.Provider;

/**
 * 소셜 인증은 끝났지만 아직 가입을 확정하지 않은 신규 사용자의 임시 상태. (Redis 단기 보관)
 */
public record SignupTicket(
        Provider provider,
        String providerUserId,
        String email,
        @JsonAlias("nickname") String socialName
) {
}
