package com.nalssilog.member.application.dto;

import com.nalssilog.member.domain.Provider;
import com.nalssilog.member.domain.SocialAccount;
import java.time.Instant;

/**
 * 회원에 연동된 소셜 계정 한 건의 정보.
 */
public record SocialAccountInfo(Provider provider, String email, Instant lastLoginAt) {

    public static SocialAccountInfo of(SocialAccount account) {
        return new SocialAccountInfo(account.getProvider(), account.getProviderEmail(), account.getLastLoginAt());
    }
}
