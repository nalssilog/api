package com.nalssilog.member.api.dto;

import com.nalssilog.member.application.dto.SocialAccountInfo;
import com.nalssilog.member.domain.Provider;
import java.time.Instant;

public record SocialAccountResponse(Provider provider, String email, Instant lastLoginAt) {

    public static SocialAccountResponse from(SocialAccountInfo info) {
        return new SocialAccountResponse(info.provider(), info.email(), info.lastLoginAt());
    }
}
