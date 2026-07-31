package com.nalssilog.auth.token;

import java.time.Instant;

/**
 * 로그인된 기기(세션) 표시 정보. current 는 지금 요청을 보낸 바로 그 기기인지 여부.
 */
public record SessionView(
        String sessionId,
        String deviceName,
        String ip,
        Instant loginAt,
        Instant lastActiveAt,
        boolean current
) {
}
