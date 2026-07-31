package com.nalssilog.auth.token;

import com.nalssilog.auth.config.AuthProperties;
import com.nalssilog.auth.core.AuthErrorCode;
import com.nalssilog.common.exception.NalssiLogException;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 로그인된 기기(세션) 관리. 목록 조회 + 특정 기기 로그아웃.
 * currentTokenHash 는 현재 요청의 refresh 쿠키에서 파생한 세션 키로, '이 기기' 표시/판별에 쓴다.
 */
@Service
@RequiredArgsConstructor
public class AuthSessionService {

    private final RefreshTokenStore refreshTokenStore;
    private final AuthProperties properties;

    public List<SessionView> listSessions(Long memberId, String currentSessionId) {
        return refreshTokenStore.findSessionsByMember(memberId).stream()
                .sorted(Comparator.comparing(SessionData::lastActiveAt).reversed())
                .map(session -> new SessionView(
                        session.sessionId(),
                        session.deviceName(),
                        session.ip(),
                        session.loginAt(),
                        session.lastActiveAt(),
                        session.sessionId().equals(currentSessionId)))
                .toList();
    }

    /**
     * 특정 세션(기기) 로그아웃. 대상이 현재 세션이면 true 를 반환해 컨트롤러가 쿠키까지 정리하게 한다.
     */
    public boolean revokeSession(Long memberId, String sessionId, String currentSessionId) {
        List<SessionData> sessions = refreshTokenStore.findSessionsByMember(memberId);
        SessionData target = sessions.stream()
                .filter(session -> session.sessionId().equals(sessionId))
                .findFirst()
                .orElseThrow(() -> new NalssiLogException(AuthErrorCode.SESSION_NOT_FOUND));

        boolean current = currentSessionId != null && sessionId.equals(currentSessionId);

        refreshTokenStore.revokeSession(memberId, target.sessionId(), properties.jwt().refreshTokenTtl());

        return current;
    }
}
