package com.nalssilog.member.api.dto;

import com.nalssilog.member.application.dto.MemberInfo;
import com.nalssilog.member.domain.MemberRole;

public record AdminMemberRoleResponse(String memberId, MemberRole role) {

    public static AdminMemberRoleResponse from(MemberInfo member) {
        return new AdminMemberRoleResponse(String.valueOf(member.id()), member.role());
    }
}
