package com.nalssilog.member.api.dto;

import com.nalssilog.member.application.dto.MemberInfo;
import com.nalssilog.member.domain.AvatarType;

/**
 * 다른 사용자에게 공개되는 회원 프로필. 민감정보(email·연동 소셜·상태)는 노출하지 않는다.
 * id 는 TSID(대형 Long)라 문자열로 내려준다. avatar.value: PRESET=프리셋 id, CUSTOM=이미지 URL, DEFAULT=null.
 */
public record MemberPublicProfileResponse(
        String id,
        String nickname,
        Avatar avatar
) {

    public record Avatar(AvatarType type, String value) {
    }

    public static MemberPublicProfileResponse from(MemberInfo member) {
        return new MemberPublicProfileResponse(
                String.valueOf(member.id()),
                member.nickname(),
                new Avatar(member.avatarType(), member.avatarValue())
        );
    }
}
