package com.nalssilog.auth.api.dto;

import com.nalssilog.member.application.dto.TermsAgreement;
import com.nalssilog.member.domain.Member;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 회원가입 입력. OAuth 정보(provider/이메일 등)는 백엔드 signup 티켓에 있으므로 프론트가 보내지 않는다.
 * agreedTerms 는 동의한 약관(종류+버전) 목록이며 필수 약관 누락은 서버가 검증한다.
 */
public record SignupRequest(
        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(max = 30, message = "이름은 30자 이하여야 합니다.")
        String name,

        @NotBlank(message = "닉네임을 입력해 주세요.")
        @Size(min = Member.NICKNAME_MIN_LENGTH, max = Member.NICKNAME_MAX_LENGTH,
                message = "닉네임은 2~10자여야 합니다.")
        @Pattern(regexp = Member.NICKNAME_PATTERN, message = "닉네임은 한글·영문·숫자만 쓸 수 있습니다.")
        String nickname,

        @NotNull(message = "약관 동의 정보가 필요합니다.")
        List<TermsAgreement> agreedTerms
) {
}
