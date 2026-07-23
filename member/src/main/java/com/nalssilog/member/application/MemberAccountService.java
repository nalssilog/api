package com.nalssilog.member.application;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.member.application.dto.MemberInfo;
import com.nalssilog.member.application.dto.SocialLoginResult;
import com.nalssilog.member.application.dto.TermsAgreement;
import com.nalssilog.member.domain.Member;
import com.nalssilog.member.domain.MemberErrorCode;
import com.nalssilog.member.domain.Provider;
import com.nalssilog.member.domain.SocialAccount;
import com.nalssilog.member.domain.event.MemberRegisteredEvent;
import com.nalssilog.member.domain.event.MemberWithdrawnEvent;
import com.nalssilog.member.repository.MemberRepository;
import com.nalssilog.member.repository.SocialAccountRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberAccountService {

    private final MemberRepository memberRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final ConsentService consentService;
    private final ApplicationEventPublisher eventPublisher;

    /** 소셜 인증 결과 분기(생성·병합 안 함). 가입된 소셜=EXISTING, 이메일로 기존 회원 있으면 LINK_REQUIRED, 없으면 NEW. */
    @Transactional
    public SocialLoginResult resolveSocialLogin(Provider provider, String providerUserId, String email) {
        Optional<SocialAccount> linked = socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId);

        if (linked.isPresent()) {
            SocialAccount account = linked.get();
            account.touchLogin();

            return SocialLoginResult.existing(account.getMember().getId(), account.getMember().getStatus());
        }

        if (email == null || email.isBlank()) {
            return SocialLoginResult.newMember(email);
        }

        return memberRepository.findMemberInfoByEmail(email)
                .map(found -> SocialLoginResult.linkRequired(found.id(), email, found.connectedProviders()))
                .orElseGet(() -> SocialLoginResult.newMember(email));
    }

    /** 가입 확정. ACTIVE 회원 + 소셜 계정 + 약관 동의를 한 번에 생성. provider·email 은 signup 티켓에서 온다. */
    @Transactional
    public MemberInfo registerMember(Provider provider, String providerUserId, String email,
                                     String name, String nickname, List<TermsAgreement> agreedTerms) {
        String trimmed = nickname.strip();

        if (memberRepository.existsByNickname(trimmed)) {
            throw new NalssiLogException(MemberErrorCode.DUPLICATE_NICKNAME);
        }

        Member member = memberRepository.save(Member.register(email, name, trimmed));
        socialAccountRepository.save(SocialAccount.link(member, provider, providerUserId, email));
        consentService.recordOnboardingConsents(member.getId(), agreedTerms);

        eventPublisher.publishEvent(MemberRegisteredEvent.of(member.getId(), provider));

        return memberRepository.getMemberInfo(member.getId());
    }

    /** 기존 회원에 새 소셜 계정 연동(호출 전 재인증으로 소유권 증명 전제). */
    @Transactional
    public MemberInfo linkSocial(Long targetMemberId, Provider provider, String providerUserId, String email) {
        if (socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId).isPresent()) {
            throw new NalssiLogException(MemberErrorCode.SOCIAL_ACCOUNT_IN_USE);
        }

        Member member = memberRepository.getMember(targetMemberId);
        socialAccountRepository.save(SocialAccount.link(member, provider, providerUserId, email));

        return memberRepository.getMemberInfo(targetMemberId);
    }

    /** 회원 탈퇴. 익명화 + 소셜 삭제 + 제보 익명화용 MemberWithdrawnEvent 발행. 세션·쿠키 정리는 auth. */
    @Transactional
    public void withdraw(Long memberId) {
        Member member = memberRepository.getMember(memberId);

        member.withdraw();
        socialAccountRepository.deleteAllByMemberId(memberId);

        eventPublisher.publishEvent(MemberWithdrawnEvent.of(memberId));
    }

    public MemberInfo getMemberInfo(Long memberId) {
        return memberRepository.getMemberInfo(memberId);
    }

    public Optional<MemberInfo> findMemberInfo(Long memberId) {
        return memberRepository.findMemberInfo(memberId);
    }
}
