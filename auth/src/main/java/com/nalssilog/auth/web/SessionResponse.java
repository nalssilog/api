package com.nalssilog.auth.web;

import com.nalssilog.auth.token.SessionView;
import java.time.Instant;

/**
 * 로그인된 기기(세션) 응답. sessionId 로 특정 기기를 로그아웃(DELETE /api/auth/sessions/{sessionId})한다.
 * current 는 지금 보고 있는 바로 그 기기.
 */
public record SessionResponse(
        String sessionId,
        String deviceName,
        String ip,
        Instant loginAt,
        Instant lastActiveAt,
        boolean current
) {

    public static SessionResponse from(SessionView view) {

        return new SessionResponse(
                view.sessionId(),
                view.deviceName(),
                view.ip(),
                view.loginAt(),
                view.lastActiveAt(),
                view.current());
    }
}
