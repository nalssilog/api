package com.nalssilog.member.api.dto;

import com.nalssilog.member.domain.Member;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeNameRequest(
        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(max = Member.NAME_MAX_LENGTH, message = "이름은 30자 이하여야 합니다.")
        String name
) {

    public ChangeNameRequest {
        if (name != null) {
            name = name.strip();
        }
    }
}
