package com.nalssilog.member.application.dto;

import com.nalssilog.member.domain.MemberStatus;
import com.nalssilog.member.domain.Provider;
import java.util.List;

/**
 * 소셜 인증 결과를 세 갈래로 표현하는 계약. (회원을 즉시 생성/병합하지 않고 분기만 한다)
 * <ul>
 *     <li>{@code EXISTING} — (provider, providerUserId) 로 이미 가입된 회원 → 바로 로그인</li>
 *     <li>{@code NEW} — 미가입 + 이메일 매칭 없음 → 신규 가입 대상</li>
 *     <li>{@code LINK_REQUIRED} — 미가입 + 이메일이 기존 회원과 매칭 → 계정 연동 필요(1사용자 1계정)</li>
 * </ul>
 */
public record SocialLoginResult(
        Outcome outcome,
        Long memberId,
        MemberStatus status,
        String email,
        List<Provider> existingProviders
) {

    public enum Outcome {
        EXISTING,
        NEW,
        LINK_REQUIRED
    }

    public static SocialLoginResult existing(Long memberId, MemberStatus status) {
        return new SocialLoginResult(Outcome.EXISTING, memberId, status, null, List.of());
    }

    public static SocialLoginResult newMember(String email) {
        return new SocialLoginResult(Outcome.NEW, null, null, email, List.of());
    }

    public static SocialLoginResult linkRequired(Long targetMemberId, String email, List<Provider> existingProviders) {
        return new SocialLoginResult(Outcome.LINK_REQUIRED, targetMemberId, null, email, existingProviders);
    }
}
