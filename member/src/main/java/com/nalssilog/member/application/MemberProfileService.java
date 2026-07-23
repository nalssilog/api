package com.nalssilog.member.application;

import com.nalssilog.common.exception.NalssiLogException;
import com.nalssilog.member.application.dto.AvatarPresign;
import com.nalssilog.member.application.dto.MemberInfo;
import com.nalssilog.member.application.dto.SocialAccountInfo;
import com.nalssilog.member.client.AvatarStorageClient;
import com.nalssilog.member.domain.AvatarType;
import com.nalssilog.member.domain.Member;
import com.nalssilog.member.domain.MemberErrorCode;
import com.nalssilog.member.domain.MemberStatus;
import com.nalssilog.member.domain.Provider;
import com.nalssilog.member.domain.SocialAccount;
import com.nalssilog.member.repository.MemberRepository;
import com.nalssilog.member.repository.SocialAccountRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 로그인 회원의 프로필 자기관리(이름·닉네임·아바타·소셜 연동 조회/해제). 로그인/가입/연동은 {@link MemberAccountService}. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberProfileService {

    private final MemberRepository memberRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final AvatarStorageClient avatarStorageClient;

    public boolean isNicknameAvailable(String nickname) {
        return !memberRepository.existsByNickname(nickname.strip());
    }

    @Transactional
    public MemberInfo changeName(Long memberId, String name) {
        Member member = memberRepository.getMember(memberId);
        member.changeName(name.strip());

        return memberRepository.getMemberInfo(memberId);
    }

    @Transactional
    public MemberInfo changeNickname(Long memberId, String nickname) {
        Member member = memberRepository.getMember(memberId);
        String trimmed = nickname.strip();

        if (memberRepository.existsByNicknameExcept(trimmed, memberId)) {
            throw new NalssiLogException(MemberErrorCode.DUPLICATE_NICKNAME);
        }

        member.changeNickname(trimmed);

        return memberRepository.getMemberInfo(memberId);
    }

    /** 커스텀 아바타 업로드용 presign 발급. key 에 memberId 를 박아 변경 시 본인 발급 여부 검증. */
    public AvatarPresign presignAvatar(Long memberId, String contentType, long size) {
        return avatarStorageClient.presign(memberId, contentType, size);
    }

    @Transactional
    public MemberInfo changeAvatar(Long memberId, AvatarType avatarType, String avatarValue) {
        Member member = memberRepository.getMember(memberId);

        member.changeAvatar(avatarType, resolveAvatarValue(memberId, avatarType, avatarValue));

        return memberRepository.getMemberInfo(memberId);
    }

    // CUSTOM=본인 발급 key 검증 + 업로드 HEAD 검증 후 완전 URL 로 저장, PRESET=프리셋 id 그대로, DEFAULT=엔티티에서 null.
    private String resolveAvatarValue(Long memberId, AvatarType avatarType, String avatarValue) {
        if (avatarType == AvatarType.CUSTOM) {
            avatarStorageClient.validateKey(memberId, avatarValue);
            avatarStorageClient.verifyUploaded(avatarValue);

            return avatarStorageClient.toPublicUrl(avatarValue);
        }

        return avatarValue;
    }

    public MemberInfo getMe(Long memberId) {
        return memberRepository.getMemberInfo(memberId);
    }

    public MemberInfo getPublicProfile(Long memberId) {
        return memberRepository.findMemberInfo(memberId)
                .filter(member -> member.status() == MemberStatus.ACTIVE)
                .orElseThrow(() -> new NalssiLogException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    public List<SocialAccountInfo> getSocialAccounts(Long memberId) {
        return socialAccountRepository.findInfosByMemberId(memberId);
    }

    @Transactional
    public void unlinkSocial(Long memberId, Provider provider) {
        SocialAccount account = socialAccountRepository.findByMemberIdAndProvider(memberId, provider)
                .orElseThrow(() -> new NalssiLogException(MemberErrorCode.SOCIAL_ACCOUNT_NOT_FOUND));

        if (socialAccountRepository.countByMemberId(memberId) <= 1) {
            throw new NalssiLogException(MemberErrorCode.LAST_SOCIAL_ACCOUNT);
        }

        socialAccountRepository.delete(account);
    }
}
