package com.nalssilog.member.api.dto;

import com.nalssilog.member.domain.Member;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangeNicknameRequest(
        @NotBlank
        @Size(min = Member.NICKNAME_MIN_LENGTH, max = Member.NICKNAME_MAX_LENGTH,
                message = "닉네임은 2~10자여야 합니다.")
        @Pattern(regexp = Member.NICKNAME_PATTERN, message = "닉네임은 한글·영문·숫자만 쓸 수 있습니다.")
        String nickname
) {
}
