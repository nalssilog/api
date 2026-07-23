package com.nalssilog.auth.api.dto;

import com.nalssilog.member.application.dto.TermsAgreement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 회원가입 입력. OAuth 정보와 이름은 백엔드 signup 티켓에 있고, 닉네임은 가입 시 자동 생성한다.
 * agreedTerms 는 동의한 약관(종류+버전) 목록이며 필수 약관 누락은 서버가 검증한다.
 */
public record SignupRequest(
        @NotNull(message = "약관 동의 정보가 필요합니다.")
        List<@Valid @NotNull(message = "약관 동의 항목이 비어 있습니다.") TermsAgreement> agreedTerms
) {
}
