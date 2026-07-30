package com.nalssilog.auth.ticket;

import com.nalssilog.member.domain.Provider;
import java.util.List;

/**
 * 다른 소셜로 처음 로그인한 기존 회원이 연동을 확정하기 전의 임시 상태. (Redis 단기 보관)
 * {@code targetMemberId} 는 이메일로 찾은 연동 대상 회원이며, 기존 로그인 수단 재인증으로 소유권이 증명돼야 연동된다.
 * {@code provider} 는 이번에 시도한 소셜, {@code existingProviders} 는 대상 회원이 이미 연동해 둔 소셜(프론트 재인증 안내용).
 */
public record LinkTicket(
        Provider provider,
        String providerUserId,
        String email,
        Long targetMemberId,
        List<Provider> existingProviders,
        AuthChannel channel
) {

    public LinkTicket(
            Provider provider,
            String providerUserId,
            String email,
            Long targetMemberId,
            List<Provider> existingProviders
    ) {
        this(provider, providerUserId, email, targetMemberId, existingProviders, AuthChannel.WEB);
    }

    public AuthChannel effectiveChannel() {

        return channel == null ? AuthChannel.WEB : channel;
    }
}
