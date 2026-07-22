package com.nalssilog.member.domain;

import java.util.Arrays;
import java.util.List;

/**
 * 약관 종류와 필수 여부. 약관 문구/버전은 프론트가 관리하고, 백엔드는 어떤 종류가 필수인지만 권위를 갖는다.
 */
public enum TermsType {
    SERVICE(true),      // 이용약관
    PRIVACY(true),      // 개인정보 수집·이용 동의
    LOCATION(false),    // 위치기반 서비스 이용약관(선택 — 프론트에 별도 약관 화면 없음, 추후 필수화 가능)
    MARKETING(false);   // 마케팅 정보 수신 동의(선택)

    private final boolean required;

    TermsType(boolean required) {
        this.required = required;
    }

    public boolean isRequired() {
        return required;
    }

    public static List<TermsType> requiredTypes() {
        return Arrays.stream(values())
                .filter(TermsType::isRequired)
                .toList();
    }
}
