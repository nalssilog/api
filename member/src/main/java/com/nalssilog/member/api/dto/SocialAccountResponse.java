package com.nalssilog.member.api.dto;

import com.nalssilog.member.application.dto.SocialAccountInfo;
import com.nalssilog.member.domain.Provider;
import java.time.Instant;

/** 추가 연동 후 아직 해당 제공자로 로그인하지 않았다면 lastLoginAt 은 null. */
public record SocialAccountResponse(Provider provider, String email, Instant lastLoginAt) {

    public static SocialAccountResponse from(SocialAccountInfo info) {
        return new SocialAccountResponse(info.provider(), info.email(), info.lastLoginAt());
    }
}
