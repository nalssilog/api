package com.nalssilog.member.application.dto;

import com.nalssilog.member.domain.AvatarType;
import com.nalssilog.member.domain.Member;
import com.nalssilog.member.domain.MemberStatus;
import com.nalssilog.member.domain.Provider;
import com.nalssilog.member.domain.SocialAccount;
import java.util.Comparator;
import java.util.List;

/**
 * member 모듈이 외부(다른 모듈)에 제공하는 회원 정보 계약.
 * lastLoginProvider 는 계정 전체의 최근 로그인 이력이며, 현재 요청의 인증 수단은 토큰에서 별도로 결정한다.
 */
public record MemberInfo(
        Long id,
        String nickname,
        String name,
        String email,
        AvatarType avatarType,
        String avatarValue,
        MemberStatus status,
        Provider lastLoginProvider,
        List<Provider> connectedProviders
) {

    public static MemberInfo of(Member member, List<SocialAccount> accounts) {
        Provider lastLoginProvider = accounts.stream()
                .filter(account -> account.getLastLoginAt() != null)
                .max(Comparator.comparing(SocialAccount::getLastLoginAt))
                .map(SocialAccount::getProvider)
                .orElse(null);
        List<Provider> connectedProviders = accounts.stream()
                .map(SocialAccount::getProvider)
                .distinct()
                .toList();

        return new MemberInfo(
                member.getId(),
                member.getNickname(),
                member.getName(),
                member.getEmail(),
                member.getAvatarType(),
                member.getAvatarValue(),
                member.getStatus(),
                lastLoginProvider,
                connectedProviders
        );
    }
}
