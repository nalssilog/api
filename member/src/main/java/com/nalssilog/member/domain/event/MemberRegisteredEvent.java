package com.nalssilog.member.domain.event;

import com.nalssilog.member.domain.Provider;
import java.time.Instant;

/**
 * 최초 소셜 로그인으로 가입 대기(PENDING) 회원이 생성됐을 때 발행되는 도메인 이벤트.
 *
 * <p>지금은 Spring {@code ApplicationEventPublisher} 로 모듈 내부 발행/구독만 한다.
 * MSA 분리 시 이 이벤트를 메시지 브로커로 릴레이(outbox 등)해 다른 서비스가 구독하도록 확장한다.
 * 구독자는 {@code @TransactionalEventListener(phase = AFTER_COMMIT)} 로 커밋 이후에만 반응한다.
 */
public record MemberRegisteredEvent(Long memberId, Provider provider, Instant occurredAt) {

    public static MemberRegisteredEvent of(Long memberId, Provider provider) {
        return new MemberRegisteredEvent(memberId, provider, Instant.now());
    }
}
