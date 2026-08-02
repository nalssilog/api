package com.nalssilog.member.api.dto;

import com.nalssilog.member.domain.MemberRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull(message = "변경할 회원 권한이 필요합니다.")
        MemberRole role
) {
}
