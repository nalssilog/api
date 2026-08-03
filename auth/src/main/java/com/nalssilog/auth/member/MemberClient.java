package com.nalssilog.auth.member;

import com.nalssilog.auth.oauth.OAuthUserInfo;
import com.nalssilog.member.application.MemberAccountService;
import com.nalssilog.member.application.MemberRegistrationService;
import com.nalssilog.member.application.dto.MemberInfo;
import com.nalssilog.member.application.dto.SocialLoginResult;
import com.nalssilog.member.application.dto.TermsAgreement;
import com.nalssilog.member.domain.Provider;
import com.nalssilog.member.domain.MemberRole;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * member 모듈 호출 창구. 단일 프로세스 동안은 내부 호출이고, MSA 분리 시 HTTP 클라이언트로 교체한다.
 */
@Component
@RequiredArgsConstructor
public class MemberClient {

    private final MemberAccountService memberAccountService;
    private final MemberRegistrationService memberRegistrationService;

    public SocialLoginResult resolveSocialLogin(OAuthUserInfo userInfo) {
        return memberAccountService.resolveSocialLogin(
                userInfo.provider(), userInfo.providerUserId(), userInfo.email());
    }

    public MemberInfo registerMember(OAuthUserInfo userInfo, List<TermsAgreement> agreedTerms) {
        return memberRegistrationService.registerMember(
                userInfo.provider(), userInfo.providerUserId(), userInfo.email(), userInfo.socialName(), agreedTerms);
    }

    public MemberInfo linkSocial(Long targetMemberId, OAuthUserInfo userInfo) {
        return memberAccountService.linkSocial(
                targetMemberId, userInfo.provider(), userInfo.providerUserId(), userInfo.email());
    }

    public MemberInfo getMemberInfo(Long memberId) {
        return memberAccountService.getMemberInfo(memberId);
    }

    public void recordLogin(Long memberId, Provider provider) {
        memberAccountService.recordLogin(memberId, provider);
    }

    public Optional<MemberInfo> findMemberInfo(Long memberId) {
        return memberAccountService.findMemberInfo(memberId);
    }

    public Optional<MemberRole> findRole(Long memberId) {
        return memberAccountService.findRole(memberId);
    }

    public Optional<MemberInfo> findMemberInfo(
            Provider provider,
            String providerUserId
    ) {
        return memberAccountService.findMemberInfo(provider, providerUserId);
    }

    public void withdraw(Long memberId) {
        memberAccountService.withdraw(memberId);
    }
}
