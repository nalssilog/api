package com.nalssilog.auth.client;

import com.nalssilog.member.application.MemberAccountService;
import com.nalssilog.member.application.dto.MemberInfo;
import com.nalssilog.member.application.dto.SocialLoginResult;
import com.nalssilog.member.application.dto.TermsAgreement;
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

    public SocialLoginResult resolveSocialLogin(OAuthUserInfo userInfo) {
        return memberAccountService.resolveSocialLogin(
                userInfo.provider(), userInfo.providerUserId(), userInfo.email());
    }

    public MemberInfo registerMember(OAuthUserInfo userInfo, String name, String nickname,
                                     List<TermsAgreement> agreedTerms) {
        return memberAccountService.registerMember(
                userInfo.provider(), userInfo.providerUserId(), userInfo.email(), name, nickname, agreedTerms);
    }

    public MemberInfo linkSocial(Long targetMemberId, OAuthUserInfo userInfo) {
        return memberAccountService.linkSocial(
                targetMemberId, userInfo.provider(), userInfo.providerUserId(), userInfo.email());
    }

    public MemberInfo getMemberInfo(Long memberId) {
        return memberAccountService.getMemberInfo(memberId);
    }

    public Optional<MemberInfo> findMemberInfo(Long memberId) {
        return memberAccountService.findMemberInfo(memberId);
    }

    public void withdraw(Long memberId) {
        memberAccountService.withdraw(memberId);
    }
}
