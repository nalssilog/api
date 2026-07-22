package com.nalssilog.member.application.dto;

import com.nalssilog.member.domain.TermsType;

/**
 * 온보딩 시 프론트가 알려주는 약관 동의 한 건 (종류 + 동의한 문구 버전).
 */
public record TermsAgreement(TermsType type, String version) {
}
