package com.nalssilog.report.client;

import com.nalssilog.member.application.MemberAccountService;
import com.nalssilog.member.domain.MemberStatus;
import com.nalssilog.report.application.dto.AuthorInfo;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * report 모듈이 member 를 호출하는 창구. MSA 분리 시 HTTP 클라이언트로 교체.
 * 탈퇴 회원은 익명화 정책상 작성자 정보를 노출하지 않는다(ACTIVE 만 반환).
 */
@Component("reportMemberClient")
@RequiredArgsConstructor
public class MemberClient {

    private final MemberAccountService memberAccountService;

    public Optional<AuthorInfo> findActiveAuthor(Long memberId) {
        return memberAccountService.findMemberInfo(memberId)
                .filter(member -> member.status() == MemberStatus.ACTIVE)
                .map(member -> new AuthorInfo(
                        member.id(), member.nickname(), member.avatarType(), member.avatarValue()));
    }
}
