package com.nalssilog.member.api.dto;

import com.nalssilog.member.application.dto.MemberInfo;
import com.nalssilog.member.domain.AvatarType;
import com.nalssilog.member.domain.Provider;
import java.util.List;

/**
 * 본인 계정 정보(설정 화면용). 공개 프로필과 달리 name·email·연동 소셜까지 포함한다.
 * id 는 TSID(대형 Long)라 JS 안전 정수 초과 방지를 위해 문자열로 내려준다.
 * avatar.value 는 PRESET 이면 프리셋 id(avatar-0X, 프론트가 번들 이미지로 렌더), CUSTOM 이면 이미지 URL, DEFAULT 면 null.
 */
public record MemberMeResponse(
        String id,
        String name,
        String nickname,
        String email,
        Avatar avatar,
        List<Provider> connectedProviders,
        Provider currentProvider
) {

    public record Avatar(AvatarType type, String value) {
    }

    public static MemberMeResponse from(MemberInfo member) {
        return new MemberMeResponse(
                String.valueOf(member.id()),
                member.name(),
                member.nickname(),
                member.email(),
                new Avatar(member.avatarType(), member.avatarValue()),
                member.connectedProviders(),
                member.currentProvider()
        );
    }
}
