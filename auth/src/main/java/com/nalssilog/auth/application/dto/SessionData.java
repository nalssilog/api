package com.nalssilog.auth.application.dto;

import com.nalssilog.member.domain.Provider;
import java.time.Instant;

/**
 * refresh 세션 1건의 저장 메타데이터(내부 계약). tokenHash 는 Redis 키이자 '현재 세션' 판별용.
 * sessionId 는 프론트에 노출되는 불투명 식별자(기기별 로그아웃 대상 지정용). rotation 이 일어나도 유지된다.
 */
public record SessionData(
        String tokenHash,
        String sessionId,
        Long memberId,
        Provider provider,
        String deviceName,
        String ip,
        Instant loginAt,
        Instant lastActiveAt
) {
}
