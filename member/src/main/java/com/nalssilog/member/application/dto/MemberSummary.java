package com.nalssilog.member.application.dto;

import com.nalssilog.member.domain.AvatarType;
import com.nalssilog.member.domain.Member;
import com.nalssilog.member.domain.MemberStatus;

/**
 * 다른 모듈의 목록 조회에서 사용하는 회원 표시 정보.
 * 소셜 계정 컬렉션을 조립하지 않아 여러 회원을 한 번의 쿼리로 조회할 수 있다.
 */
public record MemberSummary(
        Long id,
        String nickname,
        AvatarType avatarType,
        String avatarValue,
        MemberStatus status
) {

    public static MemberSummary of(Member member) {
        return new MemberSummary(
                member.getId(),
                member.getNickname(),
                member.getAvatarType(),
                member.getAvatarValue(),
                member.getStatus());
    }
}
