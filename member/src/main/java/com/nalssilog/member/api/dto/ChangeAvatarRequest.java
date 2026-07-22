package com.nalssilog.member.api.dto;

import com.nalssilog.member.domain.AvatarType;
import jakarta.validation.constraints.NotNull;

/**
 * value 는 PRESET(프리셋 id)·CUSTOM(storage key)일 때 필요하고 DEFAULT 면 무시된다.
 */
public record ChangeAvatarRequest(
        @NotNull
        AvatarType type,
        String value
) {
}
