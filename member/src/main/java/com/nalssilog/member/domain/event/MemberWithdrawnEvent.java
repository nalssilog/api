package com.nalssilog.member.domain.event;

import java.time.Instant;

/**
 * 회원 탈퇴가 확정됐을 때 발행되는 도메인 이벤트. 구독자(report)는 해당 회원의 제보를 익명화한다.
 *
 * <p>지금은 Spring {@code ApplicationEventPublisher} 로 모듈 내부 발행/구독만 한다.
 * MSA 분리 시 브로커로 릴레이한다. 구독은 {@code @TransactionalEventListener(phase = AFTER_COMMIT)}.
 */
public record MemberWithdrawnEvent(Long memberId, Instant occurredAt) {

    public static MemberWithdrawnEvent of(Long memberId) {
        return new MemberWithdrawnEvent(memberId, Instant.now());
    }
}
