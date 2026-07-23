package com.nalssilog.member.application.dto;

import com.nalssilog.member.domain.TermsType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 온보딩 시 프론트가 알려주는 약관 동의 한 건 (종류 + 동의한 문구 버전).
 */
public record TermsAgreement(
        @NotNull(message = "약관 종류가 필요합니다.")
        TermsType type,

        @NotBlank(message = "약관 버전이 필요합니다.")
        @Size(max = 20, message = "약관 버전은 20자 이하여야 합니다.")
        String version
) {
}
